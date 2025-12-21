package com.example.smarthomeui.smarthome.network;

public class UpdateHouseReq {
    public String name;
    public String location;

    public UpdateHouseReq() {}
    public UpdateHouseReq(String name, String location) {
        this.name = name;
        this.location = location;
    }
}
