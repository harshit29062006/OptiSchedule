package com.university.scheduler.model;

import java.util.Objects;

public class Faculty {
    private final String facultyId;   // institutional short-code, e.g. "TA8" (no full-name legend available)
    private String facultyName;       // nullable until/unless a legend is found
    private String department;        // nullable
    private int maxHoursPerWeek;

    public Faculty(String facultyId, String facultyName, String department, int maxHoursPerWeek) {
        if (facultyId == null || facultyId.isBlank()) {
            throw new IllegalArgumentException("facultyId cannot be blank");
        }
        if (maxHoursPerWeek < 0) {
            throw new IllegalArgumentException("maxHoursPerWeek cannot be negative");
        }
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.department = department;
        this.maxHoursPerWeek = maxHoursPerWeek;
    }

    public String getFacultyId() { return facultyId; }
    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public int getMaxHoursPerWeek() { return maxHoursPerWeek; }
    public void setMaxHoursPerWeek(int maxHoursPerWeek) { this.maxHoursPerWeek = maxHoursPerWeek; }

    /** Faculty identity is defined purely by facultyId (the institutional short-code). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Faculty)) return false;
        return facultyId.equals(((Faculty) o).facultyId);
    }

    @Override
    public int hashCode() { return Objects.hash(facultyId); }

    @Override
    public String toString() { return facultyName != null ? facultyName : facultyId; }
}
