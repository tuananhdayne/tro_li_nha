package com.example.smarthomeui.smarthome.network;

public class ToggleActiveRequest {
    private boolean isActive;

    public ToggleActiveRequest(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
