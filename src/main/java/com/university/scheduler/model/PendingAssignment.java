package com.university.scheduler.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
    private final int expectedStrength;
    private final List<Faculty> eligibleFaculty;

    public PendingAssignment(Section section, Course course, Faculty faculty, List<Room> candidateRooms) {
        this(section, course, faculty, candidateRooms, 0, List.of(faculty));
    }

    public PendingAssignment(Section section, Course course, Faculty faculty, List<Room> candidateRooms,
                             int expectedStrength) {
        this(section, course, faculty, candidateRooms, expectedStrength, List.of(faculty));
    }

    public PendingAssignment(Section section, Course course, Faculty faculty, List<Room> candidateRooms,
                             int expectedStrength, List<Faculty> eligibleFaculty) {
        this.section = Objects.requireNonNull(section, "section cannot be null");
        this.course = Objects.requireNonNull(course, "course cannot be null");
        this.faculty = Objects.requireNonNull(faculty, "faculty cannot be null");
        if (candidateRooms == null || candidateRooms.isEmpty()) {
            throw new IllegalArgumentException("candidateRooms cannot be null or empty");
        }
        this.candidateRooms = candidateRooms;
        if (expectedStrength < 0) throw new IllegalArgumentException("expectedStrength cannot be negative");
        this.expectedStrength = expectedStrength;
        Objects.requireNonNull(eligibleFaculty, "eligibleFaculty cannot be null");
        LinkedHashSet<Faculty> unique = new LinkedHashSet<>();
        unique.add(faculty);
        unique.addAll(eligibleFaculty);
        this.eligibleFaculty = new ArrayList<>(unique);
    }

    public Section getSection() { return section; }
    public Course getCourse() { return course; }
    public Faculty getFaculty() { return faculty; }
    public List<Room> getCandidateRooms() { return candidateRooms; }
    public int getExpectedStrength() { return expectedStrength; }
    public List<Faculty> getEligibleFaculty() { return List.copyOf(eligibleFaculty); }

    @Override
    public String toString() {
        return section + " needs " + course + " (faculty " + faculty + ", "
            + candidateRooms.size() + " candidate room(s))";
    }
}
