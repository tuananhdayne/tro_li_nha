package com.example.smarthomeui.smarthome.ui.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.provision.ProvisionSession;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPDevice;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.listeners.BleScanListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BLEScanActivity extends AppCompatActivity {

    private static final String TAG = "BLEScanActivity";
    private static final String PRIMARY_SERVICE_UUID = "021a9004-0382-4aea-bff4-6b3f1c5adfb4";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private final List<BluetoothDevice> devices = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView listView;
    private Button btnConnect;
    private BluetoothDevice selectedDevice;
    private BluetoothAdapter bluetoothAdapter;
    private String roomId;
    private String userId;

    // Safe toast method to prevent DeadObjectException
    private void showSafeToast(String message, int duration) {
        if (!isFinishing() && !isDestroyed()) {
            try {
                Toast.makeText(this, message, duration).show();
            } catch (Exception e) {
                Log.w(TAG, "Failed to show toast: " + e.getMessage());
            }
        }
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);

        WiFiProvisionActivity.start(this,
                getIntent().getStringExtra("room_id"),
                getIntent().getStringExtra("user_id"));

        // Khởi tạo Bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        TextView tvBack = findViewById(R.id.tvBack);
        TextView tvCancel = findViewById(R.id.tvCancel);
        tvBack.setOnClickListener(v -> finish());
        tvCancel.setOnClickListener(v -> finish());

        listView = findViewById(R.id.listDevices);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        btnConnect = findViewById(R.id.btnPrimary);
        btnConnect.setText("Quét thiết bị");
        btnConnect.setOnClickListener(v -> {
            if (selectedDevice == null) {
                checkPermissionsAndStartScan();
            } else {
                connect(selectedDevice);
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < devices.size()) {
                selectedDevice = devices.get(position);
                btnConnect.setText("Kết nối");
                btnConnect.setEnabled(true);

                // Highlight selected item
                for (int i = 0; i < listView.getChildCount(); i++) {
                    listView.getChildAt(i).setBackgroundColor(android.graphics.Color.TRANSPARENT);
                }
                view.setBackgroundColor(android.graphics.Color.LTGRAY);
            }
        });

        // Kiểm tra và yêu cầu quyền
        checkPermissionsAndStartScan();
    }

    private void checkPermissionsAndStartScan() {
        // Kiểm tra Bluetooth có được bật không
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        // Kiểm tra quyền runtime
        List<String> missingPermissions = new ArrayList<>();

        // Luôn cần quyền vị trí để quét BLE trên tất cả phiên bản Android
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                missingPermissions.toArray(new String[0]),
                REQUEST_PERMISSIONS);
            return;
        }

        startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startScan();
            } else {
                showSafeToast("Cần cấp quyền Bluetooth để quét thiết bị", Toast.LENGTH_LONG);
                btnConnect.setText("Quét thiết bị");
                btnConnect.setEnabled(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                checkPermissionsAndStartScan();
            } else {
                showSafeToast("Cần bật Bluetooth để quét thiết bị", Toast.LENGTH_LONG);
            }
        }
    }

    // Java
    private void startScan() {
        Log.d(TAG, "Bắt đầu quét thiết bị BLE");
        devices.clear();
        adapter.clear();
        selectedDevice = null;
        btnConnect.setText("Đang quét...");
        btnConnect.setEnabled(false);

        adapter.add("Đang tìm thiết bị Bluetooth...");

        ESPProvisionManager pm = ESPProvisionManager.getInstance(this);

        // Kiểm tra quyền vị trí cho tất cả phiên bản Android
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showSafeToast("Thiếu quyền vị trí để quét BLE", Toast.LENGTH_LONG);
            btnConnect.setText("Quét thiết bị");
            btnConnect.setEnabled(true);
            return;
        }

        // Kiểm tra quyền Bluetooth cho Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                showSafeToast("Thiếu quyền BLUETOOTH_SCAN", Toast.LENGTH_LONG);
                btnConnect.setText("Quét thiết bị");
                btnConnect.setEnabled(true);
                return;
            }
        }

        try {
            pm.searchBleEspDevices("PROV_", new BleScanListener() {
                @Override
                public void scanStartFailed() {
                    Log.e(TAG, "Quét thiết bị thất bại khi bắt đầu");
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            showSafeToast("Không thể bắt đầu quét thiết bị", Toast.LENGTH_SHORT);
                            btnConnect.setText("Quét thiết bị");
                            btnConnect.setEnabled(true);
                            adapter.clear();
                            adapter.add("Lỗi khi quét thiết bị");
                            adapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onPeripheralFound(BluetoothDevice device, ScanResult scanResult) {
                    Log.d(TAG, "Tìm thấy thiết bị: " + getDeviceAddress(device) + " - " + getSafeName(device));
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            addDevice(device);
                        }
                    });
                }

                @Override
                public void scanCompleted() {
                    Log.d(TAG, "Quét thiết bị hoàn tất. Tìm thấy " + devices.size() + " thiết bị");
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            showSafeToast("Quét thiết bị hoàn tất", Toast.LENGTH_SHORT);
                            btnConnect.setText("Quét lại");
                            btnConnect.setEnabled(true);

                            if (devices.isEmpty()) {
                                adapter.clear();
                                adapter.add("Không tìm thấy thiết bị ESP nào");
                                adapter.add("Hãy đảm bảo thiết bị ESP đang ở chế độ provisioning");
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Quét thiết bị thất bại: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            showSafeToast("Quét thiết bị thất bại: " + e.getMessage(), Toast.LENGTH_SHORT);
                            btnConnect.setText("Quét thiết bị");
                            btnConnect.setEnabled(true);
                            adapter.clear();
                            adapter.add("Lỗi: " + e.getMessage());
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException khi quét: " + se.getMessage(), se);
            showSafeToast("Thiếu quyền để quét BLE: " + se.getMessage(), Toast.LENGTH_LONG);
            btnConnect.setText("Quét thiết bị");
            btnConnect.setEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "Exception khi quét: " + e.getMessage(), e);
            showSafeToast("Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT);
            btnConnect.setText("Quét thiết bị");
            btnConnect.setEnabled(true);
        }
    }

    private void addDevice(@Nullable BluetoothDevice device) {
        if (device == null) return;

        // Loại bỏ thiết bị trùng lặp theo địa chỉ MAC
        for (BluetoothDevice d : devices) {
            if (Objects.equals(getDeviceAddress(d), getDeviceAddress(device))) return;
        }

        devices.add(device);

        // Tạo chuỗi hiển thị an toàn
        String name = getSafeName(device);
        String address = getDeviceAddress(device);
        String display = name + " (" + address + ")";

        // Xóa thông báo "Đang tìm thiết bị..." nếu có
        if (adapter.getCount() > 0 && adapter.getItem(0).contains("Đang tìm thiết bị")) {
            adapter.clear();
        }

        adapter.add(display);
        adapter.notifyDataSetChanged();
    }

    private String getSafeName(BluetoothDevice device) {
        if (device == null) return "Không rõ";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return "Không rõ";
                }
            }
            String name = device.getName();
            return (name == null || name.isEmpty()) ? "Không rõ" : name;
        } catch (SecurityException se) {
            Log.w(TAG, "SecurityException khi lấy tên thiết bị: " + se.getMessage());
            return "Không rõ";
        }
    }

    private String getDeviceAddress(BluetoothDevice device) {
        if (device == null) return "00:00:00:00:00:00";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return "00:00:00:00:00:00";
                }
            }
            return device.getAddress();
        } catch (SecurityException se) {
            Log.w(TAG, "SecurityException khi lấy địa chỉ thiết bị: " + se.getMessage());
            return "00:00:00:00:00:00";
        }
    }

    private void connect(BluetoothDevice device) {
        btnConnect.setText("Đang kết nối...");
        btnConnect.setEnabled(false);

        try {
            // Kiểm tra quyền trước khi kết nối
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    showSafeToast("Thiếu quyền BLUETOOTH_CONNECT", Toast.LENGTH_LONG);
                    btnConnect.setText("Kết nối");
                    btnConnect.setEnabled(true);
                    return;
                }
            }

            ESPProvisionManager pm = ESPProvisionManager.getInstance(this);
            ESPDevice esp = pm.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_2);

            String pop = ProvisionSession.get().getPop();
            try {
                esp.setProofOfPossession(pop);
            } catch (Exception ignored) {}

            // Kết nối BLE và đợi callback
            esp.connectBLEDevice(device, PRIMARY_SERVICE_UUID);

            // Đợi một chút để kết nối ổn định trước khi chuyển activity
            new android.os.Handler().postDelayed(() -> {
                ProvisionSession.get().setEspDevice(esp);
                showSafeToast("Đã kết nối thành công với " + getSafeName(device), Toast.LENGTH_SHORT);
                WiFiProvisionActivity.start(this, roomId, userId);
            }, 2000); // Đợi 2 giây để kết nối ổn định

        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException khi kết nối: " + se.getMessage(), se);
            showSafeToast("Lỗi quyền khi kết nối: " + se.getMessage(), Toast.LENGTH_LONG);
            btnConnect.setText("Kết nối");
            btnConnect.setEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "Exception khi kết nối: " + e.getMessage(), e);
            showSafeToast("Kết nối thất bại: " + e.getMessage(), Toast.LENGTH_LONG);
            btnConnect.setText("Kết nối");
            btnConnect.setEnabled(true);
        }
    }
}
