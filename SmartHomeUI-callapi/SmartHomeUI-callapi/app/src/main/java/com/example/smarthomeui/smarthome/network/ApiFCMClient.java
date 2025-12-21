package com.example.smarthomeui.smarthome.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiFCMClient {

    // !!! QUAN TRỌNG: ĐÂY LÀ ĐỊA CHỈ SERVER PYTHON CỦA BẠN !!!
    // Nếu chạy server Python trên máy tính và dùng Android Emulator:
    private static final String FCM_BASE_URL = "http://10.0.2.2:8000/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(FCM_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}