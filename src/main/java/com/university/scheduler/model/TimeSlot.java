package com.university.scheduler.model;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Immutable value object representing a single scheduling slot
 * (e.g. Monday, period 2, 8:50-9:40).
 *
 * Used as a HashMap key in ConflictChecker, so equals()/hashCode()
 * correctness matters a lot — see class-level notes below.
 */
public final class TimeSlot {
    private final Day day;
    private final int periodNumber;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public TimeSlot(Day day, int periodNumber, LocalTime startTime, LocalTime endTime) {
        if (day == null) {
            throw new IllegalArgumentException("day cannot be null");
        }
        if (periodNumber <= 0) {
            throw new IllegalArgumentException("periodNumber must be positive");
        }
        this.day = day;
        this.periodNumber = periodNumber;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Day getDay() { return day; }
    public int getPeriodNumber() { return periodNumber; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    /**
     * Equality is based on (day, periodNumber) only, not exact start/end time.
     * Two bookings on the same day and period number are considered the same
     * slot even if minor time-entry variance exists across source sheets.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        TimeSlot t = (TimeSlot) o;
        return periodNumber == t.periodNumber && day == t.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, periodNumber);
    }

    @Override
    public String toString() {
        return day + " P" + periodNumber + (startTime != null ? " @" + startTime : "");
    }
}
