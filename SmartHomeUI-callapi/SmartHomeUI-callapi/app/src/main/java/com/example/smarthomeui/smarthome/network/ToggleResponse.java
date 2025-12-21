package com.example.smarthomeui.smarthome.network;

public class ToggleResponse {
    private String message;
    private boolean isActive;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
