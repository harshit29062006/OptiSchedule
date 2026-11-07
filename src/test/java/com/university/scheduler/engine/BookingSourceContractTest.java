package com.university.scheduler.engine;

import com.university.scheduler.model.Booking;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract every BookingSource must satisfy (Week 4 Day 5).
 *
 * To test a new source, extend this class and return the source from
 * createSource(). The JDBC implementation (Member A) should do exactly this,
 * so the database version is held to the same rules as the CSV version.
 *
 * Rules:
 *   C1 loadAll never returns null
 *   C2 two calls on the same source return the same number of bookings
 *   C3 every booking has a section, course, faculty, room and slot
 *   C4 loading through ScheduleLoader never accepts more bookings than loadAll returned
 */
public abstract class BookingSourceContractTest {

    /** Supplies the source under test. Called once per test. */
    protected abstract BookingSource createSource() throws Exception;

    @Test
    void c1_loadAllNeverReturnsNull() throws Exception {
        List<Booking> bookings = createSource().loadAll();
        assertNotNull(bookings, "loadAll must return a list, possibly empty, never null");
    }

    @Test
    void c2_loadAllIsRepeatable() throws Exception {
        BookingSource source = createSource();
        int first = source.loadAll().size();
        int second = source.loadAll().size();
        assertEquals(first, second, "the source must give the same rows on every call");
    }

    @Test
    void c3_everyBookingIsComplete() throws Exception {
        for (Booking b : createSource().loadAll()) {
            assertNotNull(b.getSection(), "section");
            assertNotNull(b.getCourse(), "course");
            assertNotNull(b.getFaculty(), "faculty");
            assertNotNull(b.getRoom(), "room");
            assertNotNull(b.getTimeSlot(), "time slot");
        }
    }

    @Test
    void c4_loaderNeverAcceptsMoreThanTheSourceReturned() throws Exception {
        BookingSource source = createSource();
        int offered = source.loadAll().size();
        int accepted = new ScheduleLoader().loadFrom(source).totalBookings();
        assertTrue(accepted <= offered,
            "accepted " + accepted + " must not exceed offered " + offered);
    }
}
