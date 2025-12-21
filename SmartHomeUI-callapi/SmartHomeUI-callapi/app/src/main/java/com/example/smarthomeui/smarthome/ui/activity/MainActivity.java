package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.FcmTokenRequest;
import com.example.smarthomeui.smarthome.utils.UserManager;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity - Điểm khởi đầu của ứng dụng
 * Kiểm tra trạng thái đăng nhập và chuyển hướng tương ứng
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Lấy FCM token
        getFCMToken();

        checkLoginStatus();
    }

    /**
     * Lấy FCM Token để nhận push notifications
     */
    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    Log.d(TAG, "============================================");
                    Log.d(TAG, "FCM Token: " + token);
                    Log.d(TAG, "============================================");

                    // TODO: Gửi token lên backend để lưu vào database
                    sendTokenToBackend(token);
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.getException());
                }
            });
    }

    /**
     * Gửi FCM token lên backend
     */
    private void sendTokenToBackend(String token) {
        UserManager userManager = new UserManager(this);

        // Chỉ gửi token nếu user đã đăng nhập
        if (!userManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping FCM token upload");
            return;
        }

        Api api = ApiClient.getClient(this).create(Api.class);
        FcmTokenRequest request = new FcmTokenRequest(token);

        api.updateFCMToken(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "FCM Token sent to server successfully");
                } else {
                    Log.e(TAG, "Failed to send FCM token. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to send FCM token to server", t);
            }
        });
    }

    /**
     * Kiểm tra trạng thái đăng nhập và điều hướng
     */
    private void checkLoginStatus() {
        UserManager userManager = new UserManager(this);

        if (userManager.isLoggedIn()) {
            // User đã đăng nhập và token còn hạn
            String userRole = userManager.getUserRole();
            navigateToHome(userRole);
        } else {
            // User chưa đăng nhập hoặc token đã hết hạn
            navigateToLogin();
        }
    }

    /**
     * Chuyển đến màn hình chính dựa trên role
     */
    private void navigateToHome(String userRole) {
        Intent intent;
        if ("admin".equals(userRole)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, HouseListActivity.class);
        }
        startActivity(intent);
        finish();
    }

    /**
     * Chuyển đến màn hình đăng nhập
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
