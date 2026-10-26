package com.university.scheduler.engine;

import com.university.scheduler.model.*;

import java.util.*;

/**
 * Computes utilization metrics from a ConflictChecker's current bookings:
 * what fraction of available slots each room is actually being used for,
 * and how each faculty member's load compares to their weekly limit.
 *
 * This is read-only reporting logic — it does not modify the checker.
 * Intended to feed the JavaFX dashboard (Member C) and the project report
 * (room utilization numbers, faculty load-balance numbers).
 */
public class UtilizationReport {
    private final ConflictChecker checker;

    public UtilizationReport(ConflictChecker checker) {
        this.checker = checker;
    }

    /**
     * Returns each room's utilization as a fraction (0.0 - 1.0) of the
     * given candidate slots that it is actually booked for.
     *
     * Example: a room booked in 40 of 84 weekly slots scores ~0.476 (47.6%).
     */
    public Map<Room, Double> computeRoomUtilization(List<Room> allRooms, List<TimeSlot> allSlots) {
        if (allSlots.isEmpty()) {
            throw new IllegalArgumentException("allSlots cannot be empty — utilization is undefined with zero slots");
        }

        Map<Room, Double> utilization = new LinkedHashMap<>();
        int totalSlots = allSlots.size();

        for (Room room : allRooms) {
            long bookedCount = allSlots.stream()
                .filter(slot -> checker.getBookingsAt(slot).stream().anyMatch(b -> b.getRoom().equals(room)))
                .count();
            utilization.put(room, (double) bookedCount / totalSlots);
        }
        return utilization;
    }

    /**
     * Returns rooms sorted by utilization, highest first — useful for a
     * dashboard "most/least used rooms" view without the caller needing
     * to sort the map itself.
     */
    public List<Map.Entry<Room, Double>> computeRoomUtilizationRanked(List<Room> allRooms, List<TimeSlot> allSlots) {
        Map<Room, Double> utilization = computeRoomUtilization(allRooms, allSlots);
        List<Map.Entry<Room, Double>> ranked = new ArrayList<>(utilization.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return ranked;
    }

    /**
     * Returns each faculty member's current load as a fraction (0.0 - 1.0)
     * of their personal maxHoursPerWeek. A value near or at 1.0 signals
     * that faculty member is at or near their limit.
     */
    public Map<Faculty, Double> computeFacultyLoadRatio() {
        Map<Faculty, Integer> loads = checker.getAllFacultyLoads();
        Map<Faculty, Double> ratios = new LinkedHashMap<>();
        for (Map.Entry<Faculty, Integer> entry : loads.entrySet()) {
            Faculty faculty = entry.getKey();
            int currentHours = entry.getValue();
            int max = faculty.getMaxHoursPerWeek();
            ratios.put(faculty, max == 0 ? 0.0 : (double) currentHours / max);
        }
        return ratios;
    }

    /**
     * Returns faculty members whose load ratio is at or above the given
     * threshold (e.g. 0.9 for "90% or more of their weekly limit") — a
     * direct, demo-friendly "who's overloaded or close to it" view.
     */
    public List<Faculty> findFacultyNearLoadLimit(double thresholdRatio) {
        List<Faculty> result = new ArrayList<>();
        for (Map.Entry<Faculty, Double> entry : computeFacultyLoadRatio().entrySet()) {
            if (entry.getValue() >= thresholdRatio) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Overall average room utilization across all given rooms — a single
     * summary number for the report ("average room utilization: 54%").
     */
    public double computeAverageRoomUtilization(List<Room> allRooms, List<TimeSlot> allSlots) {
        Map<Room, Double> utilization = computeRoomUtilization(allRooms, allSlots);
        if (utilization.isEmpty()) {
            return 0.0;
        }
        return utilization.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
