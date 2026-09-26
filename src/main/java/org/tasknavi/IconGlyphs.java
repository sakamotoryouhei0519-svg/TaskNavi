package org.tasknavi;

/**
 * アイコン種別ごとの絵文字／ファイル名カタログ。
 */
final class IconGlyphs {

    private IconGlyphs() {
    }

    static String emoji(IconManager.IconType type) {
        return switch (type) {
            case TAB_WBS -> "☰";
            case TAB_KANBAN -> "▧";
            case TAB_GANTT -> "📊";
            case TAB_CALENDAR, CALENDAR -> new String(Character.toChars(0x1F4C5));
            case LOGO -> "📋";
            case USER -> "👤";
            case THEME -> "☾";
            case ADD -> "➕";
            case SEARCH -> "🔍";
            case FILTER -> "▼";
            case DELETE -> "🗑️";
            case REFRESH -> "↺";
            case EXPORT -> "⇓";
            case IMPORT -> "⇑";
            case LOGOUT -> "↙";
            case CHECK -> "✓";
            case CROSS -> "✕";
            case FOLDER -> "📁";
        };
    }

    static String filename(IconManager.IconType type) {
        return switch (type) {
            case TAB_WBS -> "wbs.png";
            case TAB_KANBAN -> "kanban.png";
            case TAB_GANTT -> "gantt.png";
            case TAB_CALENDAR, CALENDAR -> "calendar.png";
            case LOGO -> "logo.png";
            case USER -> "user.png";
            case THEME -> "theme.png";
            case ADD -> "add.png";
            case SEARCH -> "search.png";
            case FILTER -> "filter.png";
            case DELETE -> "delete.png";
            case REFRESH -> "refresh.png";
            case EXPORT -> "export.png";
            case IMPORT -> "import.png";
            case LOGOUT -> "logout.png";
            case CHECK -> "check.png";
            case CROSS -> "cross.png";
            case FOLDER -> "folder.png";
        };
    }
}
