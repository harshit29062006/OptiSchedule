package com.university.scheduler.engine;

import com.university.scheduler.model.*;
import com.university.scheduler.util.TimeSlotGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleCsvExporterTest {

    @TempDir
    Path dir;

    private static final Course COURSE = new Course("CS101", "Maths, Part 1", 4, PeriodType.LECTURE);

    private static ConflictChecker sample() throws Exception {
        Faculty a = new Faculty("A", "Ann", "CSE", 18);
        Room r1 = new Room("R1", "R1", RoomType.CLASSROOM, 60);
        Room r2 = new Room("R2", "R2", RoomType.CLASSROOM, 60);
        var slots = TimeSlotGenerator.generateWeek(3, LocalTime.of(8, 0), 50);

        ConflictChecker checker = new ConflictChecker();
        checker.addBooking(new Booking(new Section("S1", "CSE", 1, "A"), COURSE, a, r1, slots.get(0)));
        checker.addBooking(new Booking(new Section("S2", "CSE", 1, "A"), COURSE, a, r1, slots.get(0)));
        checker.addBooking(new Booking(new Section("S3", "CSE", 1, "A"), COURSE, a, r2, slots.get(5)));
        return checker;
    }

    @Test
    void headerIsFirstWithByteOrderMark() throws Exception {
        Path file = dir.resolve("out.csv");
        ScheduleCsvExporter.export(sample(), file);

        String first = Files.readAllLines(file, StandardCharsets.UTF_8).get(0);
        assertTrue(first.startsWith(ScheduleCsvExporter.BOM), "Excel needs the BOM to read UTF-8");
        assertTrue(first.contains("Day,Period,Start,Course,CourseName,Faculty,Room,Sections,SectionCount"));
    }

    @Test
    void oneDataRowPerSession() throws Exception {
        ConflictChecker checker = sample();
        Path file = dir.resolve("rows.csv");
        ScheduleCsvExporter.export(checker, file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(checker.totalBookings() + 1, lines.size(), "header plus one row per session");
    }

    @Test
    void commaInCourseNameIsQuoted() throws Exception {
        Path file = dir.resolve("quote.csv");
        ScheduleCsvExporter.export(sample(), file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"Maths, Part 1\""), "a comma in a field must be quoted");
    }

    @Test
    void combinedClassListsBothSectionsInOneCell() throws Exception {
        Path file = dir.resolve("merged.csv");
        ScheduleCsvExporter.export(sample(), file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("S1; S2"), "a merged session shows its sections in one cell");
        assertTrue(content.contains(",2\n") || content.contains(",2\r\n"), "and its section count is 2");
    }

    @Test
    void escapeDoublesInnerQuotes() {
        assertEquals("\"say \"\"hi\"\"\"", ScheduleCsvExporter.escape("say \"hi\""));
        assertEquals("plain", ScheduleCsvExporter.escape("plain"));
        assertEquals("", ScheduleCsvExporter.escape(null));
    }

    @Test
    void realDatasetExportHasOneRowPerBooking() throws Exception {
        ConflictChecker checker = new ScheduleLoader()
            .loadFrom(new CsvBookingSource(OptimizationRunDriver.DEFAULT_DATA));
        Path file = dir.resolve("real.csv");
        ScheduleCsvExporter.export(checker, file);

        long lines = Files.lines(file, StandardCharsets.UTF_8).count();
        assertEquals(checker.totalBookings() + 1, lines);
    }
}
