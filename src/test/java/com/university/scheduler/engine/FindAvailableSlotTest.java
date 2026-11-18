package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FindAvailableSlotTest {

    private ConflictChecker checker;
    private Faculty faculty;
    private Room room;
    private Course course;
    private Section section1, section2;
    private List<TimeSlot> weekSlots;

    @BeforeEach
    void setUp() {
        checker = new ConflictChecker();
        faculty = new Faculty("F1", null, null, 18);
        room = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        course = new Course("C1", "Test Course", 3, PeriodType.LECTURE);
        section1 = new Section("SEC1", "CSE", 1, "A");
        section2 = new Section("SEC2", "CSE", 1, "A");
        weekSlots = TimeSlotGenerator.generateStandardWeek();
    }

    @Test
    void timeSlotGeneratorProducesExpectedCount() {
        // 6 working days x 14 periods = 84 slots
        assertEquals(84, weekSlots.size());
    }

    @Test
    void findsFirstFreeSlotWhenNothingBooked() {
        Optional<TimeSlot> result = checker.findAvailableSlot(faculty, room, weekSlots);

        assertTrue(result.isPresent());
        assertEquals(Day.MONDAY, result.get().getDay());
        assertEquals(1, result.get().getPeriodNumber());
    }

    @Test
    void skipsSlotsWhereRoomIsBusy() throws SchedulingException {
        TimeSlot mondayP1 = weekSlots.get(0); // Monday, period 1
        checker.addBooking(new Booking(section1, course, faculty, room, mondayP1));

        // same room, different faculty, searching same candidate list —
        // should skip Monday P1 since the ROOM is busy, and land on the next free slot
        Faculty otherFaculty = new Faculty("F2", null, null, 18);
        Optional<TimeSlot> result = checker.findAvailableSlot(otherFaculty, room, weekSlots);

        assertTrue(result.isPresent());
        assertNotEquals(mondayP1, result.get());
    }

    @Test
    void skipsSlotsWhereFacultyIsBusy() throws SchedulingException {
        TimeSlot mondayP1 = weekSlots.get(0);
        checker.addBooking(new Booking(section1, course, faculty, room, mondayP1));

        Room otherRoom = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        Optional<TimeSlot> result = checker.findAvailableSlot(faculty, otherRoom, weekSlots);

        assertTrue(result.isPresent());
        assertNotEquals(mondayP1, result.get());
    }

    @Test
    void returnsEmptyWhenFacultyAtLoadLimit() throws SchedulingException {
        Faculty lowLimit = new Faculty("F3", null, null, 1); // can only take 1 period total

        TimeSlot mondayP1 = weekSlots.get(0);
        Room anotherRoom = new Room("R9", "R9", RoomType.CLASSROOM, 60);
        checker.addBooking(new Booking(section1, course, lowLimit, anotherRoom, mondayP1));

        // lowLimit is now at their cap; searching for ANY further slot should find none
        Optional<TimeSlot> result = checker.findAvailableSlot(lowLimit, room, weekSlots);
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoCandidateSlotsGiven() {
        Optional<TimeSlot> result = checker.findAvailableSlot(faculty, room, List.of());
        assertTrue(result.isEmpty());
    }
}
