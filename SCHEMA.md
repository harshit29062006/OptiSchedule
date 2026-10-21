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
  - suffix letter `P` → `courses.period_type` (`L`=Lecture, `T`=Tutorial, `P`=Practical) — **confirm with Member A whether `L` means Lecture or Lab in this institution's convention before finalizing**
- `room_faculty_raw` (e.g. `CBCL(G114)`) → split into:
  - faculty initials → `faculty.faculty_id` (e.g. `CBCL`)
  - room code → `rooms.room_id` (e.g. `G114`)
  - (exact split regex to be finalized by Member A once more raw examples are reviewed — format isn't 100% consistent across lecture/lab/tutorial rows)
- `day` + `period` + `start_time` → `timeslots` row
- `sheet` name (e.g. `FIRST YEAR A`) → gives `branch`/`year`/`division` for `sections`

---

## 2. Tables

### `branches`
| Column | Type | Notes |
|---|---|---|
| branch_id | VARCHAR(10) PK | e.g. "CSE", "ECE" — confirm actual branch codes used across all 9 sheets |
| branch_name | VARCHAR(100) | full name |

### `faculty`
| Column | Type | Notes |
|---|---|---|
| faculty_id | VARCHAR(20) PK | institutional short-code, e.g. "TA8", "CBCL" (no full-name legend available) |
| faculty_name | VARCHAR(100) NULL | leave null unless/until a legend is found |
| department | VARCHAR(50) NULL | optional, fill if derivable from branch |
| max_hours_per_week | INT | default placeholder (e.g. 18) until real policy confirmed |

### `rooms`
| Column | Type | Notes |
|---|---|---|
| room_id | VARCHAR(20) PK | e.g. "G114", "LP101", "E304" |
| room_name | VARCHAR(50) | can default to room_id if no separate display name |
| room_type | ENUM('CLASSROOM','LAB','TUTORIAL_ROOM') | infer from code prefix (LP = lab, E/G = classroom — confirm convention) |
| capacity | INT | default placeholder until real capacity data available |

### `courses`
| Column | Type | Notes |
|---|---|---|
| course_code | VARCHAR(10) PK | base code, e.g. "UCB009" |
| course_name | VARCHAR(150) | joined from the separate course list (Member D) |
| credits | INT NULL | from course list if available |
| period_type | ENUM('LECTURE','TUTORIAL','PRACTICAL') | from code suffix L/T/P |

### `sections`
| Column | Type | Notes |
|---|---|---|
| section_id | INT PK AUTO_INCREMENT | surrogate key |
| section_code | VARCHAR(10) UNIQUE | e.g. "1A2A" — matches real batch code directly |
| branch_id | VARCHAR(10) FK → branches | |
| year | TINYINT | 1–4, or 0 for PG |
| division | VARCHAR(5) NULL | e.g. "A", "B" if separable from section_code |

### `timeslots`
| Column | Type | Notes |
|---|---|---|
| timeslot_id | INT PK AUTO_INCREMENT | surrogate key |
| day | ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') | |
| period_number | TINYINT | 1–14 range seen in real data (double-shift school day) |
| start_time | TIME | |
| end_time | TIME NULL | compute from next period's start_time if not directly given |

### `bookings`
| Column | Type | Notes |
|---|---|---|
| booking_id | INT PK AUTO_INCREMENT | surrogate key |
| section_id | INT FK → sections | |
| course_code | VARCHAR(10) FK → courses | |
| faculty_id | VARCHAR(20) FK → faculty | |
| room_id | VARCHAR(20) FK → rooms | |
| timeslot_id | INT FK → timeslots | |

Uniqueness constraint candidates (to be enforced at DB + app level):
- `(room_id, timeslot_id)` should be unique → room double-booking check
- `(faculty_id, timeslot_id)` should be unique → faculty double-booking check

---

## 3. Java field naming convention (for Member B's model classes)

DB `snake_case` columns map to Java `camelCase` fields directly, e.g.:
- `faculty_id` → `facultyId`
- `max_hours_per_week` → `maxHoursPerWeek`
- `period_type` → `periodType` (as enum `PeriodType`)
- `room_type` → `roomType` (as enum `RoomType`)

---

## 4. Open questions to resolve with Member A before Day 2

1. Does `L` suffix in course codes mean **Lecture** or **Lab**? (affects `PeriodType` enum semantics)
2. What's the actual branch-code convention across all 9 sheets (not just FIRST YEAR A/B)?
3. Room prefix convention — is `LP` always Lab, `E`/`G` always classroom? Any exceptions?
4. Should `end_time` be stored explicitly per slot, or computed as "next period's start_time"?
5. Confirm whether `period_number` resets per day (1–14) or is continuous — parser currently assumes it resets.
