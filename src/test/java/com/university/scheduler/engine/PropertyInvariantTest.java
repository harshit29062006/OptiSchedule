package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property tests (Week 4 Day 4).
 *
 * Each seed builds a random scenario and runs the optimizer. The result must
 * satisfy every invariant below, whatever the seed:
 *
 *   I1 accounting        scheduled + unscheduled = requested
 *   I2 commit integrity  committed bookings = scheduled bookings
 *   I3 room uniqueness   no room in two bookings at one slot
 *   I4 teacher uniqueness no teacher in two bookings at one slot
 *   I5 load exact        each teacher's load = bookings they hold (combined sessions count once)
 *   I6 load limit        no teacher above their weekly limit
 *   I7 request fidelity  each booking uses an eligible teacher, a candidate room,
 *                        a room large enough, and a slot from the week
 *   I8 section once      each section appears in at most one booking
 *
 * Invariants I3 and I4 are checked only against this scenario's own slots.
 * A section clash (I8 across slots) is not enforced by the engine. Here it
 * cannot occur because each generated section is unique.
 *
 * REGRESSION_SEEDS: any seed a sweep ever fails on goes here permanently.
 */
class PropertyInvariantTest {

    static final int SEED_COUNT = 200;

    /** Seeds that once failed. Empty until a sweep reports one. */
    static final long[] REGRESSION_SEEDS = {};

    static final class Scenario {
        final List<TimeSlot> slots;
        final List<PendingAssignment> pending;

        Scenario(List<TimeSlot> slots, List<PendingAssignment> pending) {
            this.slots = slots;
            this.pending = pending;
        }
    }

    static Scenario build(long seed) {
        Random rng = new Random(seed);

        int periods = 3 + rng.nextInt(4);
        List<TimeSlot> slots = TimeSlotGenerator.generateWeek(periods, LocalTime.of(8, 0), 50);

        int nRooms = 3 + rng.nextInt(4);
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < nRooms; i++) {
            rooms.add(new Room("R" + i, "R" + i, RoomType.CLASSROOM, 20 + rng.nextInt(100)));
        }

        int nFaculty = 3 + rng.nextInt(5);
        List<Faculty> faculty = new ArrayList<>();
        for (int i = 0; i < nFaculty; i++) {
            faculty.add(new Faculty("F" + i, null, null, 2 + rng.nextInt(5)));
        }

        List<Course> courses = List.of(
            new Course("C1", "Course 1", 3, PeriodType.LECTURE),
            new Course("C2", "Course 2", 3, PeriodType.TUTORIAL),
            new Course("C3", "Course 3", 3, PeriodType.PRACTICAL));

