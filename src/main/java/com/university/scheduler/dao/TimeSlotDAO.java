package com.university.scheduler.dao;

import com.university.scheduler.model.*;
import java.sql.*;
import java.util.Objects;

public final class TimeSlotDAO {
    private final ConnectionProvider connections;
    public TimeSlotDAO(ConnectionProvider connections){this.connections=Objects.requireNonNull(connections);}
    public long findOrInsert(TimeSlot slot){
        String find="SELECT timeslot_id FROM timeslots WHERE weekday=? AND period_number=?";
        try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement(find)){
            bindKey(p,slot);try(ResultSet r=p.executeQuery()){if(r.next())return r.getLong(1);}
            try(PreparedStatement ins=c.prepareStatement("INSERT INTO timeslots(weekday,period_number,start_time,end_time) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                bind(ins,slot);ins.executeUpdate();try(ResultSet keys=ins.getGeneratedKeys()){if(!keys.next())throw new SQLException("No generated timeslot id");return keys.getLong(1);}
            }
        }catch(SQLException e){
            try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement(find)){bindKey(p,slot);try(ResultSet r=p.executeQuery()){if(r.next())return r.getLong(1);}}
            catch(SQLException retry){e.addSuppressed(retry);}
            throw new DatabaseException("Could not save time slot "+slot,e);
        }
    }
    private static void bindKey(PreparedStatement p,TimeSlot x)throws SQLException{p.setString(1,x.getDay().name());p.setInt(2,x.getPeriodNumber());}
    private static void bind(PreparedStatement p,TimeSlot x)throws SQLException{bindKey(p,x);if(x.getStartTime()==null)p.setNull(3,Types.TIME);else p.setTime(3,Time.valueOf(x.getStartTime()));if(x.getEndTime()==null)p.setNull(4,Types.TIME);else p.setTime(4,Time.valueOf(x.getEndTime()));}
}
