package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingOptimizerTest {

    private ConflictChecker checker;
    private List<TimeSlot> weekSlots;
    private Course course;
    private Room roomA, roomB;

    @BeforeEach
    void setUp() {
        checker = new ConflictChecker();
        weekSlots = TimeSlotGenerator.generateStandardWeek();
        course = new Course("C1", "Test Course", 3, PeriodType.LECTURE);
        roomA = new Room("RA", "RA", RoomType.CLASSROOM, 60);
        roomB = new Room("RB", "RB", RoomType.CLASSROOM, 60);
    }

    @Test
    void schedulesAllWhenNoConflicts() {
        Faculty f1 = new Faculty("F1", null, null, 18);
        Faculty f2 = new Faculty("F2", null, null, 18);
        Section s1 = new Section("S1", "CSE", 1, "A");
        Section s2 = new Section("S2", "CSE", 1, "A");

        PendingAssignment pa1 = new PendingAssignment(s1, course, f1, List.of(roomA));
        PendingAssignment pa2 = new PendingAssignment(s2, course, f2, List.of(roomB));

        SchedulingOptimizer optimizer = new SchedulingOptimizer(checker, weekSlots);
        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> result = optimizer.scheduleAll(List.of(pa1, pa2), unscheduled);

        assertEquals(2, result.size());
        assertTrue(unscheduled.isEmpty());
    }

    @Test
    void reportsUnscheduledWhenNoValidSlotExists() {
        // faculty with maxHoursPerWeek = 0 can never be scheduled for anything
        Faculty impossibleFaculty = new Faculty("F3", null, null, 0);
        Section s1 = new Section("S1", "CSE", 1, "A");

        PendingAssignment pa = new PendingAssignment(s1, course, impossibleFaculty, List.of(roomA));

        SchedulingOptimizer optimizer = new SchedulingOptimizer(checker, weekSlots);
        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> result = optimizer.scheduleAll(List.of(pa), unscheduled);

        assertTrue(result.isEmpty());
        assertEquals(1, unscheduled.size());
        assertEquals(pa, unscheduled.get(0));
    }

    @Test
    void mostConstrainedAssignmentIsPlacedEvenWhenCompetingForSameRoom() {
        // Two sections both want ONLY roomA (single candidate room each) —
        // both are equally constrained, but both should still get scheduled
        // since there are many available slots in the week.
        Faculty f1 = new Faculty("F1", null, null, 18);
        Faculty f2 = new Faculty("F2", null, null, 18);
        Section s1 = new Section("S1", "CSE", 1, "A");
        Section s2 = new Section("S2", "CSE", 1, "A");

        PendingAssignment pa1 = new PendingAssignment(s1, course, f1, List.of(roomA));
        PendingAssignment pa2 = new PendingAssignment(s2, course, f2, List.of(roomA));

        SchedulingOptimizer optimizer = new SchedulingOptimizer(checker, weekSlots);
        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> result = optimizer.scheduleAll(List.of(pa1, pa2), unscheduled);

        // both can be placed in roomA, just at different time slots
        assertEquals(2, result.size());
        assertNotEquals(result.get(0).getTimeSlot(), result.get(1).getTimeSlot());
    }

    @Test
    void triesSecondCandidateRoomWhenFirstIsFullyBooked() {
        Faculty f1 = new Faculty("F1", null, null, 18);
        Section s1 = new Section("S1", "CSE", 1, "A");

        // PendingAssignment offers roomA and roomB as candidates
        PendingAssignment pa = new PendingAssignment(s1, course, f1, List.of(roomA, roomB));

        SchedulingOptimizer optimizer = new SchedulingOptimizer(checker, weekSlots);
        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> result = optimizer.scheduleAll(List.of(pa), unscheduled);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getRoom().equals(roomA) || result.get(0).getRoom().equals(roomB));
    }
}
