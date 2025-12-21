package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.adapter.SingleRoomAdapter;
import com.example.smarthomeui.smarthome.adapter.UnassignedDeviceAdapter;
import com.example.smarthomeui.smarthome.components.DeviceControlBottomSheet;
import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.model.Room;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.DeviceDto;
import com.example.smarthomeui.smarthome.network.DeviceListWrap;
import com.example.smarthomeui.smarthome.network.RoomDto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomDetailsActivity extends AppCompatActivity {

    private String houseId;
    private String roomId;
    private Room room;

    private RecyclerView rv;
    private SingleRoomAdapter adapter;
    private List<Device> devices;   // list thiết bị thuộc phòng
    private TextView tvTitle;       // Reference to title TextView

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        houseId = getIntent().getStringExtra("house_id");
        roomId  = getIntent().getStringExtra("room_id");


        // Header
        tvTitle = findViewById(R.id.tvRoomTitle);
        View ivBack = findViewById(R.id.ivBack);
        if (tvTitle != null) tvTitle.setText(room != null ? room.getName() : getString(R.string.app_name));
        if (ivBack != null) ivBack.setOnClickListener(v -> onBackPressed());

        // RecyclerView
        rv = findViewById(R.id.rvDevices);
        rv.setLayoutManager(new LinearLayoutManager(this));

        devices = new ArrayList<>();

        adapter = new SingleRoomAdapter(devices, new SingleRoomAdapter.OnDeviceClick() {
            @Override
            public void onClick(Device device, int pos) {
                DeviceControlBottomSheet.newInstance(device, changed -> {
                    int idx = pos;
                    if (idx < 0 || idx >= devices.size()) idx = devices.indexOf(changed);
                    if (idx >= 0) adapter.notifyItemChanged(idx);
                }).show(getSupportFragmentManager(), "device_control");
            }

            @Override
            public void onEdit(Device device, int pos) {
                openEditDeviceDialog(device, pos);
            }

            @Override
            public void onDelete(Device device, int pos) {
                showDeleteDeviceConfirmation(device, pos);
            }
        });
        rv.setAdapter(adapter);

        // Load room info and devices from API
        loadRoomFromAPI();
        loadDevicesFromAPI();

        // FAB: LẤY THIẾT BỊ TỪ KHO
        FloatingActionButton fab = findViewById(R.id.fabAddDevice);
        if (fab != null) fab.setOnClickListener(v -> openPickFromInventory());
    }

    /** Load room information from API */
    private void loadRoomFromAPI() {
        // Parse roomId to int for API call
        int apiRoomId;
        try {
            apiRoomId = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            // If roomId is not a number, use local data
            return;
        }

        Api api = ApiClient.getClient(this).create(Api.class);
        Call<RoomDto> call = api.getRoomById(apiRoomId);

        call.enqueue(new Callback<RoomDto>() {
            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RoomDto roomDto = response.body();

                    // Update room title with name from API
                    if (tvTitle != null && roomDto.name != null) {
                        tvTitle.setText(roomDto.name);
                    }
                }
                // If API fails, keep the current title from local data
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {
                // Keep using local room name if API fails
            }
        });
    }

    /** Load devices from API by room ID */
    private void loadDevicesFromAPI() {
        // Parse roomId to int for API call
        int apiRoomId;
        try {
            apiRoomId = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            // If roomId is not a number, fallback to local data
            loadDevicesFromLocal();
            return;
        }

        Api api = ApiClient.getClient(this).create(Api.class);
        Call<DeviceListWrap> call = api.getDevicesByRoomId(apiRoomId);

        call.enqueue(new Callback<DeviceListWrap>() {
            @Override
            public void onResponse(Call<DeviceListWrap> call, Response<DeviceListWrap> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeviceListWrap deviceWrap = response.body();
                    List<DeviceDto> apiDevices = deviceWrap.getDevices();

                    if (apiDevices != null && !apiDevices.isEmpty()) {
                        devices.clear();

                        // Convert API devices to local Device model
                        for (DeviceDto deviceDto : apiDevices) {
                            Device device = convertApiDeviceToDevice(deviceDto);
                            devices.add(device);
                        }

                        // Update RecyclerView
                        adapter.notifyDataSetChanged();
                    } else {
                        // No devices found, show empty state
                        devices.clear();
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    // API call failed, fallback to local data
                    Toast.makeText(RoomDetailsActivity.this,
                            "Không thể tải từ server, hiển thị dữ liệu local",
                            Toast.LENGTH_SHORT).show();
                    loadDevicesFromLocal();
                }
            }

            @Override
            public void onFailure(Call<DeviceListWrap> call, Throwable t) {
                // Network error, fallback to local data
                Toast.makeText(RoomDetailsActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                loadDevicesFromLocal();
            }
        });
    }

    /** Fallback: Load devices from local repository */
    private void loadDevicesFromLocal() {
        devices.clear();
        if (room != null && room.getDevices() != null) {
            devices.addAll(room.getDevices());
        }
        adapter.notifyDataSetChanged();
    }

    /** Convert DeviceDto from API to local Device model */
    private Device convertApiDeviceToDevice(DeviceDto deviceDto) {
        Device device = new Device(
                String.valueOf(deviceDto.getId()),
                deviceDto.getName(),
                room != null ? room.getName() : "",
                deviceDto.getType() != null ? deviceDto.getType() : "Unknown",
                false, // default to off
                0,     // default power
                "N/A"  // default wattage
        );

        // Store API ID as token for future reference
        device.setToken(String.valueOf(deviceDto.getId()));

        // Set default properties based on type
        if ("Light".equalsIgnoreCase(deviceDto.getType())) {
            device.setBrightness(100);
            device.setColor(0xFFFFFFFF);
        } else if ("Fan".equalsIgnoreCase(deviceDto.getType())) {
            device.setSpeed(1);
        }

        return device;
    }

    /** Mở dialog chọn 1 thiết bị từ Kho và gán vào phòng */
    private void openPickFromInventory() {
        // Fetch all devices from API
        Api api = ApiClient.getClient(this).create(Api.class);
        Call<DeviceListWrap> call = api.getDevices(0, 100); // Load 100 devices

        call.enqueue(new Callback<DeviceListWrap>() {
            @Override
            public void onResponse(Call<DeviceListWrap> call, Response<DeviceListWrap> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeviceListWrap deviceWrap = response.body();
                    List<DeviceDto> allDevices = deviceWrap.getDevices();

                    if (allDevices != null && !allDevices.isEmpty()) {
                        // Filter unassigned devices (roomId == null)
                        List<DeviceDto> unassignedDevices = new ArrayList<>();
                        for (DeviceDto deviceDto : allDevices) {
                            // Log để debug
                            android.util.Log.d("RoomDetails", "Device: " + deviceDto.getName() +
                                ", roomId: " + deviceDto.getRoomId());

                            if (deviceDto.getRoomId() == null) {
                                unassignedDevices.add(deviceDto);
                            }
                        }

                        // Log số lượng
                        android.util.Log.d("RoomDetails", "Total devices: " + allDevices.size() +
                            ", Unassigned: " + unassignedDevices.size());

                        if (!unassignedDevices.isEmpty()) {
                            // Show devices in a dialog
                            showDevicesDialog(unassignedDevices);
                        } else {
                            Toast.makeText(RoomDetailsActivity.this,
                                "Không có thiết bị nào chưa được gán vào phòng (" + allDevices.size() + " thiết bị tổng)",
                                Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(RoomDetailsActivity.this, "Không có thiết bị nào", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RoomDetailsActivity.this, "Không thể tải thiết bị từ server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeviceListWrap> call, Throwable t) {
                Toast.makeText(RoomDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Hiển thị dialog danh sách thiết bị chưa được gán */
    private void showDevicesDialog(List<DeviceDto> deviceDtos) {
        // Dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pick_device, null);
        RecyclerView rvUnassignedDevices = dialogView.findViewById(R.id.rvUnassignedDevices);
        TextView tvEmptyMessage = dialogView.findViewById(R.id.tvEmptyMessage);

        rvUnassignedDevices.setLayoutManager(new LinearLayoutManager(this));
        rvUnassignedDevices.setVisibility(View.VISIBLE);
        tvEmptyMessage.setVisibility(View.GONE);

        // Create dialog first
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Đóng", null)
                .create();

        // Adapter for RecyclerView in dialog
        UnassignedDeviceAdapter dialogAdapter = new UnassignedDeviceAdapter(deviceDtos, deviceDto -> {
            // Handle device click: call API to assign device to room
            addDeviceToRoomAPI(deviceDto, dialog);
        });

        rvUnassignedDevices.setAdapter(dialogAdapter);
        dialog.show();
    }

    /** Gọi API để thêm thiết bị vào phòng */
    private void addDeviceToRoomAPI(DeviceDto deviceDto, AlertDialog dialog) {
        // Parse roomId to int for API call
        int apiRoomId;
        try {
            apiRoomId = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Room ID không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        int deviceId = deviceDto.getId();

        // Call API to add device to room
        Api api = ApiClient.getClient(this).create(Api.class);
        Call<Void> call = api.addDeviceToRoom(apiRoomId, deviceId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RoomDetailsActivity.this, "Đã thêm thiết bị vào phòng", Toast.LENGTH_SHORT).show();

                    // Convert DeviceDto to Device and add to list
                    Device newDevice = convertApiDeviceToDevice(deviceDto);
                    devices.add(newDevice);
                    adapter.notifyItemInserted(devices.size() - 1);

                    // Close dialog
                    dialog.dismiss();

                    // Reload devices to ensure sync
                    loadDevicesFromAPI();
                } else {
                    Toast.makeText(RoomDetailsActivity.this, "Không thể thêm thiết bị: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RoomDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Mở dialog sửa tên thiết bị */
    private void openEditDeviceDialog(Device device, int pos) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_device, null, false);
        EditText edtName = view.findViewById(R.id.edtDeviceName);
        Spinner spType = view.findViewById(R.id.spDeviceType);

        // Pre-fill current values
        edtName.setText(device.getName());

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.device_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        // Select current type in spinner
        String currentType = device.getType();
        if (currentType != null) {
            int spinnerPosition = typeAdapter.getPosition(currentType);
            if (spinnerPosition >= 0) {
                spType.setSelection(spinnerPosition);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Sửa thiết bị")
                .setView(view)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String newName = edtName.getText().toString().trim();
                    String newType = String.valueOf(spType.getSelectedItem());

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int deviceId = Integer.parseInt(device.getToken());
                    Api api = ApiClient.getClient(this).create(Api.class);
                    api.updateDevice(deviceId, new com.example.smarthomeui.smarthome.network.UpdateDeviceReq(newName, null))
                        .enqueue(new Callback<com.example.smarthomeui.smarthome.network.DeviceDto>() {
                            @Override
                            public void onResponse(Call<com.example.smarthomeui.smarthome.network.DeviceDto> call, Response<com.example.smarthomeui.smarthome.network.DeviceDto> response) {
                                if (response.isSuccessful()) {
                                    device.setName(newName);
                                    device.setType(newType);
                                    if (pos >= 0 && pos < devices.size()) {
                                        adapter.notifyItemChanged(pos);
                                        Toast.makeText(RoomDetailsActivity.this, "Đã cập nhật thiết bị", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(RoomDetailsActivity.this, "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<com.example.smarthomeui.smarthome.network.DeviceDto> call, Throwable t) {
                                Toast.makeText(RoomDetailsActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                })
                .show();
    }

    /** Hiển thị dialog xác nhận xóa thiết bị */
    private void showDeleteDeviceConfirmation(Device device, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thiết bị")
                .setMessage("Bạn có chắc chắn muốn xóa thiết bị \"" + device.getName() + "\"?")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xóa", (d, w) -> {
                    // Gọi API để xóa thiết bị hoàn toàn
                    int deviceId = Integer.parseInt(device.getToken());
                    Api api = ApiClient.getClient(this).create(Api.class);
                    api.deleteDevice(deviceId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                if (pos >= 0 && pos < devices.size()) {
                                    devices.remove(pos);
                                    adapter.notifyItemRemoved(pos);
                                }
                                Toast.makeText(RoomDetailsActivity.this, "Đã xóa thiết bị", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RoomDetailsActivity.this, "Lỗi xóa: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(RoomDetailsActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    /** Gọi API để xóa thiết bị ra khỏi phòng */
    private void removeDeviceFromRoomAPI(Device device, int pos) {
        // Parse roomId to int for API call
        int apiRoomId;
        try {
            apiRoomId = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Room ID không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get device ID from token (API ID)
        int deviceId;
        try {
            deviceId = Integer.parseInt(device.getToken());
        } catch (NumberFormatException e) {
            // Fallback to device.getId() if token is not set
            try {
                deviceId = Integer.parseInt(device.getId());
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Device ID không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Call API to remove device from room
        Api api = ApiClient.getClient(this).create(Api.class);
        Call<Void> call = api.removeDeviceFromRoom(apiRoomId, deviceId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RoomDetailsActivity.this, "Đã xóa thiết bị ra khỏi phòng", Toast.LENGTH_SHORT).show();

                    // Remove device from list
                    if (pos >= 0 && pos < devices.size()) {
                        devices.remove(pos);
                        adapter.notifyItemRemoved(pos);
                    }

                    // Reload devices to ensure sync
                    loadDevicesFromAPI();
                } else {
                    Toast.makeText(RoomDetailsActivity.this, "Không thể xóa thiết bị: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RoomDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
