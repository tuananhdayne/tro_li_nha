package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.ui.adapter.UserAdapter;
import com.example.smarthomeui.smarthome.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * User Management Activity
 * Màn hình quản lý người dùng cho admin
 */
public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsers;
    private EditText etSearch;
    private ImageView btnBack, btnAddUser;
    private UserAdapter userAdapter;
    private List<User> userList;
    private List<User> filteredUserList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();
        loadUsers();
    }

    private void initViews() {
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        btnAddUser = findViewById(R.id.btnAddUser);
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        filteredUserList = new ArrayList<>();
        userAdapter = new UserAdapter(filteredUserList, this);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnAddUser.setOnClickListener(v -> {
            // TODO: Mở dialog thêm user hoặc chuyển đến AddUserActivity
            showAddUserDialog();
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterUsers(String query) {
        filteredUserList.clear();
        if (query.isEmpty()) {
            filteredUserList.addAll(userList);
        } else {
            for (User user : userList) {
                if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                    filteredUserList.add(user);
                }
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    private void loadUsers() {
        // TODO: Gọi API để lấy danh sách users
        // Tạm thời tạo dữ liệu mẫu
        userList.clear();
        userList.add(new User("1", "Nguyễn Văn A", "nguyenvana@gmail.com", "admin", "avatar1.jpg"));
        userList.add(new User("2", "Trần Thị B", "tranthib@gmail.com", "user", "avatar2.jpg"));
        userList.add(new User("3", "Lê Văn C", "levanc@gmail.com", "user", "avatar3.jpg"));
        userList.add(new User("4", "Phạm Thị D", "phamthid@gmail.com", "moderator", "avatar4.jpg"));
        userList.add(new User("5", "Hoàng Văn E", "hoangvane@gmail.com", "user", "avatar5.jpg"));

        filteredUserList.clear();
        filteredUserList.addAll(userList);
        userAdapter.notifyDataSetChanged();
    }

    private void showAddUserDialog() {
        // TODO: Implement add user dialog
        // Có thể tạo dialog hoặc chuyển đến activity riêng
    }

    public void editUser(User user) {
        // TODO: Implement edit user functionality
    }

    public void deleteUser(User user) {
        // TODO: Implement delete user functionality
        // Hiển thị confirmation dialog trước khi xóa
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers(); // Refresh danh sách khi quay lại
    }
}
