# Scheduling Engine

## 1. Purpose

The engine checks an institutional timetable for clashes, places new classes into free slots, and reports the load and utilization that result. It is built to run on real data: nine sheets of the college's own July to December 2026 timetable.

## 2. Domain model

Each scheduled class is a `Booking`: one course, taught by one faculty member, in one room, at one time slot, attended by one or more sections. Faculty, rooms, courses, sections and time slots are immutable value objects, compared by their identifying code. Two bookings for the same teacher and room at one slot are one combined session.

## 3. Conflict rules

| Situation | Outcome |
|---|---|
| Same room, different teacher, same slot | Rejected: room double-booked |
| Same teacher, different room, same slot | Rejected: teacher cannot be in two rooms |
| Same teacher, same room, same slot | Merged: one combined session, sections joined |
| Teacher beyond weekly limit | Rejected: overload |

A rejected booking leaves no trace. All checks run before any state changes.

## 4. Data cleaning

The source timetable was built for humans to read on a wall, not for machines. Three problems had to be resolved:

1. **Merged cells and shifted columns.** Some sheets have a duplicate column that moves all data one place right. Column positions are found from the header, not fixed in code.
2. **Placeholder rows.** A lab that spans two periods shows the text `LAB` in its second period. The course code is carried forward from the previous period within the same section.
3. **Mixed room and teacher fields.** One cell can hold a room, a teacher, a room-and-teacher pair, or a mid-semester change written as `first/second`. Patterns were confirmed with the timetable owner, and any unmatched form is logged rather than guessed.

After cleaning, {{ROWS_PARSED}} of {{ROWS_RAW}} rows were read. The remainder have an unresolved course code, traced to blank or mis-entered cells in the source. They are skipped and logged.

## 5. Optimizer

The optimizer is greedy. It orders classes from most to least constrained, then places each in the first free combination of teacher, room and slot. Three refinements:

- **Multi-pass retry.** Later passes shuffle the slot order and keep the pass that places the most classes. A fixed seed makes the run repeatable.
- **Capacity fit.** Rooms too small for a class are excluded. The rest are tried tightest fit first.
- **Load balance.** Where a class has several eligible teachers, the least-loaded is tried first.

The optimizer does not backtrack. It can fail to place a class even when a placement exists. Section 7 says how often this happened.

## 6. Load balancing: the trade-off

Load is the first criterion when placing a class and room fit is second. A class may therefore move into a larger room if that lets a less-loaded teacher take it. This was a deliberate choice.

On {{BALANCE_BATCH}} new classes (seed 42):

| Strategy | Scheduled | Load spread after |
|---|---|---|
| Naive (locked to one teacher) | {{NAIVE_SCHEDULED}} | {{NAIVE_SPREAD}} |
| Balanced (three eligible teachers) | {{BALANCED_SCHEDULED}} | {{BALANCED_SPREAD}} |

Balancing redistributes load and does not reduce it. Total load is the same in both strategies.

## 7. Optimization results

Seeded runs of 20 new classes:

| Seed | Scheduled | Unscheduled | Winning pass | Utilization before to after |
|---|---|---|---|---|
| 42 | {{S42_SCHED}} | {{S42_UNSCHED}} | {{S42_PASS}} | {{S42_UTIL}} |
| 7 | {{S7_SCHED}} | {{S7_UNSCHED}} | {{S7_PASS}} | {{S7_UTIL}} |
| 2024 | {{S2024_SCHED}} | {{S2024_UNSCHED}} | {{S2024_PASS}} | {{S2024_UTIL}} |

Failures are explained by reason in each run's summary. The most common reason was {{TOP_REASON}}.

Test run with every room set to 150 seats: 20 of 20 test classes placed (seed 42). This is an upper bound, since the real building has rooms of varying size. With the 60-seat placeholder, 10 of 20 placed, and all failures were caused by capacity.

## 8. Verification

- **Unit and integration tests:** {{TEST_COUNT}} tests, {{TEST_FAILURES}} failures.
- **Property tests:** 200 seeded random scenarios checked against eight invariants: accounting, commit integrity, room and teacher uniqueness, exact load, load limit, request fidelity, and section uniqueness.
- **Contract test:** any booking source must satisfy four rules. The CSV source passes; the database source is held to the same test.
- **Round trip:** a saved and reloaded schedule matches the original booking for booking, including loads.

## 9. Limitations

| Limitation | Effect |
|---|---|
| Section clash not enforced | {{CLASH_COUNT}} clashes exist in the source; enforcement would reject real data |
| Faculty limit of 18 is a placeholder | Load results are illustrative |
| Room capacity of 60 is a placeholder | Capacity results are illustrative |
| Class strength is synthetic | Fit results depend on the assumed sizes |
| Eligible teachers are simulated | Balance results depend on the assumed pool |
| Multi-period courses not modelled | Duplicate section and course is rejected |
| Mid-semester changes not modelled | Second half of term under-represented |
| Greedy, no backtracking | Some feasible timetables not found |
| {{UNRESOLVED_COUNT}} rows unresolved | Skipped and logged, not guessed |
