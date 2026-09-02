package org.example;

public enum TaskStatus {
    NOT_STARTED("未着手"),
    IN_PROGRESS("進行中"),
    COMPLETED("完了");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TaskStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NOT_STARTED;
        }

        String normalized = value.trim();
        for (TaskStatus status : values()) {
            if (status.label.equals(normalized) || status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        return NOT_STARTED;
    }
}
