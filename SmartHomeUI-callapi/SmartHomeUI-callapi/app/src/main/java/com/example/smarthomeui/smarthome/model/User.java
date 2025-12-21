package com.example.smarthomeui.smarthome.model;

/**
 * User model class
 * Đại diện cho thông tin người dùng trong hệ thống
 */
public class User {
    private String id;
    private String name;
    private String email;
    private String role;
    private String avatar;
    private boolean isActive;
    private String createdAt;
    private String lastLogin;

    public User() {}

    public User(String id, String name, String email, String role, String avatar) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.avatar = avatar;
        this.isActive = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public String getRoleDisplayName() {
        switch (role.toLowerCase()) {
            case "admin":
                return "Quản trị viên";
            case "moderator":
                return "Điều hành viên";
            default:
                return "Người dùng";
        }
    }
}
