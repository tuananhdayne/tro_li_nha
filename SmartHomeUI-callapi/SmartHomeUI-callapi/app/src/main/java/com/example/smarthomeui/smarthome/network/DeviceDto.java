package com.example.smarthomeui.smarthome.network;

import com.google.gson.annotations.SerializedName;

public class DeviceDto {
    private int id;
    private String name;
    private String userId;
    private String type;

    @SerializedName("roomID") // API trả về "roomID" với chữ ID viết hoa
    private Integer roomId; // null nếu chưa gắn vào phòng nào

    public DeviceDto() {}

    public DeviceDto(int id, String name, String userId, String type) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.type = type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
}
