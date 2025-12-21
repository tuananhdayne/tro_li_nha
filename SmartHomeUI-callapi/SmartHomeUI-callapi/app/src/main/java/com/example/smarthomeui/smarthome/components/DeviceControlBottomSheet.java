package com.example.smarthomeui.smarthome.components;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.DeviceControlRequest;
import com.example.smarthomeui.smarthome.network.DeviceControlResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceControlBottomSheet extends BottomSheetDialogFragment {

    public interface OnDeviceChanged { void onChanged(Device d); }

    private static final String ARG_DEVICE = "arg_device";

    private Device device;
    private OnDeviceChanged callback;

    // Factory khởi tạo
    public static DeviceControlBottomSheet newInstance(Device d, OnDeviceChanged cb) {
        DeviceControlBottomSheet f = new DeviceControlBottomSheet();
        Bundle b = new Bundle();
        b.putSerializable(ARG_DEVICE, d); // Device implements Serializable
        f.setArguments(b);
        f.setOnDeviceChanged(cb);
        return f;
    }

    public void setOnDeviceChanged(OnDeviceChanged cb){ this.callback = cb; }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
        if (getArguments() != null) {
            Object obj = getArguments().getSerializable(ARG_DEVICE);
            if (obj instanceof Device) device = (Device) obj;
        }
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View content = LayoutInflater.from(getContext()).inflate(R.layout.dialog_device_control, null, false);
        dialog.setContentView(content);

        if (device == null) { dismiss(); return dialog; }

        TextView tvTitle      = content.findViewById(R.id.tvTitle);
        SwitchCompat swPower  = content.findViewById(R.id.swPower);
        View groupLight       = content.findViewById(R.id.groupLight);
        View groupFan         = content.findViewById(R.id.groupFan);
        SeekBar seekBrightness= content.findViewById(R.id.seekBrightness);
        SeekBar seekSpeed     = content.findViewById(R.id.seekSpeed);
        View btnPickColor     = content.findViewById(R.id.btnPickColor);
        View btnClose         = content.findViewById(R.id.btnClose);

        // Tiêu đề
        if (tvTitle != null) tvTitle.setText(device.getName());

        // Power
        if (swPower != null) {
            swPower.setOnCheckedChangeListener(null);
            swPower.setChecked(device.isOn());
            swPower.setOnCheckedChangeListener((b, checked) -> {
                device.setOn(checked);
                syncEnabledState(groupLight, groupFan, seekBrightness, seekSpeed, btnPickColor, checked);

                // Send API command for power state based on device type
                if ("Fan".equalsIgnoreCase(device.getType())) {
                    sendDeviceControlCommand("setFanStatus", checked ? 1 : 0);
                } else if ("RGBLight".equalsIgnoreCase(device.getType())) {
                    if (!checked) {
                        // Turn off RGBLight by setting RGB to 0,0,0
                        sendDeviceControlCommand("setRgbColor", "{\"r\":0,\"g\":0,\"b\":0}");
                    } else {
                        // Turn on RGBLight by setting to current color
                        int color = device.getColor();
                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int blue = color & 0xFF;
                        sendDeviceControlCommand("setRgbColor", "{\"r\":" + r + ",\"g\":" + g + ",\"b\":" + blue + "}");
                    }
                } else {
                    // For Light
                    sendDeviceControlCommand("setLedStatus", checked ? 1 : 0);
                }

                fireChanged();
            });
        }

        // Phân loại theo type
        String deviceType = device.getType();
        boolean isLight = "Light".equalsIgnoreCase(deviceType);
        boolean isLightRGB = "RGBLight".equalsIgnoreCase(deviceType);
        boolean isFan = "Fan".equalsIgnoreCase(deviceType);

        // LIGHT hoặc LIGHT_RGB
        if (isLight || isLightRGB) {
            if (groupLight != null) groupLight.setVisibility(View.VISIBLE);
            if (groupFan != null) groupFan.setVisibility(View.GONE);

            // Brightness control (cho cả Light và LightRGB)
            if (seekBrightness != null) {
                seekBrightness.setMax(100);
                seekBrightness.setOnSeekBarChangeListener(null);
                seekBrightness.setProgress(device.getBrightness());
                seekBrightness.setOnSeekBarChangeListener(new SimpleSeek(p -> {
                    device.setBrightness(p);

                    // Convert brightness from 0-100 to 0-255 for API
                    int dimValue = (int) ((p / 100.0) * 255);
                    sendDeviceControlCommand("setLedDim", dimValue);

                    fireChanged();
                }));
            }

            // Color picker (CHỈ cho LightRGB)
            if (btnPickColor != null) {
                if (isLightRGB) {
                    btnPickColor.setVisibility(View.VISIBLE);
                    // Tô preview theo màu hiện tại
                    try { btnPickColor.getBackground().setTint(device.getColor()); } catch (Exception ignore) {}
                    btnPickColor.setOnClickListener(v ->
                            AdvancedColorPickerDialog.newInstance(device.getColor(), picked -> {
                                device.setColor(picked);
                                try { btnPickColor.getBackground().setTint(picked); } catch (Exception ignore) {}

                                // Send RGB color command for RGBLight
                                int r = (picked >> 16) & 0xFF;
                                int g = (picked >> 8) & 0xFF;
                                int blue = picked & 0xFF;
                                sendDeviceControlCommand("setRgbColor", "{\"r\":" + r + ",\"g\":" + g + ",\"b\":" + blue + "}");

                                fireChanged();
                            }).show(getParentFragmentManagerSafe(), "adv_color_picker")
                    );
                } else {
                    // Light thông thường: ẨN nút chọn màu
                    btnPickColor.setVisibility(View.GONE);
                }
            }
        }
        // FAN
        else if (isFan) {
            if (groupLight != null) groupLight.setVisibility(View.GONE);
            if (groupFan != null) groupFan.setVisibility(View.VISIBLE);

            if (seekSpeed != null) {
                seekSpeed.setMax(3);
                seekSpeed.setOnSeekBarChangeListener(null);
                seekSpeed.setProgress(device.getSpeed());
                seekSpeed.setOnSeekBarChangeListener(new SimpleSeek(p -> {
                    device.setSpeed(p);

                    // Convert speed from 0-3 to percentage (0-100)
                    // Speed 0 = 0%, Speed 1 = 33%, Speed 2 = 66%, Speed 3 = 100%
                    int speedPercentage = (int) ((p / 3.0) * 100);
                    sendDeviceControlCommand("setFanSpeed", "{\"speed\":" + speedPercentage + "}");

                    fireChanged();
                }));
            }
        }
        // Loại khác (ẩn tất cả controls)
        else {
            if (groupLight != null) groupLight.setVisibility(View.GONE);
            if (groupFan != null) groupFan.setVisibility(View.GONE);
        }

        // Khóa / mở controls theo power ban đầu
        syncEnabledState(groupLight, groupFan, seekBrightness, seekSpeed, btnPickColor, device.isOn());

        // Đóng
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        return dialog;
    }

    /**
     * Send device control command to API
     * @param method The command method (e.g., "setLedStatus", "setLedDim")
     * @param params The command parameter value
     */
    private void sendDeviceControlCommand(String method, Object params) {
        if (device == null || device.getId() == null) {
            android.util.Log.e("DeviceControl", "Device or Device ID is null");
            return;
        }

        // Get device ID as String (supports UUID format)
        String deviceId = device.getId();

        // Build JSON command string
        String jsonCommand = "{\"method\":\"" + method + "\",\"params\":" + params + "}";

        // ===== LOG JSON COMMAND =====
        android.util.Log.d("DeviceControl", "==================== DEVICE CONTROL ====================");
        android.util.Log.d("DeviceControl", "Device ID: " + deviceId);
        android.util.Log.d("DeviceControl", "Device Name: " + device.getName());
        android.util.Log.d("DeviceControl", "JSON Command: " + jsonCommand);
        android.util.Log.d("DeviceControl", "=======================================================");

        // Create API request with JSON string
        DeviceControlRequest request = new DeviceControlRequest(jsonCommand);

        Api api = ApiClient.getClient(getContext()).create(Api.class);
        Call<DeviceControlResponse> call = api.controlDevice(deviceId, request);

        call.enqueue(new Callback<DeviceControlResponse>() {
            @Override
            public void onResponse(Call<DeviceControlResponse> call, Response<DeviceControlResponse> response) {
                android.util.Log.d("DeviceControl", "==================== RESPONSE ====================");
                android.util.Log.d("DeviceControl", "Response Code: " + response.code());
                android.util.Log.d("DeviceControl", "Response Message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    DeviceControlResponse controlResponse = response.body();
                    android.util.Log.d("DeviceControl", "Success: " + controlResponse.isSuccess());
                    android.util.Log.d("DeviceControl", "Message: " + controlResponse.getMessage());

                    if (controlResponse.isSuccess()) {
                        android.util.Log.d("DeviceControl", "✓ Command sent successfully");
                    } else {
                        String errorMsg = controlResponse.getMessage() != null
                                ? controlResponse.getMessage()
                                : "Command failed";
                        android.util.Log.e("DeviceControl", "✗ Command failed: " + errorMsg);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null
                                ? response.errorBody().string()
                                : "No error body";
                        android.util.Log.e("DeviceControl", "✗ Error Body: " + errorBody);
                    } catch (Exception e) {
                        android.util.Log.e("DeviceControl", "Error reading error body", e);
                    }
                }
                android.util.Log.d("DeviceControl", "==================================================");
            }

            @Override
            public void onFailure(Call<DeviceControlResponse> call, Throwable t) {
                android.util.Log.e("DeviceControl", "==================== FAILURE ====================");
                android.util.Log.e("DeviceControl", "✗ Network error: " + t.getMessage(), t);
                android.util.Log.e("DeviceControl", "=================================================");
            }
        });
    }

    private void syncEnabledState(@Nullable View groupLight, @Nullable View groupFan,
                                  @Nullable SeekBar seekBrightness, @Nullable SeekBar seekSpeed,
                                  @Nullable View btnPickColor, boolean enabled) {
        // Chỉ disable tương tác; vẫn hiển thị để người dùng hiểu ngữ cảnh
        if (groupLight != null) groupLight.setAlpha(enabled ? 1f : 0.5f);
        if (groupFan   != null) groupFan.setAlpha(enabled ? 1f : 0.5f);
        if (seekBrightness != null) seekBrightness.setEnabled(enabled);
        if (seekSpeed != null) seekSpeed.setEnabled(enabled);
        if (btnPickColor != null) btnPickColor.setEnabled(enabled);
    }

    private void fireChanged() {
        if (callback != null) callback.onChanged(device);
    }

    // Helper rút gọn cho SeekBar
    private static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        interface OnChange { void on(int progress); }
        private final OnChange cb;
        SimpleSeek(OnChange cb){ this.cb = cb; }
        @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser){ if (fromUser) cb.on(p); }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    private FragmentManager getParentFragmentManagerSafe() {
        // Với BottomSheetDialogFragment, getParentFragmentManager() là đúng;
        // phòng trường hợp gọi từ Fragment cha, vẫn an toàn.
        return getParentFragmentManager();
    }
}
