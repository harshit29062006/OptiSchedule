package com.university.scheduler.dao;

import com.university.scheduler.engine.*;
import com.university.scheduler.exception.SchedulingException;
import com.university.scheduler.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Imports the cleaned timetable in lookup-first order and commits the batch atomically. */
public final class TimetableImporter {
    private static final Logger LOG=LogManager.getLogger(TimetableImporter.class);
    private static final int DEFAULT_FACULTY_LIMIT=18;
    private TimetableImporter() {}

    public static ImportReport importCsv(Path csv,ConnectionProvider provider){
        SchemaManager.createSchema(provider);
        List<Booking> offered=new CsvBookingSource(csv).loadAll();
        Map<String,String> sourceSheets=readSectionSheets(csv);
        ConflictChecker checker=new ConflictChecker();int rejected=0;
        for(Booking booking:offered)try{checker.addBooking(booking);}catch(SchedulingException e){rejected++;LOG.warn("import skipped conflicting booking: {}",e.getMessage());}
        List<Booking> accepted=checker.getAllBookings();
        try(Connection c=provider.getConnection()){
            boolean oldAuto=c.getAutoCommit();c.setAutoCommit(false);
            try{
                Map<String,Faculty> faculties=new LinkedHashMap<>();Map<String,Room> rooms=new LinkedHashMap<>();
                Map<String,Course> courses=new LinkedHashMap<>();Map<String,Section> sections=new LinkedHashMap<>();
                Map<String,TimeSlot> slots=new LinkedHashMap<>();
                for(Booking b:accepted){faculties.putIfAbsent(b.getFaculty().getFacultyId(),b.getFaculty());rooms.putIfAbsent(b.getRoom().getRoomId(),b.getRoom());
                    Course course=b.getCourse();courses.putIfAbsent(course.getCourseCode()+"|"+course.getPeriodType(),course);
                    slots.putIfAbsent(slotKey(b.getTimeSlot()),b.getTimeSlot());
                    for(Section s:b.getSections())sections.putIfAbsent(s.getSectionCode(),sectionMetadata(sourceSheets.getOrDefault(s.getSectionCode(),""),s));
                }
                insertFaculties(c,faculties.values());insertRooms(c,rooms.values());insertCourses(c,courses.values());insertSections(c,sections.values());insertSlots(c,slots.values());
                Map<String,Long> slotIds=readSlotIds(c);long attendances=0;
                for(Booking b:accepted){long bookingId=insertBooking(c,b,slotIds.get(slotKey(b.getTimeSlot())));attendances+=b.getSections().size();
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO booking_sections(booking_id,section_id) SELECT ?,section_id FROM sections WHERE section_code=?")){
                        for(Section s:b.getSections()){p.setLong(1,bookingId);p.setString(2,s.getSectionCode());if(p.executeUpdate()!=1)throw new SQLException("Section row not found: "+s.getSectionCode());}
                    }
                }
                c.commit();LOG.info("Timetable import complete: {} parsed rows, {} accepted sessions, {} section attendances, {} rejected conflicts",offered.size(),accepted.size(),attendances,rejected);
                return new ImportReport(offered.size(),accepted.size(),attendances,rejected);
            }catch(Exception e){c.rollback();if(e instanceof SQLException)throw new DatabaseException("Timetable import failed; transaction rolled back",e);if(e instanceof RuntimeException)throw (RuntimeException)e;throw new DatabaseException("Timetable import failed; transaction rolled back",e);}
            finally{c.setAutoCommit(oldAuto);}
        }catch(SQLException e){throw new DatabaseException("Could not connect to import timetable",e);}
    }

    private static void insertFaculties(Connection c,Collection<Faculty> values)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO faculty(faculty_id,faculty_name,department,max_hours_per_week,is_synthetic) VALUES(?,?,?,?,?)")){
            for(Faculty f:values){p.setString(1,f.getFacultyId());p.setString(2,f.getFacultyName());p.setString(3,f.getDepartment());p.setInt(4,Math.max(DEFAULT_FACULTY_LIMIT,f.getMaxHoursPerWeek()));p.setBoolean(5,f.getFacultyId().startsWith("UNKNOWN_FACULTY_"));p.addBatch();}p.executeBatch();
        }
    }
    private static void insertRooms(Connection c,Collection<Room> values)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO rooms(room_id,room_name,room_type,capacity,is_synthetic) VALUES(?,?,?,?,?)")){
            for(Room r:values){p.setString(1,r.getRoomId());p.setString(2,r.getRoomName());p.setString(3,r.getRoomType().name());p.setInt(4,r.getCapacity());p.setBoolean(5,r.getRoomId().startsWith("UNKNOWN_ROOM_"));p.addBatch();}p.executeBatch();
        }
    }
    private static void insertCourses(Connection c,Collection<Course> values)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO courses(course_code,course_name,credits,period_type) VALUES(?,?,?,?)")){
            for(Course x:values){p.setString(1,x.getCourseCode());p.setString(2,x.getCourseName());if(x.getCredits()==null)p.setNull(3,Types.INTEGER);else p.setInt(3,x.getCredits());p.setString(4,x.getPeriodType().name());p.addBatch();}p.executeBatch();
        }
    }
    private static void insertSections(Connection c,Collection<Section> values)throws SQLException{
        try(PreparedStatement branch=c.prepareStatement("INSERT INTO branches(branch_id,branch_name) VALUES(?,?)");PreparedStatement p=c.prepareStatement("INSERT INTO sections(section_code,branch_id,academic_year,division) VALUES(?,?,?,?)")){
            Set<String> branchIds=new HashSet<>();for(Section s:values)if(s.getBranchId()!=null&&branchIds.add(s.getBranchId())){branch.setString(1,s.getBranchId());branch.setString(2,s.getBranchId());branch.addBatch();}branch.executeBatch();
            for(Section s:values){p.setString(1,s.getSectionCode());p.setString(2,s.getBranchId());p.setInt(3,s.getYear());p.setString(4,s.getDivision());p.addBatch();}p.executeBatch();
        }
    }
    private static void insertSlots(Connection c,Collection<TimeSlot> values)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO timeslots(weekday,period_number,start_time,end_time) VALUES(?,?,?,?)")){
            for(TimeSlot t:values){p.setString(1,t.getDay().name());p.setInt(2,t.getPeriodNumber());if(t.getStartTime()==null)p.setNull(3,Types.TIME);else p.setTime(3,Time.valueOf(t.getStartTime()));if(t.getEndTime()==null)p.setNull(4,Types.TIME);else p.setTime(4,Time.valueOf(t.getEndTime()));p.addBatch();}p.executeBatch();
        }
    }
    private static Map<String,Long> readSlotIds(Connection c)throws SQLException{
        Map<String,Long> ids=new HashMap<>();try(PreparedStatement p=c.prepareStatement("SELECT timeslot_id,weekday,period_number FROM timeslots");ResultSet r=p.executeQuery()){while(r.next())ids.put(r.getString(2)+"|"+r.getInt(3),r.getLong(1));}return ids;
    }
    private static long insertBooking(Connection c,Booking b,Long slotId)throws SQLException{
        if(slotId==null)throw new SQLException("Time slot missing for "+b.getTimeSlot());
        try(PreparedStatement p=c.prepareStatement("INSERT INTO bookings(course_code,period_type,faculty_id,room_id,timeslot_id) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            p.setString(1,b.getCourse().getCourseCode());p.setString(2,b.getCourse().getPeriodType().name());p.setString(3,b.getFaculty().getFacultyId());p.setString(4,b.getRoom().getRoomId());p.setLong(5,slotId);p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){if(!k.next())throw new SQLException("No booking id generated");return k.getLong(1);}
        }
    }
    private static String slotKey(TimeSlot x){return x.getDay().name()+"|"+x.getPeriodNumber();}
    private static Section sectionMetadata(String sheet,Section section){
        // Metadata remains conservative: no branch code is invented from a sheet name.
        String normalized=sheet.toUpperCase(Locale.ROOT);
        int year=normalized.startsWith("FIRST YEAR")?1:normalized.startsWith("2ND YEAR")?2:normalized.startsWith("3RD YEAR")?3:normalized.startsWith("4TH YEAR")?4:0;
        String division=null;String trimmed=normalized.trim();if(trimmed.endsWith(" A"))division="A";else if(trimmed.endsWith(" B"))division="B";
        return new Section(section.getSectionCode(),null,year,division);
    }
    private static Map<String,String> readSectionSheets(Path csv){
        Map<String,String> result=new HashMap<>();
        try(BufferedReader r=Files.newBufferedReader(csv)){r.readLine();String line;while((line=r.readLine())!=null){String[] p=line.split(",",-1);if(p.length>=5)result.putIfAbsent(p[4].trim(),p[0].trim());}return result;}
        catch(IOException e){throw new DatabaseException("Could not inspect source sheet metadata",e);}
    }
    public static final class ImportReport{
        public final int parsedRows,acceptedSessions,rejectedConflicts;public final long sectionAttendances;
        private ImportReport(int rows,int sessions,long attendances,int rejected){parsedRows=rows;acceptedSessions=sessions;sectionAttendances=attendances;rejectedConflicts=rejected;}
    }
}
