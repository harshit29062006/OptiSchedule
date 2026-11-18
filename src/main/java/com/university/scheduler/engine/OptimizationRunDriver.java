package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Full-dataset optimization run.
 *
 * 1. Loads every real booking from the cleaned 9-sheet timetable as the existing schedule.
 * 2. Generates a seeded batch of NEW classes, using real rooms, faculty and courses
 *    with random strengths. This simulates "add N new sections next semester".
 * 3. Runs the multi-pass optimizer over them.
 * 4. Reports how many were placed, why the rest failed, and how utilization and
 *    faculty load changed.
 *
 * Run from the project root:
 *   mvn exec:java -Dexec.mainClass="com.university.scheduler.engine.OptimizationRunDriver"
 * Optional arguments: <csvPath> <count> <seed>
 *
 * The conflict log from ScheduleLoader is printed before the summary, so
 * the summary is at the bottom of the output.
 */
public class OptimizationRunDriver {

    static final Path DEFAULT_DATA = Path.of("src/main/resources/data/all_sheets_cleaned.csv");
    static final int DEFAULT_COUNT = 20;
    static final long DEFAULT_SEED = 42L;
    static final int MAX_PASSES = 10;

    public static void main(String[] args) {
        Path data = args.length > 0 ? Path.of(args[0]) : DEFAULT_DATA;
        int count = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_COUNT;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : DEFAULT_SEED;
        run(data, count, seed).print(System.out);
    }

    public static RunReport run(Path dataPath, int requested, long seed) {
        long t0 = System.nanoTime();
        ConflictChecker checker = new ScheduleLoader().loadFrom(new CsvBookingSource(dataPath));
        long loadMs = (System.nanoTime() - t0) / 1_000_000;

        List<Booking> existing = checker.getAllBookings();
        List<Room> rooms = existing.stream().map(Booking::getRoom).distinct()
            .sorted(Comparator.comparing(Room::getRoomId)).toList();
        List<Faculty> faculty = existing.stream().map(Booking::getFaculty).distinct()
            .sorted(Comparator.comparing(Faculty::getFacultyId)).toList();
        List<Course> courses = existing.stream().map(Booking::getCourse).distinct()
            .sorted(Comparator.comparing(Course::getCourseCode)).toList();

        List<TimeSlot> slots = TimeSlotGenerator.generateStandardWeek();

        RunReport report = new RunReport();
        report.seed = seed;
        report.existingBookings = checker.totalBookings();
        report.loadMs = loadMs;
        report.requested = requested;
        report.maxPasses = MAX_PASSES;
        report.utilizationBefore = new UtilizationReport(checker).computeAverageRoomUtilization(rooms, slots);
        report.facultyAtLimitBefore = countFacultyAtLimit(checker);

        List<PendingAssignment> pending = generatePending(rooms, faculty, courses, requested, seed);

        SchedulingOptimizer optimizer = new SchedulingOptimizer(checker, slots, MAX_PASSES, new Random(seed));
        long t1 = System.nanoTime();
        SchedulingOptimizer.PassResult result = optimizer.scheduleAll(pending);
        report.optimizeMs = (System.nanoTime() - t1) / 1_000_000;
        report.winningPass = result.passNumber;
        report.scheduledCount = result.scheduled.size();
        report.unscheduledCount = result.unscheduled.size();

        for (PendingAssignment pa : result.unscheduled) {
            report.failureReasons.merge(diagnose(checker, pa, slots), 1, Integer::sum);
        }

        report.utilizationAfter = new UtilizationReport(checker).computeAverageRoomUtilization(rooms, slots);
        report.facultyAtLimitAfter = countFacultyAtLimit(checker);
        return report;
    }

