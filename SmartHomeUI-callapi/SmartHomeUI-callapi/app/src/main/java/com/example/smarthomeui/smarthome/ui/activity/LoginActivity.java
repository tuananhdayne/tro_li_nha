package com.example.smarthomeui.smarthome.ui.activity;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.FcmTokenRequest;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.ApiFCMClient;
import com.example.smarthomeui.smarthome.network.ApiServiceFCM;
import com.example.smarthomeui.smarthome.network.LoginRequest;
import com.example.smarthomeui.smarthome.network.LoginResponse;
import com.example.smarthomeui.smarthome.utils.UserManager;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Smart Home - Login Activity
 * Màn hình đăng nhập chính của ứng dụng
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Không cần setContentView vì activity này chỉ để điều hướng

        // Thiết lập giao diện full screen và status bar trong suốt
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        handleIntentData();
    }

    /** Khởi tạo các view components */
    private void initViews() {
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
    }

    /** Xử lý dữ liệu từ Intent (auto-fill email từ RegisterActivity) */
    private void handleIntentData() {
        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            etEmail.setText(email);
        }
    }

    /** Click nút Đăng nhập */
    public void onLoginClicked(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        performLogin(email, password);
    }

    /** Thực hiện đăng nhập */
    private void performLogin(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ email và mật khẩu.");
            return;
        }

        // Dùng client NO-AUTH cho login
        Api apiService = ApiClient.getClientNoAuth().create(Api.class);
        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showError("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.");
                    return;
                }

                LoginResponse loginResponse = response.body();

                // Lấy token (accessToken) và chuẩn hoá (bỏ "Bearer " nếu có)
                String token = loginResponse.getAccessToken();
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                if (token == null || token.isEmpty()) {
                    showError("Không nhận được token từ server.");
                    return;
                }

                // Tính thời gian hết hạn token
                long currentTime = System.currentTimeMillis();
                long expiryTime = currentTime + (24 * 60 * 60 * 1000); // mặc định 24h
                if (loginResponse.getExpiresIn() > 0) {
                    expiryTime = currentTime + (loginResponse.getExpiresIn() * 1000L);
                }

                // Lưu session
                String userRole = "user";
                String userId   = email;
                if (loginResponse.getUser() != null) {
                    if (loginResponse.getUser().getRole() != null) {
                        userRole = loginResponse.getUser().getRole();
                    }
                    if (loginResponse.getUser().getId() != null) {
                        userId = loginResponse.getUser().getId();
                    }
                }

                new UserManager(LoginActivity.this).saveUserSession(
                        token,                                 // <-- CHỈ JWT, KHÔNG kèm "Bearer "
                        loginResponse.getRefreshToken(),       // có thể null
                        email,
                        userRole,
                        userId,
                        expiryTime
                );
                sendToken();
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToHome(userRole);

            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showError("Lỗi kết nối: " + (t != null ? t.getMessage() : "Không xác định"));
            }
        });
    }
    private void sendToken (){
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.d("FCM_DEBUG", "Fetching FCM Token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    callapi(token);
                    Log.d("FCM_DEBUG", "Manual Token: " + token);
                });

    }

    private void callapi( String token){

        Log.d(TAG, "Đang gửi token đến SERVER : " + token );

        ApiServiceFCM apiService = ApiFCMClient.getClient().create(ApiServiceFCM.class);

        // 2. Tạo đối tượng request body
        FcmTokenRequest requestBody = new FcmTokenRequest(token);

        // 3. Thực hiện cuộc gọi API (đến server Python)
        apiService.registerToken(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Gửi token lên SERVER  thành công!");
                } else {
                    Log.e(TAG, "Gửi token đến SERVER  thất bại, mã lỗi: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Gửi token đến SERVER thất bại, lỗi kết nối: " + t.getMessage());
            }
        });
    }

    /** Chuyển đến màn hình chính sau khi đăng nhập thành công */
    private void navigateToHome(String userRole) {
        Intent intent;
        if ("admin".equalsIgnoreCase(userRole)) {
            intent = new Intent(getApplicationContext(), AdminDashboardActivity.class);
        } else {
            intent = new Intent(getApplicationContext(), HouseListActivity.class);
        }
        startActivity(intent);
        finish();
    }

    // Overload cũ (nếu cần)
    private void navigateToHome() {
        navigateToHome("user");
    }

    /** Hiển thị thông báo lỗi */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void onRegisterClicked(View view) {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
