package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CapacityFitTest {

    private static final Course COURSE = new Course("C1", "Test", 3, PeriodType.LECTURE);

    private static List<TimeSlot> oneSlot() {
        return List.of(new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50)));
    }

    @Test
    void tightestFitRoomIsChosenWhenSeveralFit() throws Exception {
        Room hall = new Room("HALL", "HALL", RoomType.CLASSROOM, 120);
        Room small = new Room("SMALL", "SMALL", RoomType.CLASSROOM, 30);
        Faculty f = new Faculty("F1", null, null, 18);

        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, f, List.of(hall, small), 25);

        ConflictChecker checker = new ConflictChecker();
        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> result = new SchedulingOptimizer(checker, oneSlot())
            .scheduleAll(List.of(pa), unscheduled);

        assertEquals(1, result.size());
        assertEquals(small, result.get(0).getRoom(), "25 students should go to the 30-seat room, not the 120-seat hall");
    }

    @Test
    void roomsTooSmallAreExcluded() {
        Room tiny = new Room("TINY", "TINY", RoomType.CLASSROOM, 20);
        Faculty f = new Faculty("F1", null, null, 18);

        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, f, List.of(tiny), 25);

        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> result = new SchedulingOptimizer(new ConflictChecker(), oneSlot())
            .scheduleAll(List.of(pa), unscheduled);

        assertTrue(result.isEmpty(), "a 20-seat room cannot hold 25 students");
        assertEquals(1, unscheduled.size());
    }

    @Test
    void unknownStrengthKeepsGivenRoomOrder() throws Exception {
        Room first = new Room("FIRST", "FIRST", RoomType.CLASSROOM, 120);
        Room second = new Room("SECOND", "SECOND", RoomType.CLASSROOM, 30);
        Faculty f = new Faculty("F1", null, null, 18);

        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, f, List.of(first, second));

        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> result = new SchedulingOptimizer(new ConflictChecker(), oneSlot())
            .scheduleAll(List.of(pa), unscheduled);

        assertEquals(first, result.get(0).getRoom(), "with unknown strength, the given order must be kept");
    }

    @Test
    void orderedRoomsSortsByWastedSeats() {
        Room a = new Room("A", "A", RoomType.CLASSROOM, 100);
        Room b = new Room("B", "B", RoomType.CLASSROOM, 40);
        Room c = new Room("C", "C", RoomType.CLASSROOM, 10);
        Faculty f = new Faculty("F1", null, null, 18);

        PendingAssignment pa = new PendingAssignment(
            new Section("S1", "CSE", 1, "A"), COURSE, f, List.of(a, b, c), 30);

        List<Room> ordered = SchedulingOptimizer.orderedRooms(pa);

        assertEquals(List.of(b, a), ordered, "room C is too small; B (10 wasted) before A (70 wasted)");
    }
}
