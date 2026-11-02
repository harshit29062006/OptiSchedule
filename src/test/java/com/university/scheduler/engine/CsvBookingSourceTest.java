package com.university.scheduler.engine;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the CSV-to-Booking pipeline against the FULL real dataset
 * (all 9 sheets: FIRST/2ND/3RD/4TH YEAR A & B, plus PG), as of Week 3 Day 1.
 *
 * Previously (Week 1/2) this test ran against only FIRST YEAR A/B (2 of 9
 * sheets). The data file has been extended at the source (Week 3 Day 1
 * extraction + cleaning), so this test now exercises the pipeline at
 * realistic full-institution scale.
 *
 * KNOWN DATA LIMITATION (see WEEK3_STATUS.md): ~4.3% of rows across every
 * sheet have an unresolved course code due to a recurring column-alignment
 * issue in certain sub-batch columns. These rows are safely skipped (not
 * guessed), which is why the loaded count is meaningfully less than the
 * total row count — that gap is expected, not a bug.
 */
class CsvBookingSourceTest {

    private static final Path FULL_DATASET_PATH =
        Path.of("src/main/resources/data/all_sheets_cleaned.csv");

    @Test
    void loadsFullRealDatasetWithoutCrashing() {
        BookingSource source = new CsvBookingSource(FULL_DATASET_PATH);
        ScheduleLoader loader = new ScheduleLoader();
        ConflictChecker checker = loader.loadFrom(source);

        assertTrue(checker.totalBookings() > 0,
            "Should load a nonzero number of real bookings from the full dataset");
    }

    @Test
    void loadedCountIsWithinExpectedRangeForFullDataset() {
        // Sanity bound, not an exact pin: the full cleaned dataset has 7,093
        // rows; after skipping unresolved-course-code rows (~4.3%) and any
        // genuine room/faculty conflicts, the loaded count should land
        // comfortably above half the raw row count. A big unexplained drop
        // below this would signal a real regression worth investigating,
        // not just "the data is messy" — that's already accounted for.
        BookingSource source = new CsvBookingSource(FULL_DATASET_PATH);
        ConflictChecker checker = new ScheduleLoader().loadFrom(source);

        int loaded = checker.totalBookings();
        assertTrue(loaded > 3500,
            "Expected a substantial majority of the ~7,093 raw rows to load successfully; got " + loaded
            + ". If this is lower than expected, check for a new parsing regression rather than assuming it's the known data-quality gap.");
    }
}
