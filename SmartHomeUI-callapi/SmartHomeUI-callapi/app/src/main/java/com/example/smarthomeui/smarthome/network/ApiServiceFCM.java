package com.example.smarthomeui.smarthome.network;

import com.example.smarthomeui.smarthome.model.FcmTokenRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiServiceFCM {
    @POST("register-token")
    Call<Void> registerToken(@Body FcmTokenRequest tokenRequest);
}