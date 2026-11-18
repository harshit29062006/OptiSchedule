package com.university.scheduler.engine;

import com.university.scheduler.model.Booking;
import java.util.List;

/**
 * Abstraction over "where bookings come from". Today implemented by
 * CsvBookingSource (Member B, standalone, no DB needed). Member A can
 * later add a JdbcBookingSource implementing this same interface —
 * ConflictChecker and everything built on it won't need to change.
 */
public interface BookingSource {
    List<Booking> loadAll();
}
