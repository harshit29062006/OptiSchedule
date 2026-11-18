package com.university.scheduler.engine;

import com.university.scheduler.dao.Database;
import com.university.scheduler.model.*;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;

/** Reads the current normalized schema (including combined multi-section sessions). */
public class JdbcBookingSource implements BookingSource {
    @Override public List<Booking> loadAll() {
        String sql="SELECT b.booking_id,c.course_code,c.course_name,c.credits,c.period_type,"
          +"f.faculty_id,f.faculty_name,f.department,f.max_hours_per_week,"
          +"r.room_id,r.room_name,r.room_type,r.capacity,"
          +"t.weekday,t.period_number,t.start_time,t.end_time,"
          +"s.section_code,s.branch_id,s.academic_year,s.division "
          +"FROM bookings b JOIN courses c ON c.course_code=b.course_code AND c.period_type=b.period_type "
          +"JOIN faculty f ON f.faculty_id=b.faculty_id JOIN rooms r ON r.room_id=b.room_id "
          +"JOIN timeslots t ON t.timeslot_id=b.timeslot_id JOIN booking_sections bs ON bs.booking_id=b.booking_id "
          +"JOIN sections s ON s.section_id=bs.section_id ORDER BY t.weekday,t.period_number,b.booking_id,s.section_code";
        Map<Long,Booking> grouped=new LinkedHashMap<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            while(r.next()){
                long id=r.getLong(1);Section section=new Section(r.getString(18),r.getString(19),r.getInt(20),r.getString(21));
                Booking b=grouped.get(id);
                if(b==null){int raw=r.getInt(4);Integer credits=r.wasNull()?null:raw;
                    Time start=r.getTime(16),end=r.getTime(17);
                    TimeSlot slot=new TimeSlot(Day.valueOf(r.getString(14)),r.getInt(15),start==null?null:start.toLocalTime(),end==null?null:end.toLocalTime());
                    b=new Booking(section,new Course(r.getString(2),r.getString(3),credits,PeriodType.valueOf(r.getString(5))),
                      new Faculty(r.getString(6),r.getString(7),r.getString(8),r.getInt(9)),
                      new Room(r.getString(10),r.getString(11),RoomType.valueOf(r.getString(12)),r.getInt(13)),slot);grouped.put(id,b);
                }else b.addSection(section);
            }
            return new ArrayList<>(grouped.values());
        }catch(SQLException|IllegalArgumentException e){throw new IllegalStateException("Could not load bookings from scheduler_db. Check config.properties and schema.sql.",e);}
    }
}
