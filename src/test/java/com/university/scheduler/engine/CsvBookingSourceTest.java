package com.university.scheduler.engine;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CsvBookingSourceTest {

    @Test
    void loadsRealDataWithoutCrashing() {
        BookingSource source = new CsvBookingSource(
            Path.of("src/main/resources/data/parsed_timetable_sample.csv"));
        ScheduleLoader loader = new ScheduleLoader();
        ConflictChecker checker = loader.loadFrom(source);

        assertTrue(checker.totalBookings() > 0,
            "Should load at least some bookings from the real timetable data");
    }
}
