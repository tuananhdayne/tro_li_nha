package com.example.smarthomeui.smarthome.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.util.Log;
import android.widget.Spinner;
import android.widget.Toast;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.le.ScanResult;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.adapter.DeviceInventoryAdapter;
import com.example.smarthomeui.smarthome.ai_speech_reg.MainActivitySR;
import com.example.smarthomeui.smarthome.components.DeviceControlBottomSheet;
import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.provision.ProvisionSession;

// Import đầy đủ các class cần thiết
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.ESPDevice;
import com.espressif.provisioning.listeners.BleScanListener;
import com.espressif.provisioning.listeners.WiFiScanListener;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.WiFiAccessPoint;
import com.espressif.provisioning.listeners.ProvisionListener;

// Import cho API
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.DeviceListWrap;
import com.example.smarthomeui.smarthome.network.DeviceDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

import static com.example.smarthomeui.smarthome.model.Device.*;

public class DeviceInventoryActivity extends AppCompatActivity {

    private final List<Device> inventory = new ArrayList<>();
    private DeviceInventoryAdapter adapter;

    private static final int REQ_PERMS = 1001;
    private static final int REQ_BLE_SCAN = 1002;

    private static final String ESP_BLE_PRIMARY_SERVICE_UUID = "0000ffff-0000-1000-8000-00805f9b34fb";
    @Nullable
    private ESPDevice currentEspDevice;

    // Maintain a live list dialog for BLE scan results
    private final List<BluetoothDevice> bleDevices = new ArrayList<>();
    @Nullable
    private AlertDialog bleListDialog;
    @Nullable
    private ArrayAdapter<String> bleListAdapter;

