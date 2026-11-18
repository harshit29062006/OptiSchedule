# OptiSchedule

This project contains the original scheduling engine and timetable source files together with the JavaFX desktop interface.

For the current integrated UI and database setup instructions, read **README_INTEGRATED.md** first.

Requirements: JDK 17+, Maven 3.8+, MySQL 8.x.

Run from the project root after configuring the database:

```bash
mvn clean javafx:run
```

Database schema matching the current GitHub `main` branch: `src/main/resources/schema.sql`.

Never commit your local database password. Use environment variables or a local-only `config.properties` file.


## Weekly timetable

The Weekly Timetable screen is built from the original July–December 2026 workbook. It starts in **All programmes** mode. Choose an individual programme for a clearer weekly view, or choose **All sections** to show every class group in that programme. The view includes lectures, tutorials, practicals, combined-type source cells, source period times, and multi-period class continuation markers.

See `TIMETABLE_BUILD_AUDIT.md` for the extraction rules and source-cell validation.
