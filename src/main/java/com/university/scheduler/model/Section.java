package com.university.scheduler.model;

import java.util.Objects;

public class Section {
    private final String sectionCode;  // e.g. "1A2A" — matches real batch code directly
    private String branchId;
    private int year;
    private String division;           // nullable, e.g. "A", "B" if separable

    public Section(String sectionCode, String branchId, int year, String division) {
        if (sectionCode == null || sectionCode.isBlank()) {
            throw new IllegalArgumentException("sectionCode cannot be blank");
        }
        this.sectionCode = sectionCode;
        this.branchId = branchId;
        this.year = year;
        this.division = division;
    }

    public String getSectionCode() { return sectionCode; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Section)) return false;
        return sectionCode.equals(((Section) o).sectionCode);
    }

    @Override
    public int hashCode() { return Objects.hash(sectionCode); }

    @Override
    public String toString() { return sectionCode; }
}
