package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;

import java.nio.file.Path;
import java.util.*;

/**
 * Measures faculty load balance and compares two strategies on the real dataset:
 *
 *   NAIVE    each new class is locked to its preferred teacher
 *   BALANCED each new class may be taught by any of three eligible teachers,
 *            and the optimizer picks the least loaded
 *
 * Both strategies receive the exact same random batch (same seed), so the
 * difference comes only from the balancing rule.
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass="com.university.scheduler.engine.FacultyBalance"
 */
public final class FacultyBalance {

    private FacultyBalance() {}

    /**
     * Population standard deviation of weekly load across a faculty pool.
     * Idle faculty count as 0, so leaving someone unused raises the spread.
     * Lower means more even. 0 means every faculty member has the same load.
     */
    public static double loadStdDev(ConflictChecker checker, Collection<Faculty> pool) {
        if (pool.isEmpty()) {
            return 0.0;
        }
        double mean = pool.stream().mapToInt(checker::getFacultyLoad).average().orElse(0.0);
        double variance = pool.stream()
            .mapToDouble(f -> {
                double d = checker.getFacultyLoad(f) - mean;
                return d * d;
            })
            .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    /** Result of one strategy run. */
    static final class Outcome {
        final String label;
        final int scheduled;
        final int unscheduled;
        final double spreadBefore;
        final double spreadAfter;

        Outcome(String label, int scheduled, int unscheduled, double spreadBefore, double spreadAfter) {
            this.label = label;
            this.scheduled = scheduled;
            this.unscheduled = unscheduled;
            this.spreadBefore = spreadBefore;
            this.spreadAfter = spreadAfter;
        }
    }

    static Outcome run(Path dataPath, int n, long seed, boolean balanced) {
        ConflictChecker checker = new ScheduleLoader().loadFrom(new CsvBookingSource(dataPath));
        List<Booking> existing = checker.getAllBookings();

        List<Room> rooms = existing.stream().map(Booking::getRoom).distinct()
            .sorted(Comparator.comparing(Room::getRoomId)).toList();
        List<Faculty> pool = existing.stream().map(Booking::getFaculty).distinct()
            .sorted(Comparator.comparing(Faculty::getFacultyId)).toList();
        List<Course> courses = existing.stream().map(Booking::getCourse).distinct()
            .sorted(Comparator.comparing(Course::getCourseCode)).toList();

        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();
        double before = loadStdDev(checker, pool);

        List<PendingAssignment> pending = generate(rooms, pool, courses, n, seed, balanced);
        List<PendingAssignment> unscheduled = new ArrayList<>();
        List<Booking> placed = new SchedulingOptimizer(checker, slots, 1, new Random(seed))
            .scheduleAll(pending, unscheduled);

        double after = loadStdDev(checker, pool);
        return new Outcome(balanced ? "BALANCED" : "NAIVE", placed.size(), unscheduled.size(), before, after);
    }

    /**
     * Builds a batch. The random draws are identical whatever the mode, so both
     * strategies see the same classes. Only the eligible list differs.
     */
    static List<PendingAssignment> generate(List<Room> rooms, List<Faculty> pool, List<Course> courses,
                                            int n, long seed, boolean balanced) {
        Random rng = new Random(seed);
        List<PendingAssignment> out = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Section section = new Section(String.format("BAL-%02d", i), "BAL", 1, null);
            Course course = courses.get(rng.nextInt(courses.size()));
            Faculty primary = pool.get(rng.nextInt(pool.size()));
            Faculty extra1 = pool.get(rng.nextInt(pool.size()));
            Faculty extra2 = pool.get(rng.nextInt(pool.size()));

            List<Room> candidates = new ArrayList<>(rooms);
            Collections.shuffle(candidates, rng);
            candidates = new ArrayList<>(candidates.subList(0, Math.min(3, candidates.size())));

            int strength = 20 + rng.nextInt(101);
            List<Faculty> eligible = balanced ? List.of(primary, extra1, extra2) : List.of(primary);
            out.add(new PendingAssignment(section, course, primary, candidates, strength, eligible));
        }
        return out;
    }

    public static void main(String[] args) {
        Path data = args.length > 0 ? Path.of(args[0]) : OptimizationRunDriver.DEFAULT_DATA;
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 40;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 42L;

        Outcome naive = run(data, n, seed, false);
        Outcome balanced = run(data, n, seed, true);

        System.out.println();
        System.out.println("============ FACULTY LOAD BALANCE COMPARISON ============");
        System.out.printf("Batch: %d new classes, seed %d%n", n, seed);
        System.out.printf("%-26s %12s %12s%n", "", "NAIVE", "BALANCED");
        System.out.printf("%-26s %12d %12d%n", "Scheduled", naive.scheduled, balanced.scheduled);
        System.out.printf("%-26s %12d %12d%n", "Unscheduled", naive.unscheduled, balanced.unscheduled);
        System.out.printf("%-26s %12.3f %12.3f%n", "Load std dev (before)", naive.spreadBefore, balanced.spreadBefore);
        System.out.printf("%-26s %12.3f %12.3f%n", "Load std dev (after)", naive.spreadAfter, balanced.spreadAfter);
        System.out.println("=========================================================");
    }
}
