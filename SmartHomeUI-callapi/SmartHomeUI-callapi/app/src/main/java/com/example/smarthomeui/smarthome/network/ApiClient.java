package com.example.smarthomeui.smarthome.network;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
  
    private static final String BASE_URL = "https://nonsequent-frederic-nonceremonious.ngrok-free.dev";
    
    // Gemini Chatbot now integrated in backend - use same URL
    // Endpoint: POST /api/assistant
    private static final String GEMINI_BASE_URL = BASE_URL;

    private static Retrofit authedRetrofit;
    private static Retrofit noAuthRetrofit;
    private static Retrofit geminiRetrofit;

    private static Gson buildGson() {
        return new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(com.example.smarthomeui.smarthome.network.HouseListWrap.class,
                        new com.example.smarthomeui.smarthome.network.HouseListWrap.Deserializer())
                .registerTypeAdapter(com.example.smarthomeui.smarthome.network.RoomsByHouseWrap.class,
                        new com.example.smarthomeui.smarthome.network.RoomsByHouseWrap.Deserializer())
                .registerTypeAdapter(com.example.smarthomeui.smarthome.network.RoomListWrap.class,
                        new com.example.smarthomeui.smarthome.network.RoomListWrap.Deserializer())
                .registerTypeAdapter(com.example.smarthomeui.smarthome.network.DeviceListWrap.class,
                        new com.example.smarthomeui.smarthome.network.DeviceListWrap.Deserializer())
                .create();
    }

    /** Retrofit KHÔNG auth (login/register) */
    public static Retrofit getClientNoAuth() {
        if (noAuthRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder().build();
            noAuthRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(buildGson()))
                    .client(client)
                    .build();
        }
        return noAuthRetrofit;
    }

    /** Retrofit CÓ auth: tự gắn Bearer token từ UserManager */
    public static Retrofit getClient(Context ctx) {
        if (authedRetrofit == null) {
            // Thêm logging interceptor để xem request/response
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                android.util.Log.d("API_LOG", message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor) // Thêm logger
                    .addInterceptor((Interceptor) chain -> {
                        Request req = chain.request();
                        String token = new com.example.smarthomeui.smarthome.utils.UserManager(
                                ctx.getApplicationContext()).getAccessToken();
                        if (token != null && !token.isEmpty()) {
                            req = req.newBuilder()
                                    .addHeader("Authorization",
                                            token.startsWith("Bearer ") ? token : ("Bearer " + token))
                                    .build();
                        }
                        return chain.proceed(req);
                    })
                    .build();

            authedRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(buildGson()))
                    .client(client)
                    .build();
        }
        return authedRetrofit;
    }
    
    /** Retrofit for Gemini Chatbot (no auth needed) */
    public static Retrofit getGeminiClient() {
        if (geminiRetrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                android.util.Log.d("GEMINI_API", message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();
            
            geminiRetrofit = new Retrofit.Builder()
                    .baseUrl(GEMINI_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return geminiRetrofit;
    }
}
