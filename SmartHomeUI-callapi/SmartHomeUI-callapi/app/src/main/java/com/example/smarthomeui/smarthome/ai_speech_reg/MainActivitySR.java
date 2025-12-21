package com.example.smarthomeui.smarthome.ai_speech_reg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.ai_speech_reg.DeviceModels.ParseResult;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.DeviceControlRequest;
import com.example.smarthomeui.smarthome.network.DeviceControlResponse;
import com.example.smarthomeui.smarthome.ui.activity.DeviceInventoryActivity;
import com.example.smarthomeui.smarthome.ui.activity.HouseListActivity;
import com.example.smarthomeui.smarthome.ui.activity.HouseRoomsActivity;
import com.example.smarthomeui.smarthome.ui.activity.SettingsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import com.example.smarthomeui.smarthome.network.GeminiRequest;
import com.example.smarthomeui.smarthome.network.GeminiResponse;
import com.google.gson.Gson;

public class MainActivitySR extends AppCompatActivity {

    private TextView tvHeard, tvResult;
    private FloatingActionButton fabMic;

    private SpeechRecognizer recognizer;
    private SpeechHelper speechHelper;
    private TTSHelper ttsHelper;

    private DeviceRegistry registry;
    private VietnameseCommandParser parser;

    private  ImageView ivHome, ivRooms, ivControl, ivDevices, ivSetting;
    private final ActivityResultLauncher<String> micPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startListening();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_speech_recognition);

        init();
        action();
        navbarAciton();
    }

    private void init(){
        tvHeard = findViewById(R.id.tvHeard);
        tvResult = findViewById(R.id.tvResult);
        fabMic  = findViewById(R.id.fabMic);

        registry = new DeviceRegistry();
        parser   = new VietnameseCommandParser(registry);
        speechHelper = new SpeechHelper(this);
        ttsHelper = new TTSHelper(this);

        ivHome    = findViewById(R.id.ivHome);
        ivRooms   = findViewById(R.id.ivRooms);
        ivControl = findViewById(R.id.ivControl);
        ivDevices = findViewById(R.id.ivDevices);
        ivSetting = findViewById(R.id.ivSetting);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void action(){
        // Push-to-talk: nhấn giữ để nói, thả ra để gửi
        fabMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    requestMicAndStart();
                    fabMic.setAlpha(0.7f);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopListening();
                    fabMic.setAlpha(1.0f);
                    return true;
            }
            return false;
        });

        registry.refreshFromApi(this, 0, 200, new DeviceRegistry.LoadCallback() {
            @Override public void onLoaded(int count) {
                // (tuỳ chọn) thông báo nhẹ
                runOnUiThread(() -> {
                    if (tvResult != null && count >= 0) {
                        tvResult.setText("Đã tải " + count + " thiết bị từ máy chủ.");
                    }
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    if (tvResult != null) tvResult.setText("Không tải được thiết bị: " + message);
                });
            }
        });
    }

    private void navbarAciton(){
        ivHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivitySR.this, HouseListActivity.class);
                startActivity(intent);
            }
        });
        ivRooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivitySR.this, HouseRoomsActivity.class);
                startActivity(intent);
            }
        });
        ivDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivitySR.this, DeviceInventoryActivity.class);
                startActivity(intent);
            }
        });
        ivSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivitySR.this, SettingsActivity.class);
            }
        });
    }


    private void requestMicAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startListening();
        }
    }

    private void startListening() {
        stopListening(); // dọn nếu có
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { tvHeard.setText("Đang lắng nghe…"); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) { tvHeard.setText("Mic đang bận"); }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) {
                    tvHeard.setText("Bạn nói (tạm): " + list.get(0));
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
            @Override public void onResults(Bundle results) {
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (list != null && !list.isEmpty()) ? list.get(0) : "";
                tvHeard.setText("Bạn nói: " + text);
                emitParse(text);
            }
        });
        Intent intent = speechHelper.buildIntentVI();
        recognizer.startListening(intent);
    }

    private void stopListening() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
    }

    private void emitParse(String text) {
        if (text == null || text.isEmpty()) {
            ttsHelper.speak("Xin lỗi, tôi không nghe rõ.");
            return;
        }
        
        tvResult.setText("Đang xử lý...");
        
        // Gọi Gemini chatbot API trực tiếp
        callGeminiAssistant(text);
    }
    
    private void callGeminiAssistant(String text) {
        GeminiRequest request = new GeminiRequest(text);
        
        Api api = ApiClient.getGeminiClient().create(Api.class);
        api.askGemini(request).enqueue(new retrofit2.Callback<GeminiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<GeminiResponse> call, 
                                   retrofit2.Response<GeminiResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        GeminiResponse.GeminiAssistant assistant = response.body().getAssistant();
                        if (assistant != null) {
                            String message = assistant.getMessage();
                            tvResult.setText(message);
                            ttsHelper.speak(message);
                            
                            // Nếu có action điều khiển thiết bị
                            if (assistant.hasAction()) {
                                executeDeviceCommand(assistant);
                            }
                        }
                    } else {
                        String error = "Không thể kết nối trợ lý AI";
                        tvResult.setText(error);
                        ttsHelper.speak(error);
                    }
                });
            }
            
            @Override
            public void onFailure(retrofit2.Call<GeminiResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    String error = "Lỗi kết nối: " + t.getMessage();
                    tvResult.setText(error);
                    ttsHelper.speak("Không thể kết nối máy chủ");
                });
            }
        });
    }
    
    private void executeDeviceCommand(GeminiResponse.GeminiAssistant assistant) {
        String deviceType = assistant.getDeviceType();
        GeminiResponse.CommandData cmdData = assistant.getCommand();
        
        if (deviceType == null || cmdData == null) return;
        
        // Tìm thiết bị theo loại
        List<Device> devices = registry.getAll();
        Device targetDevice = null;
        for (Device d : devices) {
            if (d.getType() != null && d.getType().equals(deviceType)) {
                targetDevice = d;
                break;
            }
        }
        
        if (targetDevice == null) {
            ttsHelper.speak("Không tìm thấy thiết bị " + deviceType);
            return;
        }
        
        // Gửi lệnh điều khiển
        String command = new Gson().toJson(cmdData);
        DeviceControlRequest req = new DeviceControlRequest(command);
        
        Api api = ApiClient.getClient(this).create(Api.class);
        Device finalDevice = targetDevice;
        api.controlDevice(targetDevice.getId(), req).enqueue(new retrofit2.Callback<DeviceControlResponse>() {
            @Override
            public void onResponse(retrofit2.Call<DeviceControlResponse> call, 
                                   retrofit2.Response<DeviceControlResponse> resp) {
                runOnUiThread(() -> {
                    if (resp.isSuccessful()) {
                        tvResult.setText("Đã điều khiển " + finalDevice.getName());
                    }
                });
            }
            
            @Override
            public void onFailure(retrofit2.Call<DeviceControlResponse> call, Throwable t) {
                // Silent fail - đã thông báo qua chatbot rồi
            }
        });
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int percentToRaw(int percent) { return clamp((int)Math.round(percent * 255.0 / 100.0), 0, 255); }
    private static int levelToRaw(int level0_3) { return clamp((int)Math.round(level0_3 * (255.0 / 3.0)), 0, 255); }

    // Lấy current raw nếu có; nếu chưa có thì baseline hợp lý theo loại
    private static int currentOrBaseline(Device d, String typeL) {
        Integer v = d.getValue();
        if (v != null) return clamp(v, 0, 255);
        if (typeL.contains("fan") || typeL.contains("quat")) return levelToRaw(1); // quạt mặc định level 1
        return percentToRaw(50); // light & rgb mặc định 50%
    }

    private DeviceControlRequest buildVoiceRequestForDevice(Device d, ParseResult planned) {
        String typeL = (planned.deviceType == null ? "" : planned.deviceType.toLowerCase(Locale.ROOT));
        DeviceModels.Action a = planned.action;
        Integer v = planned.value;
        if (v != null && v < 0) v = null; // normalize -1 -> null

        final int STEP_LIGHT = 26;  // ~10%
        final int STEP_FAN       = 85;  // ~1 level

        // ===== LIGHT (param = raw 0..255) =====
        if (typeL.contains("light") && !typeL.contains("rgb")) {
            int cur = currentOrBaseline(d, typeL);
            int out;

            switch (a) {
                case TURN_ON:  out = 255; break;
                case TURN_OFF: out = 0;   break;

                case INCREASE:
                case DECREASE:
                    if (v != null) {
                        // Có số => đặt tuyệt đối bằng số nói
                        out = clamp(percentToRaw(v), 0, 255);
                    } else {
                        // Không số => bước mặc định
                        out = clamp(cur + (a == DeviceModels.Action.INCREASE ? STEP_LIGHT : -STEP_LIGHT), 0, 255);
                    }
                    break;

                case SET:
                default:
                    out = (v != null) ? clamp(percentToRaw(v), 0, 255) : cur;
                    break;
            }
            String jsonCommand = "{\"method\":\"setLedDim\",\"params\":" + out + "}";
            return new DeviceControlRequest(jsonCommand);
        }

        // ===== RGB (param = {r,g,b} raw 0..255) =====
        if (typeL.contains("rgb")) {
            // Nếu người nói không có "đặt/đổi màu" => bỏ qua
            if (a != DeviceModels.Action.SET || planned.colorRgb == null) {
                return new DeviceControlRequest("unknown");
            }

            String jsonCommand = "{\"method\":\"setRgbColor\",\"params\":" + "{\"r\":" + planned.colorRgb[0] + ",\"g\":" + planned.colorRgb[1] + ",\"b\":" + planned.colorRgb[2] + "}" + "}";
            return new DeviceControlRequest(jsonCommand);
        }

        // ===== FAN (param = raw 0..255; level 0..3 cũng quy ra 0..255 từ parser) =====
        if (typeL.contains("fan")) {
            int cur = currentOrBaseline(d, typeL);
            int out;

            switch (a) {
                case TURN_ON:  out = levelToRaw(3); break;
                case TURN_OFF: out = 0;break;

                case INCREASE:
                case DECREASE:
                    if (v != null) {
                        // Có số (vd: "mức 2") => đặt tuyệt đối bằng số nói
                        out = clamp(levelToRaw(v), 0, 255);
                    } else {
                        // Không số => bước mặc định
                        out = clamp(cur + (a == DeviceModels.Action.INCREASE ? STEP_FAN : -STEP_FAN), 0, 255);
                    }
                    break;

                case SET:
                default:
                    out = (v != null) ? clamp(levelToRaw(v), 0, 255) : cur;
                    break;
            }
            String jsonCommand = "{\"method\":\"setFanSpeed\",\"params\":" + out + "}";
            return new DeviceControlRequest(jsonCommand);
        }
        return new DeviceControlRequest("unknownCommand");
    }


    // Nếu muốn giữ deviceId dạng số cho ParseResult (hiển thị), nhưng API nhận String:
    private Integer tryParseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
    private String mapActionToCommand(DeviceModels.Action action) {
        switch (action) {
            case TURN_ON:  return "turn_on";
            case TURN_OFF: return "turn_off";
            case INCREASE: return "increase";
            case DECREASE: return "decrease";
            case SET:      return "set";
            default:       return "unknown";
        }
    }
    
    private String mapActionToVietnamese(DeviceModels.Action action) {
        switch (action) {
            case TURN_ON:  return "bật";
            case TURN_OFF: return "tắt";
            case INCREASE: return "tăng";
            case DECREASE: return "giảm";
            case SET:      return "đặt";
            default:       return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListening();
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
    }
}
