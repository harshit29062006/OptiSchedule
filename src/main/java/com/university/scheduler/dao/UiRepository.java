package com.university.scheduler.dao;

import com.university.scheduler.model.*;
import java.sql.*;
import java.util.*;

/** JDBC facade used by the JavaFX screens. All writes go directly to MySQL. */
public final class UiRepository {
    private UiRepository() {}
    private static Connection open() throws SQLException { return Database.getConnection(); }

    public static List<Room> rooms() throws SQLException {
        List<Room> out=new ArrayList<>();
        try(Connection c=open(); PreparedStatement p=c.prepareStatement("SELECT room_id,room_name,room_type,capacity FROM rooms ORDER BY room_id"); ResultSet r=p.executeQuery()) {
            while(r.next()) out.add(new Room(r.getString(1),r.getString(2),RoomType.valueOf(r.getString(3)),r.getInt(4)));
        } return out;
    }
    public static void saveRoom(Room x, boolean update) throws SQLException {
        String sql=update?"UPDATE rooms SET room_name=?,room_type=?,capacity=? WHERE room_id=?":"INSERT INTO rooms(room_name,room_type,capacity,room_id,is_synthetic) VALUES(?,?,?,?,FALSE)";
        try(Connection c=open();PreparedStatement p=c.prepareStatement(sql)) {
            if(update){p.setString(1,x.getRoomName());p.setString(2,x.getRoomType().name());p.setInt(3,x.getCapacity());p.setString(4,x.getRoomId());}
            else {p.setString(1,x.getRoomName());p.setString(2,x.getRoomType().name());p.setInt(3,x.getCapacity());p.setString(4,x.getRoomId());} p.executeUpdate();
        }
    }
    public static void deleteRoom(String id) throws SQLException { try(Connection c=open();PreparedStatement p=c.prepareStatement("DELETE FROM rooms WHERE room_id=?")){p.setString(1,id);p.executeUpdate();} }

    public static List<Faculty> faculty() throws SQLException {
        List<Faculty> out=new ArrayList<>();try(Connection c=open();PreparedStatement p=c.prepareStatement("SELECT faculty_id,faculty_name,department,max_hours_per_week FROM faculty ORDER BY faculty_id");ResultSet r=p.executeQuery()){
            while(r.next())out.add(new Faculty(r.getString(1),r.getString(2),r.getString(3),r.getInt(4)));}return out;
    }
    public static void saveFaculty(Faculty x,boolean update)throws SQLException{
        String sql=update?"UPDATE faculty SET faculty_name=?,department=?,max_hours_per_week=? WHERE faculty_id=?":"INSERT INTO faculty(faculty_id,faculty_name,department,max_hours_per_week,is_synthetic) VALUES(?,?,?,?,FALSE)";
        try(Connection c=open();PreparedStatement p=c.prepareStatement(sql)){if(update){p.setString(1,x.getFacultyName());p.setString(2,x.getDepartment());p.setInt(3,x.getMaxHoursPerWeek());p.setString(4,x.getFacultyId());}else{p.setString(1,x.getFacultyId());p.setString(2,x.getFacultyName());p.setString(3,x.getDepartment());p.setInt(4,x.getMaxHoursPerWeek());}p.executeUpdate();}
    }
    public static void deleteFaculty(String id)throws SQLException{try(Connection c=open();PreparedStatement p=c.prepareStatement("DELETE FROM faculty WHERE faculty_id=?")){p.setString(1,id);p.executeUpdate();}}

    public static List<Course> courses()throws SQLException{
        List<Course> out=new ArrayList<>();try(Connection c=open();PreparedStatement p=c.prepareStatement("SELECT course_code,course_name,credits,period_type FROM courses ORDER BY course_code,period_type");ResultSet r=p.executeQuery()){
            while(r.next()){int n=r.getInt(3);Integer credits=r.wasNull()?null:n;out.add(new Course(r.getString(1),r.getString(2),credits,PeriodType.valueOf(r.getString(4))));}}return out;
    }
    public static void saveCourse(Course x,boolean update)throws SQLException{
        String sql=update?"UPDATE courses SET course_name=?,credits=? WHERE course_code=? AND period_type=?":"INSERT INTO courses(course_code,course_name,credits,period_type) VALUES(?,?,?,?)";
        try(Connection c=open();PreparedStatement p=c.prepareStatement(sql)){if(update){p.setString(1,x.getCourseName());if(x.getCredits()==null)p.setNull(2,Types.INTEGER);else p.setInt(2,x.getCredits());p.setString(3,x.getCourseCode());p.setString(4,x.getPeriodType().name());}else{p.setString(1,x.getCourseCode());p.setString(2,x.getCourseName());if(x.getCredits()==null)p.setNull(3,Types.INTEGER);else p.setInt(3,x.getCredits());p.setString(4,x.getPeriodType().name());}p.executeUpdate();}
    }
    public static void deleteCourse(String code,PeriodType type)throws SQLException{try(Connection c=open();PreparedStatement p=c.prepareStatement("DELETE FROM courses WHERE course_code=? AND period_type=?")){p.setString(1,code);p.setString(2,type.name());p.executeUpdate();}}

