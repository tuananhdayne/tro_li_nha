package com.example.smarthomeui.smarthome.model;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {

    // Tên "fcm_token" này phải khớp 100% với FcmTokenModel trong Python
    @SerializedName("token")
    private String fcmToken;

    public FcmTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}