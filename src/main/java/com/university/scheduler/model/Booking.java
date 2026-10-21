package com.university.scheduler.model;

import java.util.Objects;

/**
 * A single scheduled class period: a Section attending a Course,
 * taught by a Faculty, in a Room, at a TimeSlot.
 * This is the central entity the conflict-checking engine operates on.
 */
public class Booking {
    private final Section section;
    private final Course course;
    private final Faculty faculty;
    private final Room room;
    private final TimeSlot timeSlot;

    public Booking(Section section, Course course, Faculty faculty, Room room, TimeSlot timeSlot) {
        this.section = Objects.requireNonNull(section, "section cannot be null");
        this.course = Objects.requireNonNull(course, "course cannot be null");
        this.faculty = Objects.requireNonNull(faculty, "faculty cannot be null");
        this.room = Objects.requireNonNull(room, "room cannot be null");
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot cannot be null");
    }

    public Section getSection() { return section; }
    public Course getCourse() { return course; }
    public Faculty getFaculty() { return faculty; }
    public Room getRoom() { return room; }
    public TimeSlot getTimeSlot() { return timeSlot; }

    @Override
    public String toString() {
        return section + " | " + course + " | " + faculty + " | " + room + " | " + timeSlot;
    }
}
