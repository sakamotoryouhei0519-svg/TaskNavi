package org.tasknavi.model;

/**
 * タスクの状態。保存するのは {@link #name()}（NOT_STARTED など）、画面に出すのは messages の文言。
 */
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

    /** 日本語の表示名でもコードでも読む。空や知らない値は未着手にする。 */
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
