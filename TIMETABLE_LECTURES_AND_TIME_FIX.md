# OptiSchedule timetable correction — v3

This update addresses the issue where choosing a practical subgroup showed only labs and hid lectures.

## Changes
- The section selector includes parent sections and their tutorial/practical subgroups.
- Selecting a parent section displays lectures, tutorials, and all practical/lab subgroups under it.
- Selecting one subgroup displays the parent-section lectures plus that selected subgroup's tutorial or practical entries.
- Every entry is labelled Lecture, Tutorial, or Practical / Lab using the official course-code suffix L, T, or P.
- Period start times come from the selected sheet's Excel data.
- AM/PM times are normalized correctly (for example, 6:00PM becomes 18:00).
- Rows are no longer forced to a fixed height, reducing clipped simultaneous subgroup entries.

## Data check
The regenerated CSV contains 5,502 entries:
- Lecture entries: 2,436
- Tutorial entries: 754
- Practical/lab entries: 2,312

These are extracted entries, not unique courses. Some entries represent separate subgroup sessions in the same period.

## Run
Open the `OptiSchedule` folder in VS Code and run:

```powershell
mvn clean javafx:run
```
