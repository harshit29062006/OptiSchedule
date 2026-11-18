# OptiSchedule database schema (current)

This document describes the normalized schema used by the current GitHub `main` branch and by the integrated JavaFX screens. The executable DDL is `src/main/resources/schema.sql`.

- `branches`: optional academic branch lookup.
- `faculty`: faculty ID, optional display name/department, weekly teaching-hour limit, synthetic marker.
- `rooms`: room ID, name, room type, capacity, synthetic marker.
- `courses`: composite primary key `(course_code, period_type)`; the same base course may have lecture, tutorial and practical variants.
- `sections`: unique section code, optional branch, academic year and division.
- `timeslots`: unique `(weekday, period_number)` with optional start/end times.
- `bookings`: one scheduled session referencing a course-period variant, faculty, room and timeslot.
- `booking_sections`: many-to-many relationship from a session to its attending sections, allowing combined sessions.

The database enforces foreign keys and unique room/time and faculty/time assignments. The UI does not create guessed master records. To add a timetable entry, its section, course-period, faculty and room must already exist.

Course suffix mapping confirmed by the current backend: `L = LECTURE`, `T = TUTORIAL`, `P = PRACTICAL`.
