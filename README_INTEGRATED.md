# OptiSchedule — integrated JavaFX + MySQL project

This archive preserves the uploaded project (engine, tests, source timetable files, CSS and JavaFX navigation) and connects the Rooms, Faculty, Courses, Dashboard and Timetable screens to the current normalized MySQL schema used by the GitHub `main` branch.

## Requirements
- JDK 17 or newer
- Maven 3.8+
- MySQL 8.x

## One-time local setup
1. Create the database: `CREATE DATABASE scheduler_db CHARACTER SET utf8mb4;`
2. Execute `src/main/resources/schema.sql` against `scheduler_db`.
3. Open `src/main/resources/config.properties` and set your local MySQL username/password. Keep real credentials out of Git. The existing connection class also supports `OPTISCHEDULE_DB_URL`, `OPTISCHEDULE_DB_USERNAME`, and `OPTISCHEDULE_DB_PASSWORD` environment variables.
4. Start the application with `mvn clean javafx:run`.

If your friend has already created and populated the current `scheduler_db` schema, do not recreate or reset it; just configure the same connection details.

## Integrated functionality
- **Rooms:** loads, adds, edits and deletes records in MySQL `rooms`.
- **Faculty:** loads, adds, edits and deletes records in MySQL `faculty`.
- **Courses:** loads, adds, edits and deletes records using the composite `(course_code, period_type)` key.
- **Dashboard:** live record counts from `rooms`, `faculty`, `courses` and `bookings`.
- **Official University Timetable:** read-only browser for all imported Excel sheets in `timetable_source_cells`; choose a sheet and search course codes, section labels, rooms, or faculty initials. Results are paged and retain original Excel row/cell coordinates.
- **Booking Manager:** displays joined booking/section/course/faculty/room/timeslot data; add and delete operations are persisted.

## Official timetable browser scope
The workbook uses a complex multi-block layout: lecture, tutorial, and practical groups are arranged in separate column patterns, weekday names are written vertically, and each class uses paired course/annotation rows. The timetable parser reads those structures and displays a student-facing weekly grid. The raw-cell browser is not part of the Timetable screen.

## Important behavior / limitations
- New timetable entries require an existing section, course-period, faculty and room. The UI will show a database error if an ID does not exist or a room/faculty/time conflict violates the schema.
- Existing combined-section sessions are displayed as one row per linked section. The add dialog currently creates a session for one section; the database still supports combined sessions.
- Timetable editing and live utilization charts are not implemented in this pass.
- Deleting a master record referenced by a booking may be blocked by foreign keys; delete/reassign the booking first.
- The older `import_timetable.py` and `scripts/import_cleaned_csv.py` scripts target earlier staging schemas and must not be run against the current normalized schema. The current backend CSV importer is in the latest GitHub `main` branch; consult its `DATA_IMPORT.md` before importing. Do not reset a database containing data you need.

## Verification status
The container used to prepare this archive has JDK 21 but does not have Maven installed. Therefore I could not run `mvn test` or `mvn clean javafx:run` here. Run `mvn clean javafx:run` on your PC and send any error output for a targeted correction. I have not represented the project as successfully compiled or runtime-tested.

## Structured timetable development build

The Timetable screen now includes a `Weekly Timetable` tab backed by `src/main/resources/data/weekly_timetable.csv`. The CSV is generated from the included official workbook by running:

```bash
python scripts/build_weekly_timetable.py
```

This extracts course entries from each detected DAY / serial / HOURS panel in the workbook. It correctly reads weekday names written vertically (MONDAY, TUESDAY, etc.), handles the paired course and room/faculty rows, and detects multiple timetable panels placed side-by-side. The JavaFX screen displays a complete 14-period weekly grid with programme and section filters. Empty periods remain visible; no raw Excel-cell browser is shown in the Timetable screen.

## Reconstructed weekly timetable
The `Weekly Timetable` tab is now a Monday–Saturday grid. It reads the generated `src/main/resources/data/weekly_timetable.csv` and supports sheet/programme, group, and course/room/faculty search. The parser reads the workbook's repeating period rows, uses the day initials printed down the left side (`M O N D A Y`), maps course suffixes (`L`, `T`, `P`) to the lecture/tutorial/practical header rows, and carries room/faculty annotations from the following row where the layout provides them. Each class retains its source Excel cell coordinate. The latest extraction contains **2,472 course slots across all nine sheets**.

To regenerate the timetable index after replacing the workbook, run from the project root:

```bash
python scripts/build_weekly_timetable.py
```

The workbook has irregular merged blocks and some cells contain multiple simultaneous subgroup entries. The view preserves all extracted entries and shows room/faculty annotations only when they can be read from the corresponding group block; blank annotations mean the source layout did not provide a reliable value at that coordinate. The `4TH YEAR A` sheet retains its original January–May 2026 heading where present; the workbook itself is not silently edited.
