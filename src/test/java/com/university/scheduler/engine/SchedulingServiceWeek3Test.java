package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingServiceWeek3Test {

    private static final Course COURSE = new Course("C1", "Test", 3, PeriodType.LECTURE);

    @Test
    void retryBasedSchedulingAccountsForEveryAssignment() {
        SchedulingService service = SchedulingService.fromExistingChecker(new ConflictChecker());
        Room r1 = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Room r2 = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        Faculty f1 = new Faculty("F1", null, null, 18);
        Faculty f2 = new Faculty("F2", null, null, 18);
        Faculty f3 = new Faculty("F3", null, null, 18);

        List<PendingAssignment> pending = List.of(
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, f1, List.of(r1)),
            new PendingAssignment(new Section("S2", "CSE", 1, "A"), COURSE, f2, List.of(r2)),
            new PendingAssignment(new Section("S3", "CSE", 1, "A"), COURSE, f3, List.of(r1, r2)));

        SchedulingService.OptimizationResult result = service.scheduleWithRetries(pending, 5, 42L);

        assertEquals(3, result.getScheduled().size() + result.getUnscheduled().size(),
            "every pending assignment must be either scheduled or reported unscheduled");
        assertTrue(result.allScheduled(), "this small batch fits and should be fully placed");
    }

    @Test
    void bookingSnapshotMatchesBookingCount() {
        SchedulingService service = SchedulingService.fromExistingChecker(new ConflictChecker());
        Room r = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Faculty f = new Faculty("F1", null, null, 18);

        service.scheduleWithRetries(List.of(
            new PendingAssignment(new Section("S1", "CSE", 1, "A"), COURSE, f, List.of(r))), 3, 1L);

        assertEquals(service.getTotalBookingCount(), service.getAllBookings().size());
        assertEquals(1, service.getAllBookings().size());
    }

    @Test
    void spreadIsZeroForEmptyPool() {
        SchedulingService service = SchedulingService.fromExistingChecker(new ConflictChecker());
        assertEquals(0.0, service.getFacultyLoadSpread(List.of()), 1e-9);
    }
}
