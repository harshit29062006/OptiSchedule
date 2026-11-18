# Timetable rebuilt from the original workbook

This timetable CSV was generated directly from `TIME TABLE JULYTODEC2026(1).xlsx`, not from the previously flattened CSV.

## Correct workbook structure
- Row 4 contains Lecture section headers.
- Row 5 contains Tutorial subgroup headers.
- Row 6 contains Practical subgroup headers.
- Each period has its course-code row and the following room/facility row.
- Weekday blocks are identified from the vertically written day letters in column A.
- Period number and start time are read from columns B and D.

## Critical correction
The course suffix determines which header row supplies its section:
- `...L` -> Lecture header row 4
- `...T` -> Tutorial header row 5
- `...P` -> Practical header row 6

The earlier parser incorrectly used the same group header for different class types. This caused tutorials/lectures to be assigned to practical groups or excluded by filters.

## Extracted entries
- Total entries: 3,404
- Lectures: 1,165
- Tutorials: 592
- Practicals: 1,647

The display uses the actual weekday, period, time, course, and matching type-specific group from the workbook. Room/facility text is read from the row immediately below the course cell.
