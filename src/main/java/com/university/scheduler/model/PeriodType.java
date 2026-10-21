package com.university.scheduler.model;

/**
 * Represents the type of a scheduled period, derived from the suffix
 * letter on course codes in the real dataset (e.g. UCB009P, UCB009T, UCB009L).
 *
 * OPEN QUESTION (flagged in SCHEMA.md, confirm with Member A):
 * In the sample data, "L"-suffixed codes (e.g. UCB009L) appeared paired
 * with lab rooms (e.g. LP101), suggesting L = Lab/Practical-style session,
 * NOT "Lecture" as the letter might first suggest. Until confirmed, this
 * enum treats:
 *   T -> TUTORIAL
 *   P -> PRACTICAL
 *   L -> PRACTICAL  (tentative — re-map to LECTURE if Member A confirms
 *                     the institution's convention differs)
 *
 * Update CourseCodeParser (Member A's loader) and this enum together if
 * the convention is confirmed to be different.
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
                // TENTATIVE — see class-level note above.
                return PRACTICAL;
            default:
                throw new IllegalArgumentException("Unknown period type suffix: " + suffix);
        }
    }
}
