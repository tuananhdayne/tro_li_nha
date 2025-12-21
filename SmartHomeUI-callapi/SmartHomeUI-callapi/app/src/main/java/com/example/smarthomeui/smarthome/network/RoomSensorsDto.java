package com.example.smarthomeui.smarthome.network;

public class RoomSensorsDto {
    public int roomId;
    public String roomName;
    public Double temperature;   // null if no sensor
    public Double humidity;      // null if no sensor
    public Boolean motion;       // null if no sensor
    public boolean hasSensors;
}
