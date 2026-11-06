package com.university.scheduler.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A single class that still needs a (room, timeSlot, faculty) assigned.
 *
 * faculty is the preferred teacher. eligibleFaculty is every teacher who
 * may take the class (always including faculty). When more than one is
 * eligible, the optimizer places the class with the least-loaded eligible
 * teacher first, which spreads teaching load evenly.
 *
 * expectedStrength is the number of students. 0 means unknown, and the
 * candidate rooms are then used in the order given.
 */
public class PendingAssignment {
    private final Section section;
    private final Course course;
    private final Faculty faculty;
    private final List<Room> candidateRooms;
    private final int expectedStrength;
    private final List<Faculty> eligibleFaculty;

    public PendingAssignment(Section section, Course course, Faculty faculty, List<Room> candidateRooms) {
        this(section, course, faculty, candidateRooms, 0);
    }

    public PendingAssignment(Section section, Course course, Faculty faculty,
                             List<Room> candidateRooms, int expectedStrength) {
        this(section, course, faculty, candidateRooms, expectedStrength, List.of(faculty));
    }

    public PendingAssignment(Section section, Course course, Faculty faculty,
                             List<Room> candidateRooms, int expectedStrength, List<Faculty> eligible) {
        this.section = Objects.requireNonNull(section, "section cannot be null");
        this.course = Objects.requireNonNull(course, "course cannot be null");
        this.faculty = Objects.requireNonNull(faculty, "faculty cannot be null");
        if (candidateRooms == null || candidateRooms.isEmpty()) {
            throw new IllegalArgumentException("candidateRooms cannot be null or empty");
        }
        if (expectedStrength < 0) {
            throw new IllegalArgumentException("expectedStrength cannot be negative");
        }
        this.candidateRooms = candidateRooms;
        this.expectedStrength = expectedStrength;

        // The preferred faculty always comes first; duplicates are dropped.
        List<Faculty> list = new ArrayList<>();
        list.add(faculty);
        if (eligible != null) {
            for (Faculty e : eligible) {
                if (!list.contains(e)) {
                    list.add(e);
                }
            }
        }
        this.eligibleFaculty = List.copyOf(list);
    }

    public Section getSection() { return section; }
    public Course getCourse() { return course; }
    public Faculty getFaculty() { return faculty; }
    public List<Room> getCandidateRooms() { return candidateRooms; }
    public int getExpectedStrength() { return expectedStrength; }
    public List<Faculty> getEligibleFaculty() { return eligibleFaculty; }

    @Override
    public String toString() {
        return section + " needs " + course + " (faculty " + faculty + " or "
            + (eligibleFaculty.size() - 1) + " other(s), " + candidateRooms.size()
            + " candidate room(s), strength " + expectedStrength + ")";
    }
}
