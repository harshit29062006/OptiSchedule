package com.university.scheduler.engine;

import com.university.scheduler.model.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evidence for the section-clash decision (Week 4 Day 1).
 *
 * A section clash is one section listed in two different bookings at the same
 * time slot (for example, batch 1A1A in room X and in room Y at Monday period 3).
 * The engine does not currently check this. This class counts how many existing
 * bookings a strict check would reject, so the decision rests on data.
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass="com.university.scheduler.engine.DecisionAnalysis"
 */
public final class DecisionAnalysis {

    private DecisionAnalysis() {}

    public static void main(String[] args) {
        Path data = args.length > 0 ? Path.of(args[0]) : OptimizationRunDriver.DEFAULT_DATA;
        ConflictChecker checker = new ScheduleLoader().loadFrom(new CsvBookingSource(data));
        Result r = analyse(checker);

        System.out.println();
        System.out.println("============ SECTION-CLASH ANALYSIS ============");
        System.out.printf("Bookings loaded:                   %d%n", checker.totalBookings());
        System.out.printf("Slots with a section clash:        %d%n", r.slotsWithClash);
        System.out.printf("Bookings a strict rule would reject: %d%n", r.wouldReject);
        System.out.println("Examples:");
        r.examples.forEach(e -> System.out.println("  " + e));
        System.out.println("=================================================");
        System.out.println(recommendation(r));
    }

    static Result analyse(ConflictChecker checker) {
        Result r = new Result();

        Map<TimeSlot, List<Booking>> bySlot = checker.getAllBookings().stream()
            .collect(Collectors.groupingBy(Booking::getTimeSlot));

        for (Map.Entry<TimeSlot, List<Booking>> slotEntry : bySlot.entrySet()) {
            Map<String, List<Booking>> bySection = new TreeMap<>();
            for (Booking b : slotEntry.getValue()) {
                for (Section s : b.getSections()) {
                    bySection.computeIfAbsent(s.getSectionCode(), k -> new ArrayList<>()).add(b);
                }
            }

            boolean slotHasClash = false;
            for (Map.Entry<String, List<Booking>> sectionEntry : bySection.entrySet()) {
                List<Booking> holders = sectionEntry.getValue();
                if (holders.size() > 1) {
                    slotHasClash = true;
                    r.wouldReject += holders.size() - 1;
                    if (r.examples.size() < 10) {
                        String rooms = holders.stream()
                            .map(b -> b.getRoom().getRoomId())
                            .collect(Collectors.joining(", "));
                        r.examples.add(slotEntry.getKey() + " section " + sectionEntry.getKey()
                            + " in " + holders.size() + " bookings (rooms: " + rooms + ")");
                    }
                }
            }
            if (slotHasClash) {
                r.slotsWithClash++;
            }
        }
        return r;
    }

    static String recommendation(Result r) {
        if (r.wouldReject == 0) {
            return "RECOMMENDATION: no existing clashes. Enforce the section-clash rule in ConflictChecker.";
        }
        return "RECOMMENDATION: the source data contains " + r.wouldReject + " section clashes. "
            + "Enforcing the rule would reject real bookings, so do not enforce it yet. "
            + "Report it as an advisory warning, and check the examples above against the Excel sheet.";
    }

    static final class Result {
        int slotsWithClash;
        int wouldReject;
        final List<String> examples = new ArrayList<>();
    }
}
