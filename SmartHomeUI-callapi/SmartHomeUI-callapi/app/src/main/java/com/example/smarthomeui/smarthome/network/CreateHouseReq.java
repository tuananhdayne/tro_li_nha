package com.example.smarthomeui.smarthome.network;

public class CreateHouseReq {
    public String name;
    public String location;

    public CreateHouseReq() {}
    public CreateHouseReq(String name, String location) {
        this.name = name;
        this.location = location;
    }
}
