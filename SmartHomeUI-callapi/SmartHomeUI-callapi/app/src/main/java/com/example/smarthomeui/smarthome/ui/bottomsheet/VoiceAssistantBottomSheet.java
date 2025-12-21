package com.example.smarthomeui.smarthome.ui.bottomsheet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.smarthomeui.R;
import com.example.smarthomeui.smarthome.ai_speech_reg.DeviceRegistry;
import com.example.smarthomeui.smarthome.ai_speech_reg.TTSHelper;
import com.example.smarthomeui.smarthome.model.Device;
import com.example.smarthomeui.smarthome.network.Api;
import com.example.smarthomeui.smarthome.network.ApiClient;
import com.example.smarthomeui.smarthome.network.DeviceControlRequest;
import com.example.smarthomeui.smarthome.network.DeviceControlResponse;
import com.example.smarthomeui.smarthome.network.GeminiRequest;
import com.example.smarthomeui.smarthome.network.GeminiResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.os.Handler;

public class VoiceAssistantBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "VoiceAssistant";
    
    private TextView tvStatus, tvUserSpeech, tvAssistantResponse;
    private FloatingActionButton fabMic;
    private View viewRing;

    private SpeechRecognizer recognizer;
    private TTSHelper ttsHelper;
    private DeviceRegistry registry;

    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // Trạng thái để theo dõi
    private boolean isListening = false;
    private boolean hasResults = false;
    private String lastRecognizedText = "";

    private final ActivityResultLauncher<String> micPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    android.util.Log.d(TAG, "Mic permission granted");
                    startListening();
                } else {
                    android.util.Log.e(TAG, "Mic permission denied");
                    tvStatus.setText("Cần cấp quyền microphone");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_voice_assistant, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init views
        tvStatus = view.findViewById(R.id.tvStatus);
        tvUserSpeech = view.findViewById(R.id.tvUserSpeech);
        tvAssistantResponse = view.findViewById(R.id.tvAssistantResponse);
        fabMic = view.findViewById(R.id.fabMic);
        viewRing = view.findViewById(R.id.viewRing);

        // Init helpers
        ttsHelper = new TTSHelper(requireContext());
        registry = new DeviceRegistry();

        // Load devices
        registry.refreshFromApi(requireContext(), 0, 200, new DeviceRegistry.LoadCallback() {
            @Override public void onLoaded(int count) {
                android.util.Log.d(TAG, "Loaded " + count + " devices");
            }
            @Override public void onError(String message) {
                android.util.Log.e(TAG, "Error loading devices: " + message);
            }
        });

        // Kiểm tra Speech Recognition có sẵn không
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            tvStatus.setText("Thiết bị không hỗ trợ nhận dạng giọng nói");
            fabMic.setEnabled(false);
            return;
        }

        // Push-to-talk pattern - NHẤN GIỮ để nói
        fabMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    android.util.Log.d(TAG, "ACTION_DOWN - Starting to listen");
                    hasResults = false;
                    lastRecognizedText = "";
                    requestMicAndStart();
                    fabMic.setAlpha(0.7f);
                    viewRing.setVisibility(View.VISIBLE);
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    android.util.Log.d(TAG, "ACTION_UP - Stopping listening, hasResults=" + hasResults);
                    fabMic.setAlpha(1.0f);
                    viewRing.setVisibility(View.GONE);
                    
                    // Chỉ stop listening, KHÔNG destroy recognizer ngay
                    // Để recognizer có thời gian xử lý và trả kết quả
                    if (recognizer != null && isListening) {
                        recognizer.stopListening();
                        isListening = false;
                        
                        // Đợi 0.8s để nhận kết quả, nếu không có thì xử lý partial results
                        handler.postDelayed(() -> {
                            if (!hasResults && !lastRecognizedText.isEmpty()) {
                                android.util.Log.d(TAG, "Using partial results: " + lastRecognizedText);
                                processVoiceCommand(lastRecognizedText);
                            } else if (!hasResults) {
                                tvStatus.setText("Không nghe thấy gì");
                            }
                            destroyRecognizer();
                        }, 800); // Giảm từ 1500ms xuống 800ms
                    }
                    return true;
            }
            return false;
        });
        
        // Thêm click listener để hỗ trợ nhấn 1 lần (continuous mode)
        fabMic.setOnClickListener(v -> {
            // Click đơn giản sẽ được xử lý bởi touch listener
        });
    }

    private void requestMicAndStart() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.d(TAG, "Requesting mic permission");
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startListening();
        }
    }

    private void startListening() {
        // Hủy recognizer cũ nếu có
        destroyRecognizer();
        
        tvStatus.setText("Đang khởi động...");
        tvUserSpeech.setText("");
        tvAssistantResponse.setText("");
        hasResults = false;
        lastRecognizedText = "";

        try {
            android.util.Log.d(TAG, "Creating SpeechRecognizer");
            recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            
            if (recognizer == null) {
                android.util.Log.e(TAG, "SpeechRecognizer is null");
                tvStatus.setText("Không thể tạo bộ nhận dạng giọng nói");
                return;
            }
            
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override 
                public void onReadyForSpeech(Bundle params) {
                    android.util.Log.d(TAG, "onReadyForSpeech");
                    isListening = true;
                    if (isAdded()) {
                        tvStatus.setText("Hãy nói...");
                    }
                }
                
                @Override 
                public void onBeginningOfSpeech() {
                    android.util.Log.d(TAG, "onBeginningOfSpeech");
                    if (isAdded()) {
                        tvStatus.setText("Đang nghe...");
                    }
                }
                
                @Override 
                public void onRmsChanged(float rmsdB) {
                    // Có thể dùng để hiển thị animation âm thanh
                }
                
                @Override 
                public void onBufferReceived(byte[] buffer) {}
                
                @Override 
                public void onEndOfSpeech() {
                    android.util.Log.d(TAG, "onEndOfSpeech");
                    isListening = false;
                    if (isAdded()) {
                        tvStatus.setText("Đang xử lý...");
                    }
                }
                
                @Override 
                public void onError(int error) {
                    isListening = false;
                    String errorMsg = getErrorMessage(error);
                    android.util.Log.e(TAG, "onError: " + error + " - " + errorMsg);
                    
                    if (isAdded()) {
                        // Nếu có partial results, vẫn xử lý
                        if (!lastRecognizedText.isEmpty() && 
                            (error == SpeechRecognizer.ERROR_NO_MATCH || 
                             error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                            android.util.Log.d(TAG, "Has partial text, processing: " + lastRecognizedText);
                            processVoiceCommand(lastRecognizedText);
                        } else {
                            tvStatus.setText(errorMsg);
                        }
                    }
                }
                
                @Override 
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (list != null && !list.isEmpty()) {
                        lastRecognizedText = list.get(0);
                        android.util.Log.d(TAG, "onPartialResults: " + lastRecognizedText);
                        if (isAdded()) {
                            tvUserSpeech.setText(lastRecognizedText);
                        }
                    }
                }
                
                @Override 
                public void onEvent(int eventType, Bundle params) {
                    android.util.Log.d(TAG, "onEvent: " + eventType);
                }
                
                @Override 
                public void onResults(Bundle results) {
                    hasResults = true;
                    isListening = false;
                    ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text = (list != null && !list.isEmpty()) ? list.get(0) : "";
                    android.util.Log.d(TAG, "onResults: " + text);
                    
                    if (isAdded()) {
                        if (!text.isEmpty()) {
                            tvUserSpeech.setText(text);
                            processVoiceCommand(text);
                        } else if (!lastRecognizedText.isEmpty()) {
                            // Fallback đến partial results
                            processVoiceCommand(lastRecognizedText);
                        } else {
                            tvStatus.setText("Không nghe rõ, hãy thử lại");
                        }
                    }
                }
            });

            // Tạo intent với các tùy chọn tối ưu cho tiếng Việt
            Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            
            // Thiết lập ngôn ngữ tiếng Việt
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN");
            intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"vi-VN"});
            
            // Bật partial results để hiển thị text realtime
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            
            // Số kết quả tối đa
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            
            // Thử online trước (chất lượng tốt hơn)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
            
            android.util.Log.d(TAG, "Starting listening with vi-VN");
            recognizer.startListening(intent);
            tvStatus.setText("Đang lắng nghe...");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error starting recognition", e);
            if (isAdded()) {
                tvStatus.setText("Lỗi khởi động: " + e.getMessage());
            }
        }
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            try {
                android.util.Log.d(TAG, "Destroying recognizer");
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error destroying recognizer", e);
            }
            recognizer = null;
            isListening = false;
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Lỗi ghi âm thiết bị";
            case SpeechRecognizer.ERROR_CLIENT: return "Lỗi ứng dụng";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Thiếu quyền microphone";
            case SpeechRecognizer.ERROR_NETWORK: return "Lỗi mạng - kiểm tra kết nối";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Hết thời gian kết nối mạng";
            case SpeechRecognizer.ERROR_NO_MATCH: return "Không nhận dạng được";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Bộ nhận dạng đang bận";
            case SpeechRecognizer.ERROR_SERVER: return "Lỗi server Google";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "Không nghe thấy giọng nói";
            default: return "Lỗi không xác định: " + error;
        }
    }

    private void processVoiceCommand(String text) {
        if (text == null || text.trim().isEmpty()) {
            if (isAdded()) {
                tvStatus.setText("Không nghe thấy gì");
                speakAndDismiss("Xin lỗi, tôi không nghe rõ.");
            } else {
                dismissWhenReady();
            }
            return;
        }

        if (isAdded()) {
            tvStatus.setText("Đang gửi đến AI...");
            tvUserSpeech.setText(text);
        }
        
        android.util.Log.d(TAG, "Processing command: " + text);

        // Get current user ID
        String userId = new com.example.smarthomeui.smarthome.utils.UserManager(requireContext()).getUserId();
        android.util.Log.d(TAG, "User ID: " + userId);

        // Call Gemini chatbot with userId
        GeminiRequest request = new GeminiRequest(text, userId);
        android.util.Log.d(TAG, "Request payload: text=" + request.getText() + ", userId=" + request.getUserId());
        
        Api api = ApiClient.getGeminiClient().create(Api.class);
        api.askGemini(request).enqueue(new retrofit2.Callback<GeminiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<GeminiResponse> call,
                                   retrofit2.Response<GeminiResponse> response) {
                android.util.Log.d(TAG, "Response code: " + response.code());
                
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d(TAG, "Response body received");
                    
                    GeminiResponse.GeminiAssistant assistant = response.body().getAssistant();
                    if (assistant != null) {
                        String message = assistant.getMessage();
                        android.util.Log.d(TAG, "Assistant message: " + message);
                        
                        tvStatus.setText("✓ Hoàn tất");
                        tvAssistantResponse.setText(message);

                        // Nói và đợi xong rồi mới đóng
                        speakAndDismiss(message);
                    } else {
                        android.util.Log.e(TAG, "Assistant is null in response");
                        tvStatus.setText("Lỗi - AI không phản hồi");
                        speakAndDismiss("Có lỗi xảy ra");
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        android.util.Log.e(TAG, "Error response: " + response.code() + " - " + errorBody);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Cannot read error body", e);
                    }
                    
                    tvStatus.setText("Lỗi kết nối AI: " + response.code());
                    speakAndDismiss("Không thể kết nối trợ lý AI");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<GeminiResponse> call, Throwable t) {
                android.util.Log.e(TAG, "Network error", t);
                if (isAdded()) {
                    tvStatus.setText("Lỗi mạng: " + t.getMessage());
                    speakAndDismiss("Không thể kết nối máy chủ");
                } else {
                    dismissWhenReady();
                }
            }
        });
    }
    /**
     * Nói message và đợi nói xong rồi mới đóng bottom sheet
     */
    private void speakAndDismiss(String message) {
        if (ttsHelper != null && message != null && !message.isEmpty()) {
            ttsHelper.speak(message, new TTSHelper.SpeechCompleteListener() {
                @Override
                public void onSpeechComplete() {
                    android.util.Log.d(TAG, "TTS completed, dismissing...");
                    // Chạy trên UI thread
                    handler.post(() -> dismissWhenReady());
                }

                @Override
                public void onSpeechError(String error) {
                    android.util.Log.e(TAG, "TTS error: " + error);
                    // Vẫn đóng sau 2 giây nếu có lỗi
                    handler.postDelayed(() -> dismissWhenReady(), 2000);
                }
            });
        } else {
            // Nếu không có TTS hoặc message rỗng, đóng sau 1.5 giây
            handler.postDelayed(() -> dismissWhenReady(), 1500);
        }
    }

    /**
     * Đóng bottom sheet nếu vẫn còn attached
     */
    private void dismissWhenReady() {
        if (isAdded()) {
            try {
                dismiss();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error dismissing", e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        android.util.Log.d(TAG, "onDestroyView");
        destroyRecognizer();
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
    }
}
