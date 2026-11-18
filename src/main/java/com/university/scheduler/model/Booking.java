package com.university.scheduler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single scheduled class SESSION: a Course taught by a Faculty, in a
 * Room, at a TimeSlot, attended by one or more Sections.
 *
 * Multiple sections are legitimately in ONE Booking when they attend a
 * combined/shared/elective class together (same faculty, same room, same
 * slot) — this is normal in real timetables (e.g. an elective taught once
 * to several batches), not a scheduling conflict. ConflictChecker is
 * responsible for merging new sections into an existing Booking when it
 * detects this case, rather than rejecting it as a clash.
 *
 * A TRUE conflict is: same faculty at the same slot in a DIFFERENT room
 * (can't be in two places at once), or the same room at the same slot
 * used by a DIFFERENT faculty (room double-booked).
 */
public class Booking {
    private final List<Section> sections = new ArrayList<>();
    private final Course course;
    private final Faculty faculty;
    private final Room room;
    private final TimeSlot timeSlot;

    public Booking(Section section, Course course, Faculty faculty, Room room, TimeSlot timeSlot) {
        this.sections.add(Objects.requireNonNull(section, "section cannot be null"));
        this.course = Objects.requireNonNull(course, "course cannot be null");
        this.faculty = Objects.requireNonNull(faculty, "faculty cannot be null");
        this.room = Objects.requireNonNull(room, "room cannot be null");
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot cannot be null");
    }

    /** Backward-compatible accessor: returns the first section. Prefer getSections() for new code. */
    public Section getSection() {
        return sections.get(0);
    }

    public List<Section> getSections() {
        return Collections.unmodifiableList(sections);
    }

    /** Adds another section to this booking, representing a combined/shared class session. */
    public void addSection(Section section) {
        if (!sections.contains(section)) {
            sections.add(section);
        }
    }

    public Course getCourse() { return course; }
    public Faculty getFaculty() { return faculty; }
    public Room getRoom() { return room; }
    public TimeSlot getTimeSlot() { return timeSlot; }

    @Override
    public String toString() {
        String sectionList = sections.size() == 1
            ? sections.get(0).toString()
            : sections.size() + " sections (" + sections.get(0) + ", ...)";
        return sectionList + " | " + course + " | " + faculty + " | " + room + " | " + timeSlot;
    }
}
