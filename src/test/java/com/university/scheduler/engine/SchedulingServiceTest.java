package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingServiceTest {

    private SchedulingService service;
    private Course course;
    private Room roomA;
    private Faculty facultyA;
    private Section section1;

    @BeforeEach
    void setUp() {
        ConflictChecker checker = new ConflictChecker();
        service = SchedulingService.fromExistingChecker(checker);
        course = new Course("C1", "Test Course", 3, PeriodType.LECTURE);
        roomA = new Room("RA", "RA", RoomType.CLASSROOM, 60);
        facultyA = new Faculty("FA", null, null, 18);
        section1 = new Section("S1", "CSE", 1, "A");
    }

    @Test
    void tryAddBookingSucceedsForValidBooking() {
        TimeSlot slot = new TimeSlot(Day.MONDAY, 1, null, null);
        Booking booking = new Booking(section1, course, facultyA, roomA, slot);

        SchedulingService.BookingResult result = service.tryAddBooking(booking);

        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
        assertEquals(1, service.getTotalBookingCount());
    }

    @Test
    void tryAddBookingReturnsFailureInsteadOfThrowing() {
        TimeSlot slot = new TimeSlot(Day.MONDAY, 1, null, null);
        Booking first = new Booking(section1, course, facultyA, roomA, slot);
        service.tryAddBooking(first);

        // same room, same slot, different section -> should conflict
        Booking clashing = new Booking(new Section("S2", "CSE", 1, "A"), course, facultyA, roomA, slot);
        SchedulingService.BookingResult result = service.tryAddBooking(clashing);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        // confirms the caller never had to catch a checked exception to find this out
    }

    @Test
    void getBookingsAtReturnsCorrectBookings() {
        TimeSlot slot = new TimeSlot(Day.TUESDAY, 3, null, null);
        Booking booking = new Booking(section1, course, facultyA, roomA, slot);
        service.tryAddBooking(booking);

        List<Booking> result = service.getBookingsAt(Day.TUESDAY, 3);
        assertEquals(1, result.size());
        assertEquals(section1, result.get(0).getSection());
    }

    @Test
    void scheduleAllReturnsBothScheduledAndUnscheduled() {
        PendingAssignment pa = new PendingAssignment(section1, course, facultyA, List.of(roomA));

        SchedulingService.OptimizationResult result = service.scheduleAll(List.of(pa));

        assertTrue(result.allScheduled());
        assertEquals(1, result.getScheduled().size());
        assertTrue(result.getUnscheduled().isEmpty());
    }

    @Test
    void facultyLoadReflectsServiceLevelBookings() {
        TimeSlot slot = new TimeSlot(Day.WEDNESDAY, 2, null, null);
        service.tryAddBooking(new Booking(section1, course, facultyA, roomA, slot));

        assertEquals(1, service.getFacultyLoad(facultyA));
    }

    @Test
    void averageUtilizationIsValidFraction() {
        double avg = service.getAverageRoomUtilization(List.of(roomA));
        assertTrue(avg >= 0.0 && avg <= 1.0);
    }
}
