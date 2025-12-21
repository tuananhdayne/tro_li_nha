package com.example.smarthomeui.smarthome.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smarthomeui.R;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText edtOldPassword, edtNewPassword, edtConfirmPassword;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        edtOldPassword     = findViewById(R.id.edtOldPassword);
        edtNewPassword     = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        View back = findViewById(R.id.ivBack);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btnChangePassword).setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldP = get(edtOldPassword);
        String newP = get(edtNewPassword);
        String cfP  = get(edtConfirmPassword);

        if (TextUtils.isEmpty(oldP)) { toast("Nhập mật khẩu hiện tại"); return; }
        if (newP.length() < 6) { toast("Mật khẩu mới tối thiểu 6 ký tự"); return; }
        if (!newP.equals(cfP)) { toast("Xác nhận mật khẩu không khớp"); return; }

        // TODO: gọi API đổi mật khẩu
        toast("Đổi mật khẩu thành công");
        finish();
    }

    private String get(TextInputEditText e){ return e.getText()==null?"":e.getText().toString(); }
    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
