package org.tasknavi.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.model.Priority;
import org.tasknavi.model.TaskStatus;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * アプリ全体の色・フォント・枠線。ライトとダークの切り替えもここで持つ。
 */
public class AppTheme {

    private static final Logger logger = LoggerFactory.getLogger(AppTheme.class);

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

    // 状態色
    public static final Color STATUS_NOT_STARTED = new Color(148, 163, 184);
    public static final Color STATUS_IN_PROGRESS = new Color(59, 130, 246);
    public static final Color STATUS_COMPLETED = new Color(34, 197, 94);
    public static final Color STATUS_NOT_STARTED_SOFT = new Color(243, 244, 246);
    public static final Color STATUS_IN_PROGRESS_SOFT = new Color(219, 234, 254);
    public static final Color STATUS_COMPLETED_SOFT = new Color(220, 252, 231);

    // 白い文字を載せる塗り（札やボタン）。ライトとダークで同じ。
    public static final Color FILL_SUCCESS = new Color(22, 163, 74);
    public static final Color FILL_INFO = new Color(37, 99, 235);
    public static final Color FILL_DANGER = new Color(220, 38, 38);
    public static final Color FILL_WARNING = new Color(234, 88, 12);
    public static final Color FILL_ACCENT = new Color(147, 51, 234);
    public static final Color FILL_TEAL = new Color(13, 148, 136);

    public static Theme getCurrentTheme() { return currentTheme; }
    public static boolean isDarkMode() { return currentTheme == DARK_THEME; }

