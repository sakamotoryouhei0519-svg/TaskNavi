package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * 【アイコン管理クラス】
 * 絵文字とカスタム画像アイコンを一元管理し、
 * 設定によって切り替え可能にします。
 */
public class IconManager {
    
    private static final String USE_CUSTOM_ICONS_PREF_KEY = "tasknavi.icons.custom";
    private static final String ICONS_PATH_PREF_KEY = "tasknavi.icons.path";
    
    private static boolean useCustomIcons = loadUseCustomIconsPreference();
    private static String customIconsPath = loadCustomIconsPathPreference();
    
    // アイコン識別子
    public enum IconType {
        // タブアイコン
        TAB_WBS,
        TAB_KANBAN,
        TAB_GANTT,
        
        // ロゴ・ユーザー
        LOGO,
        USER,
        
        // テーマ
        THEME,
        
        // ツールバー
        ADD,
        SEARCH,
        DELETE,
        REFRESH,
        
        // ヘッダー
        EXPORT,
        IMPORT,
        LOGOUT,
        
        // カレンダー
        CALENDAR,
        
        // アクション
        CHECK,
        CROSS,
        FOLDER
    }
    
    
    /**
     * アイコンを取得します。
     * カスタムアイコンが有効で画像が存在すれば画像を、
     * そうでなければリソースから読み込み、なければ絵文字を返します。
     */
    public static Object getIcon(IconType type) {
        ImageIcon imageIcon = loadImageIcon(type);
        if (imageIcon != null) {
            return imageIcon;
        }
        return getEmoji(type);
    }
    
    /**
     * アイコンをJLabel用に取得します。
     */
    public static JLabel getIconLabel(IconType type) {
        Object icon = getIcon(type);
        JLabel label = new JLabel();
        if (icon instanceof ImageIcon) {
            label.setIcon((ImageIcon) icon);
        } else {
            label.setText((String) icon);
            label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        }
        return label;
    }
    
    /**
     * アイコンをボタン用に取得します。
     */
    public static JButton getIconButton(IconType type, String text) {
        Object icon = getIcon(type);
        JButton button;
        if (icon instanceof ImageIcon) {
            button = new JButton(text, (ImageIcon) icon);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
        } else {
            button = new JButton(icon + "  " + text);
        }
        return button;
    }
    
    /**
     * アイコンのみのボタンを取得します。
     */
    public static JButton getIconButton(IconType type) {
        Object icon = getIcon(type);
        JButton button;
        if (icon instanceof ImageIcon) {
            button = new JButton((ImageIcon) icon);
        } else {
            button = new JButton((String) icon);
            button.setFont(resolveEmojiFont(type == IconType.CALENDAR ? 18f : 20f));
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.CENTER);
        }
        return button;
    }
    
    /**
     * 絵文字を取得します。
     */
    private static String getEmoji(IconType type) {
        switch (type) {
            case TAB_WBS: return "☰";
            case TAB_KANBAN: return "▦";
            case TAB_GANTT: return "📊";
            case LOGO: return "📋";
            case USER: return "👤";
            case THEME: return "☾";
            case ADD: return "➕";
            case SEARCH: return "🔍";
            case DELETE: return "🗑️";
            case REFRESH: return "↻";
            case EXPORT: return "⇓";
            case IMPORT: return "⇑";
            case LOGOUT: return "↙";
            case CALENDAR: return new String(Character.toChars(0x1F4C5));
            case CHECK: return "✓";
            case CROSS: return "✕";
            case FOLDER: return "📁";
            default: return "";
        }
    }

    private static Font resolveEmojiFont(float size) {
        String[] candidates = {
                "Segoe UI Emoji",
                "Segoe UI Symbol",
                "Noto Color Emoji",
                "Apple Color Emoji",
                "SansSerif"
        };
        for (String candidate : candidates) {
            Font font = new Font(candidate, Font.PLAIN, Math.round(size));
            if (font.getFamily().equalsIgnoreCase(candidate) || font.getFontName().toLowerCase().contains(candidate.toLowerCase())) {
                return font.deriveFont(size);
            }
        }
        return new Font("SansSerif", Font.PLAIN, Math.round(size));
    }
    
    /**
     * 画像アイコンを読み込みます。
     * リソースから読み込み、カスタムパスが設定されている場合はそちらを優先します。
     */
    private static ImageIcon loadImageIcon(IconType type) {
        String fileName = getIconFileName(type);
        if (fileName == null) return null;
        
        // カスタムパスが設定されている場合は外部ファイルを読み込み
        if (useCustomIcons && customIconsPath != null && !customIconsPath.isEmpty()) {
            try {
                java.io.File iconFile = new java.io.File(customIconsPath, fileName);
                if (iconFile.exists()) {
                    return new ImageIcon(iconFile.getPath());
                }
            } catch (Exception e) {
                org.example.util.Logger.error("カスタムアイコン読み込みエラー: " + fileName, e);
            }
        }
        
        // リソースから読み込み
        try {
            java.net.URL resourceUrl = IconManager.class.getResource("/icons/" + fileName);
            if (resourceUrl != null) {
                return new ImageIcon(resourceUrl);
            }
        } catch (Exception e) {
            org.example.util.Logger.error("リソースアイコン読み込みエラー: " + fileName, e);
        }
        return null;
    }
    
    /**
     * アイコンタイプに対応するファイル名を取得します。
     */
    private static String getIconFileName(IconType type) {
        switch (type) {
            case TAB_WBS: return "wbs.png";
            case TAB_KANBAN: return "kanban.png";
            case TAB_GANTT: return "gantt.png";
            case LOGO: return "logo.png";
            case USER: return "user.png";
            case THEME: return "theme.png";
            case ADD: return "add.png";
            case SEARCH: return "search.png";
            case DELETE: return "delete.png";
            case REFRESH: return "refresh.png";
            case EXPORT: return "export.png";
            case IMPORT: return "import.png";
            case LOGOUT: return "logout.png";
            case CALENDAR: return "calendar.png";
            case CHECK: return "check.png";
            case CROSS: return "cross.png";
            case FOLDER: return "folder.png";
            default: return null;
        }
    }
    
    /**
     * カスタムアイコンを使用するかどうかを設定します。
     */
    public static void setUseCustomIcons(boolean use) {
        useCustomIcons = use;
        saveUseCustomIconsPreference();
    }
    
    /**
     * カスタムアイコンのパスを設定します。
     */
    public static void setCustomIconsPath(String path) {
        customIconsPath = path;
        saveCustomIconsPathPreference();
    }
    
    /**
     * カスタムアイコンが有効かどうかを返します。
     */
    public static boolean isUseCustomIcons() {
        return useCustomIcons;
    }
    
    /**
     * カスタムアイコンのパスを返します。
     */
    public static String getCustomIconsPath() {
        return customIconsPath;
    }
    
    private static boolean loadUseCustomIconsPreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
            return prefs.getBoolean(USE_CUSTOM_ICONS_PREF_KEY, false);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void saveUseCustomIconsPreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
            prefs.putBoolean(USE_CUSTOM_ICONS_PREF_KEY, useCustomIcons);
        } catch (Exception ignored) {}
    }
    
    private static String loadCustomIconsPathPreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
            return prefs.get(ICONS_PATH_PREF_KEY, "");
        } catch (Exception e) {
            return "";
        }
    }
    
    private static void saveCustomIconsPathPreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
            prefs.put(ICONS_PATH_PREF_KEY, customIconsPath != null ? customIconsPath : "");
        } catch (Exception ignored) {}
    }
}
