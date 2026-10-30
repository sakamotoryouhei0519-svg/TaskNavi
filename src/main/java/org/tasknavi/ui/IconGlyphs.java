package org.tasknavi.ui;

/**
 * アイコン種別ごとの絵文字。
 */
final class IconGlyphs {

    private IconGlyphs() {
    }

    static String emoji(IconManager.IconType type) {
        return switch (type) {
            case TAB_TODAY -> "☀";
            case TAB_WBS -> "☰";
            case TAB_KANBAN -> "▧";
            case TAB_GANTT -> "📊";
            case TAB_CALENDAR -> new String(Character.toChars(0x1F4C5));
            case LOGO -> "📋";
            case USER -> "👤";
            case THEME -> "☾";
            case ADD -> "➕";
            case DELETE -> "🗑️";
            case REFRESH -> "↺";
            case EXPORT -> "⇓";
            case IMPORT -> "⇑";
        };
    }
}
