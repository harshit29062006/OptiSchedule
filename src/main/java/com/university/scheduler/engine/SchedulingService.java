
package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import com.university.scheduler.dao.ConnectionProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Thin facade over the scheduling engine's internals.
 *
 * Provides a common entry point for JavaFX screens, DAO-backed
 * operations, scheduling and reporting.
 */
public class SchedulingService {

    private final ConflictChecker checker;
    private final List<TimeSlot> standardWeekSlots;

    private SchedulingService(ConflictChecker checker) {
        this.checker = checker;
        this.standardWeekSlots =
                TimeSlotGenerator.generateStandardWeek();
    }

    // --------------------------------------------------
    // Factory methods
    // --------------------------------------------------

    public static SchedulingService fromCsv(Path csvPath) {
        BookingSource source = new CsvBookingSource(csvPath);
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);
        return new SchedulingService(checker);
    }

    public static SchedulingService fromJdbc() {
        BookingSource source = new JdbcBookingSource();
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);
        return new SchedulingService(checker);
    }

    public static SchedulingService fromJdbc(
            ConnectionProvider connectionProvider) {

        BookingSource source = new JdbcBookingSource(connectionProvider);
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);
        return new SchedulingService(checker);
    }

    public static SchedulingService fromExistingChecker(
            ConflictChecker checker) {

        if (checker == null) {
            throw new IllegalArgumentException("checker cannot be null");
        }

        return new SchedulingService(checker);
    }

    // --------------------------------------------------
    // Booking information
    // --------------------------------------------------

    public int getTotalBookingCount() {
        return checker.totalBookings();
    }

    public List<Booking> getAllBookings() {
        return Collections.unmodifiableList(
                new ArrayList<>(checker.getAllBookings())
        );
    }

    public List<Booking> getBookingsAt(Day day, int period) {

        TimeSlot matching = standardWeekSlots.stream()
                .filter(s ->
                        s.getDay() == day &&
                        s.getPeriodNumber() == period)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No such slot: " + day + " period " + period
                ));

        return checker.getBookingsAt(matching);
    }

    public int getTotalSectionAttendances() {
        return checker.totalSectionAttendances();
    }

    // --------------------------------------------------
    // Faculty workload
    // --------------------------------------------------

    public int getFacultyLoad(Faculty faculty) {
        return checker.getFacultyLoad(faculty);
    }

    /**
     * Calculates the difference between the maximum
     * and minimum faculty workloads.
     *
     * Returns zero for an empty faculty list.
     */
    public double getFacultyLoadSpread(List<Faculty> faculties) {

        if (faculties == null || faculties.isEmpty()) {
            return 0.0;
        }

        int minLoad = Integer.MAX_VALUE;
        int maxLoad = Integer.MIN_VALUE;

        for (Faculty faculty : faculties) {

            int load = checker.getFacultyLoad(faculty);

            minLoad = Math.min(minLoad, load);
            maxLoad = Math.max(maxLoad, load);
        }

        return (double) (maxLoad - minLoad);
    }

    // --------------------------------------------------
    // Booking operations
    // --------------------------------------------------

    public BookingResult tryAddBooking(Booking booking) {

        try {
            checker.addBooking(booking);
            return BookingResult.success();

        } catch (SchedulingException e) {
            return BookingResult.failure(e.getMessage());
        }
    }

    // --------------------------------------------------
    // Scheduling
    // --------------------------------------------------

    /**
     * Schedules all pending assignments using one optimizer pass.
     */
    public OptimizationResult scheduleAll(
            List<PendingAssignment> pending) {

        SchedulingOptimizer optimizer =
                new SchedulingOptimizer(checker, standardWeekSlots);

        SchedulingOptimizer.PassResult result =
                optimizer.scheduleAll(pending);

        return new OptimizationResult(
                result.scheduled,
                result.unscheduled
        );
    }

    /**
     * Attempts scheduling using multiple independent passes.
     *
     * Each pass uses a different ordering of assignments and
     * time slots. The pass with the greatest number of scheduled
     * assignments is committed to the original checker.
     *
     * The seed makes the retry sequence reproducible.
     *
     * @param pending assignments that need scheduling
     * @param maxRetries number of optimizer passes
     * @param seed random seed for reproducibility
     */
    public OptimizationResult scheduleWithRetries(
            List<PendingAssignment> pending,
            int maxRetries,
            long seed) {

        if (maxRetries < 1) {
            throw new IllegalArgumentException(
                    "maxRetries must be at least 1"
            );
        }

        SchedulingOptimizer optimizer =
                new SchedulingOptimizer(
                        checker,
                        standardWeekSlots,
                        maxRetries,
                        new Random(seed)
                );

        SchedulingOptimizer.PassResult result =
                optimizer.scheduleAll(pending);

        return new OptimizationResult(
                result.scheduled,
                result.unscheduled
        );
    }

    // --------------------------------------------------
    // Utilization reports
    // --------------------------------------------------

    public Map<Room, Double> getRoomUtilization(List<Room> rooms) {

        return new UtilizationReport(checker)
                .computeRoomUtilization(rooms, standardWeekSlots);
    }

    public double getAverageRoomUtilization(List<Room> rooms) {

        return new UtilizationReport(checker)
                .computeAverageRoomUtilization(rooms, standardWeekSlots);
    }

    public List<Faculty> getFacultyNearLoadLimit(
            double thresholdRatio) {

        return new UtilizationReport(checker)
                .findFacultyNearLoadLimit(thresholdRatio);
    }

    // --------------------------------------------------
    // Result classes
    // --------------------------------------------------

    public static class BookingResult {

        private final boolean success;
        private final String message;

        private BookingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static BookingResult success() {
            return new BookingResult(true, null);
        }

        public static BookingResult failure(String message) {
            return new BookingResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
        public String getErrorMessage() {
            return message;
        }
    }

    public static class OptimizationResult {

        private final List<Booking> scheduled;
        private final List<PendingAssignment> unscheduled;

        public OptimizationResult(
                List<Booking> scheduled,
                List<PendingAssignment> unscheduled) {

            this.scheduled = List.copyOf(scheduled);
            this.unscheduled = List.copyOf(unscheduled);
        }

        public List<Booking> getScheduled() {
            return scheduled;
        }

        public List<PendingAssignment> getUnscheduled() {
            return unscheduled;
        }

        public boolean allScheduled() {
            return unscheduled.isEmpty();
        }
    }
}