    // Helper: safely get device name without throwing SecurityException on S+
    private String getDeviceDisplayName(@Nullable BluetoothDevice device) {
        if (device == null)
            return "Unknown";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPerm(Manifest.permission.BLUETOOTH_CONNECT)) {
                    return "Unknown";
                }
            }
            String n = device.getName();
            return (n == null || n.isEmpty()) ? "Unknown" : n;
        } catch (SecurityException se) {
            return "Unknown";
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_inventory);
        findViewById(R.id.ivDevices).setSelected(true);

        findViewById(R.id.ivHome).setOnClickListener(v -> startActivity(new Intent(this, HouseListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        findViewById(R.id.ivControl).setOnClickListener(v -> {
            com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet sheet = 
                new com.example.smarthomeui.smarthome.ui.bottomsheet.VoiceAssistantBottomSheet();
            sheet.show(getSupportFragmentManager(), "VoiceAssistant");
        });

        findViewById(R.id.ivRooms).setOnClickListener(v -> startActivity(new Intent(this, AllRoomsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        // Plus ở giữa: mở dialog thêm thiết bị vào KHO (không gán phòng)
        findViewById(R.id.ivSetting).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        findViewById(R.id.ivBack).setOnClickListener(v -> onBackPressed());

        RecyclerView rv = findViewById(R.id.rvInventory);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DeviceInventoryAdapter(inventory, new DeviceInventoryAdapter.OnItemAction() {
            @Override
            public void onControl(Device d, int pos) {
                // dùng lại bottom sheet điều khiển theo capabilities (tuỳ chọn)
                DeviceControlBottomSheet.newInstance(d, changed -> adapter.notifyItemChanged(pos))
                        .show(getSupportFragmentManager(), "control");
            }

            @Override
            public void onEdit(Device d, int pos) {
                // Mở dialog sửa thông tin thiết bị
                showEditDeviceDialog(d, pos);
            }

            @Override
            public void onDelete(Device d, int pos) {
                // Hiển thị dialog xác nhận xóa
                showDeleteConfirmDialog(d, pos);
            }
        });
        rv.setAdapter(adapter);

        findViewById(R.id.fabAddInventory).setOnClickListener(v -> openAddToInventoryDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh inventory khi quay lại từ BLE scan flow
        refreshInventory();
        // Load thiết bị từ API
        loadDevicesFromAPI();
    }

    private void refreshInventory() {
        inventory.clear();
        adapter.notifyDataSetChanged();
    }

    private void loadDevicesFromAPI() {
        Api api = ApiClient.getClient(this).create(Api.class);
        Call<DeviceListWrap> call = api.getDevices(0, 100); // Load 100 devices đầu tiên

        call.enqueue(new Callback<DeviceListWrap>() {
            @Override
            public void onResponse(Call<DeviceListWrap> call, Response<DeviceListWrap> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeviceListWrap deviceWrap = response.body();
                    List<DeviceDto> apiDevices = deviceWrap.getDevices();

                    if (apiDevices != null && !apiDevices.isEmpty()) {
                        for (DeviceDto deviceDto : apiDevices) {
                            // Chuyển đổi từ DeviceDto sang Device model
                            Device device = convertApiDeviceToDevice(deviceDto);

                            // Kiểm tra xem thiết bị đã tồn tại trong inventory chưa
                            boolean exists = false;
                            for (Device existingDevice : inventory) {
                                if (existingDevice.getToken() != null &&
                                        existingDevice.getToken().equals(String.valueOf(deviceDto.getId()))) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                // Thêm vào local repository và UI
                                inventory.add(device);
                            }
                        }

                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            Toast.makeText(DeviceInventoryActivity.this,
                                    "Đã tải " + apiDevices.size() + " thiết bị từ server",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                            "Không thể tải thiết bị từ server", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<DeviceListWrap> call, Throwable t) {
                runOnUiThread(() -> {
                    Log.e("API_LOAD_DEVICES", "Error loading devices", t);
                    Toast.makeText(DeviceInventoryActivity.this,
                            "Lỗi kết nối server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Device convertApiDeviceToDevice(DeviceDto deviceDto) {
        String deviceId = String.valueOf(deviceDto.getId());
        String name = deviceDto.getName();
        String type = deviceDto.getType();

        Device device = new Device(deviceId, name, type, false);
        device.setToken(String.valueOf(deviceDto.getId())); // Dùng API ID làm token
        device.setRoomId(deviceDto.getRoomId()); // Lưu roomId từ API

        // Phân loại điều khiển dựa trên type - chỉ hỗ trợ quạt và đèn
        String lowerType = type.toLowerCase(Locale.US);
        if (lowerType.contains("light") || lowerType.contains("đèn")) {
            // Thiết bị đèn
            device.addCaps(CAP_POWER, CAP_BRIGHTNESS, CAP_COLOR);
            device.setBrightness(100);
            device.setColor(0xFFFFFFFF);
        } else if (lowerType.contains("fan") || lowerType.contains("quạt")) {
            // Thiết bị quạt
            device.addCaps(CAP_POWER, CAP_SPEED);
            device.setSpeed(1);
        } else {
            // Mặc định: thiết bị không xác định sẽ được coi như đèn
            device.addCaps(CAP_POWER, CAP_BRIGHTNESS, CAP_COLOR);
            device.setBrightness(100);
            device.setColor(0xFFFFFFFF);
        }

        return device;
    }

    private void openAddToInventoryDialog() {
        // Hiển thị dialog nhập tên thiết bị trước
        showDeviceNameDialog();
    }

    private void showDeviceNameDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_enter_device_name, null, false);
        EditText edtDeviceName = view.findViewById(R.id.edtDeviceName);
        Spinner spinnerType = view.findViewById(R.id.spinnerDeviceType);

        String[] displayNames = { "Đèn", "Khóa cửa", "Cảm biến nhiệt độ", "Cảm biến chuyển động" };
        String[] deviceTypeValues = { "Light", "DoorLock", "TemperatureHumiditySensor", "MotionSensor" };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, displayNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog)
                .setTitle("Thêm thiết bị ESP")
                .setView(view)
                .setPositiveButton("Tiếp tục", null)
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String deviceName = String.valueOf(edtDeviceName.getText()).trim();

                if (deviceName.isEmpty()) {
                    edtDeviceName.setError("Nhập tên thiết bị");
                    return;
                }
                // Lấy vị trí được chọn từ spinner
                int selectedPosition = spinnerType.getSelectedItemPosition();
                String deviceType = deviceTypeValues[selectedPosition];

                // Lưu tên thiết bị và loại thiết bị vào session
                ProvisionSession.get().setDeviceName(deviceName);
                ProvisionSession.get().setDeviceType(deviceType);

                dialog.dismiss();

                // Lưu tên thiết bị vào session
                ProvisionSession.get().setDeviceName(deviceName);

                dialog.dismiss();

                // Hiển thị dialog chọn cách kết nối
                showConnectionMethodDialog();
            });
        });

        dialog.show();
    }

    private void showConnectionMethodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog);
        builder.setTitle("Chọn cách kết nối");

        String[] options = { "Quét thiết bị Bluetooth", "Thêm thủ công" };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Lấy userId từ UserManager
                    com.example.smarthomeui.smarthome.utils.UserManager userManager = new com.example.smarthomeui.smarthome.utils.UserManager(
                            this);
                    String userId = userManager.getUserId();

                    // Gọi BLEScanActivity với userId (roomId để null vì đây là inventory)
                    Intent intent = new Intent(this, BLEScanActivity.class);
                    intent.putExtra("user_id", userId);
                    // Không truyền room_id vì đây là inventory chung
                    startActivity(intent);
                    break;
                case 1:
                    openManualAddDialog();
                    break;
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void startBluetoothScan() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        try {
            ESPProvisionManager provisionManager = ESPProvisionManager.getInstance(this);
            Toast.makeText(this, "Đang quét thiết bị Bluetooth...", Toast.LENGTH_SHORT).show();

            // Show BLE devices picker dialog
            runOnUiThread(() -> showBleDevicesPicker());

            try {
                provisionManager.searchBleEspDevices("", new BleScanListener() {
                    @Override
                    public void scanStartFailed() {
                        runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                                "Không thể bắt đầu quét Bluetooth", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onPeripheralFound(BluetoothDevice device, ScanResult scanResult) {
                        runOnUiThread(() -> addBleDeviceToList(device));
                    }

                    @Override
                    public void scanCompleted() {
                        runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                                "Quét Bluetooth hoàn tất", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                                "Lỗi quét Bluetooth: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
            } catch (SecurityException se) {
                Toast.makeText(this, "Thiếu quyền Bluetooth", Toast.LENGTH_SHORT).show();
                requestPermissions();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi tạo ESP Manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ESP_SCAN", "Error initializing ESP Manager", e);
        }
    }

    // Show a single BLE devices picker dialog and keep it updated
    private void showBleDevicesPicker() {
        // Reset list and adapter
        bleDevices.clear();
        List<String> names = new ArrayList<>();
        bleListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Chọn thiết bị Bluetooth")
                .setAdapter(bleListAdapter, (d, which) -> {
                    if (which >= 0 && which < bleDevices.size()) {
                        BluetoothDevice sel = bleDevices.get(which);
                        d.dismiss();
                        connectToBluetoothDevice(sel);
                    }
                })
                .setNegativeButton("Đóng", (d, w) -> d.dismiss());
        bleListDialog = b.create();
        bleListDialog.show();

        // If nothing found yet, show a hint row
        if (bleListAdapter.getCount() == 0) {
            bleListAdapter.add("Đang quét...");
        }
    }

    private void addBleDeviceToList(BluetoothDevice device) {
        if (device == null)
            return;
        // Avoid duplicates by MAC address
        String addr = device.getAddress();
        for (BluetoothDevice d : bleDevices) {
            if (Objects.equals(d.getAddress(), addr)) {
                return;
            }
        }
        bleDevices.add(device);

        if (bleListAdapter != null) {
            String name = getDeviceDisplayName(device);
            String display = name + " (" + addr + ")";
            // Remove placeholder if present
            int idx = -1;
            for (int i = 0; i < bleListAdapter.getCount(); i++) {
                if (Objects.equals(bleListAdapter.getItem(i), "Đang quét...")) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0)
                bleListAdapter.remove("Đang quét...");
            bleListAdapter.add(display);
            bleListAdapter.notifyDataSetChanged();
        } else {
            // If adapter not ready yet, ensure dialog is shown
            showBleDevicesPicker();
        }
    }

    private void showBleDeviceDialog(BluetoothDevice device) {
        // Replaced by list-based picker; keep for backward-compat if needed
        String name = getDeviceDisplayName(device);
        new AlertDialog.Builder(this)
                .setTitle("Thiết bị BLE tìm thấy")
                .setMessage("Tên: " + name + "\nĐịa chỉ: " + device.getAddress())
                .setPositiveButton("Kết nối", (d, w) -> connectToBluetoothDevice(device))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void connectToBluetoothDevice(BluetoothDevice device) {
        try {
            if (!checkPermissions()) {
                requestPermissions();
                return;
            }

            Toast.makeText(this, "Đang kết nối đến " + getDeviceDisplayName(device) + "...", Toast.LENGTH_SHORT).show();

            ESPProvisionManager pm = ESPProvisionManager.getInstance(this);
            ESPDevice esp = pm.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE,
                    ESPConstants.SecurityType.SECURITY_2);
            currentEspDevice = esp;

            try {
                esp.setProofOfPossession("abcd1234");
            } catch (Exception ignored) {
            }

            esp.connectBLEDevice(device, ESP_BLE_PRIMARY_SERVICE_UUID);

            // Dismiss the list dialog if showing
            if (bleListDialog != null && bleListDialog.isShowing()) {
                bleListDialog.dismiss();
            }

            // Save ESP device to session and navigate to WiFiProvisionActivity
            ProvisionSession.get().setEspDevice(esp);

            Toast.makeText(this, "Kết nối Bluetooth thành công! Chuyển đến cấu hình WiFi...", Toast.LENGTH_SHORT)
                    .show();

            // Start WiFiProvisionActivity for WiFi scanning and provisioning
            WiFiProvisionActivity.start(this);

        } catch (SecurityException se) {
            Toast.makeText(this, "Thiếu quyền Bluetooth", Toast.LENGTH_SHORT).show();
            requestPermissions();
        } catch (Exception e) {
            Toast.makeText(this, "Kết nối Bluetooth thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ESP_BLE", "connectToBluetoothDevice error", e);
        }
    }

    private void showWifiScanAndProvision() {
        if (currentEspDevice == null) {
            Toast.makeText(this, "Thiết bị ESP chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Đang quét WiFi gần thiết bị...", Toast.LENGTH_SHORT).show();
        try {
            currentEspDevice.scanNetworks(new WiFiScanListener() {
                @Override
                public void onWifiListReceived(ArrayList<WiFiAccessPoint> wifiList) {
                    runOnUiThread(() -> showProvisionChoiceDialog(wifiList));
                }

                @Override
                public void onWiFiScanFailed(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(DeviceInventoryActivity.this, "Quét WiFi thất bại: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        // Cho nhập tay nếu quét lỗi
                        showManualProvisionDialog(null);
                    });
                }
            });
        } catch (SecurityException se) {
            Toast.makeText(this, "Thiếu quyền Location/WiFi để quét mạng", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProvisionChoiceDialog(@Nullable ArrayList<WiFiAccessPoint> wifiList) {
        List<String> entries = new ArrayList<>();
        if (wifiList != null) {
            for (WiFiAccessPoint ap : wifiList) {
                try {
                    entries.add(ap.getWifiName());
                } catch (Exception e) {
                    entries.add(String.valueOf(ap));
                }
            }
        }
        entries.add("Nhập SSID / mật khẩu thủ công");
        String[] items = entries.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Chọn mạng WiFi để cung cấp")
                .setItems(items, (d, which) -> {
                    if (wifiList != null && which < wifiList.size()) {
                        String ssid = items[which];
                        showManualProvisionDialog(ssid);
                    } else {
                        showManualProvisionDialog(null);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showManualProvisionDialog(@Nullable String prefillSsid) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_provision_wifi, null, false);
        EditText edtSsid = view.findViewById(R.id.edtSsid);
        EditText edtPass = view.findViewById(R.id.edtPass);
        if (prefillSsid != null)
            edtSsid.setText(prefillSsid);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog)
                .setTitle("Cấu hình WiFi cho thiết bị")
                .setView(view)
                .setPositiveButton("Cấu hình", null)
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String ssid = String.valueOf(edtSsid.getText()).trim();
                String pass = String.valueOf(edtPass.getText());
                if (ssid.isEmpty()) {
                    edtSsid.setError("Nhập SSID");
                    return;
                }
                provisionToNetwork(ssid, pass, dialog);
            });
        });
        dialog.show();
    }

    private void provisionToNetwork(String ssid, String pass, AlertDialog dialogToDismiss) {
        if (currentEspDevice == null) {
            Toast.makeText(this, "Thiết bị ESP chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Đang gửi cấu hình WiFi...", Toast.LENGTH_SHORT).show();
        try {
            currentEspDevice.provision(ssid, pass, new ProvisionListener() {
                @Override
                public void createSessionFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                            "Tạo phiên thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void wifiConfigSent() {
                    runOnUiThread(() -> Toast
                            .makeText(DeviceInventoryActivity.this, "Đã gửi cấu hình WiFi", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void wifiConfigFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                            "Gửi cấu hình thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void wifiConfigApplied() {
                    runOnUiThread(() -> Toast
                            .makeText(DeviceInventoryActivity.this, "Thiết bị áp dụng cấu hình", Toast.LENGTH_SHORT)
                            .show());
                }

                @Override
                public void wifiConfigApplyFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                            "Áp dụng cấu hình thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void provisioningFailedFromDevice(ESPConstants.ProvisionFailureReason failureReason) {
                    runOnUiThread(
                            () -> Toast
                                    .makeText(DeviceInventoryActivity.this,
                                            "Provisioning thất bại từ thiết bị: " + failureReason, Toast.LENGTH_SHORT)
                                    .show());
                }

                @Override
                public void onProvisioningFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                            "Provisioning thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void deviceProvisioningSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(DeviceInventoryActivity.this, "Provisioning thành công!", Toast.LENGTH_LONG)
                                .show();
                        // Sau khi provision thành công, thêm vào inventory nếu chưa có
                        String name = "ESP Device";
                        Device newDevice = new Device(UUID.randomUUID().toString(), name, "ESP Device", false);
                        newDevice.setToken(ssid);
                        newDevice.addCaps(CAP_POWER, CAP_BRIGHTNESS, CAP_COLOR);
                        newDevice.setBrightness(100);
                        newDevice.setColor(0xFFFFFFFF);

                        inventory.add(newDevice);
                        adapter.notifyItemInserted(inventory.size() - 1);
                        if (dialogToDismiss != null)
                            dialogToDismiss.dismiss();
                    });
                }
            });
        } catch (SecurityException se) {
            Toast.makeText(this, "Thiếu quyền mạng/Location để provisioning", Toast.LENGTH_SHORT).show();
        }
    }

    private void openManualAddDialog() {
        // Giữ lại dialog thêm thủ công cũ cho trường hợp cần thiết
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_device_global, null, false);

        EditText edtName = view.findViewById(R.id.edtDeviceName);
        EditText edtToken = view.findViewById(R.id.edtDeviceToken);
        com.google.android.material.chip.ChipGroup chipGroupType = view.findViewById(R.id.chipGroupType);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog)
                .setView(view).create();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String name = String.valueOf(edtName.getText()).trim();
            String token = String.valueOf(edtToken.getText()).trim();

            if (name.isEmpty()) {
                edtName.setError("Nhập tên thiết bị");
                return;
            }
            if (token.isEmpty()) {
                edtToken.setError("Nhập device token");
                return;
            }
            if (!token.matches("^[A-Za-z0-9_-]{6,64}$")) {
                edtToken.setError("Token không hợp lệ (6–64 ký tự)");
                return;
            }

            // Lấy type từ ChipGroup
            int checkedId = chipGroupType.getCheckedChipId();
            if (checkedId == View.NO_ID)
                return;
            com.google.android.material.chip.Chip chip = view.findViewById(checkedId);
            String type = chip.getText().toString();

            Device dev = new Device(UUID.randomUUID().toString(), name, type, false);
            dev.setToken(token);

            String lower = type.toLowerCase(Locale.US);
            if (lower.contains("light") || lower.contains("đèn")) {
                dev.addCaps(CAP_POWER, CAP_BRIGHTNESS, CAP_COLOR);
                dev.setBrightness(100);
                dev.setColor(0xFFFFFFFF);
            } else if (lower.contains("fan") || lower.contains("quạt")) {
                dev.addCaps(CAP_POWER, CAP_SPEED);
                dev.setSpeed(1);
            } else {
                // Mặc định: thiết bị không xác định sẽ được coi như đèn
                dev.addCaps(CAP_POWER, CAP_BRIGHTNESS, CAP_COLOR);
                dev.setBrightness(100);
                dev.setColor(0xFFFFFFFF);
            }

            inventory.add(dev);
            adapter.notifyItemInserted(inventory.size() - 1);
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasPerm(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPerm(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            return hasPerm(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private boolean hasPerm(String p) {
        return ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQ_PERMS);
        } else {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQ_PERMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startBluetoothScan();
            } else {
                Toast.makeText(this, "Cần cấp quyền để quét/kết nối thiết bị ESP", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Hiển thị dialog sửa thông tin thiết bị
     */
    private void showEditDeviceDialog(Device device, int position) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_device, null, false);
        EditText edtName = view.findViewById(R.id.edtDeviceName);
        android.widget.Spinner spinnerRoom = view.findViewById(R.id.spinnerRoom);

        // Điền thông tin hiện tại
        edtName.setText(device.getName());

        // Log current device state
        Log.d("EDIT_DEVICE", "Opening edit dialog for device: " + device.getName());
        Log.d("EDIT_DEVICE", "Current device roomId: " + device.getRoomId());

        // Tải danh sách phòng
        loadRoomsForEditDialog(spinnerRoom, device);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog)
                .setTitle("Sửa thông tin thiết bị")
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newName = String.valueOf(edtName.getText()).trim();

                if (newName.isEmpty()) {
                    edtName.setError("Nhập tên thiết bị");
                    return;
                }

                // Lấy roomID từ Spinner
                Object selectedItem = spinnerRoom.getSelectedItem();
                Log.d("EDIT_DEVICE", "Selected item: " + selectedItem);

                if (selectedItem == null) {
                    Toast.makeText(DeviceInventoryActivity.this,
                            "Vui lòng chọn phòng", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!(selectedItem instanceof RoomSpinnerItem)) {
                    Toast.makeText(DeviceInventoryActivity.this,
                            "Lỗi: Item không hợp lệ", Toast.LENGTH_SHORT).show();
                    Log.e("EDIT_DEVICE", "Selected item is not RoomSpinnerItem: " + selectedItem.getClass().getName());
                    return;
                }

                RoomSpinnerItem roomItem = (RoomSpinnerItem) selectedItem;
                Log.d("EDIT_DEVICE", "Selected room ID: " + roomItem.id + ", Name: " + roomItem.roomName);

                // Kiểm tra nếu chưa chọn phòng thực sự (vẫn là placeholder)
                if (roomItem.id == -1) {
                    Toast.makeText(DeviceInventoryActivity.this,
                            "Vui lòng chọn một phòng để gắn thiết bị", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Đảm bảo roomId là một số nguyên dương hợp lệ
                if (roomItem.id <= 0) {
                    Toast.makeText(DeviceInventoryActivity.this,
                            "Lỗi: Room ID không hợp lệ", Toast.LENGTH_SHORT).show();
                    Log.e("EDIT_DEVICE", "Invalid room ID: " + roomItem.id);
                    return;
                }

                // Gọi API cập nhật thiết bị với roomId đã chọn (đảm bảo không null)
                Integer roomIdToSend = Integer.valueOf(roomItem.id);
                Log.d("EDIT_DEVICE", "Sending roomId to API: " + roomIdToSend);
                updateDeviceAPI(device, newName, roomIdToSend, position, dialog);
            });
        });

        dialog.show();
    }

    /**
     * Tải danh sách phòng cho dialog sửa thiết bị
     */
    private void loadRoomsForEditDialog(android.widget.Spinner spinner, Device device) {
        Api api = ApiClient.getClient(this).create(Api.class);

        // Tạo list cho các phòng kèm thông tin nhà
        List<RoomSpinnerItem> roomItems = new ArrayList<>();

        // Tải danh sách phòng từ API (đã nhóm theo nhà)
        Api roomApi = ApiClient.getClient(DeviceInventoryActivity.this).create(Api.class);
        roomApi.getRoomsGrouped(0, 100)
                .enqueue(new Callback<com.example.smarthomeui.smarthome.network.RoomsByHouseWrap>() {
                    @Override
                    public void onResponse(Call<com.example.smarthomeui.smarthome.network.RoomsByHouseWrap> call,
                            Response<com.example.smarthomeui.smarthome.network.RoomsByHouseWrap> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Integer deviceRoomId = device.getRoomId();

                            // Luôn thêm placeholder ở đầu
                            roomItems.add(new RoomSpinnerItem(-1, "-- Chọn phòng --", ""));

                            // Lấy groups để có thông tin về nhà
                            List<com.example.smarthomeui.smarthome.network.RoomsByHouseWrap.Group> groups = response
                                    .body().groups;

                            if (groups != null) {
                                // Duyệt qua từng group (nhà) để lấy phòng và tên nhà
                                for (com.example.smarthomeui.smarthome.network.RoomsByHouseWrap.Group group : groups) {
                                    String houseName = group.houseName != null ? group.houseName : "Nhà";

                                    if (group.rooms != null) {
                                        for (com.example.smarthomeui.smarthome.network.RoomDto room : group.rooms) {
                                            roomItems.add(new RoomSpinnerItem(room.id, room.name, houseName));
                                        }
                                    }
                                }
                            }

                            runOnUiThread(() -> {
                                if (roomItems.size() <= 1) { // Chỉ có placeholder
                                    Toast.makeText(DeviceInventoryActivity.this,
                                            "Không có phòng nào. Vui lòng tạo phòng trước.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                ArrayAdapter<RoomSpinnerItem> adapter = new ArrayAdapter<>(
                                        DeviceInventoryActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        roomItems);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinner.setAdapter(adapter);

                                // Tự động chọn phòng hiện tại của thiết bị
                                int selectedPosition = 0; // Mặc định là placeholder "-- Chọn phòng --"

                                if (deviceRoomId != null) {
                                    // Tìm vị trí của phòng hiện tại
                                    boolean found = false;
                                    for (int i = 0; i < roomItems.size(); i++) {
                                        if (roomItems.get(i).id == deviceRoomId) {
                                            selectedPosition = i;
                                            found = true;
                                            Log.d("EDIT_DEVICE", "Found matching room at position: " + i);
                                            break;
                                        }
                                    }

                                    // Nếu không tìm thấy phòng khớp với roomId, để trống (position 0)
                                    if (!found) {
                                        Log.d("EDIT_DEVICE", "Room ID " + deviceRoomId
                                                + " not found in list. Setting to placeholder.");
                                        selectedPosition = 0;
                                    }
                                }

                                spinner.setSelection(selectedPosition);
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                                    "Không thể tải danh sách phòng", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.smarthomeui.smarthome.network.RoomsByHouseWrap> call,
                            Throwable t) {
                        runOnUiThread(() -> {
                            Log.e("LOAD_ROOMS", "Error loading rooms", t);
                            Toast.makeText(DeviceInventoryActivity.this,
                                    "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Setup spinner mặc định nếu không tải được danh sách phòng
     */
    private void setupDefaultSpinner(android.widget.Spinner spinner, List<RoomSpinnerItem> roomItems, Device device) {
        runOnUiThread(() -> {
            ArrayAdapter<RoomSpinnerItem> adapter = new ArrayAdapter<>(
                    DeviceInventoryActivity.this,
                    android.R.layout.simple_spinner_item,
                    roomItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            if (!roomItems.isEmpty()) {
                spinner.setSelection(0);
            }
        });
    }

    /**
     * Class helper cho Spinner items với thông tin nhà
     */
    private static class RoomSpinnerItem {
        int id;
        String roomName;
        String houseName;

        RoomSpinnerItem(int id, String roomName, String houseName) {
            this.id = id;
            this.roomName = roomName;
            this.houseName = houseName;
        }

        @Override
        public String toString() {
            // Hiển thị: "Tên phòng - Tên nhà"
            return roomName + " - " + houseName;
        }
    }

    /**
     * Gọi API để cập nhật thông tin thiết bị
     */
    private void updateDeviceAPI(Device device, String newName, Integer roomId, int position, AlertDialog dialog) {
        // Lấy device ID từ token (đã lưu API ID vào token)
        String deviceIdStr = device.getToken();
        if (deviceIdStr == null || deviceIdStr.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID thiết bị", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int deviceId = Integer.parseInt(deviceIdStr);

            // Log để debug
            Log.d("UPDATE_DEVICE", "Device ID: " + deviceId);
            Log.d("UPDATE_DEVICE", "New Name: " + newName);
            Log.d("UPDATE_DEVICE", "Room ID: " + roomId);

            Api api = ApiClient.getClient(this).create(Api.class);
            com.example.smarthomeui.smarthome.network.UpdateDeviceReq request = new com.example.smarthomeui.smarthome.network.UpdateDeviceReq(
                    newName, roomId);

            // Log request để kiểm tra
            Log.d("UPDATE_DEVICE", "Request created with roomID: " + roomId);

            Call<DeviceDto> call = api.updateDevice(deviceId, request);
            call.enqueue(new Callback<DeviceDto>() {
                @Override
                public void onResponse(Call<DeviceDto> call, Response<DeviceDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DeviceDto updatedDevice = response.body();
                        Log.d("UPDATE_DEVICE", "Response - Device updated successfully. RoomID from response: "
                                + updatedDevice.getRoomId());

                        runOnUiThread(() -> {
                            // Cập nhật device trong danh sách với roomId từ response
                            device.setName(newName);
                            device.setRoomId(updatedDevice.getRoomId()); // Lấy roomId từ response
                            adapter.notifyItemChanged(position);
                            Toast.makeText(DeviceInventoryActivity.this,
                                    "Đã cập nhật thiết bị", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } else {
                        Log.e("UPDATE_DEVICE", "Response failed. Code: " + response.code());
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string()
                                    : "No error body";
                            Log.e("UPDATE_DEVICE", "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e("UPDATE_DEVICE", "Error reading error body", e);
                        }

                        runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                                "Không thể cập nhật thiết bị. Code: " + response.code(), Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onFailure(Call<DeviceDto> call, Throwable t) {
                    runOnUiThread(() -> {
                        Log.e("API_UPDATE_DEVICE", "Error updating device", t);
                        Toast.makeText(DeviceInventoryActivity.this,
                                "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ID thiết bị không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hiển thị dialog xác nhận xóa thiết bị
     */
    private void showDeleteConfirmDialog(Device device, int position) {
        new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog)
                .setTitle("Xóa thiết bị")
                .setMessage("Bạn có chắc chắn muốn xóa thiết bị \"" + device.getName() + "\"?")
                .setPositiveButton("Xóa", (d, w) -> deleteDeviceAPI(device, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Gọi API để xóa thiết bị
     */
    private void deleteDeviceAPI(Device device, int position) {
        // Lấy device ID từ token
        String deviceIdStr = device.getToken();
        if (deviceIdStr == null || deviceIdStr.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID thiết bị", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int deviceId = Integer.parseInt(deviceIdStr);

            Api api = ApiClient.getClient(this).create(Api.class);
            Call<Void> call = api.deleteDevice(deviceId);

            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            // Xóa device khỏi danh sách
                            inventory.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(DeviceInventoryActivity.this,
                                    "Đã xóa thiết bị", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(DeviceInventoryActivity.this,
                                "Không thể xóa thiết bị", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    runOnUiThread(() -> {
                        Log.e("API_DELETE_DEVICE", "Error deleting device", t);
                        Toast.makeText(DeviceInventoryActivity.this,
                                "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ID thiết bị không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }
}
