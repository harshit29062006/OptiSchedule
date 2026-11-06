package com.university.scheduler.engine;

import com.university.scheduler.exception.*;
import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress and negative tests for Day 6: bad input must fail clearly, a
 * full timetable must leave assignments unscheduled rather than throw, and
 * a failed booking must leave no partial state behind.
 */
class EdgeCaseTest {

    private static final Course COURSE = new Course("C1", "Test", 3, PeriodType.LECTURE);

    private static PendingAssignment pending(String sectionCode, Faculty f, Room room) {
        return new PendingAssignment(new Section(sectionCode, "CSE", 1, "A"), COURSE, f, List.of(room));
    }

    // ---------- invalid construction ----------

    @Test
    void emptyCandidateRoomListRejectedAtConstruction() {
        Faculty f = new Faculty("F1", null, null, 18);
        assertThrows(IllegalArgumentException.class, () ->
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, f, List.of()));
    }

    @Test
    void nullCandidateRoomListRejectedAtConstruction() {
        Faculty f = new Faculty("F1", null, null, 18);
        assertThrows(IllegalArgumentException.class, () ->
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, f, null));
    }

    @Test
    void negativeStrengthRejected() {
        Faculty f = new Faculty("F1", null, null, 18);
        Room r = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        assertThrows(IllegalArgumentException.class, () ->
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, f, List.of(r), -5));
    }

    @Test
    void zeroCapacityRoomRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            new Room("R0", "R0", RoomType.CLASSROOM, 0));
    }

    @Test
    void negativeFacultyLimitRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            new Faculty("F1", null, null, -1));
    }

    @Test
    void bookingRejectsNullParts() {
        Faculty f = new Faculty("F1", null, null, 18);
        Room r = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        TimeSlot t = new TimeSlot(Day.MONDAY, 1, null, null);
        assertThrows(NullPointerException.class, () ->
            new Booking(null, COURSE, f, r, t));
        assertThrows(NullPointerException.class, () ->
            new Booking(new Section("S1", "CSE", 1, "A"), COURSE, f, r, null));
    }

    @Test
    void eligibleFacultyListIsDeduplicatedAndPreferredFirst() {
        Faculty a = new Faculty("A", null, null, 18);
        Faculty b = new Faculty("B", null, null, 18);
        Room r = new Room("R1", "R1", RoomType.CLASSROOM, 60);

        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, a, List.of(r), 0, List.of(b, a, b));

        assertEquals(List.of(a, b), pa.getEligibleFaculty());
    }

    // ---------- faculty and load edge cases ----------

    @Test
    void facultyWithZeroWeeklyLimitIsNeverScheduled() {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Faculty zero = new Faculty("Z", null, null, 0);
        Room r = new Room("R1", "R1", RoomType.CLASSROOM, 60);

        ConflictChecker checker = new ConflictChecker();
        PendingAssignment pa = pending("S1", zero, r);
        SchedulingOptimizer.PassResult result = new SchedulingOptimizer(checker, slots).scheduleAll(List.of(pa));

        assertTrue(result.scheduled.isEmpty());
        assertEquals(1, result.unscheduled.size());
        assertTrue(OptimizationRunDriver.diagnose(checker, pa, slots).contains("load limit"),
            "the reason for failure should name the load limit, not a room or slot");
    }

    // ---------- full and impossible timetables ----------

    @Test
    void fullTimetableLeavesAssignmentUnscheduledWithoutThrowing() throws SchedulingException {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek(); // 84 slots
        Faculty busy = new Faculty("BUSY", null, null, 500);
        Faculty newcomer = new Faculty("NEW", null, null, 18);
        Room shared = new Room("R1", "R1", RoomType.CLASSROOM, 60);

        ConflictChecker checker = new ConflictChecker();
        for (int i = 0; i < slots.size(); i++) {
            checker.addBooking(new Booking(new Section("F" + i, "CSE", 1, "A"), COURSE, busy, shared, slots.get(i)));
        }

        PendingAssignment pa = pending("S1", newcomer, shared);
        SchedulingOptimizer.PassResult result = new SchedulingOptimizer(checker, slots).scheduleAll(List.of(pa));

        assertTrue(result.scheduled.isEmpty());
        assertEquals(1, result.unscheduled.size());
        assertEquals(84, checker.totalBookings(), "a failed run must not add any bookings");
        assertTrue(OptimizationRunDriver.diagnose(checker, pa, slots).contains("no free slot"));
    }

    @Test
    void noRoomLargeEnoughIsReportedAsCapacityFailure() {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Faculty f = new Faculty("F1", null, null, 18);
        Room small = new Room("S", "S", RoomType.CLASSROOM, 20);

        ConflictChecker checker = new ConflictChecker();
        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, f, List.of(small), 50);

        SchedulingOptimizer.PassResult result = new SchedulingOptimizer(checker, slots).scheduleAll(List.of(pa));
        assertEquals(1, result.unscheduled.size());
        assertTrue(OptimizationRunDriver.diagnose(checker, pa, slots).contains("large enough"));
    }

    // ---------- input validation at the optimizer boundary ----------

    @Test
    void emptyPendingListIsValidAndSchedulesNothing() {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        ConflictChecker checker = new ConflictChecker();

        SchedulingOptimizer.PassResult result = new SchedulingOptimizer(checker, slots).scheduleAll(List.of());

        assertTrue(result.scheduled.isEmpty());
        assertTrue(result.unscheduled.isEmpty());
        assertEquals(0, checker.totalBookings());
    }

    @Test
    void nullPendingListRejected() {
        SchedulingOptimizer opt = new SchedulingOptimizer(new ConflictChecker(),
            TimeSlotGenerator.generateStandardWeek());
        assertThrows(NullPointerException.class, () -> opt.scheduleAll(null));
    }

    @Test
    void nullElementInPendingListRejected() {
        SchedulingOptimizer opt = new SchedulingOptimizer(new ConflictChecker(),
            TimeSlotGenerator.generateStandardWeek());
        List<PendingAssignment> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> opt.scheduleAll(withNull));
    }

    @Test
    void duplicateSectionAndCourseRejectedBeforeAnyPlacement() {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Faculty f = new Faculty("F1", null, null, 18);
        Room r = new Room("R1", "R1", RoomType.CLASSROOM, 60);

        ConflictChecker checker = new ConflictChecker();
        List<PendingAssignment> dup = List.of(pending("SAME", f, r), pending("SAME", f, r));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            new SchedulingOptimizer(checker, slots).scheduleAll(dup));
        assertTrue(ex.getMessage().contains("SAME"), "the message should name the duplicated section");
        assertEquals(0, checker.totalBookings(), "rejected input must not leave anything committed");
    }

    @Test
    void zeroPassesRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            new SchedulingOptimizer(new ConflictChecker(), TimeSlotGenerator.generateStandardWeek(), 0, new Random(1)));
    }

    // ---------- failed bookings leave no partial state ----------

    @Test
    void failedRoomConflictLeavesCheckerUnchanged() throws SchedulingException {
        Faculty a = new Faculty("A", null, null, 18);
        Faculty b = new Faculty("B", null, null, 18);
        Room r = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        TimeSlot t = new TimeSlot(Day.MONDAY, 1, null, null);

        ConflictChecker checker = new ConflictChecker();
        checker.addBooking(new Booking(new Section("S1", "CSE", 1, "A"), COURSE, a, r, t));
        int loadBefore = checker.getFacultyLoad(b);

        assertThrows(RoomUnavailableException.class, () ->
            checker.addBooking(new Booking(new Section("S2", "CSE", 1, "A"), COURSE, b, r, t)));

        assertEquals(1, checker.totalBookings());
        assertEquals(loadBefore, checker.getFacultyLoad(b), "a rejected booking must not count toward load");
    }

    @Test
    void failedOverloadLeavesCheckerUnchanged() throws SchedulingException {
        Faculty f = new Faculty("F", null, null, 1);
        Room r1 = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Room r2 = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        ConflictChecker checker = new ConflictChecker();
        checker.addBooking(new Booking(new Section("S1", "CSE", 1, "A"), COURSE, f, r1,
            new TimeSlot(Day.MONDAY, 1, null, null)));

        assertThrows(FacultyOverloadException.class, () ->
            checker.addBooking(new Booking(new Section("S2", "CSE", 1, "A"), COURSE, f, r2,
                new TimeSlot(Day.MONDAY, 2, null, null))));

        assertEquals(1, checker.totalBookings());
        assertEquals(1, checker.getFacultyLoad(f));
    }

    // ---------- error message quality ----------

    @Test
    void roomConflictMessageNamesRoomAndSlot() throws SchedulingException {
        Faculty a = new Faculty("A", null, null, 18);
        Faculty b = new Faculty("B", null, null, 18);
        Room r = new Room("LT101", "LT101", RoomType.CLASSROOM, 60);
        TimeSlot t = new TimeSlot(Day.TUESDAY, 3, LocalTime.of(9, 40), LocalTime.of(10, 30));

        ConflictChecker checker = new ConflictChecker();
        checker.addBooking(new Booking(new Section("S1", "CSE", 1, "A"), COURSE, a, r, t));

        RoomUnavailableException ex = assertThrows(RoomUnavailableException.class, () ->
            checker.addBooking(new Booking(new Section("S2", "CSE", 1, "A"), COURSE, b, r, t)));

        assertTrue(ex.getMessage().contains("LT101"), "message should name the room");
        assertTrue(ex.getMessage().contains("TUESDAY"), "message should name the day");
    }

    @Test
    void facultyConflictMessageNamesBothRooms() throws SchedulingException {
        Faculty a = new Faculty("A", null, null, 18);
        Room r1 = new Room("R-ONE", "R-ONE", RoomType.CLASSROOM, 60);
        Room r2 = new Room("R-TWO", "R-TWO", RoomType.CLASSROOM, 60);
        TimeSlot t = new TimeSlot(Day.FRIDAY, 5, null, null);

        ConflictChecker checker = new ConflictChecker();
        checker.addBooking(new Booking(new Section("S1", "CSE", 1, "A"), COURSE, a, r1, t));

        SlotConflictException ex = assertThrows(SlotConflictException.class, () ->
            checker.addBooking(new Booking(new Section("S2", "CSE", 1, "A"), COURSE, a, r2, t)));

        assertTrue(ex.getMessage().contains("R-ONE"));
        assertTrue(ex.getMessage().contains("R-TWO"));
    }

    // ---------- commit integrity across passes ----------

    @Test
    void multiplePassesCommitEachBookingExactlyOnce() {
        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        Room r1 = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Room r2 = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        Faculty f1 = new Faculty("F1", null, null, 18);
        Faculty f2 = new Faculty("F2", null, null, 18);
        Faculty f3 = new Faculty("F3", null, null, 18);

        List<PendingAssignment> pending = List.of(
            pending("A", f1, r1), pending("B", f2, r2), pending("C", f3, r1));

        ConflictChecker checker = new ConflictChecker();
        SchedulingOptimizer.PassResult result =
            new SchedulingOptimizer(checker, slots, 8, new Random(5)).scheduleAll(pending);

        assertEquals(result.scheduled.size(), checker.totalBookings(),
            "the committed schedule must match the winning pass, with no duplicates from retries");
    }
}
