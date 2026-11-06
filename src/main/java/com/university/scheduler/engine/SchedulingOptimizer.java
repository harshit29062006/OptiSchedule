package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import com.university.scheduler.util.FacultyLoadComparator;

import java.util.*;

/**
 * Greedy scheduling optimizer with multi-pass retry, capacity-aware room
 * selection, load-balanced faculty selection, and boundary validation.
 *
 * Input rules (Day 6), checked before any placement:
 *   - pending, its elements, and the checker/slots/random must not be null
 *   - the same section+course may not appear twice in one run (IllegalArgumentException)
 *
 * Faculty (Day 5): eligible teachers are tried least-loaded first.
 * Rooms (Day 3): rooms too small for the expected strength are removed,
 *   and the rest are tried tightest fit first.
 * Slots (Day 2): pass 1 uses the given order; later passes shuffle it, and
 *   the pass placing the most assignments is kept and committed.
 *
 * This is a heuristic with no backtracking.
 */
public class SchedulingOptimizer {
    private final ConflictChecker baseChecker;
    private final List<TimeSlot> allSlots;
    private final int maxPasses;
    private final Random random;

    public SchedulingOptimizer(ConflictChecker checker, List<TimeSlot> allSlots) {
        this(checker, allSlots, 1, new Random(42));
    }

    public SchedulingOptimizer(ConflictChecker checker, List<TimeSlot> allSlots, int maxPasses, Random random) {
        this.baseChecker = Objects.requireNonNull(checker, "checker cannot be null");
        this.allSlots = Objects.requireNonNull(allSlots, "allSlots cannot be null");
        this.random = Objects.requireNonNull(random, "random cannot be null");
        if (maxPasses < 1) {
            throw new IllegalArgumentException("maxPasses must be at least 1, was " + maxPasses);
        }
        this.maxPasses = maxPasses;
    }

    /** Result of a multi-pass run, with both outcomes visible. */
    public static final class PassResult {
        public final int passNumber;
        public final List<Booking> scheduled;
        public final List<PendingAssignment> unscheduled;

        PassResult(int passNumber, List<Booking> scheduled, List<PendingAssignment> unscheduled) {
            this.passNumber = passNumber;
            this.scheduled = scheduled;
            this.unscheduled = unscheduled;
        }
    }

    /**
     * Runs up to maxPasses passes, keeps the best one, and commits its
     * bookings to the base checker. Returns the details of the winning pass.
     *
     * @throws NullPointerException     if pending or any element is null
     * @throws IllegalArgumentException if the same section+course appears twice
     */
    public PassResult scheduleAll(List<PendingAssignment> pending) {
        validate(pending);

        PassResult best = null;
        for (int pass = 1; pass <= maxPasses; pass++) {
            List<TimeSlot> slotOrder = new ArrayList<>(allSlots);
            if (pass > 1) {
                Collections.shuffle(slotOrder, random);
            }
            PassResult result = runSinglePass(pass, pending, slotOrder);

            if (best == null || result.scheduled.size() > best.scheduled.size()) {
                best = result;
            }
            if (result.unscheduled.isEmpty()) {
                break;
            }
        }

        commit(best);
        return best;
    }

    /**
     * Compatibility API used by SchedulingService and the original tests.
     * Returns the placed bookings and appends any failures to unscheduled.
     */
    public List<Booking> scheduleAll(List<PendingAssignment> pending, List<PendingAssignment> unscheduled) {
        Objects.requireNonNull(unscheduled, "unscheduled cannot be null");
        PassResult result = scheduleAll(pending);
        unscheduled.addAll(result.unscheduled);
        return result.scheduled;
    }

