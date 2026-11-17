package com.university.scheduler.model;

/**
 * Represents the type of a scheduled period, derived from the suffix
 * letter on course codes in the real dataset (e.g. UCB009P, UCB009T, UCB009L).
 *
 * The institution's convention was confirmed by Member A: L means Lecture.
 */
public enum PeriodType {
    LECTURE,
    TUTORIAL,
    PRACTICAL;

    /**
     * Maps a single-letter course code suffix to a PeriodType.
     * Centralizing this mapping here means both the DB loader (Member A)
     * and the UI (Member C) can reuse the same logic instead of each
     * re-implementing their own guess.
     */
    public static PeriodType fromSuffix(char suffix) {
        switch (Character.toUpperCase(suffix)) {
            case 'T':
                return TUTORIAL;
            case 'P':
                return PRACTICAL;
            case 'L':
                return LECTURE;
            default:
                throw new IllegalArgumentException("Unknown period type suffix: " + suffix);
        }
    }
}
