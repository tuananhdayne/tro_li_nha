package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.ProfileResponse;
import com.example.smarthomeui.smarthome.utils.UserManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Base Activity để kiểm tra session và tự động đăng xuất khi token hết hạn
 */
public abstract class BaseActivity extends AppCompatActivity {

    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSessionValidity();
    }

    /**
     * Kiểm tra tính hợp lệ của session
     * Tự động đăng xuất và chuyển về LoginActivity nếu token hết hạn
     */
    private void checkSessionValidity() {
        // Bỏ qua kiểm tra cho LoginActivity và RegisterActivity
        String className = this.getClass().getSimpleName();
        if ("LoginActivity".equals(className) || "RegisterActivity".equals(className) || "MainActivity".equals(className)) {
            return;
        }

        if (!userManager.isLoggedIn()) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        if (userManager.isTokenExpiringSoon()) {
            Toast.makeText(this, "Phiên đăng nhập sắp hết hạn.", Toast.LENGTH_SHORT).show();
        }
    }

    /** Chuyển hướng về màn hình đăng nhập */
    protected void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Đăng xuất user và chuyển về LoginActivity */
    protected void logout() {
        userManager.logout();
        Toast.makeText(this, "Đã đăng xuất thành công.", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    /** Lấy UserManager instance */
    protected UserManager getUserManager() { return userManager; }

    /** Lấy access token hiện tại */
    protected String getAccessToken() { return userManager.getAccessToken(); }

    /** Lấy email hiện tại */
    protected String getCurrentUserEmail() { return userManager.getUserEmail(); }

    /** Lấy role hiện tại */
    protected String getCurrentUserRole() { return userManager.getUserRole(); }

    /**
     * Gọi API lấy thông tin profile của user hiện tại
     * SỬA: dùng ApiClient.getClient(this) và Api.getProfile() không header
     */
    protected void getUserProfile(Callback<ProfileResponse> callback) {
        String token = getAccessToken();
        if (token != null && !token.isEmpty()) {
            Api apiService = ApiClient.getClient(this).create(Api.class);
            apiService.getProfile().enqueue(callback);

        } else {
            logout();
        }
    }

    /**
     * Gọi API lấy profile và xử lý response tự động
     */
    protected void getUserProfile(ProfileSuccessCallback onSuccess, ProfileErrorCallback onError) {
        getUserProfile(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profileResponse = response.body();
                    if (profileResponse.isSuccess() && profileResponse.getData() != null) {
                        onSuccess.onSuccess(profileResponse.getData());
                    } else {
                        onError.onError(profileResponse.getMessage() != null ?
                                profileResponse.getMessage() : "Không thể lấy thông tin profile");
                    }
                } else {
                    onError.onError("Lỗi server khi lấy thông tin profile");
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                onError.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    // Interfaces cho callback
    public interface ProfileSuccessCallback {
        void onSuccess(ProfileResponse.UserProfile profile);
    }

    public interface ProfileErrorCallback {
        void onError(String errorMessage);
    }
}
