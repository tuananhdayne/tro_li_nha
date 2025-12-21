package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.adapter.RoomAdapter;
import com.example.smarthomeui.smarthome.ai_speech_reg.MainActivitySR;
import com.example.smarthomeui.smarthome.model.Room;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.CreateRoomReq;
import com.example.smarthomeui.smarthome.network.HouseDto;
import com.example.smarthomeui.smarthome.network.RoomDto;
import com.example.smarthomeui.smarthome.network.RoomListWrap;
import com.example.smarthomeui.smarthome.network.ToggleActiveRequest;
import com.example.smarthomeui.smarthome.network.ToggleResponse;
import com.example.smarthomeui.smarthome.network.UpdateRoomReq;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HouseRoomsActivity extends BaseActivity {
    private String houseId;
    private int houseIdInt;
    private final List<Room> rooms = new ArrayList<>();
    private RoomAdapter adapter;
    private ProgressBar progressBar;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_house_rooms);

        // Tạo ProgressBar theo cách lập trình thay vì tìm từ layout
        setupProgressBar();

        View back = findViewById(R.id.ivBack);
        if (back != null) back.setOnClickListener(v -> onBackPressed());

        houseId = getIntent().getStringExtra("house_id");
        houseIdInt = Integer.parseInt(houseId);

        // Thiết lập RecyclerView và adapter
        RecyclerView rv = findViewById(R.id.rvRoomsOfHouse);
        rv.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new RoomAdapter(rooms, room -> {
            Intent i = new Intent(HouseRoomsActivity.this, RoomDetailsActivity.class);
            i.putExtra("house_id", houseId);
            i.putExtra("room_id", room.getId());
            startActivity(i);
        }, (room, position) -> {
            showRoomActionsDialog(room, position);
        });
        
        // Set toggle listener để gọi API khi bật/tắt phòng
        adapter.setOnRoomToggleListener((room, isActive) -> {
            callApiToggleRoom(room, isActive);
        });
        
        rv.setAdapter(adapter);

        // Gọi API lấy thông tin nhà
        fetchHouseDetails();

        // Gọi API lấy danh sách phòng trong nhà
        fetchRooms();

        // Thiết lập nút thêm phòng
        View fab = findViewById(R.id.fabAddRoom);
        if (fab != null) fab.setOnClickListener(v -> showAddRoomDialog());

        // Thiết lập menu dưới
        setupBottomNavigation();
    }

    private void setupProgressBar() {
        // Tạo ProgressBar bằng code thay vì tìm từ layout
        ViewGroup rootView = findViewById(android.R.id.content);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);

        // Tạo FrameLayout để chứa ProgressBar ở giữa màn hình
        FrameLayout progressBarContainer = new FrameLayout(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        progressBarContainer.addView(progressBar, layoutParams);

        // Thêm container vào rootView với layout params full màn hình
        ViewGroup.LayoutParams containerParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        rootView.addView(progressBarContainer, containerParams);

        // Mặc định ẩn đi
        progressBar.setVisibility(View.GONE);
    }

    private void setupBottomNavigation() {
        findViewById(R.id.ivDevices).setOnClickListener(v ->
                startActivity(new Intent(this, DeviceInventoryActivity.class)));
        findViewById(R.id.ivRooms).setOnClickListener(v ->
                startActivity(new Intent(this, AllRoomsActivity.class)));
        findViewById(R.id.ivPlus).setOnClickListener(v -> {
            com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet sheet = 
                new com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet();
            sheet.show(getSupportFragmentManager(), "VoiceAssistant");
        });
        findViewById(R.id.ivHome).setOnClickListener(v ->
                startActivity(new Intent(this, HouseListActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        View ivSetting = findViewById(R.id.ivSetting);
        if (ivSetting != null) ivSetting.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void fetchHouseDetails() {
        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.getHouseById(houseIdInt).enqueue(new Callback<HouseDto>() {
            @Override
            public void onResponse(Call<HouseDto> call, Response<HouseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HouseDto houseDto = response.body();
                    setTitle(houseDto.name);
                    TextView title = findViewById(R.id.tvHouseTitle);
                    if (title != null) title.setText(houseDto.name);
                } else {
                    toast("Không thể lấy thông tin nhà");
                }
            }

            @Override
            public void onFailure(Call<HouseDto> call, Throwable t) {
                Log.e("HouseRooms", "Lỗi khi lấy thông tin nhà: " + t.getMessage());
                toast("Lỗi khi lấy thông tin nhà");
            }
        });
    }

    private void fetchRooms() {
        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.getRoomsByHouseId(houseIdInt, 0, 100).enqueue(new Callback<RoomListWrap>() {
            @Override
            public void onResponse(Call<RoomListWrap> call, Response<RoomListWrap> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().rooms != null) {
                    rooms.clear();
                    for (RoomDto roomDto : response.body().rooms) {
                        Room room = mapDtoToRoom(roomDto);
                        rooms.add(room);
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    toast("Không thể lấy danh sách phòng");
                }
            }

            @Override
            public void onFailure(Call<RoomListWrap> call, Throwable t) {
                showLoading(false);
                Log.e("HouseRooms", "Lỗi khi lấy danh sách phòng: " + t.getMessage());
                toast("Lỗi khi lấy danh sách phòng");
            }
        });
    }

    private Room mapDtoToRoom(RoomDto dto) {
        int iconResId = mapIconType(dto.type, dto.iconKey);
        Room room = new Room(String.valueOf(dto.id), dto.name, iconResId);
        room.setDescription(dto.detail);
        room.setDeviceCount(dto.deviceCount != null ? dto.deviceCount : 0);
        room.setActive(dto.isActive); // set trạng thái kích hoạt từ server
        return room;
    }

    private int mapIconType(String type, String iconKey) {
        String key = (iconKey != null ? iconKey : (type != null ? type : "")).toLowerCase();
        if (key.contains("living")) return R.drawable.ic_room_living;
        if (key.contains("bed")) return R.drawable.ic_room_bed;
        if (key.contains("kitchen")) return R.drawable.ic_room_kitchen;
        return R.drawable.ic_room_generic;
    }

    private void showAddRoomDialog() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_room_form, null, false);
        EditText etName = form.findViewById(R.id.etName);
        EditText etDesc = form.findViewById(R.id.etDescription);

        new AlertDialog.Builder(this)
                .setTitle("Thêm phòng")
                .setView(form)
                .setPositiveButton("Thêm", (d, which) -> {
                    String name = etName.getText() == null ? "" : etName.getText().toString().trim();
                    String desc = etDesc.getText() == null ? "" : etDesc.getText().toString().trim();

                    if (name.isEmpty()) {
                        toast("Tên phòng không được để trống");
                        return;
                    }

                    createRoom(name, desc);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createRoom(String name, String description) {
        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.createRoom(new CreateRoomReq(name, description, houseIdInt)).enqueue(new Callback<RoomDto>() {
            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    RoomDto newRoomDto = response.body();
                    Room newRoom = mapDtoToRoom(newRoomDto);
                    rooms.add(newRoom);
                    adapter.notifyItemInserted(rooms.size() - 1);
                    toast("Đã thêm phòng mới");
                } else {
                    toast("Không thể tạo phòng mới: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {
                showLoading(false);
                Log.e("HouseRooms", "Lỗi khi tạo phòng: " + t.getMessage());
                toast("Lỗi khi tạo phòng mới");
            }
        });
    }

    private void showEditRoomDialog(Room room, int position) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_room_form, null, false);
        EditText etName = form.findViewById(R.id.etName);
        EditText etDesc = form.findViewById(R.id.etDescription);
        etName.setText(room.getName());
        etDesc.setText(room.getDescription());

        new AlertDialog.Builder(this)
                .setTitle("Sửa phòng")
                .setView(form)
                .setPositiveButton("Lưu", (d, which) -> {
                    String name = etName.getText() == null ? "" : etName.getText().toString().trim();
                    String desc = etDesc.getText() == null ? "" : etDesc.getText().toString().trim();

                    if (name.isEmpty()) {
                        toast("Tên phòng không được để trống");
                        return;
                    }

                    int roomId = Integer.parseInt(room.getId());
                    updateRoom(roomId, name, desc, position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateRoom(int roomId, String name, String description, int position) {
        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.updateRoom(roomId, new UpdateRoomReq(name, description)).enqueue(new Callback<RoomDto>() {
            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    RoomDto updatedRoomDto = response.body();
                    Room updatedRoom = mapDtoToRoom(updatedRoomDto);
                    rooms.set(position, updatedRoom);
                    adapter.notifyItemChanged(position);
                    toast("Đã cập nhật phòng");
                } else {
                    toast("Không thể cập nhật phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {
                showLoading(false);
                Log.e("HouseRooms", "Lỗi khi cập nhật phòng: " + t.getMessage());
                toast("Lỗi khi cập nhật phòng");
            }
        });
    }

    private void showRoomActionsDialog(Room room, int position) {
        String[] actions = new String[]{"Sửa", "Xóa"};
        new AlertDialog.Builder(this)
                .setTitle(room.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showEditRoomDialog(room, position);
                    } else if (which == 1) {
                        confirmDeleteRoom(room, position);
                    }
                })
                .show();
    }

    private void confirmDeleteRoom(Room room, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa phòng")
                .setMessage("Bạn có chắc muốn xóa phòng này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    int roomId = Integer.parseInt(room.getId());
                    deleteRoom(roomId, position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteRoom(int roomId, int position) {
        showLoading(true);
        Api api = ApiClient.getClient(this).create(Api.class);
        api.deleteRoom(roomId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    rooms.remove(position);
                    adapter.notifyItemRemoved(position);
                    toast("Đã xóa phòng");
                } else {
                    toast("Không thể xóa phòng: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showLoading(false);
                Log.e("HouseRooms", "Lỗi khi xóa phòng: " + t.getMessage());
                toast("Lỗi khi xóa phòng");
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void callApiToggleRoom(Room room, boolean isActive) {
        Api api = ApiClient.getClient(this).create(Api.class);
        int roomId = Integer.parseInt(room.getId());
        
        api.toggleRoomActive(roomId, new ToggleActiveRequest(isActive)).enqueue(new Callback<ToggleResponse>() {
            @Override
            public void onResponse(Call<ToggleResponse> call, Response<ToggleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String msg = isActive ? "Đã kích hoạt " + room.getName() : "Đã tắt " + room.getName();
                    toast(msg);
                } else {
                    toast("Lỗi: " + response.code());
                    // Revert state
                    room.setActive(!isActive);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ToggleResponse> call, Throwable t) {
                toast("Lỗi mạng: " + t.getMessage());
                // Revert state
                room.setActive(!isActive);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

