package org.tasknavi.model;

/**
 * ユーザー権限。登録時に users.role へ保存する。画面の操作制限にはまだ使っていない。
 */
public enum UserRole {
    ADMIN("ADMIN"),
    USER("USER");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromString(String value) {
        if (value == null) {
            return USER;
        }
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(value.trim())) {
                return role;
            }
        }
        return USER;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public static boolean isAdminRole(String value) {
        return fromString(value).isAdmin();
    }
}
