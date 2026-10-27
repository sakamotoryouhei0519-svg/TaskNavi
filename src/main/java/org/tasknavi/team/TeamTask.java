package org.tasknavi.team;

import org.tasknavi.model.Priority;
import org.tasknavi.model.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 記録を組み立てた結果の、今のタスク 1 件。ID は全員の PC で共通の UUID 文字列。
 */
public record TeamTask(
        String id,
        String parentId,
        int level,
        int order,
        String name,
        String assignee,
        LocalDate startDate,
        LocalDate endDate,
        TaskStatus status,
        Priority priority,
        int progress,
        String memo,
        String createdBy,
        String updatedBy,
        Instant updatedAt) {

    TeamTask withProgress(int newProgress, TaskStatus newStatus) {
        return new TeamTask(id, parentId, level, order, name, assignee, startDate, endDate, newStatus, priority,
                newProgress, memo, createdBy, updatedBy, updatedAt);
    }

    TeamTask withPlacement(String newParentId, int newLevel) {
        return new TeamTask(id, newParentId, newLevel, order, name, assignee, startDate, endDate, status, priority,
                progress, memo, createdBy, updatedBy, updatedAt);
    }

    /** 壊れた値は既定値に置き換え、一覧の表示を止めない。 */
    static TeamTask from(String id, Map<TeamField, String> values, String createdBy, String updatedBy, Instant updatedAt) {
        return new TeamTask(
                id,
                blankToNull(values.get(TeamField.PARENT)),
                clamp(parseInt(values.get(TeamField.LEVEL), 1), 1, 3),
                parseInt(values.get(TeamField.ORDER), 0),
                nullToEmpty(values.get(TeamField.NAME)),
                nullToEmpty(values.get(TeamField.ASSIGNEE)),
                parseDate(values.get(TeamField.START)),
                parseDate(values.get(TeamField.END)),
                TaskStatus.fromString(values.get(TeamField.STATUS)),
                Priority.fromString(values.get(TeamField.PRIORITY)),
                clamp(parseInt(values.get(TeamField.PROGRESS), 0), 0, 100),
                nullToEmpty(values.get(TeamField.MEMO)),
                createdBy,
                updatedBy,
                updatedAt);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
