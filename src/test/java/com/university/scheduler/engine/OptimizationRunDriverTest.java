package com.university.scheduler.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks invariants of the full-dataset run rather than exact numbers,
 * so the tests survive data changes. Exact figures are reported by
 * OptimizationRunDriver.main() for the report.
 */
class OptimizationRunDriverTest {

    @Test
    void everyRequestedAssignmentIsAccountedFor() {
        OptimizationRunDriver.RunReport r =
            OptimizationRunDriver.run(OptimizationRunDriver.DEFAULT_DATA, 20, 42L);

        assertEquals(20, r.scheduledCount + r.unscheduledCount,
            "each requested assignment must be either scheduled or unscheduled, never lost");
        assertEquals(r.unscheduledCount,
            r.failureReasons.values().stream().mapToInt(Integer::intValue).sum(),
            "every unscheduled assignment must have a diagnosed reason");
    }

    @Test
    void utilizationNeverDecreasesAndStaysAValidFraction() {
        OptimizationRunDriver.RunReport r =
            OptimizationRunDriver.run(OptimizationRunDriver.DEFAULT_DATA, 20, 42L);

        assertTrue(r.utilizationAfter >= r.utilizationBefore,
            "adding bookings can only raise room utilization");
        assertTrue(r.utilizationAfter >= 0.0 && r.utilizationAfter <= 1.0);
    }

    @Test
    void facultyAtLimitNeverDecreases() {
        OptimizationRunDriver.RunReport r =
            OptimizationRunDriver.run(OptimizationRunDriver.DEFAULT_DATA, 20, 42L);

        assertTrue(r.facultyAtLimitAfter >= r.facultyAtLimitBefore,
            "loads only grow during a run, so the count at the limit cannot fall");
    }

    @Test
    void sameSeedGivesSameResult() {
        OptimizationRunDriver.RunReport a =
            OptimizationRunDriver.run(OptimizationRunDriver.DEFAULT_DATA, 10, 7L);
        OptimizationRunDriver.RunReport b =
            OptimizationRunDriver.run(OptimizationRunDriver.DEFAULT_DATA, 10, 7L);

        assertEquals(a.scheduledCount, b.scheduledCount, "runs must be reproducible for the report");
        assertEquals(a.failureReasons, b.failureReasons);
    }
}
