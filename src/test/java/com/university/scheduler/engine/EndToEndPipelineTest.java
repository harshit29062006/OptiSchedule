package com.university.scheduler.engine;

import com.university.scheduler.model.Booking;
import com.university.scheduler.model.Faculty;
import com.university.scheduler.model.Room;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: proves the full real-data pipeline works together,
 * not just each piece in isolation.
 *
 *   CsvBookingSource -> ScheduleLoader -> ConflictChecker -> UtilizationReport
 *
 * Unlike the other test classes this week, this one deliberately uses NO
 * hand-built synthetic objects — every Room, Faculty, Booking here comes
 * from the real timetable CSV. If this test passes, it's genuine evidence
 * the whole chain works on real data, not just that each class works in
 * isolation against data built to be convenient.
 *
 * KNOWN LIMITATION (carried from Week 1/2): room and faculty identity both
 * currently come from the unsplit room_faculty_raw field, so room-specific
 * assertions here are sanity checks on PIPELINE CORRECTNESS, not yet
 * trustworthy real-world room utilization numbers. See CsvBookingSource's
 * class javadoc for the open fix.
 */
class EndToEndPipelineTest {

    private static final Path CSV_PATH = Path.of("src/main/resources/data/parsed_timetable_sample.csv");

    @Test
    void fullPipelineLoadsRealDataAndProducesSaneUtilization() {
        BookingSource source = new CsvBookingSource(CSV_PATH);
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);

        assertTrue(checker.totalBookings() > 0, "Pipeline should load a nonzero number of real bookings");

        // Derive the actual set of rooms seen in the loaded data, rather than
        // hardcoding a guessed list — keeps this test valid even as the CSV
        // or the cleaning pipeline changes.
        Set<Room> roomsSeen = collectDistinctRooms(checker);
        assertFalse(roomsSeen.isEmpty(), "Should have discovered at least one room from real bookings");

        List<com.university.scheduler.model.TimeSlot> weekSlots = TimeSlotGenerator.generateStandardWeek();
        UtilizationReport report = new UtilizationReport(checker);

        Map<Room, Double> utilization = report.computeRoomUtilization(List.copyOf(roomsSeen), weekSlots);

        for (Double ratio : utilization.values()) {
            assertTrue(ratio >= 0.0 && ratio <= 1.0,
                "Every utilization ratio must be a valid fraction between 0 and 1");
        }

        double avg = report.computeAverageRoomUtilization(List.copyOf(roomsSeen), weekSlots);
        assertTrue(avg >= 0.0 && avg <= 1.0, "Average utilization must be a valid fraction");

        System.out.println("Integration check: " + checker.totalBookings() + " real bookings loaded, "
            + roomsSeen.size() + " distinct rooms discovered, average utilization "
            + String.format("%.1f%%", avg * 100) + " (see known limitation note in test javadoc).");
    }

    @Test
    void fullPipelineFacultyLoadsAreInternallyConsistent() {
        BookingSource source = new CsvBookingSource(CSV_PATH);
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);

        UtilizationReport report = new UtilizationReport(checker);
        Map<Faculty, Double> loadRatios = report.computeFacultyLoadRatio();

        assertFalse(loadRatios.isEmpty(), "Should have discovered at least one faculty from real bookings");

        for (Map.Entry<Faculty, Double> entry : loadRatios.entrySet()) {
            double ratio = entry.getValue();
            assertTrue(ratio >= 0.0,
                "Faculty load ratio cannot be negative: " + entry.getKey() + " = " + ratio);
            // NOTE: ratio can legitimately exceed 1.0 only if a faculty was somehow
            // loaded beyond their max — which addBooking() should have prevented via
            // FacultyOverloadException. If this assertion ever fails, it signals a
            // real bug in the load-limit enforcement, not just messy data.
            assertTrue(ratio <= 1.0,
                "Faculty load ratio should never exceed 1.0 if FacultyOverloadException is enforced correctly: "
                + entry.getKey() + " = " + ratio);
        }

        long nearLimitCount = report.findFacultyNearLoadLimit(0.9).size();
        System.out.println("Integration check: " + loadRatios.size() + " distinct faculty discovered, "
            + nearLimitCount + " at or above 90% of their weekly limit.");
    }

    /**
     * Derives the distinct Room objects actually present in the checker's
     * bookings. ConflictChecker doesn't currently expose "all bookings"
     * directly, so this walks the standard week's slots and collects rooms
     * seen — consistent with how UtilizationReport itself queries bookings.
     */
    private Set<Room> collectDistinctRooms(ConflictChecker checker) {
        List<com.university.scheduler.model.TimeSlot> weekSlots = TimeSlotGenerator.generateStandardWeek();
        return weekSlots.stream()
            .flatMap(slot -> checker.getBookingsAt(slot).stream())
            .map(Booking::getRoom)
            .collect(Collectors.toSet());
    }
}
