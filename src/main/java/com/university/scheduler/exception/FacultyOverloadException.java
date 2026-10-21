package com.university.scheduler.exception;

/** Thrown when assigning a booking would push a faculty member over their maxHoursPerWeek. */
public class FacultyOverloadException extends SchedulingException {
    public FacultyOverloadException(String message) {
        super(message);
    }
}