    /** Builds a seeded batch of new classes using real rooms, faculty and courses. */
    static List<PendingAssignment> generatePending(List<Room> rooms, List<Faculty> faculty,
                                                   List<Course> courses, int n, long seed) {
        Random rng = new Random(seed);
        List<PendingAssignment> out = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Section section = new Section(String.format("NEW-%02d", i), "NEW", 1, null);
            Course course = courses.get(rng.nextInt(courses.size()));
            Faculty f = faculty.get(rng.nextInt(faculty.size()));

            List<Room> candidates = new ArrayList<>(rooms);
            Collections.shuffle(candidates, rng);
            candidates = new ArrayList<>(candidates.subList(0, Math.min(3, candidates.size())));

            int strength = 20 + rng.nextInt(101); // 20..120 students
            out.add(new PendingAssignment(section, course, f, candidates, strength));
        }
        return out;
    }

    /**
     * Explains why an unscheduled assignment failed, judged against the final schedule.
     * Checks are ordered from the most fundamental constraint to the least.
     */
    static String diagnose(ConflictChecker checker, PendingAssignment pa, List<TimeSlot> slots) {
    List<Room> usable = SchedulingOptimizer.orderedRooms(pa);
    if (usable.isEmpty()) {
        return "no candidate room large enough for strength " + pa.getExpectedStrength();
    }
    boolean anyFacultyFree = pa.getEligibleFaculty().stream()
        .anyMatch(f -> checker.getFacultyLoad(f) + 1 <= f.getMaxHoursPerWeek());
    if (!anyFacultyFree) {
        return "all eligible faculty at weekly load limit";
    }
    for (Faculty f : pa.getEligibleFaculty()) {
        for (Room r : usable) {
            if (checker.findAvailableSlot(f, r, slots).isPresent()) {
                return "a free slot existed at end state but was not reached in greedy order";
            }
        }
    }
    return "no free slot: candidate rooms busy or faculty teaching elsewhere in every slot";
    }

    static int countFacultyAtLimit(ConflictChecker checker) {
        int n = 0;
        for (Map.Entry<Faculty, Integer> e : checker.getAllFacultyLoads().entrySet()) {
            if (e.getValue() >= e.getKey().getMaxHoursPerWeek()) {
                n++;
            }
        }
        return n;
    }

    static double pct(int part, int whole) {
        return whole == 0 ? 0.0 : 100.0 * part / whole;
    }

    /** Metrics from one run. Fields are package-private so tests can check them. */
    public static final class RunReport {
        long seed;
        int existingBookings;
        long loadMs;
        int requested;
        int maxPasses;
        long optimizeMs;
        int winningPass;
        int scheduledCount;
        int unscheduledCount;
        double utilizationBefore;
        double utilizationAfter;
        int facultyAtLimitBefore;
        int facultyAtLimitAfter;
        final Map<String, Integer> failureReasons = new LinkedHashMap<>();

        public void print(PrintStream out) {
            out.println();
            out.println("==================== OPTIMIZATION RUN SUMMARY ====================");
            out.printf("Seed:                          %d%n", seed);
            out.printf("Existing real bookings:        %d (loaded in %d ms)%n", existingBookings, loadMs);
            out.printf("New assignments requested:     %d%n", requested);
            out.printf("Scheduled:                     %d (%.1f%%)%n", scheduledCount, pct(scheduledCount, requested));
            out.printf("Unscheduled:                   %d (%.1f%%)%n", unscheduledCount, pct(unscheduledCount, requested));
            out.printf("Winning pass:                  %d of %d (optimization %d ms)%n", winningPass, maxPasses, optimizeMs);
            out.printf("Average room utilization:      %.2f%% -> %.2f%%%n", utilizationBefore * 100, utilizationAfter * 100);
            out.printf("Faculty at weekly limit:       %d -> %d%n", facultyAtLimitBefore, facultyAtLimitAfter);
            if (!failureReasons.isEmpty()) {
                out.println("Why assignments failed:");
                failureReasons.forEach((reason, n) -> out.printf("  %4d x %s%n", n, reason));
            }
            out.println("==================================================================");
        }
    }
}
