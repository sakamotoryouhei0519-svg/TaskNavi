package org.example;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * アプリ全体の表示文言を ResourceBundle から取得する。
 */
public final class AppMessages {
    private static final String BASE_NAME = "messages";
    /** フィルタ「すべて」のドメインコード（表示文言ではない）。 */
    public static final String FILTER_ALL = "ALL";

    private static Locale currentLocale = Locale.getDefault();

    private AppMessages() {
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale != null ? locale : Locale.getDefault();
    }

    public static Locale getLocale() {
        return currentLocale;
    }

    public static String getUserMessage(AppException.ErrorCode code) {
        if (code == null) {
            return get("error.unknown", "不明なエラーが発生しました");
        }
        String key = "error." + code.name().toLowerCase(Locale.ROOT).replace('_', '.');
        return get(key, code.getDefaultMessage());
    }

    public static String get(String key) {
        return get(key, key);
    }

    public static String get(String key, String defaultValue) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, currentLocale);
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        }
    }

    public static String format(String key, String defaultPattern, Object... args) {
        String pattern = get(key, defaultPattern);
        return MessageFormat.format(pattern, args);
    }

    public static boolean isFilterAll(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        return FILTER_ALL.equalsIgnoreCase(trimmed) || "すべて".equals(trimmed) || "All".equalsIgnoreCase(trimmed);
    }

    /** ドメイン上のステータス値（DB保存値）を画面表示用に変換する。 */
    public static String statusDisplay(String domainStatus) {
        if (isFilterAll(domainStatus)) {
            return get("common.filter.all", "すべて");
        }
        TaskStatus status = TaskStatus.fromString(domainStatus);
        return switch (status) {
            case NOT_STARTED -> get("status.not_started", TaskStatus.NOT_STARTED.getLabel());
            case IN_PROGRESS -> get("status.in_progress", TaskStatus.IN_PROGRESS.getLabel());
            case COMPLETED -> get("status.completed", TaskStatus.COMPLETED.getLabel());
        };
    }

    public static String statusDisplay(TaskStatus status) {
        return statusDisplay(status != null ? status.name() : FILTER_ALL);
    }

    /** 画面のステータス選択肢に対応するドメイン値（永続化コード）を返す。 */
    public static String statusDomain(TaskStatus status) {
        return status != null ? status.name() : TaskStatus.NOT_STARTED.name();
    }

    public static String priorityDisplay(String domainPriority) {
        Priority p = Priority.fromString(domainPriority);
        return switch (p) {
            case HIGH -> get("priority.high", Priority.HIGH.getLabel());
            case MEDIUM -> get("priority.medium", Priority.MEDIUM.getLabel());
            case LOW -> get("priority.low", Priority.LOW.getLabel());
        };
    }

    public static String entryTypeDisplay(String domainType) {
        return entryTypeDisplay(EntryType.fromString(domainType));
    }

    public static String entryTypeDisplay(EntryType type) {
        EntryType resolved = type != null ? type : EntryType.TASK;
        return switch (resolved) {
            case PROJECT -> get("dialog.type.project", EntryType.PROJECT.getLabel());
            case PHASE -> get("dialog.type.phase", EntryType.PHASE.getLabel());
            case TASK -> get("dialog.type.task", EntryType.TASK.getLabel());
        };
    }

    public static String htmlUnderline(String text) {
        return "<html><u>" + text + "</u></html>";
    }
}
