package com.example.smarthomeui.smarthome.network;

import com.google.gson.annotations.SerializedName;

public class UpdateDeviceReq {
    private String name;

    @SerializedName("roomID")
    private Integer roomID;

    public UpdateDeviceReq() {}

    public UpdateDeviceReq(String name, Integer roomID) {
        this.name = name;
        this.roomID = roomID;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getRoomID() { return roomID; }
    public void setRoomID(Integer roomID) { this.roomID = roomID; }
}

