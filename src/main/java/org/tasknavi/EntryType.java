package org.tasknavi;

/**
 * ダイアログ上の登録種別（プロジェクト / 工程 / タスク）。
 * 永続化キーは {@link #name()}（PROJECT 等）。日本語ラベルは表示互換用。
 */
public enum EntryType {
    PROJECT("プロジェクト"),
    PHASE("工程"),
    TASK("タスク");

    private final String label;

    EntryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EntryType fromString(String value) {
        if (value == null || value.isBlank()) {
            return TASK;
        }
        String normalized = value.trim();
        for (EntryType type : values()) {
            if (type.label.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return TASK;
    }
}
