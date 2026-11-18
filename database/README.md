# Database setup

The canonical schema is `src/main/resources/schema.sql` (also copied to `database/schema.sql`). It matches the current GitHub `main` schema: courses use `(course_code, period_type)` as a composite primary key; sessions use `bookings` plus `booking_sections`; weekday/period are stored in `timeslots`.

1. Create `scheduler_db` in MySQL 8.
2. Execute `src/main/resources/schema.sql` against it.
3. Configure local connection values in `src/main/resources/config.properties` or environment variables `OPTISCHEDULE_DB_URL`, `OPTISCHEDULE_DB_USERNAME`, `OPTISCHEDULE_DB_PASSWORD`.
4. Start the app with `mvn clean javafx:run`.

The older scripts `import_timetable.py` and `scripts/import_cleaned_csv.py` were written for earlier staging schemas and are retained only as project history/source utilities. **Do not run them against the current normalized schema.** Use the current backend CSV importer from the GitHub `main` branch if you need to populate the normalized timetable tables. Do not reset a database containing records you need.
