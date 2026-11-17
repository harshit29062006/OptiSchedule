package com.university.scheduler.dao;

import com.university.scheduler.model.Faculty;
import java.sql.*;
import java.util.*;

public final class FacultyDAO {
    private final ConnectionProvider connections;
    public FacultyDAO(ConnectionProvider connections) { this.connections = Objects.requireNonNull(connections); }

    public Optional<Faculty> findById(String id) {
        String sql = "SELECT faculty_id, faculty_name, department, max_hours_per_week FROM faculty WHERE faculty_id=?";
        try (Connection c=connections.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,id); try(ResultSet r=p.executeQuery()) { return r.next()?Optional.of(read(r)):Optional.empty(); }
        } catch(SQLException e) { throw new DatabaseException("Could not find faculty " + id,e); }
    }
    public List<Faculty> findAll() {
        try(Connection c=connections.getConnection(); PreparedStatement p=c.prepareStatement("SELECT faculty_id, faculty_name, department, max_hours_per_week FROM faculty ORDER BY faculty_id"); ResultSet r=p.executeQuery()) {
            List<Faculty> out=new ArrayList<>(); while(r.next()) out.add(read(r)); return out;
        } catch(SQLException e) { throw new DatabaseException("Could not list faculty",e); }
    }
    public void insert(Faculty f) { execute(f,"INSERT INTO faculty(faculty_id,faculty_name,department,max_hours_per_week,is_synthetic) VALUES(?,?,?,?,?)"); }
    public void update(Faculty f) { execute(f,"UPDATE faculty SET faculty_name=?,department=?,max_hours_per_week=? WHERE faculty_id=?"); }
    public boolean deleteById(String id) {
        try(Connection c=connections.getConnection(); PreparedStatement p=c.prepareStatement("DELETE FROM faculty WHERE faculty_id=?")) { p.setString(1,id); return p.executeUpdate()==1; }
        catch(SQLException e){throw new DatabaseException("Could not delete faculty " + id,e);}
    }
    private void execute(Faculty f,String sql) {
        try(Connection c=connections.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            if(sql.startsWith("INSERT")){p.setString(1,f.getFacultyId());p.setString(2,f.getFacultyName());p.setString(3,f.getDepartment());p.setInt(4,f.getMaxHoursPerWeek());p.setBoolean(5,f.getFacultyId().startsWith("UNKNOWN_FACULTY_"));}
            else {p.setString(1,f.getFacultyName());p.setString(2,f.getDepartment());p.setInt(3,f.getMaxHoursPerWeek());p.setString(4,f.getFacultyId());}
            if(p.executeUpdate()!=1) throw new SQLException("Faculty row not found: " + f.getFacultyId());
        } catch(SQLException e){throw new DatabaseException("Could not save faculty " + f.getFacultyId(),e);}
    }
    private static Faculty read(ResultSet r)throws SQLException{return new Faculty(r.getString(1),r.getString(2),r.getString(3),r.getInt(4));}
}
