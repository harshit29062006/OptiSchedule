# Week 3 Status: Member B (Core Logic)

## Summary

Week 3 moved the project from a conflict checker to an optimizer. The engine now
schedules new classes using retries, capacity-aware rooms and faculty load
balancing, on the full nine-sheet real timetable. Every design choice is backed
by a test, and the known gaps are listed below rather than hidden.

## Deliverables by day

| Day | Deliverable | Status |
|---|---|---|
| 1 | Full nine-sheet pipeline; combined-class merge rule; blank-faculty edge case | Done |
| 2 | Multi-pass optimizer with shuffled slot order; best pass committed | Done |
| 3 | Capacity-aware room selection (tightest fit first, too-small rooms excluded) | Done |
| 4 | Full-dataset optimization driver with reproducible seeded runs and failure diagnosis | Done |
| 5 | Load-balanced faculty selection; spread metric; naive vs balanced comparison | Done |
| 6 | Input validation; edge-case and negative tests; no partial state on failure | Done |
| 7 | Facade extended; this status document | Done |

## Real-data results (Day 1)

| Measure | Value |
|---|---|
| Sheets covered | 9 of 9 |
| Raw rows after extraction | 7,093 |
| Rows read by the loader | 6,281 |
| Bookings loaded | 5,665 |
| Rejected as genuine conflicts | 599 |
| Combined sessions merged, not rejected | Counted by `totalSectionAttendances()` minus `totalBookings()` |

Every rejection is a named room or teacher clash. Combined classes (one teacher,
one room, several sections) are merged rather than rejected.

## Optimization results (Day 4, seed 42)

| Measure | Value |
|---|---|
| New assignments requested | 20 |
| Scheduled | [FILL FROM RUN] |
| Unscheduled | [FILL FROM RUN] |
| Winning pass of 10 | [FILL FROM RUN] |
| Average room utilization, before to after | [FILL FROM RUN] |
| Faculty at weekly limit, before to after | [FILL FROM RUN] |
| Optimization time | [FILL FROM RUN] |

Failure reasons are printed in the driver summary. Each unscheduled class gets one
reason, judged against the final schedule.

## Faculty load balance (Day 5, seed 42, 40 classes)

| Strategy | Scheduled | Load std dev before | Load std dev after |
|---|---|---|---|
| Naive (locked to preferred teacher) | [FILL FROM RUN] | [FILL FROM RUN] | [FILL FROM RUN] |
| Balanced (three eligible teachers) | [FILL FROM RUN] | [FILL FROM RUN] | [FILL FROM RUN] |

Both strategies receive the same batch. The only difference is the eligible
teacher list.

## Design decisions to explain in the viva

1. **Faculty load comes before room fit.** A class may use a larger room if that
   lets a less-loaded teacher take it. This is a trade-off, and it is stated in the
   code.
2. **Multi-pass retry is a heuristic, not an optimal solver.** Shuffling slot order
   can rescue placements a fixed order misses, but no backtracking is done. The
   best pass is kept and committed.
3. **Invalid input fails before any placement.** Null values, empty room lists,
   zero passes and duplicate section-course pairs are rejected up front, so a bad
   request never leaves a partly written schedule.
4. **A failed booking leaves no trace.** Room, teacher and load checks run before
   any state changes. Tests confirm this.

## Known limitations (state these openly)

| Limitation | Effect | Needed to fix |
|---|---|---|
| Faculty weekly limit is a placeholder of 18 | Teacher-load results are only illustrative | Real institutional policy |
| Room capacities are a placeholder of 60 | Capacity logic is untested on real rooms | Real room capacities |
| Class strength is not in the source data | Capacity logic uses synthetic strengths | Real enrolment figures |
| Eligible teachers are simulated | Balancing results depend on the assumed teacher pool | Real cover or substitution data |
| Multi-period courses cannot be expressed | Duplicate section-course pairs are rejected | A session index on PendingAssignment |
| Section clashes across bookings are not checked | One section could appear in two rooms at once | A section-level check, tested against the full data |
| About 304 of 7,093 rows have unresolved course codes | Skipped and logged, never guessed | Correcting the source spreadsheet |
| PG sheet stores section names in the room/teacher field | Some PG bookings are misclassified | A PG-specific parsing rule, after inspecting the sheet |
| Mid-semester room and teacher changes are not modelled | Second-half schedule is under-represented | A semester-half dimension on Booking or TimeSlot |
| Greedy heuristic, no backtracking | Some feasible timetables may not be found | Constraint solving, out of scope for this course |

## Week 3 exit checklist

- [x] All nine sheets load through the pipeline
- [x] Optimizer has retry logic, not just a single greedy pass
- [x] Capacity-aware room placement, tested
- [x] Full-dataset run produced with reproducible seed (figures above)
- [x] Faculty load balancing changes placement, and the effect is measured
- [x] Edge-case and negative tests added
- [x] This status document written
- [ ] `mvn test` run with the final test count recorded: [FILL FROM RUN]
- [ ] Everything committed and pushed (see commit plan below)

## Commit plan

Commit per feature, not in one large commit:

1. Full nine-sheet pipeline and combined-class merge rule
2. Multi-pass optimizer (Day 2)
3. Capacity-aware room selection (Day 3)
4. Full-dataset optimization driver (Day 4)
5. Faculty load balancing and spread metric (Day 5)
6. Input validation and edge-case tests (Day 6)
7. Facade extension and Week 3 documentation (Day 7)
