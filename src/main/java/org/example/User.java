package org.example;

import java.time.LocalDateTime;

public class User {
    private final int id;
    private final String username;
    private final String email;
    private final String role;
    private final String displayName;
    private final LocalDateTime lastLoginAt;

    public User(int id, String username, String email, String role, String displayName, LocalDateTime lastLoginAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.displayName = displayName != null && !displayName.trim().isEmpty() ? displayName : username;
        this.lastLoginAt = lastLoginAt;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayNameOrUsername() {
        return displayName != null ? displayName : username;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
