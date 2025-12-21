package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.network.RegisterRequest;
import com.example.smarthomeui.smarthome.network.RegisterResponse;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Smart Home - Register Activity
 * Màn hình đăng ký tài khoản mới
 */
public class RegisterActivity extends AppCompatActivity {
    
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
    }

    /**
     * Khởi tạo các view components
     */
    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
    }

    /**
     * Xử lý sự kiện khi người dùng ấn nút Đăng ký
     */
    public void onRegisterClicked(View view) {
        // Lấy thông tin từ form
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation dữ liệu đầu vào
        if (!validateInput(fullName, email, password, confirmPassword)) {
            return; // Dừng lại nếu validation fail
        }

        // Thực hiện đăng ký
        performRegister(fullName, email, password, confirmPassword);
    }

    /**
     * Validation dữ liệu đầu vào
     * @param fullName Họ và tên
     * @param email Email
     * @param password Mật khẩu
     * @param confirmPassword Xác nhận mật khẩu
     * @return true nếu tất cả dữ liệu hợp lệ
     */
    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
        // Kiểm tra họ và tên
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Vui lòng nhập họ và tên");
            etFullName.requestFocus();
            return false;
        }

        // Kiểm tra email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return false;
        }

        // Kiểm tra mật khẩu
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return false;
        }

        // Kiểm tra xác nhận mật khẩu
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true; // Tất cả validation đều pass
    }

    /**
     * Thực hiện đăng ký tài khoản
     * @param fullName Họ và tên
     * @param email Email
     * @param password Mật khẩu
     * @param confirmPassword Xác nhận mật khẩu
     */
    private void performRegister(String fullName, String email, String password, String confirmPassword) {
        // Tạo request object với email, password, confirmPassword
        RegisterRequest request = new RegisterRequest(email, password, confirmPassword);

        // Call API đăng ký
        ApiClient.getClient(this).create(Api.class)
            .register(request)
            .enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        RegisterResponse registerResponse = response.body();
                        if (registerResponse.isSuccess()) {
                            showSuccess("Đăng ký thành công! Vui lòng đăng nhập.");
                            navigateToLogin(email);
                        } else {
                            showError(registerResponse.getMessage());
                        }
                    } else {
                        showError("Đăng ký thất bại. Vui lòng thử lại.");
                    }
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    showError("Lỗi kết nối: " + t.getMessage());
                }
            });
    }

    /**
     * Hiển thị thông báo thành công
     * @param message Nội dung thông báo
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Hiển thị thông báo lỗi
     * @param message Nội dung lỗi
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Chuyển về màn hình đăng nhập và auto-fill email
     * @param email Email để auto-fill
     */
    private void navigateToLogin(String email) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("email", email); // Truyền email để tự động điền
        startActivity(intent);
        finish();
    }

    /**
     * Chuyển về màn hình đăng nhập (không auto-fill)
     */
    public void onLoginClicked(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
