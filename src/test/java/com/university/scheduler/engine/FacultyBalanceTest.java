package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FacultyBalanceTest {

    private static final Course COURSE = new Course("C1", "Test", 3, PeriodType.LECTURE);

    /** Preloads a faculty member with n bookings in a separate room, so their load starts at n. */
    private static void preload(ConflictChecker checker, Faculty f, Room room, List<TimeSlot> slots, int n)
            throws SchedulingException {
        for (int i = 0; i < n; i++) {
            checker.addBooking(new Booking(new Section("PRE-" + f.getFacultyId() + i, "CSE", 1, "A"),
                COURSE, f, room, slots.get(i)));
        }
    }

    @Test
    void lessLoadedEligibleFacultyIsChosen() throws SchedulingException {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Faculty a = new Faculty("A", null, null, 18);
        Faculty b = new Faculty("B", null, null, 18);
        Room preRoom = new Room("PRE", "PRE", RoomType.CLASSROOM, 200);
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 200);

        ConflictChecker checker = new ConflictChecker();
        preload(checker, a, preRoom, slots, 2); // A already carries 2 periods, B carries none

        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, a, List.of(room), 0, List.of(a, b));

        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> placed = new SchedulingOptimizer(checker, slots).scheduleAll(List.of(pa), unscheduled);

        assertEquals(1, placed.size());
        assertEquals(b, placed.get(0).getFaculty(), "the idle faculty member should take the class");
    }

    @Test
    void preferredFacultyIsUsedWhenNoOtherIsEligible() throws SchedulingException {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Faculty a = new Faculty("A", null, null, 18);
        Faculty b = new Faculty("B", null, null, 18);
        Room preRoom = new Room("PRE", "PRE", RoomType.CLASSROOM, 200);
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 200);

        ConflictChecker checker = new ConflictChecker();
        preload(checker, a, preRoom, slots, 2);

        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, a, List.of(room)); // eligible = [A] only

        List<Booking> placed = new SchedulingOptimizer(checker, slots)
            .scheduleAll(List.of(pa), new ArrayList<>());

        assertEquals(a, placed.get(0).getFaculty(), "a class locked to A must stay with A");
    }

    @Test
    void balancingLowersSpreadBelowNaiveOnSameBatch() throws SchedulingException {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Faculty a = new Faculty("A", null, null, 18);
        Faculty b = new Faculty("B", null, null, 18);
        Room preRoom = new Room("PRE", "PRE", RoomType.CLASSROOM, 200);
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 200);

        // Naive: three classes locked to A. A ends with 3 + 3 = 6, B with 0.
        ConflictChecker naive = new ConflictChecker();
        preload(naive, a, preRoom, slots, 3);
        List<PendingAssignment> lockedToA = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            lockedToA.add(new PendingAssignment(
                new Section("N" + i, "CSE", 1, "A"), COURSE, a, List.of(room), 0));
        }
        new SchedulingOptimizer(naive, slots).scheduleAll(lockedToA, new ArrayList<>());
        double naiveSpread = FacultyBalance.loadStdDev(naive, List.of(a, b));

        // Balanced: same three classes, each eligible for A or B.
        ConflictChecker balanced = new ConflictChecker();
        preload(balanced, a, preRoom, slots, 3);
        List<PendingAssignment> eligibleBoth = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            eligibleBoth.add(new PendingAssignment(
                new Section("N" + i, "CSE", 1, "A"), COURSE, a, List.of(room), 0, List.of(a, b)));
        }
        new SchedulingOptimizer(balanced, slots).scheduleAll(eligibleBoth, new ArrayList<>());
        double balancedSpread = FacultyBalance.loadStdDev(balanced, List.of(a, b));

        assertTrue(balancedSpread < naiveSpread,
            "balanced spread " + balancedSpread + " should be below naive " + naiveSpread);
        assertEquals(0.0, balancedSpread, 1e-9, "3 + 3 classes split evenly means A=3, B=3, spread 0");
    }

    @Test
    void loadStdDevIsZeroForEqualLoadsAndPositiveOtherwise() throws SchedulingException {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Faculty a = new Faculty("A", null, null, 18);
        Faculty b = new Faculty("B", null, null, 18);
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 200);

        ConflictChecker checker = new ConflictChecker();
        assertEquals(0.0, FacultyBalance.loadStdDev(checker, List.of(a, b)), 1e-9, "no load anywhere");

        checker.addBooking(new Booking(new Section("S", "CSE", 1, "A"), COURSE, a, room, slots.get(0)));
        assertTrue(FacultyBalance.loadStdDev(checker, List.of(a, b)) > 0.0, "A has load, B is idle");
    }
}
