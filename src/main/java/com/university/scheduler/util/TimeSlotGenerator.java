package com.university.scheduler.util;

import com.university.scheduler.model.Day;
import com.university.scheduler.model.TimeSlot;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the standard weekly grid of TimeSlots, matching the real
 * college timetable's structure: Monday-Saturday, periods 1 through
 * periodsPerDay (observed as high as 14 in the real data: 7 periods
 * before lunch, 7 after).
 *
 * This does NOT try to reconstruct exact start/end times for every
 * period (the real data's period lengths vary slightly) — it generates
 * a plausible, evenly-spaced grid good enough for the optimization
 * engine to search over. If exact real period times are needed later,
 * extend this to accept a lookup table instead of computing times.
 */
public class TimeSlotGenerator {

    private static final Day[] WORKING_DAYS = {
        Day.MONDAY, Day.TUESDAY, Day.WEDNESDAY, Day.THURSDAY, Day.FRIDAY, Day.SATURDAY
    };

    /**
     * Generates all (day, period) slots for a standard week.
     *
     * @param periodsPerDay number of periods per day (real data showed up to 14)
     * @param firstPeriodStart start time of period 1 each day
     * @param periodLengthMinutes length of each period in minutes (used to space out generated times)
     */
    public static List<TimeSlot> generateWeek(int periodsPerDay, LocalTime firstPeriodStart, int periodLengthMinutes) {
        List<TimeSlot> slots = new ArrayList<>();
        for (Day day : WORKING_DAYS) {
            LocalTime current = firstPeriodStart;
            for (int period = 1; period <= periodsPerDay; period++) {
                LocalTime end = current.plusMinutes(periodLengthMinutes);
                slots.add(new TimeSlot(day, period, current, end));
                current = end;
            }
        }
        return slots;
    }

    /**
     * Convenience overload matching the real dataset's observed structure:
     * 14 periods/day, starting 08:00, 50-minute periods.
     */
    public static List<TimeSlot> generateStandardWeek() {
        return generateWeek(14, LocalTime.of(8, 0), 50);
    }
}
