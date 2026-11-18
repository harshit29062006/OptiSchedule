package com.university.scheduler.dao;

import com.university.scheduler.model.*;
import java.sql.*;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

public final class JdbcTestDatabase implements ConnectionProvider {
    private static final AtomicInteger IDS=new AtomicInteger();
    private final String url="jdbc:h2:mem:scheduler_test_"+IDS.incrementAndGet()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
    public JdbcTestDatabase(){
        try{Class.forName("org.h2.Driver");}catch(ClassNotFoundException e){throw new AssertionError(e);}
        SchemaManager.createSchema(this);
    }
    @Override public Connection getConnection()throws SQLException{return DriverManager.getConnection(url,"sa","");}
    public Booking seedBooking(){
        Faculty f=new Faculty("F01","Faculty One","CS",18);
        Room room=new Room("R01","Room One",RoomType.CLASSROOM,60);
        Course course=new Course("CS101","Programming",3,PeriodType.LECTURE);
        Section section=new Section("1A1","CS",1,"A");
        new FacultyDAO(this).insert(f);new RoomDAO(this).insert(room);new CourseDAO(this).insert(course);new SectionDAO(this).insert(section);
        TimeSlot slot=new TimeSlot(Day.MONDAY,1,LocalTime.of(8,0),LocalTime.of(8,50));new TimeSlotDAO(this).findOrInsert(slot);
        Booking booking=new Booking(section,course,f,room,slot);new BookingDAO(this).insert(booking);return booking;
    }
}
