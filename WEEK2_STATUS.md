# Week 2 Status — Member B (Core Logic)

## What was built, in order

| Day | Deliverable | Status |
|---|---|---|
| 1 | `BookingSource`/`CsvBookingSource`/`ScheduleLoader` — standalone real-data pipeline, no DB dependency | ✅ Done |
| 2 | Faculty weekly load tracking + `FacultyOverloadException` actually enforced | ✅ Done, tested |
| 3 | `findAvailableSlot` + `TimeSlotGenerator` | ✅ Done, tested |
| 4 | `SchedulingOptimizer` — greedy, constraint-tightness-first scheduler + `PendingAssignment` | ✅ Done, tested |
| 5 | `UtilizationReport` — room utilization, faculty load ratios | ✅ Done, tested |
| 6 | `EndToEndPipelineTest` — integration test, full pipeline against real CSV | ✅ Done, passing |
| 7 | `SchedulingService` — facade for UI/other layers to call | ✅ Done, tested |
| 7+ | **Room/faculty split fix** — found, diagnosed, and resolved a real data-corruption bug (see below) | ✅ Done, verified |

Total test count: **39**, all passing, `BUILD SUCCESS`.

## The room/faculty split — found, diagnosed, fixed (the week's biggest real finding)

This was flagged as a known placeholder gap since Week 1 and left deliberately unresolved through most of Week 2 so other logic could be built and tested independently. By Day 7 it had become serious enough to fix immediately: the placeholder (same raw string used as both room ID and faculty ID) was actively corrupting results, not just imprecise.

**The bug, concretely:** `splitRoomFaculty()` originally returned the same raw string for both room and faculty. Every class ever held in a given room (e.g. `LP101`), taught by many different real faculty over the week, was counted as **one single "faculty member" named LP101**, hitting the 18-hour weekly cap almost immediately. This silently rejected 300 of 1,466 real rows as false `FacultyOverloadException` conflicts.

**Diagnosis process:** sampled raw `room_faculty_raw` values, found three distinct patterns, and confirmed each one directly against the real data's actual meaning:

| Pattern | Example | Confirmed meaning |
|---|---|---|
| `CODE(CODE)` | `CBOL(G141)` | Both sides name the SAME room (building/wing code + room number). Parenthesized part used as canonical room ID. |
| Bare code | `PWS`, `SBH`, `APC7` | Faculty initials |
| Bare code | `E305`, `LP101`, `T105` | Room code (building-letter prefix + digits) |
| `/`-separated | `H222/C201`, `NIN/APC3` | Mid-semester change: first value = before mid-sem, second = after. First value used as canonical (see limitation below). |

**Fix applied in `CsvBookingSource.splitRoomFaculty()`:** pattern-matches each raw value against these confirmed rules; any genuinely unresolved side gets a **unique synthetic ID per row** (never a shared placeholder), so unrelated classes can never be falsely merged into one faculty/room identity again.

**Verified result (before → after):**

| Metric | Broken (placeholder) | Fixed |
|---|---|---|
| Bookings loaded | 1,166 | **1,428** |
| Rows rejected | 300 (mostly false) | **38 (all genuine conflicts)** |
| False `FacultyOverloadException` | Massive, dominant cause | Essentially gone |

The remaining 38 rejected rows are now real: genuine room double-bookings (e.g. `LP102`, `E311`, `G325`) and two genuine faculty double-bookings (`RCH-RA`, `RKU`).

## Known, documented limitation carried forward

**Mid-semester room/faculty changes are not modeled.** The `/`-separated source values represent a real scheduling change partway through the term, but `Booking`/`TimeSlot` currently have no semester-half dimension — a booking is fixed for the whole term. The pre-mid-semester value is used as the sole canonical identifier. This under-represents the second half of the real schedule. Properly modeling this would require adding a semester-half concept to the domain model — a real, scoped piece of future work, not a quick fix.

## Other open issues, still accurate

1. **~78 `FIRST YEAR B` rows remain unparseable** (non-standard course-code values, isolated to that sheet's layout quirks) — genuinely ambiguous, not force-fixed.
2. **`TimeSlotGenerator.generateStandardWeek()` guesses the grid** (14 periods/day, 50-min periods, 08:00 start) from observed patterns, not a confirmed real period-time table.
3. **Default `maxHoursPerWeek = 18`** is a placeholder, not a confirmed institutional policy number.
4. **9 sheets exist in the source Excel; only 2 (`FIRST YEAR A`/`B`) have been imported.** Pipeline is proven; extending to the remaining 7 sheets is mechanical repetition, not new design work.
5. **No DB/JDBC integration yet** — by design (Day 1), built standalone so it wasn't blocked on Member A. `SchedulingService` is structured so a future `JdbcBookingSource` can slot in without changing caller code.

## What's ready to hand off, if/when A or C contribute

- **To Member A (DB/DAO):** implement `BookingSource` backed by JDBC instead of CSV.
- **To Member C (UI):** call `SchedulingService` methods directly — no need to touch `ConflictChecker`/`SchedulingOptimizer` internals.

## Recommended next move

Week 2's core logic is now genuinely trustworthy against real data, not just internally consistent. Ready to move to Week 3 (deeper optimization work, or extending the pipeline to the remaining 7 sheets).
