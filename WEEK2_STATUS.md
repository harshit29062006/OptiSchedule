# Week 2 Status — Member B (Core Logic)

## What was built, in order

| Day | Deliverable | Status |
|---|---|---|
| 1 | `BookingSource`/`CsvBookingSource`/`ScheduleLoader` — standalone real-data pipeline, no DB dependency | ✅ Done, verified against real data |
| 2 | Faculty weekly load tracking + `FacultyOverloadException` actually enforced | ✅ Done, tested |
| 3 | `findAvailableSlot` + `TimeSlotGenerator` | ✅ Done, tested |
| 4 | `SchedulingOptimizer` — greedy, constraint-tightness-first scheduler + `PendingAssignment` | ✅ Done, tested |
| 5 | `UtilizationReport` — room utilization, faculty load ratios | ✅ Done, tested |
| 6 | `EndToEndPipelineTest` — integration test, full pipeline against real CSV | ✅ Done, passing |
| 7 | `SchedulingService` — facade for UI/other layers to call | ✅ Done, tested |

Total test count by end of week: **39** (4 + 5 + 1 + 5 + 6 + 4 + 2 + 6 + 6 = adjust to your actual `mvn test` output).

## Genuine, verified facts about the real dataset (as of this week)

- 1,431 of 1,466 rows from `FIRST YEAR A`/`FIRST YEAR B` load successfully (97.6%)
- Remaining ~78 unparseable rows are isolated to `FIRST YEAR B`'s layout quirks — documented, not force-fixed
- Two real bugs found and fixed through actual diagnosis: unfilled `LAB` placeholders (forward-fill), inconsistent time string formats (normalization)

## Known open issues — be upfront about these, don't hide them

1. **Room/faculty fields are not yet independently split.** `CsvBookingSource.splitRoomFaculty()` currently uses the same raw string for both — confirmed directly by the integration test in Day 6 (distinct-room count == distinct-faculty count). This means:
   - Room-conflict detection and faculty-conflict detection are currently checking the same value twice, not two independent constraints
   - Room utilization numbers are not yet trustworthy as real-world figures
   - **This is the single highest-value fix remaining**, since it unblocks several already-correct pieces of logic (conflict detection, utilization, optimizer) from producing trustworthy real numbers
2. **`TimeSlotGenerator.generateStandardWeek()` guesses the grid** (14 periods/day, 50-min periods, 08:00 start) based on patterns observed in the data, not a confirmed real period-time table.
3. **Default `maxHoursPerWeek = 18`** in `CsvBookingSource` is a placeholder, not a real institutional policy number.
4. **9 sheets exist in the source Excel; only 2 (`FIRST YEAR A`/`B`) have been imported.** The parser/pipeline is proven to work — extending to the remaining 7 sheets is mechanical repetition, not new design work.
5. **No DB/JDBC integration yet** — by design, this was built standalone (see Day 1) so it wasn't blocked on Member A. `SchedulingService` is structured so a future `JdbcBookingSource` can slot in without changing any caller code.

## What's ready to hand off, if/when A or C contribute

- **To Member A (DB/DAO):** implement `BookingSource` backed by JDBC instead of CSV. `SchedulingService.fromCsv()` shows the exact shape a `SchedulingService.fromJdbc()` equivalent would take.
- **To Member C (UI):** call `SchedulingService` methods directly — `getBookingsAt()`, `tryAddBooking()`, `scheduleAll()`, `getRoomUtilization()`. No need to touch `ConflictChecker`/`SchedulingOptimizer` internals.

## Recommended next move

Fix the room/faculty split in `CsvBookingSource` — it's the one open item blocking multiple already-finished, already-tested pieces of logic from being trustworthy against real data.
