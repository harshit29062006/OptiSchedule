package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Regression: the optimizer must never merge unrelated classes into one combined session. */
class OptimizerNoSilentMergeTest {

    private static final Course COURSE = new Course("C1", "Test", 3, PeriodType.LECTURE);

    @Test
    void sameTeacherSameRoomGetsSeparateSlotsNotOneSession() {
        List<TimeSlot> slots = TimeSlotGenerator.generateWeek(3, LocalTime.of(8, 0), 50);
        Faculty teacher = new Faculty("T", null, null, 18);
        Room room = new Room("R1", "R1", RoomType.CLASSROOM, 60);

        List<PendingAssignment> pending = List.of(
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, teacher, List.of(room)),
            new PendingAssignment(new Section("S2", "CSE", 1, "A"), COURSE, teacher, List.of(room)));

        ConflictChecker checker = new ConflictChecker();
        new SchedulingOptimizer(checker, slots).scheduleAll(pending, new ArrayList<>());

        assertEquals(2, checker.totalBookings(), "two unrelated classes are two sessions, not one");
        assertEquals(2, checker.getFacultyLoad(teacher), "each class uses a teaching period");
        checker.getAllBookings().forEach(b ->
            assertEquals(1, b.getSections().size(), "no session may hold two unrelated sections"));
    }
}