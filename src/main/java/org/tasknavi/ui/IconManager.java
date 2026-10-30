package org.tasknavi.ui;

import javax.swing.*;
import java.awt.*;

/**
 * ボタンなどのアイコン。絵文字で描く。
 */
public class IconManager {

    // アイコン識別子
    public enum IconType {
        // タブアイコン
        TAB_TODAY,
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
        DELETE,
        REFRESH,

        // ヘッダー
        EXPORT,
        IMPORT
    }

    /**
     * アイコンを取得します。ふつうは絵文字の文字列、カレンダーだけは色付き絵文字の画像です。
     */
    public static Object getIcon(IconType type) {
        if (type == IconType.TAB_CALENDAR) {
            return createEmojiImageIcon(getEmoji(type), 18f);
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
            button.setFont(resolveEmojiFont(type == IconType.TAB_CALENDAR ? 18f : 20f));
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
        return IconGlyphs.emoji(type);
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
}
