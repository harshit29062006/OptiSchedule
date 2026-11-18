# Final timetable corrections

- Rebuilt the timetable resource directly from the user's final `all_sheets_cleaned.csv`.
- Preserved all 7,086 source rows.
- Filled blank weekday labels downward within each worksheet, matching merged-cell continuation in the cleaned source.
- All six weekdays represented in the source are retained: Monday through Saturday.
- All 13 source periods and their exact start times are represented.
- Lecture (L), Tutorial (T), Practical (P), and other source-coded entries are retained; no class type is filtered out.
- Added an `All sections` filter option and made it the default so the selected programme initially displays the complete timetable.
- Professor/room statistics were not added, as requested.

The source has 2412 lecture rows, 1001 tutorial rows, and 3313 practical rows. Other entries are retained as well.
