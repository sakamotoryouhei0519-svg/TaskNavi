package org.tasknavi.ui.team;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Priority;
import org.tasknavi.team.TeamField;

/** 重なりと履歴の画面で、項目名と値を人が読める形にする。 */
final class TeamFieldText {

    private static final int MEMO_PREVIEW = 40;

    private TeamFieldText() {
    }

    static String label(TeamField field) {
        return switch (field) {
            case PARENT -> AppMessages.get("team.field.parent", "親");
            case NAME -> AppMessages.get("team.field.name", "名前");
            case ASSIGNEE -> AppMessages.get("team.field.assignee", "担当者");
            case START -> AppMessages.get("team.field.start", "開始日");
            case END -> AppMessages.get("team.field.end", "終了日");
            case STATUS -> AppMessages.get("team.field.status", "状態");
            case PRIORITY -> AppMessages.get("team.field.priority", "優先度");
            case PROGRESS -> AppMessages.get("team.field.progress", "進捗");
            case MEMO -> AppMessages.get("team.field.memo", "メモ");
            case COMMENT -> AppMessages.get("team.field.comment", "コメント");
            default -> field.key();
        };
    }

    static String value(TeamField field, String value) {
        if (value == null || value.isBlank()) {
            return AppMessages.get("team.conflict.empty", "（空）");
        }
        return switch (field) {
            case STATUS -> AppMessages.statusDisplay(value);
            case PRIORITY -> Priority.fromString(value).getLabel();
            case PROGRESS -> value + "%";
            case MEMO, COMMENT -> {
                String oneLine = value.replace('\n', ' ').strip();
                yield oneLine.length() > MEMO_PREVIEW ? oneLine.substring(0, MEMO_PREVIEW) + "…" : oneLine;
            }
            default -> value;
        };
    }
}
