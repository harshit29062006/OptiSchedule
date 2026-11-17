package com.university.scheduler.dao;

import com.university.scheduler.engine.JdbcBookingSource;
import com.university.scheduler.engine.CsvBookingSource;
import com.university.scheduler.engine.ScheduleLoader;
import com.university.scheduler.engine.ConflictChecker;
import com.university.scheduler.engine.SchedulingService;
import com.university.scheduler.model.PeriodType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TimetableImporterTest {
    @TempDir Path temp;
    @Test void importsLookupRowsBeforeSessionsAndKeepsCourseSuffixVariants()throws Exception{
        Path csv=temp.resolve("schedule.csv");
        Files.writeString(csv,"sheet,day,period,start_time,section,course_code,room_faculty_raw\n"+
            "FIRST YEAR A,MONDAY,1,08:00:00,1A1A,CS101L,G114\n"+
            "FIRST YEAR A,TUESDAY,2,09:00:00,1A1A,CS101P,G114\n");
        JdbcTestDatabase db=new JdbcTestDatabase();
        TimetableImporter.ImportReport report=TimetableImporter.importCsv(csv,db);
        assertEquals(2,report.parsedRows);assertEquals(2,report.acceptedSessions);assertEquals(2,report.sectionAttendances);assertEquals(0,report.rejectedConflicts);
        var bookings=new JdbcBookingSource(db).loadAll();assertEquals(2,bookings.size());
        assertEquals(Set.of(PeriodType.LECTURE,PeriodType.PRACTICAL),bookings.stream().map(b->b.getCourse().getPeriodType()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(1,new SectionDAO(db).findById("1A1A").orElseThrow().getYear());
        ConflictChecker csvChecker=new ScheduleLoader().loadFrom(new CsvBookingSource(csv));
        SchedulingService jdbcService=SchedulingService.fromJdbc(db);
        assertEquals(csvChecker.totalBookings(),jdbcService.getTotalBookingCount());
        assertEquals(csvChecker.totalSectionAttendances(),jdbcService.getTotalSectionAttendances());
        csvChecker.getAllFacultyLoads().forEach((faculty,load)->assertEquals(load,jdbcService.getFacultyLoad(faculty)));
    }
}
