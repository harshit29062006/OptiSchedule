package com.university.scheduler.engine;

import com.university.scheduler.dao.BookingDAO;
import com.university.scheduler.dao.ConnectionProvider;
import com.university.scheduler.model.Booking;
import java.util.List;
import java.util.Objects;

/** Loads the persisted schedule while keeping JDBC details out of the engine. */
public final class JdbcBookingSource implements BookingSource {
    private final BookingDAO bookings;
    public JdbcBookingSource(ConnectionProvider connections) {
        bookings = new BookingDAO(Objects.requireNonNull(connections, "connections"));
    }
    @Override public List<Booking> loadAll() { return bookings.findAll(); }
}
