package com.university.scheduler.model;

import java.util.Objects;

public class Course {
    private final String courseCode;  // base code, e.g. "UCB009" (suffix letter stripped out)
    private String courseName;        // joined from the separate course list
    private Integer credits;          // nullable if not in course list
    private PeriodType periodType;

    public Course(String courseCode, String courseName, Integer credits, PeriodType periodType) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("courseCode cannot be blank");
        }
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.periodType = periodType;
    }

    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        return courseCode.equals(((Course) o).courseCode);
    }

    @Override
    public int hashCode() { return Objects.hash(courseCode); }

    @Override
    public String toString() { return courseName != null ? courseName : courseCode; }
}
