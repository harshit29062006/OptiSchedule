package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.*;

/**
 * Reads the parsed timetable CSV directly and builds Booking objects,
 * without needing a database or Member A's DAO layer.
 *
 * Expected column order: sheet,day,period,start_time,section,course_code,room_faculty_raw
 *
 * KNOWN GAP (documented in SCHEMA.md from Week 1): room_faculty_raw is not
 * yet split into a real room ID and a real faculty ID. This loader uses the
 * same raw string as a placeholder for BOTH, so the pipeline runs end-to-end,
 * but this means room-conflict and faculty-conflict checks are currently
 * checking the same value twice, not two independent constraints. Replace
 * this placeholder logic once Member A delivers a clean room/faculty split
 * (see splitRoomFaculty() below — swap its body out when that's ready).
 */
public class CsvBookingSource implements BookingSource {
    private final Path csvPath;

    public CsvBookingSource(Path csvPath) {
        this.csvPath = csvPath;
    }

    @Override
    public List<Booking> loadAll() {
        List<Booking> bookings = new ArrayList<>();
        Map<String, Faculty> facultyCache = new HashMap<>();
        Map<String, Room> roomCache = new HashMap<>();
        Map<String, Course> courseCache = new HashMap<>();
        Map<String, Section> sectionCache = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String header = reader.readLine(); // skip header row
            String line;
            int lineNum = 1;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                String[] parts = splitCsvLine(line);
                if (parts.length < 7) {
                    continue;
                }

                String day = parts[1].trim();
                String periodStr = parts[2].trim();
                String startTimeStr = parts[3].trim();
                String sectionCode = parts[4].trim();
                String courseCodeRaw = parts[5].trim();
                String roomFacultyRaw = parts[6].trim();

                if (day.isEmpty() || periodStr.isEmpty() || courseCodeRaw.isEmpty()) {
                    continue; // skip rows missing essential fields (free periods, known data gaps)
                }

                try {
                    Day dayEnum = Day.valueOf(day.toUpperCase());
                    int period = Integer.parseInt(periodStr);
                    LocalTime startTime = startTimeStr.isEmpty() ? null : LocalTime.parse(startTimeStr);
                    TimeSlot slot = new TimeSlot(dayEnum, period, startTime, null);

                    // Split course code into base code + suffix letter (e.g. UCB009P -> UCB009, 'P')
                    String baseCode = courseCodeRaw.replaceAll("[A-Z]$", "");
                    char suffix = courseCodeRaw.charAt(courseCodeRaw.length() - 1);
                    PeriodType type = PeriodType.fromSuffix(suffix);
                    Course course = courseCache.computeIfAbsent(baseCode,
                        c -> new Course(c, null, null, type));

                    Section section = sectionCache.computeIfAbsent(sectionCode,
                        s -> new Section(s, null, 0, null));

                    String[] roomFacultySplit = splitRoomFaculty(roomFacultyRaw);
                    String roomId = roomFacultySplit[0];
                    String facultyId = roomFacultySplit[1];

                    Faculty faculty = facultyCache.computeIfAbsent(facultyId,
                        f -> new Faculty(f, null, null, 18));
                    Room room = roomCache.computeIfAbsent(roomId,
                        r -> new Room(r, r, RoomType.CLASSROOM, 60));

                    bookings.add(new Booking(section, course, faculty, room, slot));
                } catch (Exception rowEx) {
                    System.err.println("Line " + lineNum + ": skipping unparseable row (" + rowEx.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV at " + csvPath, e);
        }

        return bookings;
    }

    /**
     * PLACEHOLDER split logic. Currently returns the same raw string for both
     * room and faculty, since the real format isn't consistent yet (see class
     * javadoc). Replace this method's body once a real split rule is confirmed
     * with Member A — nothing else in this class needs to change.
     *
     * @return String[2] = { roomId, facultyId }
     */
    private String[] splitRoomFaculty(String raw) {
        if (raw == null || raw.isBlank()) {
            return new String[]{ "UNKNOWN_ROOM", "UNKNOWN_FACULTY" };
        }
        return new String[]{ raw, raw };
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }
}
