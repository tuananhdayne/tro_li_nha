package com.example.smarthomeui.smarthome.network;

public class GeminiRequest {
    private String text;
    private String userId;
    
    public GeminiRequest(String text) {
        this.text = text;
    }
    
    public GeminiRequest(String text, String userId) {
        this.text = text;
        this.userId = userId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
