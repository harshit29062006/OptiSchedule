package com.university.scheduler.model;

/**
 * Represents a day of the week for scheduling purposes.
 *
 * Note: our real dataset (college timetable Excel) only ever uses
 * MONDAY–SATURDAY, but SUNDAY is included for completeness and
 * robustness — we should not hardcode assumptions about the data
 * into the type definition itself.
 */
public enum Day {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}