    /** Rejects bad input before anything is placed, so a failure never leaves a half-committed schedule. */
    static void validate(List<PendingAssignment> pending) {
        Objects.requireNonNull(pending, "pending cannot be null");
        Set<String> seen = new HashSet<>();
        for (PendingAssignment pa : pending) {
            Objects.requireNonNull(pa, "pending list contains a null assignment");
            String key = pa.getSection().getSectionCode() + "|" + pa.getCourse().getCourseCode();
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                    "duplicate pending assignment for section " + pa.getSection().getSectionCode()
                    + " course " + pa.getCourse().getCourseCode()
                    + ": one section cannot be scheduled twice for the same course in one run");
            }
        }
    }

    private PassResult runSinglePass(int passNumber, List<PendingAssignment> pending, List<TimeSlot> slotOrder) {
        ConflictChecker trial = copyOf(baseChecker);

        List<PendingAssignment> sorted = new ArrayList<>(pending);
        sorted.sort(Comparator.comparingInt(pa -> countAvailableOptions(trial, pa, slotOrder)));

        List<Booking> scheduled = new ArrayList<>();
        List<PendingAssignment> unscheduled = new ArrayList<>();

        for (PendingAssignment pa : sorted) {
            Optional<Booking> placed = tryPlace(trial, pa, slotOrder);
            if (placed.isPresent()) {
                scheduled.add(placed.get());
            } else {
                unscheduled.add(pa);
            }
        }
        return new PassResult(passNumber, scheduled, unscheduled);
    }

    private void commit(PassResult best) {
        for (Booking b : best.scheduled) {
            try {
                baseChecker.addBooking(b);
            } catch (SchedulingException e) {
                throw new IllegalStateException("Committing winning pass failed unexpectedly", e);
            }
        }
    }

    /**
     * Candidate rooms for one assignment, in the order they should be tried.
     * Rooms too small for the expected strength are removed. If strength is
     * known, the rest are sorted by wasted seats, tightest fit first.
     */
    static List<Room> orderedRooms(PendingAssignment pa) {
        List<Room> rooms = new ArrayList<>(pa.getCandidateRooms());
        int strength = pa.getExpectedStrength();
        if (strength <= 0) {
            return rooms;
        }
        rooms.removeIf(r -> r.getCapacity() < strength);
        rooms.sort(Comparator.comparingInt(r -> r.getCapacity() - strength));
        return rooms;
    }

    /** Eligible teachers, least loaded first. Ties keep the preferred faculty first (stable sort). */
    static List<Faculty> balancedFaculty(ConflictChecker checker, PendingAssignment pa) {
        List<Faculty> pool = new ArrayList<>(pa.getEligibleFaculty());
        pool.sort(new FacultyLoadComparator(checker.getAllFacultyLoads()));
        return pool;
    }

    /** Faculty is the outer loop, so the least-loaded eligible teacher is tried first. */
    private Optional<Booking> tryPlace(ConflictChecker checker, PendingAssignment pa, List<TimeSlot> slotOrder) {
        List<Room> rooms = orderedRooms(pa);
        for (Faculty f : balancedFaculty(checker, pa)) {
            for (Room room : rooms) {
                Optional<TimeSlot> slot = checker.findAvailableSlot(f, room, slotOrder);
                if (slot.isPresent()) {
                    Booking booking = new Booking(pa.getSection(), pa.getCourse(), f, room, slot.get());
                    try {
                        checker.addBooking(booking);
                        return Optional.of(booking);
                    } catch (SchedulingException e) {
                        continue;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Number of valid (faculty, room, slot) options, used to order assignments most-constrained first. */
    private int countAvailableOptions(ConflictChecker checker, PendingAssignment pa, List<TimeSlot> slotOrder) {
        int count = 0;
        for (Faculty f : pa.getEligibleFaculty()) {
            boolean wouldOverload = checker.getFacultyLoad(f) + 1 > f.getMaxHoursPerWeek();
            if (wouldOverload) {
                continue;
            }
            for (Room room : orderedRooms(pa)) {
                for (TimeSlot slot : slotOrder) {
                    List<Booking> atSlot = checker.getBookingsAt(slot);
                    boolean roomBusyByOther = atSlot.stream().anyMatch(b ->
                        b.getRoom().equals(room) && !b.getFaculty().equals(f));
                    boolean facultyBusyElsewhere = atSlot.stream().anyMatch(b ->
                        b.getFaculty().equals(f) && !b.getRoom().equals(room));
                    if (!roomBusyByOther && !facultyBusyElsewhere) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    static ConflictChecker copyOf(ConflictChecker source) {
        ConflictChecker copy = new ConflictChecker();
        for (Booking b : source.getAllBookings()) {
            try {
                copy.addBooking(b);
            } catch (SchedulingException e) {
                throw new IllegalStateException("Source schedule was internally inconsistent", e);
            }
        }
        return copy;
    }
}
