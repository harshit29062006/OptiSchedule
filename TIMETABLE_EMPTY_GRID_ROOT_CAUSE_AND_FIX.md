# Timetable data repair

This version rebuilds `weekly_timetable.csv` from the exact uploaded `all_sheets_cleaned(3).csv`.

## Root cause of the blank timetable
The CSV stored weekdays in uppercase (`MONDAY`, `TUESDAY`, etc.), while the JavaFX grid looked up title-case strings (`Monday`, `Tuesday`, etc.) using case-sensitive equality. As a result, records loaded but were never placed in any weekday cell.

## Repair
- Normalized day names to title case during CSV generation and again in Java ingestion.
- Forward-filled blank weekday values within each worksheet, matching merged weekday cells in the cleaned source.
- Preserved original period numbers, start times, section labels, course codes and room/facility text.
- Did not infer or move practicals to different periods or weekdays.
- Kept L/T/P entries in the same day, period and section supplied by the source CSV.

## Source counts
- Total source rows: 7,086
- Rows assigned to a weekday and valid period: 7,086
- Lecture suffix L: 2,373
- Tutorial suffix T: 1,000
- Practical suffix P: 3,305

The course suffix counts are a source-data classification, not a claim that every other course-code suffix is invalid.
