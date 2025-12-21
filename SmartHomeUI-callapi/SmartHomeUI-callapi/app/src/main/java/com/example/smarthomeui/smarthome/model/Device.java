package com.example.smarthomeui.smarthome.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Device implements Serializable {

    // ==== Capability keys (dùng dạng String cho đơn giản) ====
    public static final String CAP_POWER        = "power";
    public static final String CAP_BRIGHTNESS   = "brightness";
    public static final String CAP_COLOR        = "color";
    public static final String CAP_SPEED        = "speed";
    public static final String CAP_TEMPERATURE  = "temperature";
    private String id;
    private String name;
    private String type;     // "Light", "Fan", "AC", "Outlet", ...
    private boolean on;
    private String status;   // "on" / "off"
    private String macAddress;
    private String room;     // Thêm thuộc tính room
    private Integer roomId;  // ID của phòng từ API
    private int value;       // Thêm thuộc tính value (brightness, speed, etc.)
    private String powerConsumption; // Thêm thuộc tính công suất tiêu thụ

    // Token để đăng ký thiết bị (yêu cầu của bạn)
    private String token;

    // Tập capabilities của thiết bị
    private final Set<String> capabilities = new LinkedHashSet<>();

    // Thuộc tính mở rộng (tuỳ capability)
    private int brightness = 100;     // 0..100
    private int color      = 0xFFFFFFFF; // ARGB
    private int speed      = 0;       // 0..3
    private int temperature = 25;     // ví dụ °C

    public Device(String id, String name, String type, boolean on) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.on = on;
    }

    public Device(String id, String name, String type, boolean on, String token) {
        this(id, name, type, on);
        this.token = token;
    }

    // Constructor mới để tương thích với DeviceManagementActivity
    public Device(String id, String name, String room, String type, boolean online, int value, String powerConsumption) {
        this.id = id;
        this.name = name;
        this.room = room;
        this.type = type;
        this.on = online;
        this.value = value;
        this.powerConsumption = powerConsumption;

        // Log để verify ID ngay khi tạo object
        android.util.Log.d("Device", "Created Device - ID: " + id + ", Name: " + name);
    }

    // ======= GET/SET cơ bản =======
    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isOn() { return on; }
    public boolean isOnline() { return on; } // Alias cho isOn
    public String getToken() { return token; }
    public String getRoom() { return room; }
    public int getValue() { return value; }
    public String getPowerConsumption() { return powerConsumption; }
    public String getStatusRaw() { return status; }
    public String getMacAddress() { return macAddress; }

    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setOn(boolean on) { this.on = on; }
    public void setOnline(boolean online) { this.on = online; } // Alias cho setOn
    public void setToken(String token) { this.token = token; }
    public void setRoom(String room) { this.room = room; }
    public void setValue(int value) { this.value = value; }
    public void setPowerConsumption(String powerConsumption) { this.powerConsumption = powerConsumption; }
    public void setStatus(String status) { this.status = status; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    // ======= Thêm các method mới cho DeviceAdminAdapter =======

    /**
     * Lấy thông tin hoạt động cuối cùng của thiết bị
     * @return String mô tả hoạt động cuối cùng
     */
    public String getLastActivity() {
        if (on) {
            return "Hoạt động " + getFormattedTime();
        } else {
            return "Tắt " + getFormattedTime();
        }
    }

    /**
     * Lấy trạng thái hiện tại của thiết bị
     * @return String trạng thái (Online/Offline)
     */
    public String getStatus() {
        if (status != null) {
            return status.equalsIgnoreCase("on") ? "Online" : "Offline";
        }
        return on ? "Online" : "Offline";
    }

    /**
     * Lấy text hiển thị giá trị hiện tại của thiết bị
     * @return String mô tả giá trị hiện tại
     */
    public String getValueText() {
        if (!on) {
            return "Tắt";
        }

        // Hiển thị giá trị dựa trên loại thiết bị
        if (has(CAP_BRIGHTNESS)) {
            return brightness + "%";
        } else if (has(CAP_SPEED)) {
            return "Tốc độ " + speed;
        } else if (has(CAP_TEMPERATURE)) {
            return temperature + "°C";
        } else {
            return "Bật";
        }
    }

    /**
     * Helper method để format thời gian
     * @return String thời gian đã format
     */
    private String getFormattedTime() {
        // Giả lập thời gian hoạt động (có thể thay thế bằng timestamp thực tế)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    // ======= Capabilities API (giải quyết lỗi .has / CAP_COLOR) =======
    public Device addCaps(String... caps) {
        if (caps != null) capabilities.addAll(Arrays.asList(caps));
        return this;
    }
    public boolean has(String cap) { return capabilities.contains(cap); }
    public Set<String> getCapabilities() { return Collections.unmodifiableSet(capabilities); }

    // ======= Thuộc tính mở rộng =======
    public int getBrightness() { return brightness; }
    public void setBrightness(int brightness) {
        this.brightness = Math.max(0, Math.min(100, brightness));
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) {
        this.speed = Math.max(0, Math.min(3, speed));
    }

    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }

    // ======= Helpers theo loại (nếu bạn vẫn dùng) =======
    public boolean isLight() {
        // Ưu tiên capabilities, fallback theo type
        return has(CAP_BRIGHTNESS) || has(CAP_COLOR) ||
                (type != null && type.toLowerCase().contains("light"));
    }
    public boolean isFan() {
        return has(CAP_SPEED) ||
                (type != null && type.toLowerCase().contains("fan"));
    }
    public boolean isAC() {
        return has(CAP_TEMPERATURE) ||
                (type != null && (type.equalsIgnoreCase("ac") || type.toLowerCase().contains("air")));
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }
}
