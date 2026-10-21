package com.university.scheduler.exception;

/** Thrown when a room is already booked at the requested TimeSlot. */
public class RoomUnavailableException extends SchedulingException {
    public RoomUnavailableException(String message) {
        super(message);
    }
}
