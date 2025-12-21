package com.example.smarthomeui.smarthome.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Room model class
 * Đại diện cho thông tin phòng trong hệ thống smart home
 */
public class Room {
    private String id;
    private String name;
    private String location;
    private int deviceCount;
    private int activeDevices;
    private float temperature;
    private boolean isActive;
    private String lastUpdated;
    private String iconResource;

    // Additional properties for compatibility with existing code
    private List<Device> devices;
    private String description;
    private int iconRes;

    public Room() {
        this.devices = new ArrayList<>();
    }

    public Room(String id, String name, String location, int deviceCount, int activeDevices, float temperature, boolean isActive) {
        this();
        this.id = id;
        this.name = name;
        this.location = location;
        this.deviceCount = deviceCount;
        this.activeDevices = activeDevices;
        this.temperature = temperature;
        this.isActive = isActive;
    }

    // Constructor for compatibility with existing code
    public Room(String id, String name, int iconRes) {
        this();
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
        this.deviceCount = 0;
        this.activeDevices = 0;
        this.temperature = 25.0f;
        this.isActive = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getDeviceCount() { return deviceCount; }
    public void setDeviceCount(int deviceCount) { this.deviceCount = deviceCount; }

    public int getActiveDevices() { return activeDevices; }
    public void setActiveDevices(int activeDevices) { this.activeDevices = activeDevices; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getIconResource() { return iconResource; }
    public void setIconResource(String iconResource) { this.iconResource = iconResource; }

    // Compatibility methods for existing code
    public List<Device> getDevices() {
        if (devices == null) devices = new ArrayList<>();
        return devices;
    }
    public void setDevices(List<Device> devices) { this.devices = devices; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }

    public String getStatusText() {
        return isActive ? "Hoạt động" : "Tạm dừng";
    }

    public String getTemperatureText() {
        return String.format("%.1f°C", temperature);
    }
}
