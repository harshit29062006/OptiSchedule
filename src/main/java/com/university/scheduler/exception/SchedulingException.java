package com.university.scheduler.exception;

/**
 * Base checked exception for all scheduling-related errors.
 * Checked (extends Exception, not RuntimeException) deliberately —
 * callers must explicitly handle scheduling failures rather than
 * letting them propagate silently.
 */
public class SchedulingException extends Exception {
    public SchedulingException(String message) {
        super(message);
    }

    public SchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}
