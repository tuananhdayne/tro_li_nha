package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.smarthomeui.R;

/**
 * Màn Cài đặt: hiển thị thông tin tài khoản từ API profile.
 */
public class SettingsActivity extends BaseActivity {

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvDevicesCount; // nếu chưa có API đếm thiết bị, hiển thị placeholder

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("Cài đặt");
        View back = findViewById(R.id.ivBack);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvDevicesCount = findViewById(R.id.tvDevicesCount);

        // Hiển thị trước giá trị mặc định để tránh trống lúc chờ API
        if (tvUserName != null)  tvUserName.setText("Đang tải…");
        if (tvUserEmail != null) tvUserEmail.setText("");

        // Nếu chưa có API đếm thiết bị, tạm để dấu gạch ngang
        if (tvDevicesCount != null) tvDevicesCount.setText("— thiết bị");

        // Gọi API profile và bind dữ liệu lên view
        loadProfile();

        // Điều hướng bảo mật
        View cardSecurity = findViewById(R.id.cardSecurity);
        if (cardSecurity != null) {
            cardSecurity.setOnClickListener(v -> showSecurityOptions());
        }

        // Đăng xuất
        View llLogout = findViewById(R.id.llLogout);
        if (llLogout != null) {
            llLogout.setOnClickListener(v -> logout());
        }
    }

    private void loadProfile() {
        getUserProfile(profile -> {
            // success
            if (tvUserName != null) {
                String name = profile.getName();
                tvUserName.setText((name != null && !name.trim().isEmpty()) ? name.trim() : "User");
            }
            if (tvUserEmail != null) {
                tvUserEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
            }
        }, err -> {
            // error -> dùng dữ liệu từ session (nếu có)
            if (tvUserName != null) tvUserName.setText("User");
            if (tvUserEmail != null) {
                String email = getCurrentUserEmail();
                tvUserEmail.setText(email != null ? email : "");
            }
            android.util.Log.e("Settings", "Lỗi load profile: " + err);
        });
    }

    private void showSecurityOptions() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Bảo mật & quyền riêng tư")
                .setItems(new CharSequence[]{"Cập nhật thông tin", "Đổi mật khẩu"}, (d, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, UpdateAccountActivity.class));
                    } else {
                        startActivity(new Intent(this, ChangePasswordActivity.class));
                    }
                })
                .show();
    }
}
