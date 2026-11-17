# Project Schema — University Academic Resource & Scheduling Optimization Platform

This document is the agreed source of truth for table structure and Java field naming.
Any change here must be agreed by Member A (DB) and Member B (Core Logic) together,
since both the DAO layer and the domain model depend on it directly.

---

## 1. How the real dataset maps into this schema

Our source data is `TIME_TABLE_JULYTODEC2026.xlsx` (9 sheets, one per year/section).
Each row of the parsed CSV looks like:

```
sheet, day, period, start_time, section, course_code, room_faculty_raw
FIRST YEAR A, MONDAY, 1, 08:00:00, 1A2A, UCB009P, CBCL(G114)
```

Mapping rules:
- `section` (e.g. `1A2A`) → `sections.section_code`
- `course_code` (e.g. `UCB009P`) → split into:
  - base code `UCB009` → `courses.course_code`
  - suffix letter → `courses.period_type` (`L`=Lecture, `T`=Tutorial, `P`=Practical), as confirmed by Member A
- `room_faculty_raw` is parsed by the existing `CsvBookingSource` heuristic. Confirmed room-like codes and parenthesized forms resolve to rooms; unclassified counterparts get a unique `UNKNOWN_FACULTY_...` identifier. Unresolved rooms use `UNKNOWN_ROOM_...`. The importer stores these in the lookup tables with `is_synthetic=TRUE` instead of guessing or leaving broken foreign keys.
- `day` + `period` + `start_time` → `timeslots` row
- `sheet` name gives the year and division when recognizable; branch remains null because the source naming convention does not establish a branch code.

---

## 2. Tables

### `branches`
| Column | Type | Notes |
|---|---|---|
| branch_id | VARCHAR(20) PK | e.g. "CSE", "ECE" — confirm actual branch codes used across all 9 sheets |
| branch_name | VARCHAR(100) | full name |

### `faculty`
| Column | Type | Notes |
|---|---|---|
| faculty_id | VARCHAR(100) PK | institutional short-code or flagged synthetic identifier |
| faculty_name | VARCHAR(100) NULL | leave null unless/until a legend is found |
| department | VARCHAR(50) NULL | optional, fill if derivable from branch |
| max_hours_per_week | INT | default placeholder (18) until real policy confirmed |
| is_synthetic | BOOLEAN | true for unresolved identifiers created by the CSV loader |

### `rooms`
| Column | Type | Notes |
|---|---|---|
| room_id | VARCHAR(100) PK | e.g. "G114", "LP101", "E304", or synthetic identifier |
| room_name | VARCHAR(100) | can default to room_id if no separate display name |
| room_type | VARCHAR(30) | current loader default is CLASSROOM; real room type convention still needs confirmation |
| capacity | INT | default placeholder (150) until real capacity data available |
| is_synthetic | BOOLEAN | true for unresolved identifiers created by the CSV loader |

### `courses`
| Column | Type | Notes |
|---|---|---|
| course_code | VARCHAR(20) PK (composite with `period_type`) | base code, e.g. "UCB009" |
| course_name | VARCHAR(150) | joined from the separate course list (Member D) |
| credits | INT NULL | from course list if available |
| period_type | VARCHAR(20) PK (composite with `course_code`) | from suffix L/T/P; `L` is confirmed Lecture |

### `sections`
| Column | Type | Notes |
|---|---|---|
| section_id | BIGINT PK AUTO_INCREMENT | surrogate key |
| section_code | VARCHAR(30) UNIQUE | e.g. "1A2A" — matches real batch code directly |
| branch_id | VARCHAR(20) FK → branches NULL | null until conventions are confirmed |
| academic_year | SMALLINT | maps to Java `Section.year`; 1–4, or 0 for PG |
| division | VARCHAR(10) NULL | e.g. "A", "B" if separable from sheet name |

### `timeslots`
| Column | Type | Notes |
|---|---|---|
| timeslot_id | BIGINT PK AUTO_INCREMENT | surrogate key |
| weekday | VARCHAR(12) | maps to Java `TimeSlot.day`; MONDAY through SUNDAY |
| period_number | SMALLINT | positive period number; keyed with day |
| start_time | TIME | |
| end_time | TIME NULL | currently null because the input CSV only gives start time |

### `bookings`
| Column | Type | Notes |
|---|---|---|
| booking_id | BIGINT PK AUTO_INCREMENT | one session, not one section attendance |
| course_code, period_type | composite FK → courses | |
| faculty_id | VARCHAR(100) FK → faculty | synthetic identifiers can be longer |
| room_id | VARCHAR(100) FK → rooms | synthetic identifiers can be longer |
| timeslot_id | BIGINT FK → timeslots | |

Combined sessions are related to sections through `booking_sections(booking_id, section_id)`, not a single `bookings.section_id` column.

Database uniqueness constraints enforce `(room_id, timeslot_id)` and `(faculty_id, timeslot_id)`. One booking row may link to many sections through `booking_sections`.

---

## 3. Java field naming convention (for Member B's model classes)

DB `snake_case` columns map to Java `camelCase` fields directly, e.g.:
- `faculty_id` → `facultyId`
- `max_hours_per_week` → `maxHoursPerWeek`
- `period_type` → `periodType` (as enum `PeriodType`)
- `room_type` → `roomType` (as enum `RoomType`)

---

## 4. Remaining data questions

1. Branch mapping and room-type classification are intentionally deferred per the timetable owner's recommendation because room labels vary and the additional source files are outside the current scope.
2. Should `end_time` be stored explicitly per slot, or computed as "next period's start_time"?
3. Confirm period numbering with the timetable owner; the current model treats each `(day, period_number)` as one slot.
