# OptiSchedule timetable rebuild audit

## Source files used

- **Placement source:** `TIME TABLE JULYTODEC2026.xlsx` (the original workbook supplied in the conversation).
- **Cross-reference:** `src/main/resources/data/all_sheets_cleaned_reference.csv` (latest uploaded `all_sheets_cleaned(4).csv`, 7,086 rows).

The timetable is positioned from the original workbook's actual schedule grid, not by copying the flattened CSV's period fields. The cleaned CSV is retained in the project for comparison.

## Why the earlier timetable was wrong

The workbook has separate Lecture, Tutorial and Practical header rows, and the header row differs by sheet. Some worksheets also shift the day/period/time columns. PG TIME TABLE has a separate seven-day layout. Earlier versions treated these structures too uniformly.

The cleaned CSV can represent a `LAB` continuation as an additional row. Example: in FIRST YEAR A, Monday P1 (08:00), cell `AA8` contains `UES103P` and `AA9` contains its room. At Monday P2, `AA10` contains the literal `LAB` continuation marker. This is one practical starting at P1 and continuing into P2, not a second new practical starting at P2. The rebuilt UI keeps the class at its original start time and shows the continuation in P2.

## Workbook layouts handled

- First Year A/B, Second Year A/B, Third Year A/B, and Fourth Year A/B are read using their individual `DAY`, serial-number and `HOURS` columns and the worksheet's own Lecture, Tutorial and Practical header rows.
- Third Year A has its shifted period/time columns and duplicate weekday markers.
- Sheets whose serial-number label is absent are handled by detecting the repeated period numbers alongside actual time values.
- PG TIME TABLE is parsed separately using its group headers and its seven-day schedule, including Saturday and Sunday entries.

## Placement and class-type rules

- Each class entry uses the exact source worksheet cell, weekday block, period number and corresponding start time.
- `...L`, `...T`, and `...P` entries use the matching Lecture, Tutorial and Practical group headings, respectively.
- A source cell containing more than one class type retains all of those types and all matching group labels. It is displayed as a combined entry rather than being forced into one category.
- A literal `LAB` in a later period is treated as a continuation marker for the preceding scheduled item in the same column, not as a new class. The UI shows the class at its start period and marks the following period as a continuation.
- The room/facility is read from the cell directly below the source course cell in the same column.
- All times are normalized to 24-hour `HH:mm` format from the workbook's time columns.

## Extracted class cells

- Total class-bearing source cells: **5,102**
- Lecture-tagged cells: **2,135**
- Tutorial-tagged cells: **788**
- Practical-tagged cells: **2,206**
- Cells carrying more than one type: **27**
- Entries with a continuation period: **1,869**
- Practical entries with a continuation period: **1,856**

The L/T/P totals overlap where a single source cell contains more than one class type; therefore they do not sum to the total source-cell count.

## Per-programme breakdown

| Programme sheet | Source cells | Lectures | Tutorials | Practicals |
|---|---:|---:|---:|---:|
| FIRST YEAR A | 507 | 172 | 134 | 201 |
| FIRST YEAR B | 644 | 162 | 200 | 282 |
| 2ND YEAR A | 759 | 309 | 157 | 293 |
| 2ND YEARB B | 732 | 287 | 63 | 382 |
| 3RD YEAR A | 835 | 372 | 172 | 295 |
| 3RD YEAR B | 587 | 288 | 0 | 316 |
| 4TH YEAR A | 324 | 163 | 30 | 137 |
| 4TH YEAR B | 182 | 42 | 0 | 140 |
| PG TIME TABLE | 532 | 340 | 32 | 160 |

## Other source text retained for review

There are **299** timetable-grid cells that do not contain an explicit L/T/P marker and therefore cannot safely be labelled as a lecture, tutorial or practical from the source alone. They are preserved in:

`src/main/resources/data/timetable_unclassified_source_cells.csv`

They are not silently relabelled as L/T/P classes.

## Period start times

| Period | Start time |
|---:|---:|
| 1 | 08:00 |
| 2 | 08:50 |
| 3 | 09:40 |
| 4 | 10:30 |
| 5 | 11:20 |
| 6 | 12:10 |
| 7 | 13:00 |
| 8 | 13:50 |
| 9 | 14:40 |
| 10 | 15:30 |
| 11 | 16:20 |
| 12 | 17:10 |
| 13 | 18:00 |
| 14 | 18:50 |

## Validation performed

- Confirmed that every generated timetable record points to a populated source cell in the workbook.
- Confirmed that every generated room/facility value matches the cell immediately below its course cell.
- Confirmed the day, period number and time against each source cell row.
- Confirmed every L/T/P group label against that type’s matching header row.
- Recomputed each continuation duration by checking the following period cell in the same column.
- Confirmed that no source coordinate is duplicated in the generated timetable CSV.
- Confirmed that all nine programme sheets are represented.
- Confirmed that the CSV consumed by JavaFX contains the same 5,102 records exported by this rebuild.

## Files

- `src/main/resources/data/weekly_timetable.csv`: JavaFX timetable data.
- `src/main/resources/data/all_sheets_cleaned_reference.csv`: latest cleaned CSV supplied by the user.
- `src/main/resources/data/timetable_unclassified_source_cells.csv`: non-L/T/P source text retained for review.
- `TIME TABLE JULYTODEC2026.xlsx`: original workbook used for the timetable placement.
