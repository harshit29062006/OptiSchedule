package com.university.scheduler.engine;

import com.university.scheduler.exception.*;
import com.university.scheduler.model.*;

import java.util.*;

/**
 * Core scheduling engine: tracks bookings per TimeSlot and detects
 * room/faculty conflicts before a booking is committed.
 */
public class ConflictChecker implements Schedulable {
    private final Map<TimeSlot, List<Booking>> scheduleMap = new HashMap<>();

    /**
     * Attempts to add a booking. Throws a specific SchedulingException
     * subtype if it conflicts with an existing booking at the same slot.
     */
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
        existing.add(booking);
        scheduleMap.put(slot, existing);
    }

    /**
     * Non-throwing check, useful for the optimization engine to probe
     * "would this work?" without committing or handling exceptions.
     */
    @Override
    public boolean hasConflict(Booking newBooking) {
        List<Booking> existing = scheduleMap.getOrDefault(newBooking.getTimeSlot(), Collections.emptyList());
        return existing.stream().anyMatch(b ->
            b.getRoom().equals(newBooking.getRoom()) || b.getFaculty().equals(newBooking.getFaculty()));
    }

    /** Returns a free room at the given slot from the candidate list, if any exists. */
    public Optional<Room> findFreeRoom(TimeSlot slot, List<Room> candidateRooms) {
        Set<Room> bookedRooms = new HashSet<>();
        for (Booking b : scheduleMap.getOrDefault(slot, Collections.emptyList())) {
            bookedRooms.add(b.getRoom());
        }
        return candidateRooms.stream().filter(r -> !bookedRooms.contains(r)).findFirst();
    }

    public List<Booking> getBookingsAt(TimeSlot slot) {
        return Collections.unmodifiableList(scheduleMap.getOrDefault(slot, Collections.emptyList()));
    }

    public int totalBookings() {
        return scheduleMap.values().stream().mapToInt(List::size).sum();
    }
}
