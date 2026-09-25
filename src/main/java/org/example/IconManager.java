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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IconManager.class);
    // アイコン識別子
    public enum IconType {
        // タブアイコン
        TAB_WBS,
        TAB_KANBAN,
        TAB_GANTT,
        TAB_CALENDAR,
        
        // ロゴ・ユーザー
        LOGO,
        USER,
        
        // テーマ
        THEME,
        
        // ツールバー
        ADD,
        SEARCH,
        FILTER,
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
        if (type == IconType.CALENDAR || type == IconType.TAB_CALENDAR) {
            return createEmojiImageIcon(getEmoji(type), 18f);
        }
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
            button.setIconTextGap(6);
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
            button.setFont(resolveEmojiFont(type == IconType.CALENDAR || type == IconType.TAB_CALENDAR ? 18f : 20f));
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.CENTER);
        }
        // ★以下を追加してボタン自体の枠・背景を消去
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        return button;
    }

    /** タブのカレンダーと共通の、テーマ色で描画できる線画カレンダーアイコンを作成します。 */
    public static Icon createCalendarOutlineIcon(Color color) {
        return new Icon() {
            private static final int SIZE = 18;

            @Override
            public int getIconWidth() {
                return SIZE;
            }

            @Override
            public int getIconHeight() {
                return SIZE;
            }

            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                try {
                    Color iconColor = component != null && component.getForeground() != null
                            ? component.getForeground()
                            : color;
                    g2.setColor(iconColor);
                    g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    paintCalendarOutline(g2, x, y);
                } finally {
                    g2.dispose();
                }
            }
        };
    }

    /** 線画カレンダー本体。呼び出し側で色と線幅を指定します。 */
    public static void paintCalendarOutline(Graphics2D g2, int x, int y) {
        g2.drawRoundRect(x + 2, y + 3, 14, 13, 2, 2);
        g2.drawLine(x + 2, y + 7, x + 16, y + 7);
        g2.drawLine(x + 6, y + 2, x + 6, y + 5);
        g2.drawLine(x + 12, y + 2, x + 12, y + 5);
        g2.fillRect(x + 5, y + 9, 2, 2);
        g2.fillRect(x + 9, y + 9, 2, 2);
        g2.fillRect(x + 13, y + 9, 2, 2);
        g2.fillRect(x + 5, y + 13, 2, 2);
        g2.fillRect(x + 9, y + 13, 2, 2);
    }
    
    /**
     * 絵文字を取得します。
     */
    private static String getEmoji(IconType type) {
        switch (type) {
            case TAB_WBS: return "☰";
            case TAB_KANBAN: return "▧";
            case TAB_GANTT: return "📊";
            case TAB_CALENDAR: return new String(Character.toChars(0x1F4C5));
            case LOGO: return "📋";
            case USER: return "👤";
            case THEME: return "☾";
            case ADD: return "➕";
            case SEARCH: return "🔍";
            case FILTER: return "▼";
            case DELETE: return "🗑️";
            case REFRESH: return "↺";
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
        for (String name : candidates) {
            Font font = new Font(name, Font.PLAIN, Math.round(size));
            if (font.canDisplay(0x1F4C5)) {
                return font.deriveFont(size);
            }
        }
        return new Font("SansSerif", Font.PLAIN, Math.round(size)).deriveFont(size);
    }

    private static ImageIcon createEmojiImageIcon(String emoji, float size) {
        int width = 20;
        int height = 20;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(resolveEmojiFont(size));
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(emoji)) / 2;
            int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
            g2.setColor(AppTheme.TEXT_PRIMARY);
            g2.drawString(emoji, x, y);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(img);
    }
    
    /**
     * 画像アイコンを読み込みます。
     */
    private static ImageIcon loadImageIcon(IconType type) {
        if (!useCustomIcons) {
            return null;
        }
        
        String filename = getIconFilename(type);
        if (filename == null) {
            return null;
        }
        
        try {
            // カスタムパスからの読み込みを試みる
            if (customIconsPath != null && !customIconsPath.isEmpty()) {
                java.io.File file = new java.io.File(customIconsPath, filename);
                if (file.exists()) {
                    return new ImageIcon(file.getAbsolutePath());
                }
            }
            
            // クラスパスからの読み込み
            java.net.URL url = IconManager.class.getResource("/icons/" + filename);
            if (url != null) {
                return new ImageIcon(url);
            }
        } catch (Exception e) {
            logger.warn("アイコンの読み込みに失敗しました: " + type, e);
        }
        
        return null;
    }
    
    /**
     * アイコンファイル名を取得します。
     */
    private static String getIconFilename(IconType type) {
        switch (type) {
            case TAB_WBS: return "wbs.png";
            case TAB_KANBAN: return "kanban.png";
            case TAB_GANTT: return "gantt.png";
            case TAB_CALENDAR: return "calendar.png";
            case LOGO: return "logo.png";
            case USER: return "user.png";
            case THEME: return "theme.png";
            case ADD: return "add.png";
            case SEARCH: return "search.png";
            case FILTER: return "filter.png";
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
     * カスタムアイコンの使用設定
     */
    public static void setUseCustomIcons(boolean use) {
        useCustomIcons = use;
        Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
        prefs.putBoolean(USE_CUSTOM_ICONS_PREF_KEY, use);
    }
    
    public static boolean isUseCustomIcons() {
        return useCustomIcons;
    }
    
    /**
     * カスタムアイコンのパス設定
     */
    public static void setCustomIconsPath(String path) {
        customIconsPath = path;
        Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
        if (path != null) {
            prefs.put(ICONS_PATH_PREF_KEY, path);
        } else {
            prefs.remove(ICONS_PATH_PREF_KEY);
        }
    }
    
    public static String getCustomIconsPath() {
        return customIconsPath;
    }
    
    private static boolean loadUseCustomIconsPreference() {
        Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
        return prefs.getBoolean(USE_CUSTOM_ICONS_PREF_KEY, false);
    }
    
    private static String loadCustomIconsPathPreference() {
        Preferences prefs = Preferences.userNodeForPackage(IconManager.class);
        return prefs.get(ICONS_PATH_PREF_KEY, null);
    }
}
