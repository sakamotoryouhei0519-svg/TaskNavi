package org.tasknavi.util;

import org.tasknavi.AppMessages;

/**
 * 各パネルで重複していた検索・ステータスフィルタ正規化。
 */
public final class SearchFilterUtil {
    private SearchFilterUtil() {
    }

    public static String normalizeKeyword(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 「すべて」や空はフィルタなし（null）として扱う。 */
    public static String normalizeStatus(String value) {
        if (AppMessages.isFilterAll(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
