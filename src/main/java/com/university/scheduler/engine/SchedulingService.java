package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;

import java.nio.file.Path;
import java.util.*;

/**
 * Facade over the scheduling engine. The UI and any other layer outside the
 * engine package should call this class, not ConflictChecker or the optimizer
 * directly.
 *
 * Week 3 additions:
 *   scheduleWithRetries  multi-pass optimization with a fixed seed
 *   getFacultyLoadSpread load balance measure over a faculty pool
 *   getAllBookings       snapshot of the committed schedule
 */
public class SchedulingService {
    private final ConflictChecker checker;
    private final List<TimeSlot> standardWeekSlots;

    private SchedulingService(ConflictChecker checker) {
        this.checker = checker;
        this.standardWeekSlots = TimeSlotGenerator.generateStandardWeek();
    }

    /** Creates a service loaded from the cleaned real timetable CSV. */
    public static SchedulingService fromCsv(Path csvPath) {
        BookingSource source = new CsvBookingSource(csvPath);
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);
        return new SchedulingService(checker);
    }

    /** Creates a service over an existing checker (used by tests). */
    public static SchedulingService fromExistingChecker(ConflictChecker checker) {
        return new SchedulingService(checker);
    }

    // ---- Querying the schedule ---------------------------------------

    /** Distinct booking sessions; a combined class counts once. */
    public int getTotalBookingCount() {
        return checker.totalBookings();
    }

    /** Section attendances; a combined class with five sections counts five. */
    public int getTotalSectionAttendances() {
        return checker.totalSectionAttendances();
    }

    /** Snapshot of every committed booking. */
    public List<Booking> getAllBookings() {
        return checker.getAllBookings();
    }

    public List<Booking> getBookingsAt(Day day, int period) {
        TimeSlot matching = standardWeekSlots.stream()
            .filter(s -> s.getDay() == day && s.getPeriodNumber() == period)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No such slot: " + day + " period " + period));
        return checker.getBookingsAt(matching);
    }

    public int getFacultyLoad(Faculty faculty) {
        return checker.getFacultyLoad(faculty);
    }

    // ---- Adding a single booking ---------------------------------------

    /** Adds one booking and reports the outcome without throwing checked exceptions. */
    public BookingResult tryAddBooking(Booking booking) {
        try {
            checker.addBooking(booking);
            return BookingResult.success();
        } catch (SchedulingException e) {
            return BookingResult.failure(e.getMessage());
        }
    }

    // ---- Optimization ---------------------------------------------------

    /** Single greedy pass (Weeks 2 behaviour). */
    public OptimizationResult scheduleAll(List<PendingAssignment> pending) {
        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> scheduled = new SchedulingOptimizer(checker, standardWeekSlots)
            .scheduleAll(pending, unscheduled);
        return new OptimizationResult(scheduled, unscheduled);
    }

    /**
     * Multi-pass optimization (Week 3). The same seed always gives the same
     * result, so runs can be reproduced for the report.
     */
    public OptimizationResult scheduleWithRetries(List<PendingAssignment> pending, int maxPasses, long seed) {
        SchedulingOptimizer.PassResult r = new SchedulingOptimizer(
            checker, standardWeekSlots, maxPasses, new Random(seed)).scheduleAll(pending);
        return new OptimizationResult(r.scheduled, r.unscheduled);
    }

    // ---- Reporting ------------------------------------------------------

    public Map<Room, Double> getRoomUtilization(List<Room> rooms) {
        return new UtilizationReport(checker).computeRoomUtilization(rooms, standardWeekSlots);
    }

    public double getAverageRoomUtilization(List<Room> rooms) {
        return new UtilizationReport(checker).computeAverageRoomUtilization(rooms, standardWeekSlots);
    }

    public List<Faculty> getFacultyNearLoadLimit(double thresholdRatio) {
        return new UtilizationReport(checker).findFacultyNearLoadLimit(thresholdRatio);
    }

    /**
     * Population standard deviation of weekly load across a faculty pool.
     * Lower means more evenly distributed. Idle faculty count as zero.
     */
    public double getFacultyLoadSpread(Collection<Faculty> pool) {
        return FacultyBalance.loadStdDev(checker, pool);
    }

    // ---- Result types ----------------------------------------------------

    /** Outcome of a single tryAddBooking call. */
    public static final class BookingResult {
        private final boolean success;
        private final String errorMessage;

        private BookingResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static BookingResult success() { return new BookingResult(true, null); }
        public static BookingResult failure(String message) { return new BookingResult(false, message); }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    /** Outcome of an optimization run: both successes and failures, never silently dropped. */
    public static final class OptimizationResult {
        private final List<Booking> scheduled;
        private final List<PendingAssignment> unscheduled;

        public OptimizationResult(List<Booking> scheduled, List<PendingAssignment> unscheduled) {
            this.scheduled = scheduled;
            this.unscheduled = unscheduled;
        }

        public List<Booking> getScheduled() { return scheduled; }
        public List<PendingAssignment> getUnscheduled() { return unscheduled; }
        public boolean allScheduled() { return unscheduled.isEmpty(); }
    }
}
