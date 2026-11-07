package com.university.scheduler.engine;

import com.university.scheduler.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exports the committed timetable as CSV, one row per session.
 *
 * Sections of a combined class are joined with "; " in one cell. Fields that
 * contain commas, quotes or line breaks are quoted, with inner quotes doubled
 * (RFC 4180). The file starts with a UTF-8 byte-order mark, so Excel reads
 * non-ASCII names correctly.
 */
public final class ScheduleCsvExporter {

    static final String HEADER = "Day,Period,Start,Course,CourseName,Faculty,Room,Sections,SectionCount";
    static final String BOM = "\uFEFF";

    private ScheduleCsvExporter() {}

    public static void export(ConflictChecker checker, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(BOM + HEADER);

        checker.getAllBookings().stream()
            .sorted(Comparator
                .comparingInt((Booking b) -> b.getTimeSlot().getDay().ordinal())
                .thenComparingInt(b -> b.getTimeSlot().getPeriodNumber())
                .thenComparing(b -> b.getRoom().getRoomId()))
            .forEach(b -> lines.add(row(b)));

        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    static String row(Booking b) {
        TimeSlot t = b.getTimeSlot();
        String sections = b.getSections().stream()
            .map(Section::getSectionCode)
            .collect(Collectors.joining("; "));

        return String.join(",",
            escape(t.getDay().name()),
            escape(String.valueOf(t.getPeriodNumber())),
            escape(t.getStartTime() == null ? "" : t.getStartTime().toString()),
            escape(b.getCourse().getCourseCode()),
            escape(b.getCourse().getCourseName() == null ? "" : b.getCourse().getCourseName()),
            escape(b.getFaculty().getFacultyId()),
            escape(b.getRoom().getRoomId()),
            escape(sections),
            escape(String.valueOf(b.getSections().size())));
    }

    /** Quotes a field when it contains a delimiter, quote or line break. Doubles inner quotes. */
    static String escape(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