        int nPending = 10 + rng.nextInt(20);
        List<PendingAssignment> pending = new ArrayList<>();
        for (int i = 0; i < nPending; i++) {
            Section section = new Section("S" + i, "B", 1, null);
            Course course = courses.get(rng.nextInt(courses.size()));
            Faculty primary = faculty.get(rng.nextInt(nFaculty));

            List<Faculty> eligible = new ArrayList<>();
            eligible.add(primary);
            int extras = rng.nextInt(3);
            for (int e = 0; e < extras; e++) {
                eligible.add(faculty.get(rng.nextInt(nFaculty)));
            }

            List<Room> shuffled = new ArrayList<>(rooms);
            Collections.shuffle(shuffled, rng);
            int take = 1 + rng.nextInt(nRooms);
            List<Room> candidates = new ArrayList<>(shuffled.subList(0, take));

            int strength = rng.nextInt(4) == 0 ? 0 : rng.nextInt(130);
            pending.add(new PendingAssignment(section, course, primary, candidates, strength, eligible));
        }
        return new Scenario(slots, pending);
    }

    /** Runs the optimizer for one seed and returns every invariant violation found. */
    static List<String> violationsFor(long seed) {
        Scenario sc = build(seed);
        ConflictChecker checker = new ConflictChecker();
        SchedulingOptimizer.PassResult res =
            new SchedulingOptimizer(checker, sc.slots, 3, new Random(seed)).scheduleAll(sc.pending);

        List<String> v = new ArrayList<>();

        // I1 accounting
        if (res.scheduled.size() + res.unscheduled.size() != sc.pending.size()) {
            v.add("I1 accounting: " + res.scheduled.size() + "+" + res.unscheduled.size()
                + " != " + sc.pending.size());
        }
        // I2 commit integrity
        if (checker.totalBookings() != res.scheduled.size()) {
            v.add("I2 commit: committed " + checker.totalBookings() + " != scheduled " + res.scheduled.size());
        }

        // I3 and I4 per slot; also confirms every booking lies in this week
        int countedAcrossSlots = 0;
        for (TimeSlot slot : sc.slots) {
            List<Booking> at = checker.getBookingsAt(slot);
            countedAcrossSlots += at.size();
            Set<Room> rooms = new HashSet<>();
            Set<Faculty> teachers = new HashSet<>();
            for (Booking b : at) {
                if (!rooms.add(b.getRoom())) {
                    v.add("I3 room " + b.getRoom().getRoomId() + " double-booked at " + slot);
                }
                if (!teachers.add(b.getFaculty())) {
                    v.add("I4 teacher " + b.getFaculty().getFacultyId() + " double-booked at " + slot);
                }
            }
        }
        if (countedAcrossSlots != checker.getAllBookings().size()) {
            v.add("I7 booking outside the week's slots");
        }

        // I5 and I6 load
        Map<Faculty, Integer> counted = new HashMap<>();
        for (Booking b : checker.getAllBookings()) {
            counted.merge(b.getFaculty(), 1, Integer::sum);
        }
        for (Map.Entry<Faculty, Integer> e : counted.entrySet()) {
            Faculty f = e.getKey();
            int load = checker.getFacultyLoad(f);
            if (load != e.getValue()) {
                v.add("I5 load " + f.getFacultyId() + ": reported " + load + " counted " + e.getValue());
            }
            if (load > f.getMaxHoursPerWeek()) {
                v.add("I6 teacher " + f.getFacultyId() + " over limit: " + load + " > " + f.getMaxHoursPerWeek());
            }
        }

        // I7 request fidelity, I8 section once
        Map<String, PendingAssignment> bySection = new HashMap<>();
        for (PendingAssignment pa : sc.pending) {
            bySection.put(pa.getSection().getSectionCode(), pa);
        }
        Set<String> placedSections = new HashSet<>();
        for (Booking b : checker.getAllBookings()) {
            for (Section s : b.getSections()) {
                if (!placedSections.add(s.getSectionCode())) {
                    v.add("I8 section " + s.getSectionCode() + " in more than one booking");
                }
                PendingAssignment pa = bySection.get(s.getSectionCode());
                if (pa == null) {
                    v.add("I7 booking for unknown section " + s.getSectionCode());
                    continue;
                }
                if (!pa.getEligibleFaculty().contains(b.getFaculty())) {
                    v.add("I7 section " + s.getSectionCode() + " taught by non-eligible " + b.getFaculty().getFacultyId());
                }
                if (!pa.getCandidateRooms().contains(b.getRoom())) {
                    v.add("I7 section " + s.getSectionCode() + " in non-candidate room " + b.getRoom().getRoomId());
                }
                if (pa.getExpectedStrength() > 0 && b.getRoom().getCapacity() < pa.getExpectedStrength()) {
                    v.add("I7 section " + s.getSectionCode() + " in room too small");
                }
            }
        }
        return v;
    }

    @Test
    void invariantsHoldAcrossSeeds() {
        List<String> failures = new ArrayList<>();
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            for (String violation : violationsFor(seed)) {
                failures.add("seed " + seed + ": " + violation);
            }
        }
        assertTrue(failures.isEmpty(),
            failures.size() + " violation(s), first few:\n" + String.join("\n",
                failures.subList(0, Math.min(10, failures.size()))));
    }

    @Test
    void regressionSeedsStillPass() {
        for (long seed : REGRESSION_SEEDS) {
            List<String> v = violationsFor(seed);
            assertTrue(v.isEmpty(), "regression seed " + seed + " reintroduced: " + v);
        }
    }

    @Test
    void sweepIsNotVacuous() {
        int scheduled = 0;
        for (long seed = 0; seed < SEED_COUNT; seed++) {
            Scenario sc = build(seed);
            ConflictChecker checker = new ConflictChecker();
            scheduled += new SchedulingOptimizer(checker, sc.slots, 3, new Random(seed))
                .scheduleAll(sc.pending).scheduled.size();
        }
        assertTrue(scheduled > SEED_COUNT, "the sweep must place classes, not pass by placing nothing");
    }
}
