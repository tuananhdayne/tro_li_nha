package com.example.smarthomeui.smarthome.network;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class DeviceControlRequest {
    private String command;

    public DeviceControlRequest() {}
    public DeviceControlRequest(String command) { this.command = command; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}