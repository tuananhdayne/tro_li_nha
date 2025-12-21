package com.example.smarthomeui.smarthome.network;

public class UpdateRoomReq {
    public String name;
    public String detail;
    public String type;
    public String iconKey;
    public int houseId;

    public UpdateRoomReq() {}

    public UpdateRoomReq(String name, String detail) {
        this.name = name;
        this.detail = detail;
    }

    public UpdateRoomReq(String name, String detail, String type, String iconKey) {
        this.name = name;
        this.detail = detail;
        this.type = type;
        this.iconKey = iconKey;
    }

    public UpdateRoomReq(String name, String detail, String type, String iconKey, int houseId) {
        this.name = name;
        this.detail = detail;
        this.type = type;
        this.iconKey = iconKey;
        this.houseId = houseId;
    }
}
