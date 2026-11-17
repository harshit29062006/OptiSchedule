package com.university.scheduler.dao;

import com.university.scheduler.model.Section;
import java.sql.*;
import java.util.*;

public final class SectionDAO {
    private final ConnectionProvider connections;
    public SectionDAO(ConnectionProvider connections){this.connections=Objects.requireNonNull(connections);}
    public Optional<Section> findById(String code){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("SELECT section_code,branch_id,academic_year,division FROM sections WHERE section_code=?")){p.setString(1,code);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}}catch(SQLException e){throw new DatabaseException("Could not find section "+code,e);}}
    public List<Section> findAll(){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("SELECT section_code,branch_id,academic_year,division FROM sections ORDER BY section_code");ResultSet r=p.executeQuery()){List<Section> out=new ArrayList<>();while(r.next())out.add(read(r));return out;}catch(SQLException e){throw new DatabaseException("Could not list sections",e);}}
    public void insert(Section x){save(x,"INSERT INTO sections(section_code,branch_id,academic_year,division) VALUES(?,?,?,?)");}
    public void update(Section x){save(x,"UPDATE sections SET branch_id=?,academic_year=?,division=? WHERE section_code=?");}
    public boolean deleteById(String code){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM sections WHERE section_code=?")){p.setString(1,code);return p.executeUpdate()==1;}catch(SQLException e){throw new DatabaseException("Could not delete section "+code,e);}}
    private void save(Section x,String sql){try(Connection c=connections.getConnection()){
        c.setAutoCommit(false);
        try {
            if(x.getBranchId()!=null&&!x.getBranchId().isBlank()){
                boolean exists;try(PreparedStatement q=c.prepareStatement("SELECT 1 FROM branches WHERE branch_id=?")){q.setString(1,x.getBranchId());try(ResultSet r=q.executeQuery()){exists=r.next();}}
                if(!exists)try(PreparedStatement b=c.prepareStatement("INSERT INTO branches(branch_id,branch_name) VALUES(?,?)")){b.setString(1,x.getBranchId());b.setString(2,x.getBranchId());b.executeUpdate();}
            }
            try(PreparedStatement p=c.prepareStatement(sql)){
                if(sql.startsWith("INSERT")){p.setString(1,x.getSectionCode());p.setString(2,x.getBranchId());p.setInt(3,x.getYear());p.setString(4,x.getDivision());p.executeUpdate();}
                else {p.setString(1,x.getBranchId());p.setInt(2,x.getYear());p.setString(3,x.getDivision());p.setString(4,x.getSectionCode());if(p.executeUpdate()!=1)throw new SQLException("Section row not found");}
            }
            c.commit();
        }catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
    }catch(SQLException e){throw new DatabaseException("Could not save section "+x.getSectionCode(),e);}}
    private static Section read(ResultSet r)throws SQLException{return new Section(r.getString(1),r.getString(2),r.getInt(3),r.getString(4));}
}
