package com.university.scheduler.engine;

import com.university.scheduler.model.Booking;
import com.university.scheduler.exception.SchedulingException;
import java.util.List;

/**
 * Loads all bookings from a given BookingSource into a fresh ConflictChecker.
 * Works with ANY BookingSource implementation (CsvBookingSource today,
 * a future JdbcBookingSource later) — this class never changes when the
 * data source changes.
 */
public class ScheduleLoader {

    public ConflictChecker loadFrom(BookingSource source) {
        ConflictChecker checker = new ConflictChecker();
        List<Booking> bookings = source.loadAll();
        int conflicts = 0;

        for (Booking b : bookings) {
            try {
                checker.addBooking(b);
            } catch (SchedulingException e) {
                conflicts++;
                System.out.println("Conflict skipped: " + e.getMessage());
            }
        }

        System.out.println("Loaded " + checker.totalBookings() + " bookings successfully, "
            + conflicts + " rows rejected due to conflicts, out of " + bookings.size() + " total rows read.");

        return checker;
    }
}
