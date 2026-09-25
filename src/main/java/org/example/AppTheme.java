package org.example;

import javax.swing.*;
import javax.swing.border.Border;
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
        return AppThemeControls.createRoundedBorder(borderColor, radius, top, left, bottom, right);
    }

    public static Border createSoftPanelBorder() {
        return AppThemeControls.createSoftPanelBorder();
    }

    public static Border createFieldBorder() {
        return AppThemeControls.createFieldBorder();
    }

    public static Border createFocusedFieldBorder() {
        return AppThemeControls.createFocusedFieldBorder();
    }

    public static JButton createPrimaryButton(String text) {
        return AppThemeControls.createPrimaryButton(text);
    }

    public static JButton createSecondaryButton(String text) {
        return AppThemeControls.createSecondaryButton(text);
    }

    public static void styleTextField(JTextField field) {
        AppThemeControls.styleTextField(field);
    }

    public static void styleComboBox(JComboBox<?> combo) {
        AppThemeControls.styleComboBox(combo);
    }

    public static class RoundedFillButton extends AppThemeControls.RoundedFillButton {
        public RoundedFillButton(String text, int radius) {
            super(text, radius);
        }
    }
}
