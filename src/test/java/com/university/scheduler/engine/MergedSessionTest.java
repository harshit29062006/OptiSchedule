package com.university.scheduler.engine;

import com.university.scheduler.exception.*;
import com.university.scheduler.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the real-world rule confirmed against the actual dataset:
 * one faculty teaching the SAME ROOM at the SAME SLOT to multiple
 * sections is a combined/shared class, not a conflict — but the same
 * faculty in a DIFFERENT room, or the same room with a DIFFERENT
 * faculty, remain genuine conflicts.
 */
class MergedSessionTest {

    private ConflictChecker checker;
    private Faculty facultyA, facultyB;
    private Room roomX, roomY;
    private Course course;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        checker = new ConflictChecker();
        facultyA = new Faculty("ROS", null, null, 18);
        facultyB = new Faculty("KSS", null, null, 18);
        roomX = new Room("LT101", "LT101", RoomType.CLASSROOM, 200);
        roomY = new Room("LT102", "LT102", RoomType.CLASSROOM, 200);
        course = new Course("UCB009", "Shared Elective", 3, PeriodType.LECTURE);
        slot = new TimeSlot(Day.TUESDAY, 13, LocalTime.of(18, 0), LocalTime.of(18, 50));
    }

    @Test
    void sameFacultySameRoomMergesInsteadOfConflicting() throws SchedulingException {
        Section s1 = new Section("3C1A", "CSE", 3, "A");
        Section s2 = new Section("3C2A", "CSE", 3, "A");
        Section s3 = new Section("3O1A", "CSE", 3, "A");

        checker.addBooking(new Booking(s1, course, facultyA, roomX, slot));
        checker.addBooking(new Booking(s2, course, facultyA, roomX, slot)); // should merge, not throw
        checker.addBooking(new Booking(s3, course, facultyA, roomX, slot)); // should also merge

        // Only ONE distinct booking (session) exists at this slot, not three
        assertEquals(1, checker.getBookingsAt(slot).size());
        assertEquals(1, checker.totalBookings());

        // But it represents 3 sections attending
        assertEquals(3, checker.totalSectionAttendances());

        Booking merged = checker.getBookingsAt(slot).get(0);
        assertEquals(3, merged.getSections().size());
        assertTrue(merged.getSections().contains(s1));
        assertTrue(merged.getSections().contains(s2));
        assertTrue(merged.getSections().contains(s3));
    }

    @Test
    void mergingDoesNotDoubleCountFacultyLoad() throws SchedulingException {
        Section s1 = new Section("3C1A", "CSE", 3, "A");
        Section s2 = new Section("3C2A", "CSE", 3, "A");

        checker.addBooking(new Booking(s1, course, facultyA, roomX, slot));
        checker.addBooking(new Booking(s2, course, facultyA, roomX, slot)); // merge

        // Still only ONE teaching period counted, not two
        assertEquals(1, checker.getFacultyLoad(facultyA));
    }

    @Test
    void sameFacultyDifferentRoomIsStillARealConflict() throws SchedulingException {
        Section s1 = new Section("3C1A", "CSE", 3, "A");
        Section s2 = new Section("3O1A", "CSE", 3, "A");

        checker.addBooking(new Booking(s1, course, facultyA, roomX, slot));

        Booking clashing = new Booking(s2, course, facultyA, roomY, slot); // different room
        assertThrows(SlotConflictException.class, () -> checker.addBooking(clashing));
    }

    @Test
    void sameRoomDifferentFacultyIsStillARealConflict() throws SchedulingException {
        Section s1 = new Section("3C1A", "CSE", 3, "A");
        Section s2 = new Section("3O1A", "CSE", 3, "A");

        checker.addBooking(new Booking(s1, course, facultyA, roomX, slot));

        Booking clashing = new Booking(s2, course, facultyB, roomX, slot); // different faculty, same room
        assertThrows(RoomUnavailableException.class, () -> checker.addBooking(clashing));
    }
}
