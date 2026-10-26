package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin facade over the scheduling engine's internals (ConflictChecker,
 * SchedulingOptimizer, UtilizationReport). This is the intended entry
 * point for anything OUTSIDE the engine package — JavaFX screens (Member
 * C), a future DAO-backed version (Member A), or report-generation code —
 * so those callers never need to touch ConflictChecker or the optimizer
 * directly, and the internal engine design can change without breaking
 * every caller.
 *
 * Today this is backed by CsvBookingSource. When Member A's JdbcBookingSource
 * exists, only the loadFromCsv()-equivalent constructor path needs to change
 * here — everything else (getConflicts, scheduleNew, getUtilizationReport,
 * etc.) stays the same for callers.
 */
public class SchedulingService {
    private final ConflictChecker checker;
    private final List<TimeSlot> standardWeekSlots;

    private SchedulingService(ConflictChecker checker) {
        this.checker = checker;
        this.standardWeekSlots = TimeSlotGenerator.generateStandardWeek();
    }

    /**
     * Creates a service instance loaded from the real timetable CSV.
     * This is today's only loading path (no DB yet) — see class javadoc
     * for how this will extend once Member A's DAO layer exists.
     */
    public static SchedulingService fromCsv(Path csvPath) {
        BookingSource source = new CsvBookingSource(csvPath);
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);
        return new SchedulingService(checker);
    }

    /** Creates a service instance over an already-populated ConflictChecker (useful for tests). */
    public static SchedulingService fromExistingChecker(ConflictChecker checker) {
        return new SchedulingService(checker);
    }

    // ---- Querying the current schedule --------------------------------

    public int getTotalBookingCount() {
        return checker.totalBookings();
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

    // ---- Adding a booking manually (e.g. from a UI "add class" form) --

    /**
     * Attempts to add a single booking, e.g. from a JavaFX form submission.
     * Returns a simple result object instead of making the caller catch
     * checked exceptions directly — keeps UI code simpler.
     */
    public BookingResult tryAddBooking(Booking booking) {
        try {
            checker.addBooking(booking);
            return BookingResult.success();
        } catch (SchedulingException e) {
            return BookingResult.failure(e.getMessage());
        }
    }

    // ---- Running the optimizer -----------------------------------------

    /**
     * Runs the greedy optimizer over a set of pending assignments.
     * Returns both what WAS scheduled and what COULDN'T be, so UI/reporting
     * code can show both outcomes rather than just a pass/fail flag.
     */
    public OptimizationResult scheduleAll(List<PendingAssignment> pending) {
        List<PendingAssignment> unscheduled = new ArrayList<>();
        SchedulingOptimizer optimizer = new SchedulingOptimizer(checker, standardWeekSlots);
        List<Booking> scheduled = optimizer.scheduleAll(pending, unscheduled);
        return new OptimizationResult(scheduled, unscheduled);
    }

    // ---- Reporting -------------------------------------------------------

    public Map<Room, Double> getRoomUtilization(List<Room> rooms) {
        return new UtilizationReport(checker).computeRoomUtilization(rooms, standardWeekSlots);
    }

    public double getAverageRoomUtilization(List<Room> rooms) {
        return new UtilizationReport(checker).computeAverageRoomUtilization(rooms, standardWeekSlots);
    }

    public List<Faculty> getFacultyNearLoadLimit(double thresholdRatio) {
        return new UtilizationReport(checker).findFacultyNearLoadLimit(thresholdRatio);
    }

    // ---- Nested result types ---------------------------------------------

    /** Outcome of a single tryAddBooking() call — avoids forcing callers to catch checked exceptions. */
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

    /** Outcome of a scheduleAll() run — both the successes and the failures, never silently dropped. */
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
