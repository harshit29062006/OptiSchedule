# Integration changes in this archive

## Preserved
- Existing project structure, JavaFX navigation shell, dark stylesheet, engine, tests, timetable CSV/XLSX source data, and utility scripts.

## Added
- `src/main/java/com/university/scheduler/dao/UiRepository.java`: direct JDBC CRUD/query layer used by the JavaFX screens.
- `src/main/java/com/university/scheduler/ui/UiSupport.java`: shared UI helpers.
- `src/main/resources/schema.sql`: canonical normalized schema from the current GitHub main branch.
- `README_INTEGRATED.md`: setup, run, functionality and limitations.

## Reworked
- `RoomView`, `FacultyView`, `CourseView`: database-backed list/add/edit/delete screens.
- `TimetableView`: database-backed session list, add and delete.
- `DashboardView`: live database counts instead of hard-coded sample numbers.
- `JdbcBookingSource`: reads current composite-course / booking-sections schema and groups combined sections.
- `SchedulingService`: adds `fromJdbc()` factory.
- `PeriodType`: maps `L` to `LECTURE`, matching the current backend convention.
- `database/schema.sql`, `SCHEMA.md`, `database/README.md`, and root `README.md`: aligned to the current schema.
- `pom.xml`: adds H2 test dependency used by the repository's tests.

## Validation
- Java source parser check found no Java syntax errors in the available source files.
- Full compile/test was not possible in the preparation environment because Maven and JavaFX libraries are not installed there. Run `mvn clean javafx:run` on the target PC before pushing.
