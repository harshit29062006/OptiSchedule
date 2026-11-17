package com.university.scheduler.dao;

import com.university.scheduler.model.*;
import java.sql.*;
import java.util.*;

public final class RoomDAO {
    private final ConnectionProvider connections;
    public RoomDAO(ConnectionProvider connections){this.connections=Objects.requireNonNull(connections);}
    public Optional<Room> findById(String id){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("SELECT room_id,room_name,room_type,capacity FROM rooms WHERE room_id=?")){p.setString(1,id);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}}catch(SQLException e){throw new DatabaseException("Could not find room "+id,e);}}
    public List<Room> findAll(){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("SELECT room_id,room_name,room_type,capacity FROM rooms ORDER BY room_id");ResultSet r=p.executeQuery()){List<Room> out=new ArrayList<>();while(r.next())out.add(read(r));return out;}catch(SQLException e){throw new DatabaseException("Could not list rooms",e);}}
    public void insert(Room room){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO rooms(room_id,room_name,room_type,capacity,is_synthetic) VALUES(?,?,?,?,?)")){bind(p,room);p.setBoolean(5,room.getRoomId().startsWith("UNKNOWN_ROOM_"));p.executeUpdate();}catch(SQLException e){throw new DatabaseException("Could not insert room "+room.getRoomId(),e);}}
    public void update(Room room){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("UPDATE rooms SET room_name=?,room_type=?,capacity=? WHERE room_id=?")){p.setString(1,room.getRoomName());p.setString(2,room.getRoomType().name());p.setInt(3,room.getCapacity());p.setString(4,room.getRoomId());if(p.executeUpdate()!=1)throw new SQLException("Room row not found");}catch(SQLException e){throw new DatabaseException("Could not update room "+room.getRoomId(),e);}}
    public boolean deleteById(String id){try(Connection c=connections.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM rooms WHERE room_id=?")){p.setString(1,id);return p.executeUpdate()==1;}catch(SQLException e){throw new DatabaseException("Could not delete room "+id,e);}}
    private static void bind(PreparedStatement p,Room r)throws SQLException{p.setString(1,r.getRoomId());p.setString(2,r.getRoomName());p.setString(3,r.getRoomType().name());p.setInt(4,r.getCapacity());}
    private static Room read(ResultSet r)throws SQLException{return new Room(r.getString(1),r.getString(2),RoomType.valueOf(r.getString(3)),r.getInt(4));}
}
