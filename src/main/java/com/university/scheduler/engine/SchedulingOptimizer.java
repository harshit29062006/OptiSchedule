package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;

import java.util.*;

/** Greedy, capacity-aware optimizer with seeded slot-order retries. */
public class SchedulingOptimizer {
    private final ConflictChecker checker;
    private final List<TimeSlot> allSlots;
    private final int maxPasses;
    private final Random random;

    public SchedulingOptimizer(ConflictChecker checker, List<TimeSlot> allSlots) {
        this(checker, allSlots, 1, new Random(0));
    }

    public SchedulingOptimizer(ConflictChecker checker, List<TimeSlot> allSlots,
                               int maxPasses, Random random) {
        this.checker = Objects.requireNonNull(checker, "checker cannot be null");
        this.allSlots = List.copyOf(Objects.requireNonNull(allSlots, "allSlots cannot be null"));
        if (maxPasses < 1) throw new IllegalArgumentException("maxPasses must be positive");
        this.maxPasses = maxPasses;
        this.random = Objects.requireNonNull(random, "random cannot be null");
    }

    /** Compatibility entry point for callers that collect failures separately. */
    public List<Booking> scheduleAll(List<PendingAssignment> pending, List<PendingAssignment> unscheduled) {
        PassResult result = scheduleAll(pending);
        unscheduled.addAll(result.unscheduled);
        return result.scheduled;
    }

    /** Runs independent passes, then commits only the best pass to the base checker. */
    public PassResult scheduleAll(List<PendingAssignment> pending) {
        Objects.requireNonNull(pending, "pending cannot be null");
        List<PendingAssignment> requests = new ArrayList<>(pending);
        if (requests.stream().anyMatch(Objects::isNull)) throw new NullPointerException("pending contains null");
        rejectDuplicateRequests(requests);

        PassResult best = new PassResult(List.of(), List.copyOf(requests), 0);
        for (int pass = 1; pass <= maxPasses; pass++) {
            ConflictChecker trial = checker.copy();
            List<TimeSlot> slots = new ArrayList<>(allSlots);
            if (pass > 1) Collections.shuffle(slots, random);
            List<PendingAssignment> ordered = new ArrayList<>(requests);
            if (pass > 1) Collections.shuffle(ordered, random);
            ordered.sort(Comparator.comparingInt(pa -> countAvailableOptions(trial, pa, slots)));

            List<Booking> placed = new ArrayList<>();
            List<PendingAssignment> missed = new ArrayList<>();
            for (PendingAssignment pa : ordered) {
                Booking booking = tryPlace(trial, pa, slots);
                if (booking == null) missed.add(pa); else placed.add(booking);
            }
            if (placed.size() > best.scheduled.size()) best = new PassResult(placed, missed, pass);
        }

        for (Booking booking : best.scheduled) {
            for (Section section : booking.getSections()) {
                try {
                    checker.addBooking(new Booking(section, booking.getCourse(), booking.getFaculty(),
                        booking.getRoom(), booking.getTimeSlot()));
                } catch (SchedulingException e) {
                    throw new IllegalStateException("Winning optimizer pass could not be committed", e);
                }
            }
        }
        return best;
    }

    private static void rejectDuplicateRequests(List<PendingAssignment> pending) {
        Set<String> seen = new HashSet<>();
        for (PendingAssignment pa : pending) {
            String key = pa.getSection().getSectionCode() + "\u0000" + pa.getCourse().getCourseCode()
                + "\u0000" + pa.getCourse().getPeriodType();
            if (!seen.add(key)) {
                throw new IllegalArgumentException("Duplicate request for section " + pa.getSection().getSectionCode()
                    + " and course " + pa.getCourse().getCourseCode());
            }
        }
    }

    private Booking tryPlace(ConflictChecker state, PendingAssignment pa, List<TimeSlot> slots) {
        List<Faculty> faculty = new ArrayList<>(pa.getEligibleFaculty());
        faculty.sort(Comparator.comparingInt(state::getFacultyLoad));
        for (Faculty teacher : faculty) {
            for (Room room : orderedRooms(pa)) {
                for (TimeSlot slot : slots) {
                    if (state.getFacultyLoad(teacher) >= teacher.getMaxHoursPerWeek()) break;
                    List<Booking> atSlot = state.getBookingsAt(slot);
                    // Optimizer assignments must remain separate sessions; do not silently merge requests.
                    if (atSlot.stream().anyMatch(b -> b.getRoom().equals(room) || b.getFaculty().equals(teacher))) continue;
                    Booking booking = new Booking(pa.getSection(), pa.getCourse(), teacher, room, slot);
                    try {
                        state.addBooking(booking);
                        return booking;
                    } catch (SchedulingException ignored) {
                        // A constraint changed or was caught by the checker; try the next slot.
                    }
                }
            }
        }
        return null;
    }

    private int countAvailableOptions(ConflictChecker state, PendingAssignment pa, List<TimeSlot> slots) {
        int count = 0;
        for (Faculty teacher : pa.getEligibleFaculty()) {
            if (state.getFacultyLoad(teacher) >= teacher.getMaxHoursPerWeek()) continue;
            for (Room room : orderedRooms(pa)) {
                for (TimeSlot slot : slots) {
                    boolean occupied = state.getBookingsAt(slot).stream()
                        .anyMatch(b -> b.getRoom().equals(room) || b.getFaculty().equals(teacher));
                    if (!occupied) count++;
                }
            }
        }
        return count;
    }

    /** Capacity-filtered tightest fit; unknown class strength preserves the supplied room order. */
    public static List<Room> orderedRooms(PendingAssignment pa) {
        List<Room> rooms = new ArrayList<>(pa.getCandidateRooms());
        if (pa.getExpectedStrength() <= 0) return rooms;
        rooms.removeIf(room -> room.getCapacity() < pa.getExpectedStrength());
        rooms.sort(Comparator.comparingInt(room -> room.getCapacity() - pa.getExpectedStrength()));
        return rooms;
    }

    public static final class PassResult {
        public final List<Booking> scheduled;
        public final List<PendingAssignment> unscheduled;
        public final int passNumber;

        PassResult(List<Booking> scheduled, List<PendingAssignment> unscheduled, int passNumber) {
            this.scheduled = List.copyOf(scheduled);
            this.unscheduled = List.copyOf(unscheduled);
            this.passNumber = passNumber;
        }
    }
}
