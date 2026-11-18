package com.university.scheduler.dao;

import com.university.scheduler.model.*;
import java.sql.*;
import java.util.*;

public final class CourseDAO {
    private final ConnectionProvider connections;
    public CourseDAO(ConnectionProvider connections){this.connections=Objects.requireNonNull(connections);}
    public Optional<Course> findById(String code,PeriodType type){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("SELECT course_code,course_name,credits,period_type FROM courses WHERE course_code=? AND period_type=?")){p.setString(1,code);p.setString(2,type.name());try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}}catch(SQLException e){throw new DatabaseException("Could not find course "+code+"/"+type,e);}}
    /** Convenience lookup for codes with a single stored period type. */
    public Optional<Course> findById(String code){List<Course> matches=findAll().stream().filter(c->c.getCourseCode().equals(code)).toList();if(matches.size()>1)throw new IllegalArgumentException("Course code has multiple period types; specify one: "+code);return matches.stream().findFirst();}
    public List<Course> findAll(){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("SELECT course_code,course_name,credits,period_type FROM courses ORDER BY course_code,period_type");ResultSet r=p.executeQuery()){List<Course> out=new ArrayList<>();while(r.next())out.add(read(r));return out;}catch(SQLException e){throw new DatabaseException("Could not list courses",e);}}
    public void insert(Course x){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO courses(course_code,course_name,credits,period_type) VALUES(?,?,?,?)")){bind(p,x);p.executeUpdate();}catch(SQLException e){throw new DatabaseException("Could not insert course "+x.getCourseCode(),e);}}
    public void update(Course x){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("UPDATE courses SET course_name=?,credits=? WHERE course_code=? AND period_type=?")){p.setString(1,x.getCourseName());if(x.getCredits()==null)p.setNull(2,Types.INTEGER);else p.setInt(2,x.getCredits());p.setString(3,x.getCourseCode());p.setString(4,x.getPeriodType().name());if(p.executeUpdate()!=1)throw new SQLException("Course row not found");}catch(SQLException e){throw new DatabaseException("Could not update course "+x.getCourseCode(),e);}}
    public boolean deleteById(String code,PeriodType type){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM courses WHERE course_code=? AND period_type=?")){p.setString(1,code);p.setString(2,type.name());return p.executeUpdate()==1;}catch(SQLException e){throw new DatabaseException("Could not delete course "+code+"/"+type,e);}}
    public boolean deleteById(String code){Optional<Course> one=findById(code);return one.isPresent()&&deleteById(code,one.get().getPeriodType());}
    private static void bind(PreparedStatement p,Course x)throws SQLException{p.setString(1,x.getCourseCode());p.setString(2,x.getCourseName());if(x.getCredits()==null)p.setNull(3,Types.INTEGER);else p.setInt(3,x.getCredits());p.setString(4,x.getPeriodType().name());}
    private static Course read(ResultSet r)throws SQLException{int credits=r.getInt(3);Integer value=r.wasNull()?null:credits;return new Course(r.getString(1),r.getString(2),value,PeriodType.valueOf(r.getString(4)));}
}
