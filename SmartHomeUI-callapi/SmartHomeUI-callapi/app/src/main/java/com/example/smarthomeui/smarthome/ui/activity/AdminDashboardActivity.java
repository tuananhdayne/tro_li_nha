package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import com.example.smarthomeui.R;

/**
 * Admin Dashboard Activity
 * Màn hình chính cho quản trị viên
 */
public class AdminDashboardActivity extends BaseActivity {

    private TextView tvUserCount, tvDeviceCount, tvAdminEmail, tvAdminName;
    private CardView cardUserManagement, cardRoomManagement, cardDeviceManagement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupClickListeners();
        loadStatistics();
        loadUserProfile(); // Thêm gọi getUserProfile
    }

    private void initViews() {
        tvUserCount = findViewById(R.id.tvUserCount);
        tvDeviceCount = findViewById(R.id.tvDeviceCount);
        // Thêm TextView cho email và name - kiểm tra null để tránh crash
        tvAdminEmail = findViewById(R.id.tvAdminEmail);
        tvAdminName = findViewById(R.id.tvAdminEmail);

        // Nếu layout chưa có TextView này, có thể log warning
        if (tvAdminEmail == null) {
            android.util.Log.w("AdminDashboard", "TextView tvAdminEmail not found in layout");
        }
        if (tvAdminName == null) {
            android.util.Log.w("AdminDashboard", "TextView tvAdminName not found in layout");
        }

        cardUserManagement = findViewById(R.id.cardUserManagement);
        cardRoomManagement = findViewById(R.id.cardRoomManagement);
        cardDeviceManagement = findViewById(R.id.cardDeviceManagement);
    }

    private void setupClickListeners() {
        cardUserManagement.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, UserManagementActivity.class);
            startActivity(intent);
        });

        cardRoomManagement.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, RoomManagementActivity.class);
            startActivity(intent);
        });

        cardDeviceManagement.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, DeviceManagementActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void loadStatistics() {
        // TODO: Gọi API để lấy thống kê
        // Tạm thời hiển thị dữ liệu mẫu
        tvUserCount.setText("25");
        tvDeviceCount.setText("142");
    }

    /**
     * Gọi API lấy thông tin profile và hiển thị lên view
     */
    private void loadUserProfile() {
        getUserProfile(
            // Callback khi thành công
            profile -> {
                // Hiển thị email và name trên TextView
                if (tvAdminEmail != null) {
                    tvAdminEmail.setText(profile.getEmail());
                }
                if (tvAdminName != null) {
                    tvAdminName.setText(profile.getName() != null ? profile.getName() : "Admin");
                }
            },
            // Callback khi có lỗi
            errorMessage -> {
                // Hiển thị thông tin từ SharedPreferences nếu API lỗi
                String email = getCurrentUserEmail();
                if (tvAdminEmail != null && email != null) {
                    tvAdminEmail.setText(email);
                }
                if (tvAdminName != null) {
                    tvAdminName.setText("Admin");
                }

                // Log lỗi (không hiển thị Toast để tránh làm phiền user)
                android.util.Log.e("AdminDashboard", "Lỗi load profile: " + errorMessage);
            }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics khi quay lại màn hình
        loadStatistics();
    }
}
