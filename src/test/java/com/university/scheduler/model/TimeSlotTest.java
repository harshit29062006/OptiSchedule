package com.university.scheduler.model;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TimeSlotTest {

    @Test
    void equalSlotsHaveEqualHashCodes() {
        TimeSlot t1 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        TimeSlot t2 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void differentPeriodsAreNotEqual() {
        TimeSlot t1 = new TimeSlot(Day.MONDAY, 1, LocalTime.of(8, 0), LocalTime.of(8, 50));
        TimeSlot t2 = new TimeSlot(Day.MONDAY, 2, LocalTime.of(8, 50), LocalTime.of(9, 40));

        assertNotEquals(t1, t2);
    }

    @Test
    void worksCorrectlyAsHashMapKey() {
        Map<TimeSlot, String> map = new HashMap<>();
        TimeSlot key1 = new TimeSlot(Day.TUESDAY, 3, LocalTime.of(9, 40), LocalTime.of(10, 30));
        map.put(key1, "Room G114 booking");

        TimeSlot lookupKey = new TimeSlot(Day.TUESDAY, 3, LocalTime.of(9, 40), LocalTime.of(10, 30));
        assertTrue(map.containsKey(lookupKey), "Lookup with an equal-but-different TimeSlot instance should find the entry");
    }

    @Test
    void rejectsInvalidPeriodNumber() {
        assertThrows(IllegalArgumentException.class, () ->
            new TimeSlot(Day.MONDAY, 0, LocalTime.of(8, 0), LocalTime.of(8, 50)));
    }
}
