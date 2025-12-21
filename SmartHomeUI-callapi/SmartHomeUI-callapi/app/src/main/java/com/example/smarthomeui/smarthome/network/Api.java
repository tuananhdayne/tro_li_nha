package com.example.smarthomeui.smarthome.network;

import retrofit2.Call;
import retrofit2.http.*;

public interface Api {
    // ===== AUTH / PROFILE (đã có) =====
    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("/api/auth/profile")
    Call<ProfileResponse> getProfile();
    @POST("/api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    // ===== HOUSE =====
    // Danh sách nhà theo user (có phân trang)
    @GET("/api/house")
    Call<HouseListWrap> getHouses(@Query("skip") int skip,
                                  @Query("take") int take);

    // Chi tiết 1 nhà
    @GET("/api/house/{id}")
    Call<HouseDto> getHouseById(@Path("id") int id);

    // Tạo nhà
    @POST("/api/house")
    Call<HouseDto> createHouse(@Body CreateHouseReq body);

    // Cập nhật nhà
    @PUT("/api/house/{id}")
    Call<HouseDto> updateHouse(@Path("id") int id,
                               @Body UpdateHouseReq body);

    // Xoá nhà
    @DELETE("/api/house/{id}")
    Call<Void> deleteHouse(@Path("id") int id);

    // Toggle kích hoạt nhà (ON/OFF)
    @PUT("/api/house/{id}/toggle")
    Call<ToggleResponse> toggleHouseActive(@Path("id") int id, @Body ToggleActiveRequest body);

    // Toggle kích hoạt phòng (ON/OFF)
    @PUT("/api/room/{id}/toggle")
    Call<ToggleResponse> toggleRoomActive(@Path("id") int id, @Body ToggleActiveRequest body);


    // GET Danh sách phòng
    @GET("/api/room")
    Call<RoomsByHouseWrap> getRoomsGrouped(@Query("skip") int skip, @Query("take") int take);

    // GET Danh sách phòng theo nhà
    @GET("/api/house/{houseId}/rooms")
    Call<RoomListWrap> getRoomsByHouseId(@Path("houseId") int houseId,
                                       @Query("skip") int skip,
                                       @Query("take") int take);

    // Tạo phòng mới
    @POST("/api/room")
    Call<RoomDto> createRoom(@Body CreateRoomReq body);

    // Cập nhật phòng
    @PUT("/api/room/{id}")
    Call<RoomDto> updateRoom(@Path("id") int id, @Body UpdateRoomReq body);

    // Xóa phòng
    @DELETE("/api/room/{id}")
    Call<Void> deleteRoom(@Path("id") int id);

    // Lấy chi tiết phòng
    @GET("/api/room/{id}")
    Call<RoomDto> getRoomById(@Path("id") int id);

    // Lấy dữ liệu sensors trong phòng (nhiệt độ, độ ẩm, chuyển động)
    @GET("/api/room/{id}/sensors")
    Call<RoomSensorsDto> getRoomSensors(@Path("id") int id);

    // ===== DEVICE =====
    // Danh sách thiết bị theo user (có phân trang)
    @GET("/api/device")
    Call<DeviceListWrap> getDevices(@Query("skip") int skip,
                                    @Query("take") int take);

    // Danh sách thiết bị theo phòng
    @GET("/api/room/{roomId}/devices")
    Call<DeviceListWrap> getDevicesByRoomId(@Path("roomId") int roomId);

    // Thêm thiết bị vào phòng
    @POST("/api/room/{roomId}/devices/{deviceId}")
    Call<Void> addDeviceToRoom(@Path("roomId") int roomId, @Path("deviceId") int deviceId);

    // Xóa thiết bị ra khỏi phòng
    @DELETE("/api/room/{roomId}/devices/{deviceId}")
    Call<Void> removeDeviceFromRoom(@Path("roomId") int roomId, @Path("deviceId") int deviceId);

    // Điều khiển thiết bị
    @POST("/api/device/{id}/control")
    Call<DeviceControlResponse> controlDevice(@Path("id") String deviceId,
                                              @Body DeviceControlRequest request);

    // Cập nhật thiết bị
    @PUT("/api/device/{id}")
    Call<DeviceDto> updateDevice(@Path("id") int id, @Body UpdateDeviceReq body);

    // Xóa thiết bị
    @DELETE("/api/device/{id}")
    Call<Void> deleteDevice(@Path("id") int id);

    // ===== FCM TOKEN =====
    // Cập nhật FCM token cho user
    @POST("/api/users/fcm-token")
    Call<Void> updateFCMToken(@Body FcmTokenRequest request);
    
    // ===== GEMINI CHATBOT =====
    // Gọi Gemini chatbot assistant (integrated in backend)
    @POST("/api/assistant")
    Call<GeminiResponse> askGemini(@Body GeminiRequest request);
}
