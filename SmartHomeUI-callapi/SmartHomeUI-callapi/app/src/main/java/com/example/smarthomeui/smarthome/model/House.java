package com.example.smarthomeui.smarthome.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class House implements Serializable {
    private String id;
    private String name;
    private int iconRes;
    private String description; // mô tả tuỳ chọn
    private boolean isActive; // trạng thái kích hoạt nhà
    private int roomCount; // số phòng từ API
    private final List<Room> rooms = new ArrayList<>();

    public House(String id, String name, int iconRes) {
        this.id = id; this.name = name; this.iconRes = iconRes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getIconRes() { return iconRes; }
    public List<Room> getRooms() { return rooms; }
    
    // Trả về roomCount từ API nếu có, ngược lại dùng rooms.size()
    public int getRoomCount() { 
        return roomCount > 0 ? roomCount : rooms.size(); 
    }
    
    public void setRoomCount(int count) { this.roomCount = count; }

    public String getDescription() { return description; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}