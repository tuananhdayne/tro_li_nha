package com.example.smarthomeui.smarthome.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {
    private static final String PREF_NAME = "smart_home_user_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences sharedPreferences;
    private Context context;

    public UserManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Lưu thông tin đăng nhập sau khi login thành công
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param email Email của user
     * @param role Role của user (admin/user)
     * @param userId ID của user
     * @param expiryTimeInMillis Thời gian hết hạn token (timestamp)
     */
    public void saveUserSession(String accessToken, String refreshToken, String email, String role, String userId, long expiryTimeInMillis) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_USER_ID, userId);
        editor.putLong(KEY_TOKEN_EXPIRY, expiryTimeInMillis);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Kiểm tra xem user có đang đăng nhập không
     * @return true nếu đang đăng nhập và token chưa hết hạn
     */
    public boolean isLoggedIn() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        if (!isLoggedIn) {
            return false;
        }

        // Kiểm tra token có hết hạn không
        long currentTime = System.currentTimeMillis();
        long expiryTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);

        if (currentTime >= expiryTime) {
            // Token đã hết hạn, tự động đăng xuất
            logout();
            return false;
        }

        return true;
    }

    /**
     * Lấy access token
     * @return Access token hoặc null nếu không có
     */
    public String getAccessToken() {
        if (!isLoggedIn()) {
            return null;
        }
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Lấy refresh token
     * @return Refresh token hoặc null nếu không có
     */
    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * Lấy email của user hiện tại
     * @return Email hoặc null nếu không có
     */
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Lấy role của user hiện tại
     * @return Role (admin/user) hoặc null nếu không có
     */
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    /**
     * Lấy ID của user hiện tại
     * @return User ID hoặc null nếu không có
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    /**
     * Cập nhật access token mới (sau khi refresh)
     * @param newAccessToken Token mới
     * @param newExpiryTime Thời gian hết hạn mới
     */
    public void updateAccessToken(String newAccessToken, long newExpiryTime) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ACCESS_TOKEN, newAccessToken);
        editor.putLong(KEY_TOKEN_EXPIRY, newExpiryTime);
        editor.apply();
    }

    /**
     * Đăng xuất và xóa tất cả dữ liệu session
     */
    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Kiểm tra xem token sắp hết hạn không (trong vòng 5 phút)
     * @return true nếu token sắp hết hạn
     */
    public boolean isTokenExpiringSoon() {
        long currentTime = System.currentTimeMillis();
        long expiryTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        long fiveMinutes = 5 * 60 * 1000; // 5 phút

        return (expiryTime - currentTime) <= fiveMinutes;
    }
}
