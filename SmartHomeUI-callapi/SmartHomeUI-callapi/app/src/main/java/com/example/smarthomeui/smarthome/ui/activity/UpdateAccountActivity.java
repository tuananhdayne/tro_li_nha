package com.example.smarthomeui.smarthome.ui.activity;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smarthomeui.R;
import com.google.android.material.textfield.TextInputEditText;

public class UpdateAccountActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtEmail, edtPhone;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_account);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail    = findViewById(R.id.edtEmail);
        edtPhone    = findViewById(R.id.edtPhone);

        View back = findViewById(R.id.ivBack);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> save());
        // TODO: preload thông tin hiện tại nếu có (từ repo/user store)
    }

    private void save() {
        String name  = edtFullName.getText() == null ? "" : edtFullName.getText().toString().trim();
        String email = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
        String phone = edtPhone.getText() == null ? "" : edtPhone.getText().toString().trim();

        if (name.isEmpty()) { toast("Vui lòng nhập họ tên"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { toast("Email không hợp lệ"); return; }
        if (phone.length() < 8) { toast("Số điện thoại không hợp lệ"); return; }

        // TODO: gọi API cập nhật hoặc lưu local
        toast("Đã lưu thông tin");
        finish();
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
