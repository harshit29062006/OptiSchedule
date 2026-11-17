package com.university.scheduler.dao;

import com.university.scheduler.engine.JdbcBookingSource;
import com.university.scheduler.engine.SchedulingService;
import com.university.scheduler.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class DaoCrudTest {
    @Test void facultyRoomCourseAndSectionSupportCrudAndReportMissingRows(){
        JdbcTestDatabase db=new JdbcTestDatabase();
        FacultyDAO faculties=new FacultyDAO(db);Faculty f=new Faculty("F1","N","CS",18);
        assertTrue(faculties.findById("F1").isEmpty());faculties.insert(f);assertEquals("N",faculties.findById("F1").orElseThrow().getFacultyName());
        f.setFacultyName("Updated");faculties.update(f);assertEquals("Updated",faculties.findById("F1").orElseThrow().getFacultyName());assertTrue(faculties.deleteById("F1"));assertFalse(faculties.deleteById("F1"));
        RoomDAO rooms=new RoomDAO(db);Room r=new Room("R1","Room",RoomType.LAB,40);rooms.insert(r);assertEquals(RoomType.LAB,rooms.findById("R1").orElseThrow().getRoomType());r.setCapacity(50);rooms.update(r);assertEquals(50,rooms.findById("R1").orElseThrow().getCapacity());assertTrue(rooms.deleteById("R1"));
        CourseDAO courses=new CourseDAO(db);Course course=new Course("C1","Name",3,PeriodType.TUTORIAL);courses.insert(course);assertEquals(3,courses.findById("C1").orElseThrow().getCredits());course.setCourseName("Updated");courses.update(course);assertEquals("Updated",courses.findById("C1").orElseThrow().getCourseName());assertTrue(courses.deleteById("C1"));
        SectionDAO sections=new SectionDAO(db);Section section=new Section("S1","CS",1,"A");sections.insert(section);assertEquals("CS",sections.findById("S1").orElseThrow().getBranchId());section.setYear(2);sections.update(section);assertEquals(2,sections.findById("S1").orElseThrow().getYear());assertTrue(sections.deleteById("S1"));
    }

    @Test void daoFailuresAreWrappedAndPreparedValuesAreStoredLiterally(){
        JdbcTestDatabase db=new JdbcTestDatabase();FacultyDAO dao=new FacultyDAO(db);
        Faculty injectionText=new Faculty("F' OR '1'='1",null,null,18);dao.insert(injectionText);
        assertTrue(dao.findById(injectionText.getFacultyId()).isPresent());
        assertThrows(DatabaseException.class,()->dao.insert(injectionText));
        assertThrows(DatabaseException.class,()->dao.update(new Faculty("missing",null,null,18)));
        assertTrue(dao.deleteById(injectionText.getFacultyId()));

        RoomDAO rooms=new RoomDAO(db);Room room=new Room("R1","Room",RoomType.LAB,30);rooms.insert(room);
        assertThrows(DatabaseException.class,()->rooms.insert(room));assertThrows(DatabaseException.class,()->rooms.update(new Room("missing","M",RoomType.LAB,30)));
        CourseDAO courses=new CourseDAO(db);Course course=new Course("C1","Course",3,PeriodType.LECTURE);courses.insert(course);
        assertThrows(DatabaseException.class,()->courses.insert(course));assertThrows(DatabaseException.class,()->courses.update(new Course("missing","M",3,PeriodType.LECTURE)));
        SectionDAO sections=new SectionDAO(db);Section section=new Section("S1","CS",1,"A");sections.insert(section);
        assertThrows(DatabaseException.class,()->sections.insert(section));assertThrows(DatabaseException.class,()->sections.update(new Section("missing","CS",1,"A")));
        BookingDAO bookings=new BookingDAO(db);assertTrue(bookings.findById(999).isEmpty());assertFalse(bookings.deleteById(999));
        Booking broken=new Booking(new Section("missing","CS",1,"A"),new Course("missing","M",1,PeriodType.LECTURE),new Faculty("missing","M","CS",18),new Room("missing","M",RoomType.CLASSROOM,20),new TimeSlot(com.university.scheduler.model.Day.MONDAY,1,null,null));
        assertThrows(DatabaseException.class,()->bookings.insert(broken));
    }

    @Test void bookingCrudPreservesCombinedSectionsAndServiceReadsDatabase(){
        JdbcTestDatabase db=new JdbcTestDatabase();Booking original=db.seedBooking();
        Section second=new Section("1A2","CS",1,"A");new SectionDAO(db).insert(second);
        Booking combined=new Booking(original.getSection(),original.getCourse(),original.getFaculty(),original.getRoom(),original.getTimeSlot());combined.addSection(second);
        BookingDAO dao=new BookingDAO(db);long id=1; // generated identity starts at one in a fresh test database
        dao.update(id,combined);
        Booking loaded=dao.findById(id).orElseThrow();assertEquals(2,loaded.getSections().size());
        assertEquals(1,dao.findAll().size());assertEquals(2,SchedulingService.fromJdbc(db).getTotalSectionAttendances());
        assertTrue(dao.deleteById(id));assertTrue(dao.findAll().isEmpty());assertTrue(dao.findById(id).isEmpty());
    }

    @Test void roomAndFacultyConflictsAreRejectedByDatabaseKeys(){
        JdbcTestDatabase db=new JdbcTestDatabase();Booking b=db.seedBooking();
        Section other=new Section("1A2","CS",1,"A");new SectionDAO(db).insert(other);
        Booking anotherFaculty=new Booking(other,b.getCourse(),new Faculty("F02","Two","CS",18),b.getRoom(),b.getTimeSlot());
        new FacultyDAO(db).insert(anotherFaculty.getFaculty());
        assertThrows(DatabaseException.class,()->new BookingDAO(db).insert(anotherFaculty));
        Room otherRoom=new Room("R02","Room Two",RoomType.CLASSROOM,60);new RoomDAO(db).insert(otherRoom);
        Booking sameFaculty=new Booking(other,b.getCourse(),b.getFaculty(),otherRoom,b.getTimeSlot());
        assertThrows(DatabaseException.class,()->new BookingDAO(db).insert(sameFaculty));
    }
}
