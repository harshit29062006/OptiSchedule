# Official weekly timetable fix

The Timetable screen now shows the student-facing weekly grid from the supplied `TIME TABLE JULYTODEC2026.xlsx` workbook. The raw-cell browser has been removed from the Timetable tabs and the Dashboard no longer shows imported raw-cell counts.

## What was corrected

- The workbook spells weekday names vertically in the DAY column (for example, `M O N D A Y`). The previous extractor treated each letter as a separate weekday. The new extractor reads the complete weekday word.
- Each weekday has a sequence of period rows. The grid now shows all 14 periods, even when a period has no class.
- The extractor detects the timetable panels using their DAY / serial / HOURS columns, including multiple course-group panels laid out horizontally.
- Course and annotation rows are read together. Room and faculty details are displayed only where the workbook contains a corresponding value.
- Cells containing more than one course code are split into individual course entries.
- The older January–May block on the `4TH YEAR A` sheet is excluded; the July–December timetable blocks are used.
- The screen has programme/sheet and section filters, plus course/room/faculty search.

## Regenerate after changing the Excel workbook

From the `OptiSchedule` project folder:

```bash
python scripts/build_weekly_timetable.py
```

The generated file is `src/main/resources/data/weekly_timetable.csv`.

## Run in VS Code on Windows

Requirements: JDK 17 or newer and Maven 3.8 or newer.

1. Extract the ZIP.
2. Open the `OptiSchedule` folder (the folder containing `pom.xml`) in VS Code.
3. Open Terminal → New Terminal.
4. Run:

```powershell
mvn clean javafx:run
```

The weekly timetable is loaded from the bundled CSV and does not require a database connection. Rooms, Faculty, Courses, Dashboard counts and Booking Manager still use the configured MySQL database.

## Validation note

The Excel extraction script was run against the included workbook and generated 5,502 course entries across the nine worksheets. This is an extraction count, not a claim that every workbook annotation has been independently verified. Maven is not installed in the preparation environment, so the JavaFX application could not be compiled or launched here. If VS Code reports a Maven/compiler error, send the complete terminal output and I can correct it.


## Follow-up correction: lectures, subgroups, and exact times

- The section selector retains parent sections and their tutorial/practical subgroups.
- Selecting a parent section shows its lectures plus all tutorial and practical subgroups.
- Selecting an individual subgroup (for example, `2C1A` or `2C11`) now also includes the parent-section lectures, so the timetable no longer appears to contain only practical/lab sessions.
- Each class is labelled as Lecture, Tutorial, or Practical / Lab based on the course-code suffix (`L`, `T`, or `P`).
- The grid uses the time values extracted from the selected Excel worksheet instead of relying only on a fixed time list.
- The time parser now correctly converts source values such as `6:00PM` to `18:00` and `6:50PM` to `18:50`.
- The timetable rows are allowed to grow so multiple simultaneous subgroup sessions are not clipped.
