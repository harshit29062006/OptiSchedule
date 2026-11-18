package com.university.scheduler.engine;

import com.university.scheduler.exception.*;
import com.university.scheduler.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConflictCheckerTest {

    private ConflictChecker checker;
    private Faculty facultyA, facultyB;
    private Room roomG114, roomLP101;
    private Course course1, course2;
    private Section section1, section2;

    @BeforeEach
    void setUp() {
        checker = new ConflictChecker();
        facultyA = new Faculty("TA8", null, null, 18);
        facultyB = new Faculty("CBCL", null, null, 18);
        roomG114 = new Room("G114", "G114", RoomType.CLASSROOM, 60);
        roomLP101 = new Room("LP101", "LP101", RoomType.LAB, 30);
        course1 = new Course("UCB009", "Database Management Systems", 4, PeriodType.PRACTICAL);
        course2 = new Course("UES103", "Engineering Science", 3, PeriodType.PRACTICAL);
        section1 = new Section("1A1A", "CSE", 1, "A");
        section2 = new Section("1A2A", "CSE", 1, "A");
    }

    @Test
    void noConflictForDistinctSlots() throws SchedulingException {
        TimeSlot slot1 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        TimeSlot slot2 = new TimeSlot(Day.MONDAY, 2, LocalTime.of(8, 50), LocalTime.of(9, 40));

        checker.addBooking(new Booking(section1, course1, facultyA, roomG114, slot1));
        checker.addBooking(new Booking(section2, course2, facultyB, roomG114, slot2));

        assertEquals(2, checker.totalBookings());
    }

    @Test
    void detectsRoomConflict() throws SchedulingException {
        TimeSlot slot = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        checker.addBooking(new Booking(section1, course1, facultyA, roomG114, slot));

        Booking clashing = new Booking(section2, course2, facultyB, roomG114, slot);
        assertThrows(RoomUnavailableException.class, () -> checker.addBooking(clashing));
    }

    @Test
    void detectsFacultyConflict() throws SchedulingException {
        TimeSlot slot = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        checker.addBooking(new Booking(section1, course1, facultyA, roomG114, slot));

        Booking clashing = new Booking(section2, course2, facultyA, roomLP101, slot);
        assertThrows(SlotConflictException.class, () -> checker.addBooking(clashing));
    }

    @Test
    void findFreeRoomReturnsCorrectRoom() throws SchedulingException {
        TimeSlot slot = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        checker.addBooking(new Booking(section1, course1, facultyA, roomG114, slot));

        Optional<Room> free = checker.findFreeRoom(slot, List.of(roomG114, roomLP101));
        assertTrue(free.isPresent());
        assertEquals(roomLP101, free.get());
    }

    @Test
    void hasConflictDoesNotThrowJustReportsBoolean() throws SchedulingException {
        TimeSlot slot = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        checker.addBooking(new Booking(section1, course1, facultyA, roomG114, slot));

        Booking probe = new Booking(section2, course2, facultyA, roomLP101, slot);
        assertTrue(checker.hasConflict(probe));
    }
}
