package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.*;

/**
 * Reads the parsed timetable CSV directly and builds Booking objects,
 * without needing a database or Member A's DAO layer.
 *
 * Expected column order: sheet,day,period,start_time,section,course_code,room_faculty_raw
 *
 * ROOM/FACULTY SPLIT (confirmed against real examples, see notes below):
 * The source Excel's room_faculty_raw field mixes room identifiers and
 * faculty initials in the SAME field with no fixed position — there is no
 * general rule that tells which side (if split) is which, so this class
 * applies pattern rules:
 *
 *   1. "CODE(CODE)" e.g. "CBOL(G141)", "PL-2(L008)"
 *      -> CONFIRMED: both sides name the SAME physical room (a building/
 *         wing code plus the standard room number). The parenthesized
 *         part (e.g. "G141", "L008") is used as the canonical room ID,
 *         since it matches the same building-prefix+digits convention as
 *         plain room codes elsewhere in the data, so it merges correctly
 *         with any bare occurrence of that room code. Faculty is NOT
 *         present in this field for these rows and is left UNRESOLVED
 *         (unique synthetic ID per row).
 *
 *   2. A bare code matching a known ROOM pattern (building-letter prefix
 *      + digits, e.g. "E305", "LP101", "T105", "G141")
 *      -> treated as room; faculty is UNRESOLVED (unique synthetic ID
 *         per row, so unrelated classes are never falsely merged into
 *         one "faculty member")
 *
 *   3. A bare code NOT matching the room pattern (e.g. "PWS", "SBH", "APC7")
 *      -> CONFIRMED: these are faculty initials. Treated as faculty; room
 *         is UNRESOLVED (same unique-ID treatment).
 *
 *   4. A "/"-separated value e.g. "H222/C201" or "NIN/APC3"
 *      -> CONFIRMED: this represents a MID-SEMESTER CHANGE — the first
 *         value is used before mid-semester, the second after. Whether
 *         both sides are rooms (e.g. "H222/C201") or both faculty (e.g.
 *         "NIN/APC3") is detected by matching each half against the room
 *         pattern. KNOWN SIMPLIFICATION: this model has no concept of a
 *         semester split — a Booking is one fixed (room, faculty) pair for
 *         the whole term. We use the FIRST (pre-mid-sem) value as the
 *         canonical identifier. This under-represents the real schedule
 *         for the second half of the semester; modeling the mid-sem
 *         change properly would require TimeSlot or Booking to carry a
 *         semester-half dimension, which is out of scope for this pass.
 *
 * WHY UNIQUE SYNTHETIC IDS MATTER: an earlier version of this class reused
 * the same raw string as faculty AND room identity. That caused a serious
 * bug: every class ever held in room "LP101", taught by many DIFFERENT real
 * faculty over the week, was counted as ONE "faculty member named LP101"
 * hitting their weekly hour cap almost immediately — rejecting ~300 valid
 * real bookings as false "FacultyOverloadException" conflicts. Giving each
 * unresolved side a unique ID per row prevents this cross-contamination,
 * at the honest cost of not tracking real load for faculty/rooms this
 * heuristic couldn't confidently identify.
 *
 * This heuristic has NOT been verified against the original Excel file's
 * column structure (not available in this environment). Treat its output
 * as a best-effort improvement, not a final, confirmed clean dataset.
 */
public class CsvBookingSource implements BookingSource {
    private final Path csvPath;

    // Matches common room-code patterns seen in the real data: an optional
    // short letter prefix (building/block code, e.g. LP, LT, PL, E, C, G, T,
    // F, B, H, RF, RCH) followed by digits, optionally with a hyphen.
    private static final Pattern ROOM_PATTERN =
        Pattern.compile("^(LP|LT|PL|RCH|RF|[A-Z]{1,2})-?\\d{2,4}[A-Za-z]?$");

    private static final Pattern PAREN_PATTERN =
        Pattern.compile("^([A-Za-z0-9\\-]+)\\(([A-Za-z0-9]+)\\)$");

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
                    continue;
                }

                try {
                    Day dayEnum = Day.valueOf(day.toUpperCase());
                    int period = Integer.parseInt(periodStr);
                    LocalTime startTime = startTimeStr.isEmpty() ? null : LocalTime.parse(startTimeStr);
                    TimeSlot slot = new TimeSlot(dayEnum, period, startTime, null);

                    String baseCode = courseCodeRaw.replaceAll("[A-Z]$", "");
                    char suffix = courseCodeRaw.charAt(courseCodeRaw.length() - 1);
                    PeriodType type = PeriodType.fromSuffix(suffix);
                    Course course = courseCache.computeIfAbsent(baseCode,
                        c -> new Course(c, null, null, type));

                    Section section = sectionCache.computeIfAbsent(sectionCode,
                        s -> new Section(s, null, 0, null));

                    // Unique seed for this row, used only when a side can't be
                    // resolved and needs a synthetic, non-colliding ID.
                    String rowSeed = sectionCode + "_" + dayEnum + "_" + period;

                    Split split = splitRoomFaculty(roomFacultyRaw, rowSeed);

                    Faculty faculty = facultyCache.computeIfAbsent(split.facultyId,
                        f -> new Faculty(f, null, null, 18));
                    Room room = roomCache.computeIfAbsent(split.roomId,
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

    /** Holds the resolved (or synthetically-assigned) room and faculty IDs for one row. */
    private static final class Split {
        final String roomId;
        final String facultyId;
        Split(String roomId, String facultyId) { this.roomId = roomId; this.facultyId = facultyId; }
    }

    private Split splitRoomFaculty(String raw, String rowSeed) {
        if (raw == null || raw.isBlank()) {
            return new Split("UNKNOWN_ROOM_" + rowSeed, "UNKNOWN_FACULTY_" + rowSeed);
        }

        String trimmed = raw.trim();

        // Case 1: "CODE(CODE)" pattern — CONFIRMED both sides name the same
        // room. Use the parenthesized part as the canonical room ID, since
        // it matches the standard room-code convention used elsewhere.
        Matcher parenMatch = PAREN_PATTERN.matcher(trimmed);
        if (parenMatch.matches()) {
            return new Split(parenMatch.group(2), "UNKNOWN_FACULTY_" + rowSeed);
        }

        // Case 4: "/"-separated — CONFIRMED mid-semester change (first value
        // = before mid-sem, second = after). We take the first value as the
        // canonical identifier (see class javadoc "KNOWN SIMPLIFICATION").
        // Detect whether this is a room/room or faculty/faculty pair by
        // matching the first half against the room pattern.
        if (trimmed.contains("/")) {
            String firstHalf = trimmed.split("/", 2)[0].trim();
            if (ROOM_PATTERN.matcher(firstHalf).matches()) {
                return new Split(firstHalf, "UNKNOWN_FACULTY_" + rowSeed);
            } else {
                return new Split("UNKNOWN_ROOM_" + rowSeed, firstHalf);
            }
        }

        // Case 2: looks like a room code
        if (ROOM_PATTERN.matcher(trimmed).matches()) {
            return new Split(trimmed, "UNKNOWN_FACULTY_" + rowSeed);
        }

        // Case 3: doesn't look like a room -> assume faculty initials
        return new Split("UNKNOWN_ROOM_" + rowSeed, trimmed);
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }
}
