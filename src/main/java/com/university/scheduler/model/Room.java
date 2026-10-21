package com.university.scheduler.model;

import java.util.Objects;

public class Room {
    private final String roomId;      // e.g. "G114", "LP101", "E304"
    private String roomName;
    private RoomType roomType;
    private int capacity;

    public Room(String roomId, String roomName, RoomType roomType, int capacity) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("roomId cannot be blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.roomId = roomId;
        this.roomName = (roomName != null) ? roomName : roomId;
        this.roomType = roomType;
        this.capacity = capacity;
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        return roomId.equals(((Room) o).roomId);
    }

    @Override
    public int hashCode() { return Objects.hash(roomId); }

    @Override
    public String toString() { return roomName; }
}
