package com.university.scheduler.dao;

import com.university.scheduler.model.*;
import java.sql.*;
import java.util.*;

/** Persists one session row plus all its attending sections atomically. */
public final class BookingDAO {
    private final ConnectionProvider connections;
    public BookingDAO(ConnectionProvider connections){this.connections=Objects.requireNonNull(connections);}

    public long insert(Booking booking){return inTransaction(c->{long id=insertSession(c,booking);replaceSections(c,id,booking);return id;});}

    public Optional<Booking> findById(long id){
        List<Booking> all=query("WHERE b.booking_id=?",id);
        return all.stream().findFirst();
    }

    public List<Booking> findAll(){return query("",null);}

    public void update(long id,Booking booking){inTransaction(c->{try(PreparedStatement p=c.prepareStatement("UPDATE bookings SET course_code=?,period_type=?,faculty_id=?,room_id=?,timeslot_id=? WHERE booking_id=?")){
            bindSession(c,p,booking);p.setLong(6,id);if(p.executeUpdate()!=1)throw new SQLException("Booking row not found: "+id);
        }try(PreparedStatement d=c.prepareStatement("DELETE FROM booking_sections WHERE booking_id=?")){d.setLong(1,id);d.executeUpdate();}
        replaceSections(c,id,booking);return null;});}

    public boolean deleteById(long id){return inTransaction(c->{try(PreparedStatement p=c.prepareStatement("DELETE FROM bookings WHERE booking_id=?")){p.setLong(1,id);return p.executeUpdate()==1;}});}

    private List<Booking> query(String where,Long id){
        String sql="SELECT b.booking_id,c.course_code,c.course_name,c.credits,c.period_type,"+
            "f.faculty_id,f.faculty_name,f.department,f.max_hours_per_week,"+
            "r.room_id,r.room_name,r.room_type,r.capacity,"+
            "t.weekday,t.period_number,t.start_time,t.end_time,s.section_code,s.branch_id,s.academic_year,s.division "+
            "FROM bookings b JOIN courses c ON c.course_code=b.course_code AND c.period_type=b.period_type "+
            "JOIN faculty f ON f.faculty_id=b.faculty_id JOIN rooms r ON r.room_id=b.room_id "+
            "JOIN timeslots t ON t.timeslot_id=b.timeslot_id JOIN booking_sections bs ON bs.booking_id=b.booking_id "+
            "JOIN sections s ON s.section_id=bs.section_id "+where+" ORDER BY b.booking_id,s.section_code";
        try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            if(id!=null)p.setLong(1,id);
            try(ResultSet rs=p.executeQuery()){
                Map<Long,Booking> grouped=new LinkedHashMap<>();
                while(rs.next()){
                    long key=rs.getLong(1); Booking b=grouped.get(key);
                    Section section=new Section(rs.getString(18),rs.getString(19),rs.getInt(20),rs.getString(21));
                    if(b==null){
                        int credits=rs.getInt(4);Integer credit=rs.wasNull()?null:credits;
                        String start=rs.getString(16),end=rs.getString(17);
                        TimeSlot slot=new TimeSlot(Day.valueOf(rs.getString(14)),rs.getInt(15),start==null?null:Time.valueOf(start).toLocalTime(),end==null?null:Time.valueOf(end).toLocalTime());
                        b=new Booking(section,new Course(rs.getString(2),rs.getString(3),credit,PeriodType.valueOf(rs.getString(5))),
                            new Faculty(rs.getString(6),rs.getString(7),rs.getString(8),rs.getInt(9)),
                            new Room(rs.getString(10),rs.getString(11),RoomType.valueOf(rs.getString(12)),rs.getInt(13)),slot);
                        grouped.put(key,b);
                    }else b.addSection(section);
                }
                return new ArrayList<>(grouped.values());
            }
        }catch(SQLException e){throw new DatabaseException("Could not load bookings",e);}
    }

    private static long insertSession(Connection c,Booking b)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO bookings(course_code,period_type,faculty_id,room_id,timeslot_id) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            bindSession(c,p,b);p.executeUpdate();try(ResultSet keys=p.getGeneratedKeys()){if(!keys.next())throw new SQLException("No generated booking id");return keys.getLong(1);}
        }
    }
    private static void bindSession(Connection c,PreparedStatement p,Booking b)throws SQLException{
        p.setString(1,b.getCourse().getCourseCode());p.setString(2,b.getCourse().getPeriodType().name());p.setString(3,b.getFaculty().getFacultyId());p.setString(4,b.getRoom().getRoomId());
        try(PreparedStatement q=c.prepareStatement("SELECT timeslot_id FROM timeslots WHERE weekday=? AND period_number=?")){
            q.setString(1,b.getTimeSlot().getDay().name());q.setInt(2,b.getTimeSlot().getPeriodNumber());try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("Time slot must be inserted before booking: "+b.getTimeSlot());p.setLong(5,r.getLong(1));}
        }
    }
    private static void replaceSections(Connection c,long bookingId,Booking b)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO booking_sections(booking_id,section_id) SELECT ?,section_id FROM sections WHERE section_code=?")){
            for(Section section:b.getSections()){p.setLong(1,bookingId);p.setString(2,section.getSectionCode());if(p.executeUpdate()!=1)throw new SQLException("Unknown section: "+section.getSectionCode());}
        }
    }
    private <T>T inTransaction(SqlWork<T> work){
        try(Connection c=connections.getConnection()){
            boolean auto=c.getAutoCommit();c.setAutoCommit(false);
            try{T value=work.run(c);c.commit();return value;}catch(Exception e){c.rollback();if(e instanceof SQLException)throw (SQLException)e;throw new SQLException("Booking transaction failed",e);}finally{c.setAutoCommit(auto);}
        }catch(SQLException e){throw new DatabaseException("Could not persist booking",e);}
    }
    @FunctionalInterface private interface SqlWork<T>{T run(Connection c)throws SQLException;}
}
