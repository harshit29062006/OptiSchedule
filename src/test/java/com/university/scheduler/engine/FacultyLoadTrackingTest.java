package com.university.scheduler.engine;

import com.university.scheduler.exception.*;
import com.university.scheduler.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FacultyLoadTrackingTest {

    private ConflictChecker checker;
    private Faculty lowLimitFaculty;   // maxHoursPerWeek = 2, to make the limit easy to hit in a test
    private Room room1, room2, room3;
    private Course course;
    private Section section1, section2, section3;

    @BeforeEach
    void setUp() {
        checker = new ConflictChecker();
        lowLimitFaculty = new Faculty("TESTF", null, null, 2);
        room1 = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        room2 = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        room3 = new Room("R3", "R3", RoomType.CLASSROOM, 60);
        course = new Course("TEST101", "Test Course", 3, PeriodType.LECTURE);
        section1 = new Section("SEC1", "CSE", 1, "A");
        section2 = new Section("SEC2", "CSE", 1, "A");
        section3 = new Section("SEC3", "CSE", 1, "A");
    }

    @Test
    void facultyUnderLimitSucceeds() throws SchedulingException {
        TimeSlot slot1 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        checker.addBooking(new Booking(section1, course, lowLimitFaculty, room1, slot1));

        assertEquals(1, checker.getFacultyLoad(lowLimitFaculty));
    }

    @Test
    void facultyAtLimitThrowsOverloadException() throws SchedulingException {
        TimeSlot slot1 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        TimeSlot slot2 = new TimeSlot(Day.MONDAY, 2, LocalTime.of(8, 50), LocalTime.of(9, 40));
        TimeSlot slot3 = new TimeSlot(Day.MONDAY, 3, LocalTime.of(9, 40), LocalTime.of(10, 30));

        // lowLimitFaculty has maxHoursPerWeek = 2; these two should succeed...
        checker.addBooking(new Booking(section1, course, lowLimitFaculty, room1, slot1));
        checker.addBooking(new Booking(section2, course, lowLimitFaculty, room2, slot2));

        // ...and this third one should push them over the limit and throw.
        Booking overLimit = new Booking(section3, course, lowLimitFaculty, room3, slot3);
        assertThrows(FacultyOverloadException.class, () -> checker.addBooking(overLimit));
    }

    @Test
    void loadCountIncrementsCorrectlyAcrossDistinctSlots() throws SchedulingException {
        TimeSlot slot1 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        TimeSlot slot2 = new TimeSlot(Day.TUESDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));

        checker.addBooking(new Booking(section1, course, lowLimitFaculty, room1, slot1));
        assertEquals(1, checker.getFacultyLoad(lowLimitFaculty));

        checker.addBooking(new Booking(section2, course, lowLimitFaculty, room2, slot2));
        assertEquals(2, checker.getFacultyLoad(lowLimitFaculty));
    }

    @Test
    void wouldExceedLoadReportsCorrectlyWithoutCommitting() throws SchedulingException {
        TimeSlot slot1 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        TimeSlot slot2 = new TimeSlot(Day.MONDAY, 2, LocalTime.of(8, 50), LocalTime.of(9, 40));
        TimeSlot slot3 = new TimeSlot(Day.MONDAY, 3, LocalTime.of(9, 40), LocalTime.of(10, 30));

        checker.addBooking(new Booking(section1, course, lowLimitFaculty, room1, slot1));
        checker.addBooking(new Booking(section2, course, lowLimitFaculty, room2, slot2));

        Booking probe = new Booking(section3, course, lowLimitFaculty, room3, slot3);
        assertTrue(checker.wouldExceedLoad(probe));

        // probing should NOT have committed anything — load should still read 2, not 3
        assertEquals(2, checker.getFacultyLoad(lowLimitFaculty));
    }

    @Test
    void facultyWithNoBookingsHasZeroLoad() {
        assertEquals(0, checker.getFacultyLoad(lowLimitFaculty));
    }
}
