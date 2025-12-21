package com.example.smarthomeui.smarthome.ui.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.ui.adapter.DeviceAdminAdapter;
import com.example.smarthomeui.smarthome.model.Device;
import java.util.ArrayList;
import java.util.List;

/**
 * Device Management Activity
 * Màn hình quản lý thiết bị cho admin
 */
public class DeviceManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerViewDevices;
    private EditText etSearchDevice;
    private ImageView btnBack, btnAddDevice;
    private CardView chipAllDevices, chipOnlineDevices, chipOfflineDevices;
    private DeviceAdminAdapter deviceAdapter;
    private List<Device> deviceList;
    private List<Device> filteredDeviceList;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();
        setupFilterChips();
        loadDevices();
    }

    private void initViews() {
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        etSearchDevice = findViewById(R.id.etSearchDevice);
        btnBack = findViewById(R.id.btnBack);
        btnAddDevice = findViewById(R.id.btnAddDevice);
        chipAllDevices = findViewById(R.id.chipAllDevices);
        chipOnlineDevices = findViewById(R.id.chipOnlineDevices);
        chipOfflineDevices = findViewById(R.id.chipOfflineDevices);
    }

    private void setupRecyclerView() {
        deviceList = new ArrayList<>();
        filteredDeviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdminAdapter(filteredDeviceList, this);
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnAddDevice.setOnClickListener(v -> showAddDeviceDialog());
    }

    private void setupSearch() {
        etSearchDevice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchDevices(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        chipAllDevices.setOnClickListener(v -> {
            setActiveChip("all");
            filterDevices("all");
        });

        chipOnlineDevices.setOnClickListener(v -> {
            setActiveChip("online");
            filterDevices("online");
        });

        chipOfflineDevices.setOnClickListener(v -> {
            setActiveChip("offline");
            filterDevices("offline");
        });
    }

    private void setActiveChip(String filter) {
        // Reset all chips
        chipAllDevices.setCardBackgroundColor(getColor(R.color.white));
        chipOnlineDevices.setCardBackgroundColor(getColor(R.color.white));
        chipOfflineDevices.setCardBackgroundColor(getColor(R.color.white));

        // Set active chip
        switch (filter) {
            case "all":
                chipAllDevices.setCardBackgroundColor(getColor(R.color.primaryColor));
                break;
            case "online":
                chipOnlineDevices.setCardBackgroundColor(getColor(R.color.primaryColor));
                break;
            case "offline":
                chipOfflineDevices.setCardBackgroundColor(getColor(R.color.primaryColor));
                break;
        }
        currentFilter = filter;
    }

    private void filterDevices(String filter) {
        filteredDeviceList.clear();
        String searchQuery = etSearchDevice.getText().toString().toLowerCase();

        for (Device device : deviceList) {
            boolean matchesFilter = false;
            switch (filter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "online":
                    matchesFilter = device.isOnline();
                    break;
                case "offline":
                    matchesFilter = !device.isOnline();
                    break;
            }

            boolean matchesSearch = searchQuery.isEmpty() ||
                device.getName().toLowerCase().contains(searchQuery) ||
                device.getType().toLowerCase().contains(searchQuery) ||
                device.getRoom().toLowerCase().contains(searchQuery);

            if (matchesFilter && matchesSearch) {
                filteredDeviceList.add(device);
            }
        }
        deviceAdapter.notifyDataSetChanged();
    }

    private void searchDevices(String query) {
        filterDevices(currentFilter);
    }

    private void loadDevices() {
        // TODO: Gọi API để lấy danh sách thiết bị
        deviceList.clear();
        deviceList.add(new Device("DEV001", "Đèn LED thông minh", "Phòng khách", "Đèn LED", true, 80, "12W"));
        deviceList.add(new Device("DEV002", "Điều hòa Samsung", "Phòng ngủ chính", "Điều hòa", true, 24, "1200W"));
        deviceList.add(new Device("DEV003", "Quạt trần Panasonic", "Phòng khách", "Quạt trần", false, 0, "75W"));
        deviceList.add(new Device("DEV004", "Tivi Sony 55 inch", "Phòng khách", "TV", true, 100, "150W"));
        deviceList.add(new Device("DEV005", "Máy lọc không khí", "Phòng ngủ", "Máy lọc khí", true, 60, "45W"));
        deviceList.add(new Device("DEV006", "Camera an ninh", "Cửa chính", "Camera", true, 100, "8W"));
        deviceList.add(new Device("DEV007", "Chuông cửa thông minh", "Cửa chính", "Chuông cửa", false, 0, "3W"));

        filterDevices(currentFilter);
    }

    private void showAddDeviceDialog() {
        // TODO: Implement add device dialog
    }

    public void editDevice(Device device) {
        // TODO: Implement edit device functionality
    }

    public void deleteDevice(Device device) {
        // TODO: Implement delete device functionality
    }

    public void toggleDevice(Device device, boolean isOn) {
        // TODO: Implement device toggle functionality
        device.setOnline(isOn);
        device.setValue(isOn ? 100 : 0);
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDevices();
    }
}
