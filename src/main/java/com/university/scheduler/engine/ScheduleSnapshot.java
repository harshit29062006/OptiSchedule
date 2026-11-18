package com.university.scheduler.engine;

import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Saves and restores a committed schedule using Java serialization (Unit 3).
 *
 * The file holds plain serializable records, not the model classes, so the
 * model needs no changes. On load, every booking is re-added through
 * ConflictChecker.addBooking, so a file that breaks the conflict or load
 * rules is rejected instead of silently loaded.
 *
 * The version number is checked on load. A file from a different format
 * version is rejected, so future changes cannot corrupt old data unnoticed.
 */
public final class ScheduleSnapshot {

    static final int VERSION = 1;

    private ScheduleSnapshot() {}

    public static void save(ConflictChecker checker, Path file) throws IOException {
        Snapshot snap = new Snapshot();
        snap.version = VERSION;
        for (Booking b : checker.getAllBookings()) {
            snap.sessions.add(SessionRecord.from(b));
        }
        write(snap, file);
    }

    static void write(Snapshot snap, Path file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeObject(snap);
        }
    }

    public static ConflictChecker load(Path file) throws IOException {
        Object read;
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            read = in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("snapshot refers to a class this build does not contain", e);
        }

        if (!(read instanceof Snapshot snap)) {
            throw new IOException("file is not a schedule snapshot");
        }
        if (snap.version != VERSION) {
            throw new IllegalStateException(
                "unsupported snapshot version " + snap.version + " (this build reads version " + VERSION + ")");
        }

        ConflictChecker checker = new ConflictChecker();
        Map<String, Faculty> faculty = new HashMap<>();
        Map<String, Room> rooms = new HashMap<>();
        Map<String, Course> courses = new HashMap<>();

        for (SessionRecord r : snap.sessions) {
            Booking b = r.toBooking(faculty, rooms, courses);
            try {
                checker.addBooking(b);
            } catch (SchedulingException e) {
                throw new IllegalStateException(
                    "snapshot is internally inconsistent: " + e.getMessage(), e);
            }
        }
        return checker;
    }

    /** Top-level file content. Package-private so tests can write a deliberately wrong version. */
    static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        int version;
        final List<SessionRecord> sessions = new ArrayList<>();
    }

    /** One booking, flattened into plain fields. */
    static final class SessionRecord implements Serializable {
        private static final long serialVersionUID = 1L;

        String day;
        int period;
        String startTime;
        String endTime;

        String courseCode;
        String courseName;
        Integer credits;
        String periodType;

        String facultyId;
        String facultyName;
        String department;
        int facultyMaxHours;

        String roomId;
        String roomName;
        String roomType;
        int roomCapacity;

        List<SectionRecord> sections = new ArrayList<>();

        static SessionRecord from(Booking b) {
            SessionRecord r = new SessionRecord();

            TimeSlot t = b.getTimeSlot();
            r.day = t.getDay().name();
            r.period = t.getPeriodNumber();
            r.startTime = t.getStartTime() == null ? null : t.getStartTime().toString();
            r.endTime = t.getEndTime() == null ? null : t.getEndTime().toString();

            Course c = b.getCourse();
            r.courseCode = c.getCourseCode();
            r.courseName = c.getCourseName();
            r.credits = c.getCredits();
            r.periodType = c.getPeriodType() == null ? null : c.getPeriodType().name();

            Faculty f = b.getFaculty();
            r.facultyId = f.getFacultyId();
            r.facultyName = f.getFacultyName();
            r.department = f.getDepartment();
            r.facultyMaxHours = f.getMaxHoursPerWeek();

            Room room = b.getRoom();
            r.roomId = room.getRoomId();
            r.roomName = room.getRoomName();
            r.roomType = room.getRoomType() == null ? null : room.getRoomType().name();
            r.roomCapacity = room.getCapacity();

            for (Section s : b.getSections()) {
                SectionRecord sr = new SectionRecord();
                sr.code = s.getSectionCode();
                sr.branch = s.getBranchId();
                sr.year = s.getYear();
                sr.division = s.getDivision();
                r.sections.add(sr);
            }
            return r;
        }

        /** Rebuilds the booking. Shared faculty, room and course objects come from the caches. */
        Booking toBooking(Map<String, Faculty> faculty, Map<String, Room> rooms, Map<String, Course> courses)
                throws IllegalStateException {
            if (sections.isEmpty()) {
                throw new IllegalStateException("snapshot session has no sections");
            }
            TimeSlot slot = new TimeSlot(Day.valueOf(day), period, parseTime(startTime), parseTime(endTime));

            Course course = courses.computeIfAbsent(courseCode, k -> new Course(
                courseCode, courseName, credits,
                periodType == null ? null : PeriodType.valueOf(periodType)));

            Faculty f = faculty.computeIfAbsent(facultyId, k ->
                new Faculty(facultyId, facultyName, department, facultyMaxHours));

            Room room = rooms.computeIfAbsent(roomId, k -> new Room(
                roomId, roomName, roomType == null ? null : RoomType.valueOf(roomType), roomCapacity));

            List<Section> built = sections.stream()
                .map(sr -> new Section(sr.code, sr.branch, sr.year, sr.division))
                .collect(Collectors.toList());

            Booking booking = new Booking(built.get(0), course, f, room, slot);
            for (int i = 1; i < built.size(); i++) {
                booking.addSection(built.get(i));
            }
            return booking;
        }

        private static LocalTime parseTime(String s) {
            return s == null ? null : LocalTime.parse(s);
        }
    }

    /** One section inside a session. */
    static final class SectionRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        String code;
        String branch;
        int year;
        String division;
    }
}
