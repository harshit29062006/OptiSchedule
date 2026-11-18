# OptiSchedule timetable integration build status

## Implemented in this build
- Reconstructed weekly grid (Monday–Saturday) in the Timetable > Weekly Timetable tab.
- Select one of the nine official workbook sheets and a lecture/tutorial/practical group.
- Search course code, room annotation, or faculty annotation.
- Display period, time, course, room/faculty annotations when present, and original Excel cell reference.
- Raw Excel Data browser and Booking Manager remain available.
- Dashboard displays database record counts, imported source-cell count, and parsed weekly course-slot count.
- Bundled CSV contains 2,472 extracted course slots across all nine sheets.

## Important verification note
The CSV was regenerated from the workbook and programmatically checked for nine sheets, valid day labels, course codes, group labels, and source-cell coordinates. Maven is not installed in the preparation environment, so this archive has not been compiled or launched here. On Windows, run `mvn clean javafx:run` from the folder containing `pom.xml`. Do not run schema reset scripts or re-import the raw source cells.

Room/faculty text is populated only where the workbook's group-block annotation can be mapped to the course cell. The original raw workbook cell browser remains the authoritative fallback for any ambiguous merged-cell annotation.
