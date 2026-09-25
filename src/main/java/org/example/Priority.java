package org.example;

public enum Priority {
    HIGH("高"),
    MEDIUM("中"),
    LOW("低");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Priority fromString(String label) {
        if (label == null || label.isBlank()) {
            return MEDIUM;
        }
        String normalized = label.trim();
        for (Priority p : values()) {
            if (p.label.equals(normalized) || p.name().equalsIgnoreCase(normalized)) {
                return p;
            }
        }
        return MEDIUM;
    }
}