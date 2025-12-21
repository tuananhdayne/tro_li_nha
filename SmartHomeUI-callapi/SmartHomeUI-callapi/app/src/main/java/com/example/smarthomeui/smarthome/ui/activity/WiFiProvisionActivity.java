package com.example.smarthomeui.smarthome.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.listeners.ResponseListener;
import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.provision.ProvisionSession;
import com.espressif.provisioning.ESPDevice;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.WiFiAccessPoint;
import com.espressif.provisioning.listeners.ProvisionListener;
import com.espressif.provisioning.listeners.WiFiScanListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WiFiProvisionActivity extends AppCompatActivity {

    private static final String TAG = "WiFiProvision";
    private static final long INIT_DELAY = 2; // 2 giây để BLE ổn định
    private static final long CONFIG_SEND_DELAY = 1; // 1.5 giây trước khi gửi config

    private ArrayAdapter<String> adapter;
    private final List<WiFiAccessPoint> aps = new ArrayList<>();
    private String roomId;
    private String userId;
    private Handler handler;
    private Button btnScan;
    private Button btnManual;
    private boolean isInitialized = false;
    private ESPProvisionManager provisionManager;

    public static void start(AppCompatActivity activity) {
        activity.startActivity(new Intent(activity, WiFiProvisionActivity.class));

    }

    public static void start(AppCompatActivity activity, String roomId, String userId) {
        Intent intent = new Intent(activity, WiFiProvisionActivity.class);
        intent.putExtra("room_id", roomId);
        intent.putExtra("user_id", userId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        provisionManager = ESPProvisionManager.getInstance(this);
        setContentView(R.layout.activity_wifi_provision);

        handler = new Handler(Looper.getMainLooper());

        // Lấy roomId và userId từ Intent
        roomId = getIntent().getStringExtra("room_id");
        userId = getIntent().getStringExtra("user_id");

        // Nếu không có trong Intent, lấy từ UserManager
        if (userId == null || userId.isEmpty()) {
            com.example.smarthomeui.smarthome.utils.UserManager userManager = new com.example.smarthomeui.smarthome.utils.UserManager(
                    this);
            userId = userManager.getUserId();
        }

        // Setup UI
        TextView tvBack = findViewById(R.id.tvBack);
        TextView tvCancel = findViewById(R.id.tvCancel);
        tvBack.setOnClickListener(v -> finish());
        tvCancel.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listWifi);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        btnScan = findViewById(R.id.btnScan);
        btnManual = findViewById(R.id.btnManual);

        // Disable buttons ban đầu cho đến khi khởi tạo xong
        btnScan.setEnabled(false);
        btnManual.setEnabled(false);

        btnScan.setOnClickListener(v -> scan());
        btnManual.setOnClickListener(v -> showManualDialog(null));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < aps.size()) {
                WiFiAccessPoint ap = aps.get(position);
                String ssid;
                try {
                    ssid = ap.getWifiName();
                } catch (Exception e) {
                    ssid = String.valueOf(ap);
                }
                showManualDialog(ssid);
            }
        });

        // Kiểm tra ESP device
        ESPDevice device = ProvisionSession.get().getEspDevice();
        if (device == null) {
            Toast.makeText(this, "Không có thiết bị ESP trong session. Vui lòng quay lại và kết nối Bluetooth trước.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Hiển thị trạng thái đang khởi tạo
        adapter.add("Đang khởi tạo kết nối với ESP device...");
        adapter.add("Vui lòng đợi...");

        Log.d(TAG, "Waiting " + INIT_DELAY + "ms for BLE connection to stabilize...");

        // Delay để đảm bảo BLE connection ổn định
        handler.postDelayed(this::initializeDevice, INIT_DELAY);
    }

    private void initializeDevice() {
        ESPDevice device = ProvisionSession.get().getEspDevice();
        if (device == null) {
            adapter.clear();
            adapter.add("Lỗi: Mất kết nối với ESP device");
            Toast.makeText(this, "Mất kết nối với ESP device", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Log.d(TAG, "Device initialized. Transport type: " + device.getTransportType());
            isInitialized = true;

            // Enable buttons
            btnScan.setEnabled(true);
            btnManual.setEnabled(true);

            // Clear và hiển thị hướng dẫn
            adapter.clear();
            adapter.add("Kết nối thành công!");
            adapter.add("Nhấn 'Quét WiFi' để tìm mạng hoặc 'Nhập thủ công'");

            Toast.makeText(this, "Sẵn sàng quét WiFi", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing device", e);
            adapter.clear();
            adapter.add("Lỗi khởi tạo: " + e.getMessage());
            Toast.makeText(this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void scan() {
        if (!isInitialized) {
            Toast.makeText(this, "Vui lòng đợi khởi tạo hoàn tất", Toast.LENGTH_SHORT).show();
            return;
        }

        ESPDevice dev = ProvisionSession.get().getEspDevice();
        if (dev == null) {
            Toast.makeText(this, "Không có thiết bị ESP trong session", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable buttons khi đang quét
        btnScan.setEnabled(false);
        btnManual.setEnabled(false);

        adapter.clear();
        adapter.add("Đang quét mạng WiFi...");
        aps.clear();

        try {
            Log.d(TAG, "Starting WiFi scan...");

            dev.scanNetworks(new WiFiScanListener() {
                @Override
                public void onWifiListReceived(ArrayList<WiFiAccessPoint> wifiList) {
                    runOnUiThread(() -> {
                        updateList(wifiList);
                        btnScan.setEnabled(true);
                        btnManual.setEnabled(true);
                        Log.d(TAG,
                                "WiFi scan completed. Found " + (wifiList != null ? wifiList.size() : 0) + " networks");
                    });
                }

                @Override
                public void onWiFiScanFailed(Exception e) {
                    runOnUiThread(() -> {
                        adapter.clear();
                        adapter.add("Quét WiFi thất bại: " + e.getMessage());
                        adapter.add("Vui lòng thử lại hoặc nhập thủ công");
                        Toast.makeText(WiFiProvisionActivity.this, "Quét WiFi thất bại: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "WiFi scan failed", e);

                        btnScan.setEnabled(true);
                        btnManual.setEnabled(true);
                    });
                }
            });
        } catch (Exception e) {
            adapter.clear();
            adapter.add("Lỗi khi quét WiFi: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi quét WiFi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception during scan", e);
            btnScan.setEnabled(true);
            btnManual.setEnabled(true);
        }
    }

    private void updateList(ArrayList<WiFiAccessPoint> wifiList) {
        adapter.clear();
        aps.clear();
        if (wifiList != null)
            aps.addAll(wifiList);
        if (aps.isEmpty()) {
            adapter.add("Không tìm thấy mạng WiFi");
        } else {
            for (WiFiAccessPoint ap : aps) {
                try {
                    adapter.add(ap.getWifiName());
                } catch (Exception e) {
                    adapter.add(String.valueOf(ap));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showManualDialog(@Nullable String prefillSsid) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_provision_wifi, null, false);
        EditText edtSsid = view.findViewById(R.id.edtSsid);
        EditText edtPass = view.findViewById(R.id.edtPass);
        if (prefillSsid != null)
            edtSsid.setText(prefillSsid);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog)
                .setTitle(R.string.title_connect_wifi)
                .setView(view)
                .setPositiveButton(R.string.connect, null)
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String ssid = String.valueOf(edtSsid.getText()).trim();
                String pass = String.valueOf(edtPass.getText());

                if (ssid.isEmpty()) {
                    edtSsid.setError("Nhập SSID");
                    return;
                }

                // Lấy device name từ ProvisionSession
                String deviceName = ProvisionSession.get().getDeviceName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = "ESP Device"; // Fallback nếu không có
                }

                doProvision(ssid, pass, deviceName, dialog);
            });
        });
        dialog.show();
    }

    private void showDeviceInfoDialog(String ssid, String pass, AlertDialog wifiDialog) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_enter_device_name, null, false);
        EditText edtDeviceName = view.findViewById(R.id.edtDeviceName);
        android.widget.Spinner spinnerDeviceType = view.findViewById(R.id.spinnerDeviceType);

        // Map giữa tên hiển thị (Tiếng Việt) và giá trị (Tiếng Anh)
        String[] displayNames = { "Đèn", "Khóa cửa", "Cảm biến nhiệt độ", "Cảm biến chuyển động" };
        String[] deviceTypeValues = { "Light", "DoorLock", "TemperatureHumiditySensor", "MotionSensor" };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, displayNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDeviceType.setAdapter(spinnerAdapter);

        AlertDialog deviceDialog = new AlertDialog.Builder(this, R.style.ThemeOverlay_Material3_Dialog)
                .setTitle("Thông tin thiết bị")
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .create();

        deviceDialog.setOnShowListener(d -> {
            deviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String deviceName = edtDeviceName.getText().toString().trim();

                if (deviceName.isEmpty()) {
                    edtDeviceName.setError("Vui lòng nhập tên thiết bị");
                    return;
                }

                // Lấy giá trị tiếng Anh từ vị trí được chọn
                int selectedPosition = spinnerDeviceType.getSelectedItemPosition();
                String deviceType = deviceTypeValues[selectedPosition];

                // Lưu vào session để dùng trong sendConfigAndFinish
                ProvisionSession.get().setDeviceName(deviceName);
                ProvisionSession.get().setDeviceType(deviceType);

                deviceDialog.dismiss();
                doProvision(ssid, pass, deviceName, wifiDialog);
            });
        });
        deviceDialog.show();
    }

    private void doProvision(String ssid, String pass, String deviceName, @Nullable AlertDialog dismissOnSuccess) {
        ESPDevice dev = ProvisionSession.get().getEspDevice();
        if (dev == null) {
            Toast.makeText(this, R.string.no_device_in_session, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            dev.provision(ssid, pass, new ProvisionListener() {
                @Override
                public void createSessionFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(WiFiProvisionActivity.this,
                            "Tạo session thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void wifiConfigSent() {
                }

                @Override
                public void wifiConfigFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(WiFiProvisionActivity.this,
                            "Gửi cấu hình WiFi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void wifiConfigApplied() {
                }

                @Override
                public void wifiConfigApplyFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(WiFiProvisionActivity.this,
                            "Áp dụng cấu hình WiFi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void provisioningFailedFromDevice(ESPConstants.ProvisionFailureReason failureReason) {
                    runOnUiThread(() -> Toast.makeText(WiFiProvisionActivity.this,
                            "Provisioning thất bại: " + failureReason, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onProvisioningFailed(Exception e) {
                    runOnUiThread(() -> Toast.makeText(WiFiProvisionActivity.this,
                            "Provisioning thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void deviceProvisioningSuccess() {
                    sendConfigAndFinish(deviceName, dismissOnSuccess);
                    runOnUiThread(() -> {
                        Toast.makeText(WiFiProvisionActivity.this, "Provisioning thành công! Đang gửi cấu hình...",
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Provisioning successful. Waiting " + CONFIG_SEND_DELAY
                                + "ms before sending config...");

                        // Delay trước khi gửi config để đảm bảo device sẵn sàng
                        // handler.postDelayed(() -> sendConfigAndFinish(deviceName, dismissOnSuccess),
                        // CONFIG_SEND_DELAY);
                    });
                }
            });
        } catch (SecurityException se) {
            Toast.makeText(this, "Lỗi quyền: " + se.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "SecurityException during provision", se);
        }
    }

    private void sendConfigAndFinish(String deviceName, @Nullable AlertDialog dismissOnSuccess) {
        try {
            String deviceType = ProvisionSession.get().getDeviceType();
            if (deviceType == null || deviceType.isEmpty()) {
                deviceType = "Light"; // Mặc định
            }
            // Tạo JSON chỉ với 3 trường: device_name, room_id, user_id
            org.json.JSONObject jsonObject = new org.json.JSONObject();
            jsonObject.put("name", deviceName);
            jsonObject.put("roomId", roomId != null ? roomId : "");
            jsonObject.put("userId", userId != null ? userId : "");
            jsonObject.put("type", deviceType);

            String json = jsonObject.toString();
            Log.d(TAG, "Sending config to device: " + json);

            // Gửi JSON đến ESP device qua custom endpoint
            ESPDevice device = provisionManager.getEspDevice();
            if (device != null) {
                device.sendDataToCustomEndPoint("test", json.getBytes(), new ResponseListener() {
                    @Override
                    public void onSuccess(byte[] returnData) {
                        String response = new String(returnData);
                        Log.d(TAG, "Config sent successfully. Device response: " + response);
                        runOnUiThread(() -> {
                            Toast.makeText(WiFiProvisionActivity.this, "Cấu hình thiết bị thành công!",
                                    Toast.LENGTH_SHORT).show();
                            finishProvisioning(deviceName, dismissOnSuccess);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to send config: " + e.getMessage(), e);
                        runOnUiThread(() -> {
                            Toast.makeText(WiFiProvisionActivity.this,
                                    "Cảnh báo: Không gửi được cấu hình, nhưng WiFi đã được thiết lập",
                                    Toast.LENGTH_LONG).show();
                            finishProvisioning(deviceName, dismissOnSuccess);
                        });
                    }
                });
            } else {
                Log.w(TAG, "Device is null, skipping config send");
                finishProvisioning(deviceName, dismissOnSuccess);
            }

        } catch (org.json.JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage(), e);
            runOnUiThread(() -> {
                Toast.makeText(WiFiProvisionActivity.this, "Lỗi tạo JSON: " + e.getMessage(), Toast.LENGTH_SHORT)
                        .show();
                finishProvisioning(deviceName, dismissOnSuccess);
            });
        }
    }

    private void finishProvisioning(String deviceName, @Nullable AlertDialog dismissOnSuccess) {
        // Tạo device mới với thông tin
        Device newDevice = new Device(UUID.randomUUID().toString(), deviceName, "ESP Device", false);
        newDevice.addCaps(Device.CAP_POWER, Device.CAP_BRIGHTNESS, Device.CAP_COLOR);
        newDevice.setBrightness(100);
        newDevice.setColor(0xFFFFFFFF);

        if (dismissOnSuccess != null)
            dismissOnSuccess.dismiss();

        // Clear session để tránh conflict lần provision tiếp theo
        ProvisionSession.get().clear();
        Log.d(TAG, "Session cleared. Finishing activity...");

        // Quay về DeviceInventoryActivity
        Intent intent = new Intent(WiFiProvisionActivity.this, DeviceInventoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
