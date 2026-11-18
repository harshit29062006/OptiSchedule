package com.university.scheduler.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a single class that still needs a (room, timeSlot) assigned
 * to it. This is the input to SchedulingOptimizer: "Section X needs Course
 * Y taught by Faculty Z, in one of these candidate Rooms, at some point."
 *
 * Not yet a Booking — a Booking only exists once the optimizer has actually
 * found it a valid room and slot.
 */
public class PendingAssignment {
    private final Section section;
    private final Course course;
    private final Faculty faculty;
    private final List<Room> candidateRooms;

    public PendingAssignment(Section section, Course course, Faculty faculty, List<Room> candidateRooms) {
        this.section = Objects.requireNonNull(section, "section cannot be null");
        this.course = Objects.requireNonNull(course, "course cannot be null");
        this.faculty = Objects.requireNonNull(faculty, "faculty cannot be null");
        if (candidateRooms == null || candidateRooms.isEmpty()) {
            throw new IllegalArgumentException("candidateRooms cannot be null or empty");
        }
        this.candidateRooms = candidateRooms;
    }

    public Section getSection() { return section; }
    public Course getCourse() { return course; }
    public Faculty getFaculty() { return faculty; }
    public List<Room> getCandidateRooms() { return candidateRooms; }

    @Override
    public String toString() {
        return section + " needs " + course + " (faculty " + faculty + ", "
            + candidateRooms.size() + " candidate room(s))";
    }
}
