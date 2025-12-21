package com.example.smarthomeui.smarthome.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.DeviceControlRequest;
import com.example.smarthomeui.smarthome.network.DeviceControlResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper utility for controlling devices via API
 * Usage example:
 *
 * For Light devices:
 * - Turn on/off: DeviceControlHelper.setLedStatus(context, deviceId, 1); // 1 = on, 0 = off
 * - Set brightness: DeviceControlHelper.setLedDim(context, deviceId, 255); // 0-255
 */
public class DeviceControlHelper {

    /**
     * Control LED status (on/off)
     * @param context Android context
     * @param deviceId Device ID (UUID string)
     * @param status 1 for ON, 0 for OFF
     */
    public static void setLedStatus(Context context, String deviceId, int status) {
        String jsonCommand = "{\"method\":\"setLedStatus\",\"params\":" + status + "}";
        sendControlCommand(context, deviceId, jsonCommand);
    }

    /**
     * Control LED brightness
     * @param context Android context
     * @param deviceId Device ID (UUID string)
     * @param brightness Brightness value (0-255)
     */
    public static void setLedDim(Context context, String deviceId, int brightness) {
        String jsonCommand = "{\"method\":\"setLedDim\",\"params\":" + brightness + "}";
        sendControlCommand(context, deviceId, jsonCommand);
    }

    /**
     * Send control command to device
     * @param context Android context
     * @param deviceId Device ID (UUID string)
     * @param jsonCommand Full JSON command string
     */
    private static void sendControlCommand(Context context, String deviceId, String jsonCommand) {
        DeviceControlRequest request = new DeviceControlRequest(jsonCommand);

        android.util.Log.d("DeviceControlHelper", "Sending to device " + deviceId + ": " + jsonCommand);

        Api api = ApiClient.getClient(context).create(Api.class);
        Call<DeviceControlResponse> call = api.controlDevice(deviceId, request);

        call.enqueue(new Callback<DeviceControlResponse>() {
            @Override
            public void onResponse(Call<DeviceControlResponse> call, Response<DeviceControlResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeviceControlResponse controlResponse = response.body();
                    if (controlResponse.isSuccess()) {
                        android.util.Log.d("DeviceControlHelper", "Command sent successfully");
                    } else {
                        String errorMsg = controlResponse.getMessage() != null
                                ? controlResponse.getMessage()
                                : "Command failed";
                        android.util.Log.e("DeviceControlHelper", errorMsg);
                    }
                } else {
                    android.util.Log.e("DeviceControlHelper", "Failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DeviceControlResponse> call, Throwable t) {
                android.util.Log.e("DeviceControlHelper", "Network error: " + t.getMessage(), t);
            }
        });
    }

}
