package com.example.smarthomeui.smarthome.network;

public class LoginResponse {
    private String token; // Có thể API chỉ trả về "token" thay vì "accessToken"
    private String accessToken;
    private String refreshToken;
    private String message;
    private boolean success;
    private UserInfo user;
    private long expiresIn; // Thời gian hết hạn (seconds)

    // Getters - hỗ trợ cả hai format
    public String getToken() {
        return token != null ? token : accessToken;
    }
    public String getAccessToken() {
        return accessToken != null ? accessToken : token;
    }
    public String getRefreshToken() { return refreshToken; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public UserInfo getUser() { return user; }
    public long getExpiresIn() { return expiresIn; }

    // Inner class cho thông tin user
    public static class UserInfo {
        private String id;
        private String email;
        private String role;
        private String name;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getName() { return name; }
    }
}
