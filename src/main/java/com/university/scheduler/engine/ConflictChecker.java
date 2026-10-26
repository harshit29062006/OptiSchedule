package com.university.scheduler.engine;

import com.university.scheduler.exception.*;
import com.university.scheduler.model.*;

import java.util.*;

/**
 * Core scheduling engine: tracks bookings per TimeSlot and detects
 * room/faculty conflicts before a booking is committed. Also tracks
 * each faculty member's running weekly teaching load and enforces
 * their maxHoursPerWeek limit, and can search for a valid open slot
 * given a faculty, room, and candidate TimeSlots.
 */
public class ConflictChecker implements Schedulable {
    private final Map<TimeSlot, List<Booking>> scheduleMap = new HashMap<>();
    private final Map<Faculty, Integer> facultyHoursThisWeek = new HashMap<>();

    public void addBooking(Booking booking) throws SchedulingException {
        TimeSlot slot = booking.getTimeSlot();
        List<Booking> existing = scheduleMap.getOrDefault(slot, new ArrayList<>());

        for (Booking b : existing) {
            if (b.getRoom().equals(booking.getRoom())) {
                throw new RoomUnavailableException(
                    "Room " + booking.getRoom() + " already booked at " + slot
                    + " (existing: " + b.getSection() + ", new: " + booking.getSection() + ")");
            }
            if (b.getFaculty().equals(booking.getFaculty())) {
                throw new SlotConflictException(
                    "Faculty " + booking.getFaculty() + " already teaching at " + slot
                    + " (existing: " + b.getSection() + ", new: " + booking.getSection() + ")");
            }
        }

        Faculty faculty = booking.getFaculty();
        int currentHours = facultyHoursThisWeek.getOrDefault(faculty, 0);
        if (currentHours + 1 > faculty.getMaxHoursPerWeek()) {
            throw new FacultyOverloadException(
                "Faculty " + faculty + " would exceed max hours/week ("
                + faculty.getMaxHoursPerWeek() + "); currently at " + currentHours);
        }

        existing.add(booking);
        scheduleMap.put(slot, existing);
        facultyHoursThisWeek.merge(faculty, 1, Integer::sum);
    }

    @Override
    public boolean hasConflict(Booking newBooking) {
        List<Booking> existing = scheduleMap.getOrDefault(newBooking.getTimeSlot(), Collections.emptyList());
        return existing.stream().anyMatch(b ->
            b.getRoom().equals(newBooking.getRoom()) || b.getFaculty().equals(newBooking.getFaculty()));
    }

    public boolean wouldExceedLoad(Booking newBooking) {
        Faculty faculty = newBooking.getFaculty();
        int currentHours = facultyHoursThisWeek.getOrDefault(faculty, 0);
        return currentHours + 1 > faculty.getMaxHoursPerWeek();
    }

    public int getFacultyLoad(Faculty faculty) {
        return facultyHoursThisWeek.getOrDefault(faculty, 0);
    }

    public Map<Faculty, Integer> getAllFacultyLoads() {
        return Collections.unmodifiableMap(facultyHoursThisWeek);
    }

    public Optional<Room> findFreeRoom(TimeSlot slot, List<Room> candidateRooms) {
        Set<Room> bookedRooms = new HashSet<>();
        for (Booking b : scheduleMap.getOrDefault(slot, Collections.emptyList())) {
            bookedRooms.add(b.getRoom());
        }
        return candidateRooms.stream().filter(r -> !bookedRooms.contains(r)).findFirst();
    }

    /**
     * Finds the first TimeSlot (from candidateSlots, in the order given) at
     * which BOTH the given faculty and the given room are free, AND the
     * faculty would not exceed their weekly load limit by taking it.
     *
     * This is the core "when can this actually be scheduled?" query used by
     * the optimization engine (Day 4 onward) to place pending assignments.
     *
     * Order of candidateSlots matters: pass them pre-sorted by whatever
     * priority you want (e.g. earliest-in-week-first) since this returns
     * the FIRST valid match, not the "best" one by any other metric.
     */
    public Optional<TimeSlot> findAvailableSlot(Faculty faculty, Room room, List<TimeSlot> candidateSlots) {
        for (TimeSlot slot : candidateSlots) {
            List<Booking> atSlot = getBookingsAt(slot);

            boolean roomBusy = atSlot.stream().anyMatch(b -> b.getRoom().equals(room));
            boolean facultyBusy = atSlot.stream().anyMatch(b -> b.getFaculty().equals(faculty));

            int currentHours = facultyHoursThisWeek.getOrDefault(faculty, 0);
            boolean wouldOverload = currentHours + 1 > faculty.getMaxHoursPerWeek();

            if (!roomBusy && !facultyBusy && !wouldOverload) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public List<Booking> getBookingsAt(TimeSlot slot) {
        return Collections.unmodifiableList(scheduleMap.getOrDefault(slot, Collections.emptyList()));
    }

    public int totalBookings() {
        return scheduleMap.values().stream().mapToInt(List::size).sum();
    }
}
