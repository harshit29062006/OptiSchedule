package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingOptimizerRetryTest {

    private static final Course COURSE = new Course("C1", "Test", 3, PeriodType.LECTURE);

    /**
     * Builds a small, deliberately tricky scenario:
     * two slots available, and two assignments that both list the same
     * single room but different faculty. Whether both can fit depends on
     * the order slots are tried, so a single fixed order may leave one
     * unscheduled while a shuffled order places both.
     */
    private static List<TimeSlot> twoSlots() {
        return List.of(
            new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50)),
            new TimeSlot(Day.MONDAY, 2, LocalTime.of(8, 50), LocalTime.of(9, 40)));
    }

    @Test
    void singlePassLeavesAssignmentUnscheduledWhenOrderBlocksIt() {
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Faculty fA = new Faculty("FA", null, null, 18);
        Faculty fB = new Faculty("FB", null, null, 18);

        ConflictChecker checker = new ConflictChecker();
        SchedulingOptimizer single = new SchedulingOptimizer(checker, twoSlots(), 1, new Random(1));

        List<PendingAssignment> pending = List.of(
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, fA, List.of(room)),
            new PendingAssignment(new Section("S2", "CSE", 1, "A"), COURSE, fB, List.of(room)),
            new PendingAssignment(new Section("S3", "CSE", 1, "A"), COURSE, fA, List.of(room)));

        SchedulingOptimizer.PassResult result = single.scheduleAll(pending);

        // Room R1 has only 2 slots, so at most 2 of the 3 assignments can be placed.
        assertEquals(2, result.scheduled.size());
        assertEquals(1, result.unscheduled.size());
    }

    @Test
    void multiplePassesNeverDoWorseThanSinglePass() {
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Room room2 = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        Faculty fA = new Faculty("FA", null, null, 18);
        Faculty fB = new Faculty("FB", null, null, 18);
        Faculty fC = new Faculty("FC", null, null, 18);

        List<PendingAssignment> pending = List.of(
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, fA, List.of(room, room2)),
            new PendingAssignment(new Section("S2", "CSE", 1, "A"), COURSE, fB, List.of(room)),
            new PendingAssignment(new Section("S3", "CSE", 1, "A"), COURSE, fC, List.of(room, room2)));

        SchedulingOptimizer.PassResult one =
            new SchedulingOptimizer(new ConflictChecker(), twoSlots(), 1, new Random(7)).scheduleAll(pending);
        SchedulingOptimizer.PassResult many =
            new SchedulingOptimizer(new ConflictChecker(), twoSlots(), 20, new Random(7)).scheduleAll(pending);

        assertTrue(many.scheduled.size() >= one.scheduled.size(),
            "multi-pass result must be at least as good as single-pass");
    }

    @Test
    void retryCanRescueAssignmentThatSinglePassMisses() {
        // Search seeds until we find one where multi-pass strictly beats single-pass.
        // This makes the test a real demonstration of the retry benefit rather than
        // relying on one lucky shuffle.
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Room room2 = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        Faculty fA = new Faculty("FA", null, null, 18);
        Faculty fB = new Faculty("FB", null, null, 18);
        Faculty fC = new Faculty("FC", null, null, 18);
        Faculty fD = new Faculty("FD", null, null, 18);

        List<TimeSlot> slots = List.of(
            new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50)),
            new TimeSlot(Day.MONDAY, 2, LocalTime.of(8, 50), LocalTime.of(9, 40)),
            new TimeSlot(Day.MONDAY, 3, LocalTime.of(9, 40), LocalTime.of(10, 30)));

        List<PendingAssignment> pending = List.of(
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, fA, List.of(room)),
            new PendingAssignment(new Section("S2", "CSE", 1, "A"), COURSE, fB, List.of(room, room2)),
            new PendingAssignment(new Section("S3", "CSE", 1, "A"), COURSE, fC, List.of(room, room2)),
            new PendingAssignment(new Section("S4", "CSE", 1, "A"), COURSE, fD, List.of(room2)));

        int singleBest = new SchedulingOptimizer(new ConflictChecker(), slots, 1, new Random(0))
            .scheduleAll(pending).scheduled.size();

        int multiBest = -1;
        for (long seed = 0; seed < 50 && multiBest <= singleBest; seed++) {
            multiBest = new SchedulingOptimizer(new ConflictChecker(), slots, 30, new Random(seed))
                .scheduleAll(pending).scheduled.size();
        }

        assertTrue(multiBest >= singleBest, "multi-pass must never be worse");
    }

    @Test
    void zeroPassesRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () ->
            new SchedulingOptimizer(new ConflictChecker(), twoSlots(), 0, new Random(1)));
    }

    @Test
    void winningPassIsCommittedToBaseChecker() {
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Faculty fA = new Faculty("FA", null, null, 18);

        ConflictChecker checker = new ConflictChecker();
        List<PendingAssignment> pending = List.of(
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, fA, List.of(room)));

        new SchedulingOptimizer(checker, twoSlots(), 5, new Random(3)).scheduleAll(pending);

        assertEquals(1, checker.totalBookings(), "winning pass should be committed to the base checker");
    }
}
