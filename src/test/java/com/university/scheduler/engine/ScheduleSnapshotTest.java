package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleSnapshotTest {

    @TempDir
    Path dir;

    private static final Course COURSE = new Course("CS101", "Maths, Part 1", 4, PeriodType.LECTURE);

    /** Canonical, order-independent description of every booking. */
    static Set<String> keys(ConflictChecker checker) {
        return checker.getAllBookings().stream()
            .map(b -> b.getTimeSlot().getDay() + "/" + b.getTimeSlot().getPeriodNumber()
                + "|" + b.getRoom().getRoomId()
                + "|" + b.getFaculty().getFacultyId()
                + "|" + b.getSections().stream().map(Section::getSectionCode).sorted().collect(Collectors.joining(","))
                + "|" + b.getCourse().getCourseCode())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    static Map<String, Integer> loadsById(ConflictChecker checker) {
        Map<String, Integer> m = new TreeMap<>();
        checker.getAllFacultyLoads().forEach((f, n) -> m.put(f.getFacultyId(), n));
        return m;
    }

    private static ConflictChecker synthetic() throws Exception {
        Faculty a = new Faculty("A", "Ann Lee", "CSE", 18);
        Faculty b = new Faculty("B", null, null, 5);
        Room r1 = new Room("R1", "Lab One", RoomType.LAB, 60);
        Room r2 = new Room("R2", "R2", RoomType.CLASSROOM, 120);
        var slots = com.university.scheduler.util.TimeSlotGenerator.generateWeek(3, LocalTime.of(8, 0), 50);

        ConflictChecker checker = new ConflictChecker();
        checker.addBooking(new Booking(new Section("S1", "CSE", 1, "A"), COURSE, a, r1, slots.get(0)));
        // Same faculty and room at the same slot: merges into one combined session.
        checker.addBooking(new Booking(new Section("S2", "CSE", 1, "A"), COURSE, a, r1, slots.get(0)));
        checker.addBooking(new Booking(new Section("S3", "ECE", 2, null), COURSE, b, r2, slots.get(4)));
        return checker;
    }

    @Test
    void syntheticRoundTripPreservesBookingsAndLoads() throws Exception {
        ConflictChecker original = synthetic();
        Path file = dir.resolve("snap.bin");

        ScheduleSnapshot.save(original, file);
        ConflictChecker restored = ScheduleSnapshot.load(file);

        assertEquals(keys(original), keys(restored), "every booking must come back identical");
        assertEquals(loadsById(original), loadsById(restored), "faculty loads must come back identical");
        assertEquals(original.totalBookings(), restored.totalBookings());
        assertEquals(original.totalSectionAttendances(), restored.totalSectionAttendances());
    }

    @Test
    void mergedSessionKeepsBothSectionsAfterRoundTrip() throws Exception {
        ConflictChecker original = synthetic();
        Path file = dir.resolve("merged.bin");
        ScheduleSnapshot.save(original, file);

        ConflictChecker restored = ScheduleSnapshot.load(file);
        List<Booking> atSlot = restored.getBookingsAt(
            com.university.scheduler.util.TimeSlotGenerator.generateWeek(3, LocalTime.of(8, 0), 50).get(0));

        assertEquals(1, atSlot.size(), "the combined class is one session");
        assertEquals(2, atSlot.get(0).getSections().size(), "and it still lists both sections");
    }

    @Test
    void realDatasetRoundTripMatchesExactly() throws Exception {
        ConflictChecker original = new ScheduleLoader()
            .loadFrom(new CsvBookingSource(OptimizationRunDriver.DEFAULT_DATA));
        Path file = dir.resolve("real.bin");

        ScheduleSnapshot.save(original, file);
        ConflictChecker restored = ScheduleSnapshot.load(file);

        assertEquals(original.totalBookings(), restored.totalBookings());
        assertEquals(original.totalSectionAttendances(), restored.totalSectionAttendances());
        assertEquals(keys(original), keys(restored));
        assertEquals(loadsById(original), loadsById(restored));
    }

    @Test
    void wrongVersionIsRejected() throws IOException {
        ScheduleSnapshot.Snapshot snap = new ScheduleSnapshot.Snapshot();
        snap.version = 99;
        Path file = dir.resolve("future.bin");
        ScheduleSnapshot.write(snap, file);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ScheduleSnapshot.load(file));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void corruptFileIsRejected() throws IOException {
        Path file = dir.resolve("corrupt.bin");
        Files.write(file, new byte[] {1, 2, 3, 4, 5, 6, 7, 8});

        assertThrows(IOException.class, () -> ScheduleSnapshot.load(file));
    }

    @Test
    void fileWithWrongObjectTypeIsRejected() throws IOException {
        Path file = dir.resolve("wrongtype.bin");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject("not a snapshot");
        }

        assertThrows(IOException.class, () -> ScheduleSnapshot.load(file));
    }
}
