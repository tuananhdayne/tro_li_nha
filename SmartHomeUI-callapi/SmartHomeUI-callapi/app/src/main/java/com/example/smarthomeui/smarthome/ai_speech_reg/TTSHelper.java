package com.example.smarthomeui.smarthome.ai_speech_reg;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.util.Locale;
import java.util.HashMap;

/**
 * Helper class for Text-to-Speech functionality
 */
public class TTSHelper {
    private static final String TAG = "TTSHelper";
    private TextToSpeech tts;
    private boolean isReady = false;
    private SpeechCompleteListener currentListener;
    
    public interface SpeechCompleteListener {
        void onSpeechComplete();
        void onSpeechError(String error);
    }
    
    public TTSHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Tiếng Việt không được hỗ trợ");
                    // Fallback to English
                    tts.setLanguage(Locale.US);
                }
                
                // Tăng tốc độ đọc (1.0 = bình thường, 1.3 = nhanh hơn 30%)
                tts.setSpeechRate(1.3f);
                
                // Thiết lập listener để biết khi nào nói xong
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "TTS started: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "TTS completed: " + utteranceId);
                        if (currentListener != null) {
                            currentListener.onSpeechComplete();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "TTS error: " + utteranceId);
                        if (currentListener != null) {
                            currentListener.onSpeechError("TTS error");
                        }
                    }
                });
                
                isReady = true;
                Log.d(TAG, "TTS initialized successfully with speech rate 1.7x");
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }
    
    /**
     * Speak the given text
     * @param text Text to speak
     */
    public void speak(String text) {
        speak(text, null);
    }
    
    /**
     * Speak the given text with completion callback
     * @param text Text to speak
     * @param listener Callback when speech completes
     */
    public void speak(String text, SpeechCompleteListener listener) {
        this.currentListener = listener;
        
        if (tts != null && isReady) {
            // Sử dụng Bundle và utteranceId để theo dõi
            Bundle params = new Bundle();
            String utteranceId = "tts_" + System.currentTimeMillis();
            
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
            Log.d(TAG, "Speaking: " + text + " (id: " + utteranceId + ")");
        } else {
            Log.w(TAG, "TTS not ready yet");
            if (listener != null) {
                listener.onSpeechError("TTS not ready");
            }
        }
    }
    
    /**
     * Stop current speech
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }
    
    /**
     * Check if TTS is ready
     */
    public boolean isReady() {
        return isReady;
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isReady = false;
            currentListener = null;
            Log.d(TAG, "TTS shut down");
        }
    }
}

