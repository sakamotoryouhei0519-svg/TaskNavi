package org.example;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * 【アプリケーションテーマ設定クラス】
 * アプリ全体の色、フォント、ボーダーなどのデザイン設定を集約するクラスです。
 */
public class AppTheme {

    public interface Theme {
        Color primary();
        Color primaryDark();
        Color background();
        Color panel();
        Color textPrimary();
        Color textMuted();
        Color borderColor();
        Color textFieldBackground();
        Color textFieldForeground();
    }

    private static final Theme LIGHT_THEME = new Theme() {
        @Override public Color primary() { return new Color(59, 130, 246); }
        @Override public Color primaryDark() { return new Color(37, 99, 235); }
        @Override public Color background() { return new Color(248, 250, 252); }
        @Override public Color panel() { return Color.WHITE; }
        @Override public Color textPrimary() { return new Color(30, 41, 59); }
        @Override public Color textMuted() { return new Color(71, 85, 105); }
        @Override public Color borderColor() { return new Color(148, 163, 184); }
        @Override public Color textFieldBackground() { return Color.WHITE; }
        @Override public Color textFieldForeground() { return textPrimary(); }
    };

    private static final Theme DARK_THEME = new Theme() {
        @Override public Color primary() { return new Color(96, 165, 250); }
        @Override public Color primaryDark() { return new Color(59, 130, 246); }
        @Override public Color background() { return new Color(15, 23, 42); }
        @Override public Color panel() { return new Color(31, 41, 55); }
        @Override public Color textPrimary() { return new Color(248, 250, 252); }
        @Override public Color textMuted() { return new Color(203, 213, 225); }
        @Override public Color borderColor() { return new Color(100, 116, 139); }
        @Override public Color textFieldBackground() { return new Color(17, 24, 39); }
        @Override public Color textFieldForeground() { return textPrimary(); }
    };

    private static final String THEME_PREF_KEY = "tasknavi.theme.dark";
    private static Theme currentTheme = loadStoredTheme();

    public static Color PRIMARY = currentTheme.primary();
    public static Color PRIMARY_DARK = currentTheme.primaryDark();
    public static Color BACKGROUND = currentTheme.background();
    public static Color PANEL_BG = currentTheme.panel();
    public static Color TEXT_PRIMARY = currentTheme.textPrimary();
    public static Color TEXT_MUTED = currentTheme.textMuted();
    public static Color BORDER_COLOR = currentTheme.borderColor();

    public static final Color PRIORITY_HIGH = new Color(248, 113, 113);
    public static final Color PRIORITY_MEDIUM = new Color(96, 165, 250);
    public static final Color PRIORITY_LOW = new Color(148, 163, 184);
    public static final Color PRIORITY_HIGH_SOFT = new Color(254, 226, 226);
    public static final Color PRIORITY_MEDIUM_SOFT = new Color(219, 234, 254);
    public static final Color PRIORITY_LOW_SOFT = new Color(243, 244, 246);

    // 状態色
    public static final Color STATUS_NOT_STARTED = new Color(148, 163, 184);
    public static final Color STATUS_IN_PROGRESS = new Color(59, 130, 246);
    public static final Color STATUS_COMPLETED = new Color(34, 197, 94);
    public static final Color STATUS_NOT_STARTED_SOFT = new Color(243, 244, 246);
    public static final Color STATUS_IN_PROGRESS_SOFT = new Color(219, 234, 254);
    public static final Color STATUS_COMPLETED_SOFT = new Color(220, 252, 231);

    public static Theme getCurrentTheme() { return currentTheme; }
    public static boolean isDarkMode() { return currentTheme == DARK_THEME; }

