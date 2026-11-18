package com.university.scheduler.engine;

import com.university.scheduler.model.Booking;

public interface Schedulable {
    /** Returns true if adding this booking would conflict with an existing one. */
    boolean hasConflict(Booking newBooking);
}
