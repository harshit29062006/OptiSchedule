# Scheduler Project — Starter Skeleton

## How to build & run (do this first, Day 1, before writing any feature code)

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.university.scheduler.Main"
```

If you see `Scheduler project environment OK.` printed, your JDK + Maven setup is confirmed
working. Do this on every team member's machine before anyone starts writing real code —
it catches JDK version mismatches and Maven issues immediately instead of mid-week.

Requires JDK 17+ and Maven 3.8+.

## Package structure and ownership

```
com.university.scheduler
 ├── model/       → Member B: Faculty, Room, Course, Section, TimeSlot, Booking, enums
 ├── exception/   → Member B: SchedulingException hierarchy
 ├── engine/      → Member B: ConflictChecker, scheduling/optimization logic
 ├── dao/         → Member A: RoomDAO, FacultyDAO, CourseDAO, BookingDAO, DB connection
 ├── ui/          → Member C: JavaFX screens, controllers
 └── util/        → shared: Comparators, logging helpers, file I/O (Member B/D)
```

## Config

`src/main/resources/config.properties` holds DB connection settings.
Member A: replace placeholder values with real local DB credentials.
**Never commit real passwords** — for this college project a placeholder is fine, but keep
the habit for your resume story ("externalized config, did not hardcode credentials").

## Branching convention

- `main` — always buildable
- `feature/<name>` — e.g. `feature/model-classes`, `feature/jdbc-dao`, `feature/javafx-ui`
- Open a PR into `main` when a feature compiles and (where applicable) passes tests

## Next steps per member (Day 1)

- Member A: finalize `SCHEMA.md` together with Member B, then start `dao/` + `schema.sql`
- Member B: start `model/` — `Day`, `PeriodType` enums, then `TimeSlot` class
- Member C: confirm JavaFX runs (`mvn javafx:run` once a basic `Application` class exists)
- Member D: set up Log4j config, Git repo conventions, this README
