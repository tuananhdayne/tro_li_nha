package com.example.smarthomeui.smarthome.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.FcmTokenRequest;
import com.example.smarthomeui.smarthome.network.ApiFCMClient;
import com.example.smarthomeui.smarthome.network.ApiServiceFCM;
import com.example.smarthomeui.smarthome.ui.activity.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM_Service";
    private static final String CHANNEL_ID = "smarthome_notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);

        // Gửi token lên backend để lưu
        // sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Log.d(TAG, "Message from: " + message.getFrom());

        // Kiểm tra có notification payload không
        if (message.getNotification() != null) {
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();
            Log.d(TAG, "Notification - Title: " + title + ", Body: " + body);
            showNotification(title, body);
        }

        // Kiểm tra có data payload không
        if (!message.getData().isEmpty()) {
            Log.d(TAG, "Message data: " + message.getData());
            handleDataPayload(message.getData());
        }
    }

    private void showNotification(String title, String body) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Sử dụng icon mặc định, có thể thay đổi sau
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(0, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SmartHome Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo từ SmartHome app");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void handleDataPayload(java.util.Map<String, String> data) {
        // Xử lý custom data (ví dụ: device alerts, room updates...)
        String deviceId = data.get("device_id");
        String alertType = data.get("alert_type");
        String roomId = data.get("room_id");

        Log.d(TAG, "Device: " + deviceId + ", Alert: " + alertType + ", Room: " + roomId);

        // TODO: Có thể gửi broadcast hoặc update UI nếu app đang mở
    }

    private void sendTokenToServer(String token) {
        Log.d(TAG, "Đang gửi token đến SERVER : " + token);

        // 1. Lấy ApiService từ ApiFCMClient (client MỚI trỏ đến server Python)
        ApiServiceFCM apiService = ApiFCMClient.getClient().create(ApiServiceFCM.class);

        // 2. Tạo đối tượng request body
        FcmTokenRequest requestBody = new FcmTokenRequest(token);

        // 3. Thực hiện cuộc gọi API (đến server Python)
        apiService.registerToken(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Gửi token lên SERVER  thành công!");
                } else {
                    Log.e(TAG, "Gửi token đến SERVER  thất bại, mã lỗi: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Gửi token đến SERVER thất bại, lỗi kết nối: " + t.getMessage());
            }
        });
    }
}

