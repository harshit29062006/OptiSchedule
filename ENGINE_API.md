# Scheduling Engine API

Version 1.0. Everything listed here is public and stable. Anything not listed is internal, and the UI and the database layer should not depend on it.

Members A (database) and C (interface) should confirm each section, or name the method they need changed. Confirmation is recorded in the table at the end.

## 1. Entry point: `SchedulingService` (for Member C)

Call this class from the interface. Do not call `ConflictChecker` or `SchedulingOptimizer` directly.

| Method | Purpose | Throws |
|---|---|---|
| `static fromCsv(Path)` | Load the cleaned timetable from CSV | `RuntimeException` if the file cannot be read |
| `static fromExistingChecker(ConflictChecker)` | Wrap an existing schedule (tests) | none |
| `int getTotalBookingCount()` | Distinct sessions; a combined class counts once | none |
| `int getTotalSectionAttendances()` | Sections attending; a combined class with five sections counts five | none |
| `List<Booking> getAllBookings()` | Snapshot of every committed booking | none |
| `List<Booking> getBookingsAt(Day, int)` | Bookings at one day and period | `IllegalArgumentException` for an unknown slot |
| `int getFacultyLoad(Faculty)` | Weekly periods a teacher holds | none |
| `BookingResult tryAddBooking(Booking)` | Add one booking; returns the outcome | none (failure is in the result) |
| `OptimizationResult scheduleAll(List<PendingAssignment>)` | One greedy pass | `IllegalArgumentException` for duplicate section and course |
| `OptimizationResult scheduleWithRetries(List<PendingAssignment>, int maxPasses, long seed)` | Multi-pass run; same seed gives same result | `IllegalArgumentException` if `maxPasses < 1` |
| `Map<Room, Double> getRoomUtilization(List<Room>)` | Fraction of the week each room is used | none |
| `double getAverageRoomUtilization(List<Room>)` | Mean of the above | none |
| `List<Faculty> getFacultyNearLoadLimit(double ratio)` | Teachers at or above the given share of their limit | none |
| `double getFacultyLoadSpread(Collection<Faculty>)` | Standard deviation of load; lower is more even | none |

### Result types

- `BookingResult`: `isSuccess()`, `getErrorMessage()`
- `OptimizationResult`: `getScheduled()` (List of Booking), `getUnscheduled()` (List of PendingAssignment), `allScheduled()`

## 2. Data input: `BookingSource` (for Member A)

| Type | Role |
|---|---|
| `interface BookingSource` | `List<Booking> loadAll()`. Every source implements this. |
| `CsvBookingSource` | Reference implementation, reads the cleaned CSV |
| `ScheduleLoader.loadFrom(BookingSource)` | Builds a `ConflictChecker`, logs rejections at WARN |

A new source, including the JDBC one, must pass `BookingSourceContractTest`. Four rules apply: `loadAll` never returns null, repeat calls return the same count, every booking is complete, and the loader never accepts more than the source offered.

## 3. Persistence and export

| Method | Purpose | Throws |
|---|---|---|
| `ScheduleSnapshot.save(ConflictChecker, Path)` | Versioned Java-serialized snapshot | `IOException` |
| `ScheduleSnapshot.load(Path)` | Restore and revalidate a snapshot | `IOException`; `IllegalStateException` for a wrong version or inconsistent data |
| `ScheduleCsvExporter.export(ConflictChecker, Path)` | One CSV row per session, Excel-compatible | `IOException` |

## 4. Input model (for Members A and C to populate)

| Class | Constructor requirements |
|---|---|
| `Faculty(id, name, department, maxHoursPerWeek)` | id not blank; maxHours not negative |
| `Room(id, name, RoomType, capacity)` | id not blank; capacity positive |
| `Course(code, name, credits, PeriodType)` | code not blank |
| `Section(code, branch, year, division)` | code not blank |
| `TimeSlot(Day, period, start, end)` | period positive |
| `PendingAssignment(section, course, faculty, rooms, strength[, eligibleFaculty])` | rooms not empty; strength not negative |
| `Booking(section, course, faculty, room, slot)` | no nulls |

Constructor failures throw `IllegalArgumentException` or `NullPointerException`, and the message names the field.

## 5. Exceptions

| Exception | Meaning |
|---|---|
| `SchedulingException` | Base, checked. Caught and reported by `tryAddBooking` |
| `RoomUnavailableException` | Room already held by another teacher at that slot |
| `SlotConflictException` | Teacher already teaching in another room at that slot |
| `FacultyOverloadException` | Teacher would exceed their weekly limit |

## 6. Rules the engine enforces

1. A room cannot be held by two different teachers at one slot.
2. A teacher cannot be in two rooms at one slot.
3. The same teacher and room at one slot is one combined session. Sections merge; load counts once.
4. A teacher's load cannot exceed their weekly limit.
5. A failed booking changes nothing.

Not enforced: one section in two rooms at once. See `WEEK4_DECISIONS.md` (D1).

## 7. Sign-off

| Member | Area | Confirmed / requested changes | Date |
|---|---|---|---|
| A | Database and `BookingSource` | [PENDING] | [PENDING] |
| C | Interface and `SchedulingService` | [PENDING] | [PENDING] |