    private static Theme loadStoredTheme() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(AppTheme.class);
            return prefs.getBoolean(THEME_PREF_KEY, false) ? DARK_THEME : LIGHT_THEME;
        } catch (Exception e) { return LIGHT_THEME; }
    }

    private static void saveThemePreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(AppTheme.class);
            prefs.putBoolean(THEME_PREF_KEY, isDarkMode());
        } catch (Exception ignored) {}
    }

    private static void refreshThemeColors() {
        PRIMARY = currentTheme.primary();
        PRIMARY_DARK = currentTheme.primaryDark();
        BACKGROUND = currentTheme.background();
        PANEL_BG = currentTheme.panel();
        TEXT_PRIMARY = currentTheme.textPrimary();
        TEXT_MUTED = currentTheme.textMuted();
        BORDER_COLOR = currentTheme.borderColor();
    }

    public static void setTheme(Theme theme) {
        if (theme == null) throw new IllegalArgumentException("theme must not be null");
        currentTheme = theme;
        refreshThemeColors();
        applySwingDefaults();
        saveThemePreference();
    }

    public static void setDarkMode(boolean darkMode) { setTheme(darkMode ? DARK_THEME : LIGHT_THEME); }

    public static Color getPriorityColor(String priorityLabel) {
        Priority priority = Priority.fromString(priorityLabel);
        if (isDarkMode()) {
            if (priority == Priority.HIGH) return new Color(248, 113, 113);
            if (priority == Priority.LOW) return new Color(148, 163, 184);
            return new Color(96, 165, 250); // MEDIUM
        }
        if (priority == Priority.HIGH) return PRIORITY_HIGH;
        if (priority == Priority.LOW) return PRIORITY_LOW;
        return PRIORITY_MEDIUM;
    }

    public static Color getStatusColor(String status) {
        TaskStatus s = TaskStatus.fromString(status);
        if (isDarkMode()) {
            if (s == TaskStatus.NOT_STARTED) return new Color(148, 163, 184);
            if (s == TaskStatus.COMPLETED) return new Color(34, 197, 94);
            return new Color(59, 130, 246);
        }
        if (s == TaskStatus.NOT_STARTED) return STATUS_NOT_STARTED;
        if (s == TaskStatus.COMPLETED) return STATUS_COMPLETED;
        return STATUS_IN_PROGRESS;
    }

    public static Color getStatusSoftColor(String status) {
        TaskStatus s = TaskStatus.fromString(status);
        if (s == TaskStatus.NOT_STARTED) return STATUS_NOT_STARTED_SOFT;
        if (s == TaskStatus.COMPLETED) return STATUS_COMPLETED_SOFT;
        return STATUS_IN_PROGRESS_SOFT;
    }

    public static Color getPrioritySoftColor(String priority) {
        Priority p = Priority.fromString(priority);
        if (p == Priority.HIGH) return PRIORITY_HIGH_SOFT;
        if (p == Priority.LOW) return PRIORITY_LOW_SOFT;
        return PRIORITY_MEDIUM_SOFT;
    }

    public static final Font FONT_MAIN = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 15);

    static {
        applySwingDefaults();
    }

    /** 新しく開くダイアログも、現在のテーマで十分なコントラストを持たせます。 */
    private static void applySwingDefaults() {
        UIManager.put("Panel.background", PANEL_BG);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("OptionPane.background", PANEL_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("TextField.background", currentTheme.textFieldBackground());
        UIManager.put("TextField.foreground", currentTheme.textFieldForeground());
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", PANEL_BG);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("List.background", PANEL_BG);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("Table.background", PANEL_BG);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_MAIN);
        UIManager.put("Button.font", FONT_MAIN);
        UIManager.put("OptionPane.messageFont", FONT_MAIN);
    }

    public static Border createRoundedBorder(Color borderColor, int radius, int top, int left, int bottom, int right) {
        return new RoundedBorder(borderColor, radius, top, left, bottom, right);
    }

    public static Border createSoftPanelBorder() { return createRoundedBorder(currentTheme.borderColor(), 12, 2, 2, 2, 2); }

    public static Border createFieldBorder() {
        return new CompoundBorder(createRoundedBorder(currentTheme.borderColor(), 12, 2, 2, 2, 2), new EmptyBorder(4, 10, 4, 10));
    }

    public static Border createFocusedFieldBorder() {
        return new CompoundBorder(createRoundedBorder(currentTheme.primary(), 12, 2, 2, 2, 2), new EmptyBorder(4, 10, 4, 10));
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new RoundedFillButton(text, 12);
        button.putClientProperty("app.button.variant", "primary");
        button.setFont(FONT_HEADER);
        button.setBackground(currentTheme.primary());
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(createRoundedBorder(currentTheme.primaryDark(), 12, 2, 2, 2, 2));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setRolloverEnabled(true);
        return button;
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("app.button.variant", "secondary");
        button.setFont(FONT_MAIN);
        button.setBackground(Color.WHITE);
        button.setForeground(currentTheme.textPrimary());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(createRoundedBorder(currentTheme.borderColor(), 10, 2, 2, 2, 2));
        button.setMargin(new Insets(4, 14, 4, 14));
        button.setRolloverEnabled(true);
        button.setPreferredSize(new Dimension(Math.max(110, button.getFontMetrics(button.getFont()).stringWidth(text) + 28), 36));
        return button;
    }

    public static void styleTextField(JTextField field) {
        field.setFont(FONT_MAIN);
        field.setBackground(currentTheme.textFieldBackground());
        if (isDarkMode()) {
            field.setForeground(new Color(226, 232, 240));
            field.setCaretColor(Color.WHITE);
        } else {
            field.setForeground(currentTheme.textFieldForeground());
            field.setCaretColor(Color.BLACK);
        }
        field.setBorder(createFieldBorder());
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { field.setBorder(createFocusedFieldBorder()); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { field.setBorder(createFieldBorder()); }
        });
        field.putClientProperty("app.styled.textfield", Boolean.TRUE);
    }

    /** 読みやすい矢印と十分なコントラストを持つコンボボックスを適用します。 */
    public static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(FONT_MAIN);
        combo.setBackground(currentTheme.textFieldBackground());
        combo.setForeground(TEXT_PRIMARY);
        combo.setBorder(createRoundedBorder(BORDER_COLOR, 7, 1, 1, 1, 1));
        combo.setUI(new ThemedComboBoxUI());
        combo.putClientProperty("app.styled.combobox", Boolean.TRUE);
    }

    public static class RoundedFillButton extends JButton {
        private final int radius;
        public RoundedFillButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setContentAreaFilled(false);
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getBackground();
                if (getModel().isPressed()) fill = fill.darker();
                else if (getModel().isRollover()) fill = new Color(Math.max(0, fill.getRed() - 10), Math.max(0, fill.getGreen() - 10), Math.max(0, fill.getBlue() - 10));
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            } finally { g2.dispose(); }
            super.paintComponent(g);
        }
    }

    private static final class ThemedComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(BORDER_COLOR);
                        g2.drawLine(0, 4, 0, getHeight() - 5);
                        g2.setColor(TEXT_PRIMARY);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int centerX = getWidth() / 2;
                        int centerY = getHeight() / 2 - 1;
                        g2.drawLine(centerX - 4, centerY - 2, centerX, centerY + 2);
                        g2.drawLine(centerX, centerY + 2, centerX + 4, centerY - 2);
                    } finally {
                        g2.dispose();
                    }
                }
            };
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setOpaque(false);
            button.setPreferredSize(new Dimension(30, 0));
            return button;
        }
    }

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final Insets insets;

        private RoundedBorder(Color color, int radius, int top, int left, int bottom, int right) {
            this.color = color;
            this.radius = radius;
            this.insets = new Insets(top, left, bottom, right);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int thickness = insets.top;
                double offset = thickness / 2.0;
                g2.setStroke(new BasicStroke(thickness));
                g2.drawRoundRect((int)(x + offset), (int)(y + offset), width - thickness, height - thickness, radius, radius);
            } finally { g2.dispose(); }
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(insets.top, insets.left, insets.bottom, insets.right); }
    }
}
