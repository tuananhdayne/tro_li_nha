package com.example.smarthomeui.smarthome.ui.activity;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.ui.adapter.RoomAdminAdapter;
import com.example.smarthomeui.smarthome.model.Room;
import java.util.ArrayList;
import java.util.List;

/**
 * Room Management Activity
 * Màn hình quản lý phòng cho admin
 */
public class RoomManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRooms;
    private ImageView btnBack, btnAddRoom;
    private CardView btnAllRooms, btnActiveRooms, btnInactiveRooms;
    private RoomAdminAdapter roomAdapter;
    private List<Room> roomList;
    private List<Room> filteredRoomList;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_management);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupFilterTabs();
        loadRooms();
    }

    private void initViews() {
        recyclerViewRooms = findViewById(R.id.recyclerViewRooms);
        btnBack = findViewById(R.id.btnBack);
        btnAddRoom = findViewById(R.id.btnAddRoom);
        btnAllRooms = findViewById(R.id.btnAllRooms);
        btnActiveRooms = findViewById(R.id.btnActiveRooms);
        btnInactiveRooms = findViewById(R.id.btnInactiveRooms);
    }

    private void setupRecyclerView() {
        roomList = new ArrayList<>();
        filteredRoomList = new ArrayList<>();
        roomAdapter = new RoomAdminAdapter(filteredRoomList, this);
        recyclerViewRooms.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerViewRooms.setAdapter(roomAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnAddRoom.setOnClickListener(v -> showAddRoomDialog());
    }

    private void setupFilterTabs() {
        btnAllRooms.setOnClickListener(v -> {
            setActiveTab("all");
            filterRooms("all");
        });

        btnActiveRooms.setOnClickListener(v -> {
            setActiveTab("active");
            filterRooms("active");
        });

        btnInactiveRooms.setOnClickListener(v -> {
            setActiveTab("inactive");
            filterRooms("inactive");
        });
    }

    private void setActiveTab(String filter) {
        // Reset all tabs
        btnAllRooms.setCardBackgroundColor(getColor(R.color.white));
        btnActiveRooms.setCardBackgroundColor(getColor(R.color.white));
        btnInactiveRooms.setCardBackgroundColor(getColor(R.color.white));

        // Set active tab
        switch (filter) {
            case "all":
                btnAllRooms.setCardBackgroundColor(getColor(R.color.primaryColor));
                break;
            case "active":
                btnActiveRooms.setCardBackgroundColor(getColor(R.color.primaryColor));
                break;
            case "inactive":
                btnInactiveRooms.setCardBackgroundColor(getColor(R.color.primaryColor));
                break;
        }
        currentFilter = filter;
    }

    private void filterRooms(String filter) {
        filteredRoomList.clear();
        switch (filter) {
            case "all":
                filteredRoomList.addAll(roomList);
                break;
            case "active":
                for (Room room : roomList) {
                    if (room.isActive()) {
                        filteredRoomList.add(room);
                    }
                }
                break;
            case "inactive":
                for (Room room : roomList) {
                    if (!room.isActive()) {
                        filteredRoomList.add(room);
                    }
                }
                break;
        }
        roomAdapter.notifyDataSetChanged();
    }

    private void loadRooms() {
        // TODO: Gọi API để lấy danh sách phòng
        roomList.clear();
        roomList.add(new Room("1", "Phòng khách", "Tầng 1", 5, 3, 24.5f, true));
        roomList.add(new Room("2", "Phòng ngủ chính", "Tầng 2", 3, 2, 22.0f, true));
        roomList.add(new Room("3", "Nhà bếp", "Tầng 1", 4, 4, 26.0f, true));
        roomList.add(new Room("4", "Phòng làm việc", "Tầng 2", 2, 1, 23.5f, false));
        roomList.add(new Room("5", "Phòng tắm", "Tầng 1", 2, 0, 25.0f, true));

        filterRooms(currentFilter);
    }

    private void showAddRoomDialog() {
        // TODO: Implement add room dialog
    }

    public void editRoom(Room room) {
        // TODO: Implement edit room functionality
    }

    public void deleteRoom(Room room) {
        // TODO: Implement delete room functionality
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRooms();
    }
}
