package com.university.scheduler.exception;

/** Thrown when a faculty member is already booked at the requested TimeSlot. */
public class SlotConflictException extends SchedulingException {
    public SlotConflictException(String message) {
        super(message);
    }
}
