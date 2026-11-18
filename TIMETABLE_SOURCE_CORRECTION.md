# Timetable source correction

Rebuilt directly from the latest uploaded workbook `TIME TABLE JULYTODEC2026(1).xlsx`, cross-checked against `all_sheets_cleaned(3).csv`.

The original workbook uses separate header rows for LECTURE, TUTORIAL and PRACTICAL groups. Each period is represented by a course-code row followed by a room/facility row. The previous import mixed those header mappings, which caused classes to be assigned to the wrong groups or omitted.

The new import maps each cell according to its course-code suffix (L/T/P) to the matching category's group header in that exact column, and reads the room/facility from the following row. Weekday blocks are mapped Monday through Saturday, with periods read from the workbook's time cells.

Extracted entries: 4360
- Lectures: 1679
- Tutorials: 764
- Practicals: 1917

The PG sheet has a different layout and is not included in this undergraduate weekly grid import.
