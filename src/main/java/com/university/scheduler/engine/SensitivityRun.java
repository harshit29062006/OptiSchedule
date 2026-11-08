package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Capacity sensitivity check. Runs the same seeded batch twice:
 *   BASELINE  rooms at the placeholder capacity used in CsvBookingSource
 *   RAISED    the same rooms with a larger capacity
 * Everything else is identical, so any difference comes from capacity alone.
 */
public final class SensitivityRun {

    static final int BASELINE_CAPACITY = CsvBookingSource.PLACEHOLDER_ROOM_CAPACITY;

    private SensitivityRun() {}

    public static void main(String[] args) {
        Path data = args.length > 0 ? Path.of(args[0]) : OptimizationRunDriver.DEFAULT_DATA;
        int count = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 42L;
        int raised = args.length > 3 ? Integer.parseInt(args[3]) : 200;

        Outcome base = run(data, count, seed, BASELINE_CAPACITY);
        Outcome high = run(data, count, seed, raised);
        print(System.out, count, seed, base, high);
    }

    static Outcome run(Path data, int count, long seed, int capacity) {
        ConflictChecker checker = new ScheduleLoader().loadFrom(new CsvBookingSource(data));
        List<Booking> existing = checker.getAllBookings();

        List<Room> rooms = existing.stream().map(Booking::getRoom).distinct()
            .sorted(Comparator.comparing(Room::getRoomId))
            .map(r -> new Room(r.getRoomId(), r.getRoomName(), r.getRoomType(), capacity))
            .toList();
        List<Faculty> faculty = existing.stream().map(Booking::getFaculty).distinct()
            .sorted(Comparator.comparing(Faculty::getFacultyId)).toList();
        List<Course> courses = existing.stream().map(Booking::getCourse).distinct()
            .sorted(Comparator.comparing(Course::getCourseCode)).toList();

        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        List<PendingAssignment> pending =
            OptimizationRunDriver.generatePending(rooms, faculty, courses, count, seed);

        SchedulingOptimizer.PassResult result =
            new SchedulingOptimizer(checker, slots, 10, new Random(seed)).scheduleAll(pending);

        int blocked = 0;
        for (PendingAssignment pa : result.unscheduled) {
            if (SchedulingOptimizer.orderedRooms(pa).isEmpty()) {
                blocked++;
            }
        }
        return new Outcome(capacity, result.scheduled.size(), result.unscheduled.size(),
            blocked, result.passNumber);
    }

    static void print(PrintStream out, int count, long seed, Outcome base, Outcome high) {
        out.println();
        out.println("==================== CAPACITY SENSITIVITY ====================");
        out.printf("Batch: %d new classes, seed %d%n", count, seed);
        out.printf("%-28s %16s %16s%n", "", "BASELINE", "RAISED");
        out.printf("%-28s %16d %16d%n", "Room capacity", base.capacity, high.capacity);
        out.printf("%-28s %16d %16d%n", "Scheduled", base.scheduled, high.scheduled);
        out.printf("%-28s %16d %16d%n", "Unscheduled", base.unscheduled, high.unscheduled);
        out.printf("%-28s %16d %16d%n", "  blocked by capacity alone", base.blocked, high.blocked);
        out.printf("%-28s %16d %16d%n", "Winning pass", base.pass, high.pass);
        out.println("==============================================================");
    }

    static final class Outcome {
        final int capacity;
        final int scheduled;
        final int unscheduled;
        final int blocked;
        final int pass;

        Outcome(int capacity, int scheduled, int unscheduled, int blocked, int pass) {
            this.capacity = capacity;
            this.scheduled = scheduled;
            this.unscheduled = unscheduled;
            this.blocked = blocked;
            this.pass = pass;
        }
    }
}