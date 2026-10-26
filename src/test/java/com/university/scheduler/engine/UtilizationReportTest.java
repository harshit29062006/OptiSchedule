package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UtilizationReportTest {

    private ConflictChecker checker;
    private UtilizationReport report;
    private List<TimeSlot> weekSlots;
    private Course course;
    private Room roomA, roomB;
    private Faculty facultyA;
    private Section section1, section2;

    @BeforeEach
    void setUp() throws SchedulingException {
        checker = new ConflictChecker();
        report = new UtilizationReport(checker);
        weekSlots = TimeSlotGenerator.generateStandardWeek(); // 84 slots (6 days x 14 periods)
        course = new Course("C1", "Test Course", 3, PeriodType.LECTURE);
        roomA = new Room("RA", "RA", RoomType.CLASSROOM, 60);
        roomB = new Room("RB", "RB", RoomType.CLASSROOM, 60);
        facultyA = new Faculty("FA", null, null, 18);
        section1 = new Section("S1", "CSE", 1, "A");
        section2 = new Section("S2", "CSE", 1, "A");

        // Book roomA for 2 of the 84 slots; leave roomB empty
        checker.addBooking(new Booking(section1, course, facultyA, roomA, weekSlots.get(0)));
        checker.addBooking(new Booking(section2, course, new Faculty("FB", null, null, 18), roomA, weekSlots.get(1)));
    }

    @Test
    void computesCorrectUtilizationFraction() {
        Map<Room, Double> utilization = report.computeRoomUtilization(List.of(roomA, roomB), weekSlots);

        assertEquals(2.0 / 84, utilization.get(roomA), 0.0001);
        assertEquals(0.0, utilization.get(roomB), 0.0001);
    }

    @Test
    void rankedUtilizationPutsHighestFirst() {
        List<Map.Entry<Room, Double>> ranked = report.computeRoomUtilizationRanked(List.of(roomA, roomB), weekSlots);

        assertEquals(roomA, ranked.get(0).getKey());
        assertEquals(roomB, ranked.get(1).getKey());
    }

    @Test
    void facultyLoadRatioReflectsBookings() throws SchedulingException {
        // facultyA has maxHoursPerWeek=18 and currently has 1 booking (from setUp)
        Map<Faculty, Double> ratios = report.computeFacultyLoadRatio();

        assertEquals(1.0 / 18, ratios.get(facultyA), 0.0001);
    }

    @Test
    void findsFacultyNearLoadLimit() throws SchedulingException {
        Faculty nearLimitFaculty = new Faculty("FC", null, null, 2);
        checker.addBooking(new Booking(
            new Section("S3", "CSE", 1, "A"), course, nearLimitFaculty, roomB, weekSlots.get(5)));
        checker.addBooking(new Booking(
            new Section("S4", "CSE", 1, "A"), course, nearLimitFaculty, roomB, weekSlots.get(6)));
        // nearLimitFaculty is now at 2/2 = 100% of their limit

        List<Faculty> atLimit = report.findFacultyNearLoadLimit(0.9);

        assertTrue(atLimit.contains(nearLimitFaculty));
        assertFalse(atLimit.contains(facultyA)); // facultyA is only at ~5.5%, well under 90%
    }

    @Test
    void averageUtilizationIsSaneValue() {
        double avg = report.computeAverageRoomUtilization(List.of(roomA, roomB), weekSlots);

        assertTrue(avg >= 0.0 && avg <= 1.0, "Average utilization must be between 0 and 1");
    }

    @Test
    void emptySlotsListThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            report.computeRoomUtilization(List.of(roomA), List.of()));
    }
}
