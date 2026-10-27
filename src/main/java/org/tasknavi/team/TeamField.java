package org.tasknavi.team;

/**
 * 変更記録に書くタスクの項目。key は JSON に書く名前なので、変えると過去の記録が読めなくなる。
 */
public enum TeamField {
    PARENT("parent"),
    LEVEL("level"),
    ORDER("order"),
    NAME("name"),
    ASSIGNEE("assignee"),
    START("start"),
    END("end"),
    STATUS("status"),
    PRIORITY("priority"),
    PROGRESS("progress"),
    MEMO("memo"),
    /** COMMENT の記録だけに書く。タスクの値ではない。 */
    COMMENT("comment");

    private final String key;

    TeamField(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** 知らない key（新しい版のアプリが書いた項目）は null。 */
    public static TeamField fromKey(String key) {
        for (TeamField field : values()) {
            if (field.key.equals(key)) {
                return field;
            }
        }
        return null;
    }
}
