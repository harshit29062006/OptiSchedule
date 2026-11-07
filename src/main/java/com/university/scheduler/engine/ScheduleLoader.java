package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.Booking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Loads all bookings from a BookingSource into a fresh ConflictChecker.
 * Rejected bookings are logged at WARN with the reason. The load summary is
 * logged at INFO. Nothing is written to System.out.
 */
public class ScheduleLoader {

    private static final Logger LOG = LogManager.getLogger(ScheduleLoader.class);

    public ConflictChecker loadFrom(BookingSource source) {
        ConflictChecker checker = new ConflictChecker();
        List<Booking> bookings = source.loadAll();
        int rejected = 0;

        for (Booking b : bookings) {
            try {
                checker.addBooking(b);
            } catch (SchedulingException e) {
                rejected++;
                LOG.warn("rejected booking: {}", e.getMessage());
            }
        }

        LOG.info("load complete: {} rows read, {} loaded, {} rejected",
            bookings.size(), checker.totalBookings(), rejected);
        return checker;
    }
}
