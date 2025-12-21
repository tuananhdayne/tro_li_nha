package com.example.smarthomeui.smarthome.network;

public class CreateRoomReq {
    public String name;
    public String detail;
    public String type;
    public String iconKey;
    public int houseId;

    public CreateRoomReq() {}

    public CreateRoomReq(String name, String detail, int houseId) {
        this.name = name;
        this.detail = detail;
        this.houseId = houseId;
        this.type = "generic";
    }

    public CreateRoomReq(String name, String detail, String type, String iconKey, int houseId) {
        this.name = name;
        this.detail = detail;
        this.type = type;
        this.iconKey = iconKey;
        this.houseId = houseId;
    }
}
