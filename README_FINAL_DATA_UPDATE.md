# Final timetable data update

The timetable resource `weekly_timetable.csv` has been rebuilt directly from the user's final `all_sheets_cleaned.csv` upload.

- Source rows: 7,086
- Worksheets/programme sheets: 9
- Unique course codes: 992
- Distinct room/facility labels present in `room_faculty_raw`: 1352
- Course-code suffix counts (L=lecture, T=tutorial, P=practical): 2,373 / 1,000 / 3,305

## Important limitation about professor counts

The final CSV has exactly these columns: `sheet, day, period, start_time, section, course_code, room_faculty_raw`.
It contains no professor/faculty-name or professor-ID column. Therefore the app can accurately count distinct room/facility labels, but cannot calculate the number of unique professors from this source. The `faculty` field in the app CSV is intentionally left blank rather than inventing professor assignments.

Room/facility labels are preserved exactly as supplied in `room_faculty_raw` (including lab names and bracketed room identifiers).
