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
 *
 * COMBINED-CLASS HANDLING (confirmed real-world rule): if a new booking
 * has the SAME faculty AND the SAME room as an existing booking at the
 * same slot, it is not a conflict — it's one faculty teaching one
 * combined session to multiple sections at once (e.g. a shared elective).
 * In that case the new booking's section is MERGED into the existing
 * booking rather than rejected, and no additional faculty load is
 * counted (it's still one teaching period, not several).
 *
 * TRUE conflicts remain:
 *   - Same faculty, same slot, DIFFERENT room -> SlotConflictException
 *     (a faculty cannot teach in two rooms at once)
 *   - Same room, same slot, DIFFERENT faculty -> RoomUnavailableException
 *     (the room is genuinely double-booked by an unrelated session)
 */
public class ConflictChecker implements Schedulable {
    private final Map<TimeSlot, List<Booking>> scheduleMap = new HashMap<>();
    private final Map<Faculty, Integer> facultyHoursThisWeek = new HashMap<>();

    public void addBooking(Booking booking) throws SchedulingException {
        TimeSlot slot = booking.getTimeSlot();
        List<Booking> existing = scheduleMap.getOrDefault(slot, new ArrayList<>());

        for (Booking b : existing) {
            boolean sameRoom = b.getRoom().equals(booking.getRoom());
            boolean sameFaculty = b.getFaculty().equals(booking.getFaculty());

            if (sameRoom && sameFaculty) {
                // Same combined session: merge the new section(s) into the
                // existing booking. No conflict, no additional load counted
                // — this is still one teaching period for this faculty.
                for (Section s : booking.getSections()) {
                    b.addSection(s);
                }
                return;
            }
            if (sameRoom) {
                // Different faculty, same room, same slot: genuine double-booking.
                throw new RoomUnavailableException(
                    "Room " + booking.getRoom() + " already booked at " + slot
                    + " (existing: " + b.getFaculty() + " teaching " + b.getSections()
                    + ", new: " + booking.getFaculty() + " teaching " + booking.getSections() + ")");
            }
            if (sameFaculty) {
                // Same faculty, different room, same slot: can't be in two places at once.
                throw new SlotConflictException(
                    "Faculty " + booking.getFaculty() + " already teaching in room " + b.getRoom()
                    + " at " + slot + "; cannot also teach in " + booking.getRoom()
                    + " (existing: " + b.getSections() + ", new: " + booking.getSections() + ")");
            }
        }

        // No existing booking at this slot shares room or faculty — genuinely new booking.
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
        for (Booking b : existing) {
            boolean sameRoom = b.getRoom().equals(newBooking.getRoom());
            boolean sameFaculty = b.getFaculty().equals(newBooking.getFaculty());
            if (sameRoom && sameFaculty) {
                continue; // would merge, not conflict
            }
            if (sameRoom || sameFaculty) {
                return true;
            }
        }
        return false;
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

    public Optional<TimeSlot> findAvailableSlot(Faculty faculty, Room room, List<TimeSlot> candidateSlots) {
        for (TimeSlot slot : candidateSlots) {
            List<Booking> atSlot = getBookingsAt(slot);

            boolean roomBusyByOther = atSlot.stream().anyMatch(b ->
                b.getRoom().equals(room) && !b.getFaculty().equals(faculty));
            boolean facultyBusyElsewhere = atSlot.stream().anyMatch(b ->
                b.getFaculty().equals(faculty) && !b.getRoom().equals(room));

            int currentHours = facultyHoursThisWeek.getOrDefault(faculty, 0);
            boolean wouldOverload = currentHours + 1 > faculty.getMaxHoursPerWeek();

            if (!roomBusyByOther && !facultyBusyElsewhere && !wouldOverload) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public List<Booking> getBookingsAt(TimeSlot slot) {
        return Collections.unmodifiableList(scheduleMap.getOrDefault(slot, Collections.emptyList()));
    }

    /** Total DISTINCT booking sessions (a combined class counts once, not per section). */
    public int totalBookings() {
        return scheduleMap.values().stream().mapToInt(List::size).sum();
    }

    /** Total section-attendances represented (a combined class with 5 sections counts as 5). */
    public int totalSectionAttendances() {
        return scheduleMap.values().stream()
            .flatMap(List::stream)
            .mapToInt(b -> b.getSections().size())
            .sum();
    }
}