    public static int count(String table)throws SQLException{
        if(!Set.of("rooms","faculty","courses","bookings","sections","timetable_source_cells").contains(table))throw new IllegalArgumentException("Invalid table");
        try(Connection c=open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getInt(1);}
    }
    public static List<String[]> bookings()throws SQLException{
        List<String[]> out=new ArrayList<>();String sql="SELECT b.booking_id, s.section_code,c.course_code,COALESCE(c.course_name,c.course_code),f.faculty_id,COALESCE(f.faculty_name,f.faculty_id),r.room_id,t.weekday,t.period_number,COALESCE(TIME_FORMAT(t.start_time,'%H:%i'),'') FROM bookings b JOIN courses c ON c.course_code=b.course_code AND c.period_type=b.period_type JOIN faculty f ON f.faculty_id=b.faculty_id JOIN rooms r ON r.room_id=b.room_id JOIN timeslots t ON t.timeslot_id=b.timeslot_id JOIN booking_sections bs ON bs.booking_id=b.booking_id JOIN sections s ON s.section_id=bs.section_id ORDER BY t.weekday,t.period_number,s.section_code";
        try(Connection c=open();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10)});}return out;
    }
    public static void addBooking(String section,String course,String type,String faculty,String room,String day,int period)throws SQLException{
        String slotSql="SELECT timeslot_id FROM timeslots WHERE weekday=? AND period_number=?";
        try(Connection c=open()){c.setAutoCommit(false);try{
            long slot;try(PreparedStatement p=c.prepareStatement(slotSql)){p.setString(1,day);p.setInt(2,period);try(ResultSet r=p.executeQuery()){if(r.next())slot=r.getLong(1);else{try(PreparedStatement ins=c.prepareStatement("INSERT INTO timeslots(weekday,period_number) VALUES(?,?)",Statement.RETURN_GENERATED_KEYS)){ins.setString(1,day);ins.setInt(2,period);ins.executeUpdate();try(ResultSet k=ins.getGeneratedKeys()){k.next();slot=k.getLong(1);}}}}}
            long sectionId;try(PreparedStatement p=c.prepareStatement("SELECT section_id FROM sections WHERE section_code=?")){p.setString(1,section);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Section code not found. Import the timetable data first.");sectionId=r.getLong(1);}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO bookings(course_code,period_type,faculty_id,room_id,timeslot_id) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setString(1,course);p.setString(2,type);p.setString(3,faculty);p.setString(4,room);p.setLong(5,slot);p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){k.next();long booking=k.getLong(1);try(PreparedStatement link=c.prepareStatement("INSERT INTO booking_sections(booking_id,section_id) VALUES(?,?)")){link.setLong(1,booking);link.setLong(2,sectionId);link.executeUpdate();}}}
            c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException)throw (SQLException)e;throw new SQLException(e);}finally{c.setAutoCommit(true);}}
    }
    public static void deleteBooking(long id)throws SQLException{try(Connection c=open();PreparedStatement p=c.prepareStatement("DELETE FROM bookings WHERE booking_id=?")){p.setLong(1,id);p.executeUpdate();}}
    /** Names of the original workbook sheets preserved in timetable_source_cells. */
    public static List<String> sourceTimetableSheets() throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = "SELECT DISTINCT sheet_name FROM timetable_source_cells ORDER BY sheet_name";
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) out.add(r.getString(1));
        }
        return out;
    }

    /** A paged, read-only search through the imported Excel cells. */
    public static List<String[]> sourceTimetableCells(String sheet, String search, int limit, int offset) throws SQLException {
        List<String[]> out = new ArrayList<>();
        String sql = "SELECT source_row_number, cell_ref, column_number, cell_value " +
                "FROM timetable_source_cells WHERE sheet_name=? " +
                "AND (?='' OR cell_value LIKE CONCAT('%',?,'%') OR cell_ref LIKE CONCAT('%',?,'%')) " +
                "ORDER BY source_row_number, column_number LIMIT ? OFFSET ?";
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, sheet);
            p.setString(2, search);
            p.setString(3, search);
            p.setString(4, search);
            p.setInt(5, limit);
            p.setInt(6, offset);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new String[]{
                        String.valueOf(r.getInt(1)), r.getString(2), String.valueOf(r.getInt(3)), r.getString(4)
                });
            }
        }
        return out;
    }

}
