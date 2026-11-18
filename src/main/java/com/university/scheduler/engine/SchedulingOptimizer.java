package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;

import java.util.*;

/**
 * Greedy scheduling optimizer: given a set of PendingAssignments (classes
 * that need a room+slot) and a pool of candidate TimeSlots, assigns each
 * one a valid (room, slot) pair, scheduling the MOST CONSTRAINED
 * assignments first (fewest available options), since those are the ones
 * most likely to become impossible to place if left until later.
 *
 * This is a "first-fit" greedy strategy, not a globally optimal solver —
 * appropriate for this course's scope. It will not always find a solution
 * even when one exists (a true optimal solver would need backtracking or
 * constraint propagation, which is out of scope here). Document this
 * honestly in the report: greedy, not exhaustive/optimal.
 */
public class SchedulingOptimizer {
    private final ConflictChecker checker;
    private final List<TimeSlot> allSlots;

    public SchedulingOptimizer(ConflictChecker checker, List<TimeSlot> allSlots) {
        this.checker = checker;
        this.allSlots = allSlots;
    }

    /**
     * Attempts to schedule every PendingAssignment. Returns the list of
     * Bookings successfully created. Assignments that couldn't be placed
     * (no valid room+slot found) are reported separately via the
     * unscheduled output parameter, rather than silently dropped.
     *
     * @param pending      the assignments to schedule
     * @param unscheduled  an empty, mutable list the caller provides; any
     *                     PendingAssignment that could not be placed is
     *                     added to it, so the caller can see what failed
     *                     and why (e.g. for reporting or manual review)
     */
    public List<Booking> scheduleAll(List<PendingAssignment> pending, List<PendingAssignment> unscheduled) {
        // Most constrained first: assignments with fewer available (room, slot)
        // options are scheduled before ones with more options, since they're
        // more likely to become impossible to place if slots fill up first.
        List<PendingAssignment> sorted = new ArrayList<>(pending);
        sorted.sort(Comparator.comparingInt(this::countAvailableOptions));

        List<Booking> scheduled = new ArrayList<>();

        for (PendingAssignment pa : sorted) {
            Optional<Booking> result = tryPlace(pa);
            if (result.isPresent()) {
                scheduled.add(result.get());
            } else {
                unscheduled.add(pa);
            }
        }

        return scheduled;
    }

    /**
     * Tries each candidate room (in the order given on the PendingAssignment)
     * and, for each, searches allSlots for the first valid opening. Returns
     * the first (room, slot) combination that works, or empty if none of the
     * candidate rooms have ANY valid slot for this faculty.
     */
    private Optional<Booking> tryPlace(PendingAssignment pa) {
        for (Room room : pa.getCandidateRooms()) {
            Optional<TimeSlot> slot = checker.findAvailableSlot(pa.getFaculty(), room, allSlots);
            if (slot.isPresent()) {
                Booking booking = new Booking(pa.getSection(), pa.getCourse(), pa.getFaculty(), room, slot.get());
                try {
                    checker.addBooking(booking);
                    return Optional.of(booking);
                } catch (SchedulingException e) {
                    // Should be rare: findAvailableSlot already checked this exact
                    // (faculty, room, slot) combo is free. Could happen if another
                    // assignment was placed into this exact slot in between the
                    // check and the add — not possible in this single-threaded
                    // loop today, but kept defensive in case scheduleAll is later
                    // made concurrent. Treat as "this room didn't work", try the next.
                    continue;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Counts how many (room, slot) combinations are currently valid for this
     * assignment, across all its candidate rooms. Used to sort "most
     * constrained first". This does NOT mutate the checker — it's a pure
     * count, using the same findAvailableSlot logic but without committing.
     *
     * Note: this re-checks each room's full slot list independently, so an
     * assignment with many candidate rooms is more expensive to score — fine
     * at this project's scale (tens to low hundreds of assignments), but
     * would need optimizing for much larger inputs.
     */
    private int countAvailableOptions(PendingAssignment pa) {
        int count = 0;
        for (Room room : pa.getCandidateRooms()) {
            for (TimeSlot slot : allSlots) {
                List<Booking> atSlot = checker.getBookingsAt(slot);
                boolean roomBusy = atSlot.stream().anyMatch(b -> b.getRoom().equals(room));
                boolean facultyBusy = atSlot.stream().anyMatch(b -> b.getFaculty().equals(pa.getFaculty()));
                boolean wouldOverload = checker.getFacultyLoad(pa.getFaculty()) + 1 > pa.getFaculty().getMaxHoursPerWeek();
                if (!roomBusy && !facultyBusy && !wouldOverload) {
                    count++;
                }
            }
        }
        return count;
    }
}