    private static Theme loadStoredTheme() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(AppTheme.class);
            return prefs.getBoolean(THEME_PREF_KEY, false) ? DARK_THEME : LIGHT_THEME;
        } catch (SecurityException | IllegalStateException e) {
            logger.warn("前回のテーマを読めないため、ライトで始めます", e);
            return LIGHT_THEME;
        }
    }

    private static void saveThemePreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(AppTheme.class);
            prefs.putBoolean(THEME_PREF_KEY, isDarkMode());
        } catch (SecurityException | IllegalStateException e) {
            logger.warn("テーマを保存できませんでした（次の起動では前のテーマになります）", e);
        }
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

    /** ライトとダークで同じ色。 */
    public static Color getPriorityColor(String priorityLabel) {
        Priority priority = Priority.fromString(priorityLabel);
        if (priority == Priority.HIGH) return PRIORITY_HIGH;
        if (priority == Priority.LOW) return PRIORITY_LOW;
        return PRIORITY_MEDIUM;
    }

    /** ライトとダークで同じ色。 */
    public static Color getStatusColor(String status) {
        TaskStatus s = TaskStatus.fromString(status);
        if (s == TaskStatus.NOT_STARTED) return STATUS_NOT_STARTED;
        if (s == TaskStatus.COMPLETED) return STATUS_COMPLETED;
        return STATUS_IN_PROGRESS;
    }

    /** 状態を文字の色で見せるとき。塗り用の {@link #getStatusColor} は白い地では薄すぎる。 */
    public static Color statusText(String status) {
        TaskStatus s = TaskStatus.fromString(status);
        if (s == TaskStatus.NOT_STARTED) return textDisabled();
        if (s == TaskStatus.COMPLETED) return success();
        return isDarkMode() ? PRIMARY : PRIMARY_DARK;
    }

    public static Color getStatusSoftColor(String status) {
        TaskStatus s = TaskStatus.fromString(status);
        if (s == TaskStatus.NOT_STARTED) return STATUS_NOT_STARTED_SOFT;
        if (s == TaskStatus.COMPLETED) return STATUS_COMPLETED_SOFT;
        return STATUS_IN_PROGRESS_SOFT;
    }

    public static Color hoverFill() {
        return isDarkMode() ? new Color(51, 65, 85) : new Color(241, 245, 249);
    }

    public static Color selectedFill() {
        return isDarkMode() ? new Color(30, 64, 110) : new Color(219, 234, 254);
    }

    /** カード（カンバン・カレンダー・今日・重なった変更）の地。 */
    public static Color cardBackground() {
        return isDarkMode() ? new Color(42, 50, 61) : Color.WHITE;
    }

    /** ボタン・検索欄・カレンダーのマスなど、操作する部品の地。 */
    public static Color controlBackground() {
        return isDarkMode() ? new Color(30, 41, 59) : Color.WHITE;
    }

    /** タスク名や見出しなど、はっきり読ませる文字。 */
    /** 白い文字を載せる青の塗り。ダークの {@link #PRIMARY} は線や文字用に明るいので、白い文字が読めなくなる。 */
    public static Color primaryFill() {
        return isDarkMode() ? FILL_INFO : PRIMARY;
    }

    public static Color textStrong() {
        return isDarkMode() ? new Color(226, 232, 240) : TEXT_PRIMARY;
    }

    /** 押せない部品の文字。 */
    public static Color textDisabled() {
        return isDarkMode() ? new Color(148, 163, 184) : new Color(100, 116, 139);
    }

    /** 期限切れ・削除・入力の誤り。 */
    public static Color danger() {
        return isDarkMode() ? new Color(248, 113, 113) : new Color(220, 38, 38);
    }

    /** 今日まで・アプリの更新が必要など、気づいてほしいもの。 */
    public static Color warning() {
        return isDarkMode() ? new Color(251, 146, 60) : new Color(234, 88, 12);
    }

    /** 同期できている、など。 */
    public static Color success() {
        return isDarkMode() ? new Color(74, 222, 128) : new Color(22, 163, 74);
    }

    /** 進捗バーの、まだ進んでいない部分。 */
    public static Color progressTrack() {
        return isDarkMode() ? new Color(51, 65, 85) : new Color(226, 232, 240);
    }

    /** ガントのバーの、まだ進んでいない部分。ダークでは淡い色だと地から浮いて眩しいので、状態の色を暗く混ぜる。 */
    public static Color ganttBarTrack(String status) {
        if (!isDarkMode()) {
            return getStatusSoftColor(status);
        }
        Color base = getStatusColor(status);
        Color panel = PANEL_BG;
        float mix = 0.35f;
        return new Color(
                Math.round(base.getRed() * mix + panel.getRed() * (1 - mix)),
                Math.round(base.getGreen() * mix + panel.getGreen() * (1 - mix)),
                Math.round(base.getBlue() * mix + panel.getBlue() * (1 - mix)));
    }

    /** ガントの1行おきの地。 */
    public static Color ganttStripe() {
        return isDarkMode() ? new Color(37, 48, 63) : new Color(243, 246, 250);
    }

    /** ガントの土日の列に重ねる色（透明）。1行おきの地の上でも見分けられる。 */
    public static Color ganttWeekend() {
        return isDarkMode() ? new Color(0, 0, 0, 55) : new Color(100, 116, 139, 26);
    }

    /** ガントの今日の列に重ねる色（透明）。 */
    public static Color ganttToday() {
        return isDarkMode() ? new Color(96, 165, 250, 40) : new Color(59, 130, 246, 28);
    }

    /** ガントの日の区切り。月の区切りは {@link #ganttMonthLine}。 */
    public static Color ganttDayLine() {
        return isDarkMode() ? new Color(51, 65, 85) : new Color(226, 232, 240);
    }

    public static Color ganttMonthLine() {
        return isDarkMode() ? new Color(100, 116, 139) : new Color(148, 163, 184);
    }

    /** お知らせ（情報）の地と文字。 */
    public static Color infoBackground() {
        return isDarkMode() ? new Color(30, 58, 95) : new Color(219, 234, 254);
    }

    public static Color infoForeground() {
        return isDarkMode() ? new Color(226, 232, 240) : new Color(15, 23, 42);
    }

    /** お知らせ（エラー）の地と文字。 */
    public static Color errorBackground() {
        return isDarkMode() ? new Color(127, 29, 29) : new Color(254, 226, 226);
    }

    public static Color errorForeground() {
        return isDarkMode() ? new Color(254, 226, 226) : new Color(127, 29, 29);
    }

    /** カレンダー・ガント・日付選択の日曜と土曜の文字。 */
    public static Color sundayText() {
        return danger();
    }

    public static Color saturdayText() {
        return isDarkMode() ? new Color(96, 165, 250) : new Color(37, 99, 235);
    }

    public static final Font FONT_MAIN = font(Font.PLAIN, 13);
    public static final Font FONT_SMALL = font(Font.PLAIN, 12);
    public static final Font FONT_BOLD = font(Font.BOLD, 13);
    public static final Font FONT_TITLE = font(Font.BOLD, 22);
    public static final Font FONT_HEADER = font(Font.BOLD, 15);

    /** 画面の文字の書体。英語などで起動しても、日本語をゴシック体で描く（{@link AppFonts}）。 */
    public static Font font(int style, int size) {
        return AppFonts.font(style, size);
    }

    static {
        applySwingDefaults();
    }

    /** 新しく開くダイアログも、現在のテーマで十分なコントラストを持たせます。 */
    public static void applySwingDefaults() {
        AppFonts.applyToDefaults();
        Color buttonFace = controlBackground();
        UIManager.put("Panel.background", PANEL_BG);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Button.background", buttonFace);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(4, 12, 4, 12)));
        UIManager.put("ToggleButton.background", buttonFace);
        UIManager.put("ToggleButton.foreground", TEXT_PRIMARY);
        UIManager.put("OptionPane.background", PANEL_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("TextField.background", currentTheme.textFieldBackground());
        UIManager.put("TextField.foreground", currentTheme.textFieldForeground());
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", PANEL_BG);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.disabledBackground", PANEL_BG);
        UIManager.put("ComboBox.disabledForeground", textDisabled());
        UIManager.put("List.background", PANEL_BG);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("Table.background", PANEL_BG);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_MAIN);
        UIManager.put("Button.font", FONT_MAIN);
        UIManager.put("OptionPane.messageFont", FONT_MAIN);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", BACKGROUND);
        UIManager.put("ScrollPane.border", new javax.swing.plaf.BorderUIResource(new ThemeLineBorder()));
        ModernScrollBarUI.install();
        ModernSplitPaneUI.install();
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

    public static void styleDialogPrimaryButton(AbstractButton button) {
        AppThemeControls.styleDialogPrimaryButton(button);
    }

    public static void styleDialogSecondaryButton(AbstractButton button) {
        AppThemeControls.styleDialogSecondaryButton(button);
    }

    public static void styleSegmentButton(AbstractButton button, boolean selected) {
        AppThemeControls.styleSegmentButton(button, selected);
    }

    public static void styleTextField(JTextField field) {
        AppThemeControls.styleTextField(field);
    }

    /** 入力欄の背景・文字・キャレット・選択色をテーマに合わせます（枠線は変えません）。 */
    public static void applyTextColors(javax.swing.text.JTextComponent component) {
        AppThemeControls.applyTextColors(component);
    }

    public static JScrollPane createStyledTextAreaScroll(JTextArea area) {
        return AppThemeControls.createStyledTextAreaScroll(area);
    }

    public static void styleComboBox(JComboBox<?> combo) {
        AppThemeControls.styleComboBox(combo);
    }

    /** 1px の枠。色は描くたびに取るので、テーマを切り替えても作り直さなくてよい。 */
    static final class ThemeLineBorder extends javax.swing.border.AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(BORDER_COLOR);
            g.drawRect(x, y, width - 1, height - 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }

    public static class RoundedFillButton extends AppThemeControls.RoundedFillButton {
        public RoundedFillButton(String text, int radius) {
            super(text, radius);
        }
    }
}
