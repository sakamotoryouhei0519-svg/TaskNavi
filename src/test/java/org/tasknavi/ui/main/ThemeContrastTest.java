package org.tasknavi.ui.main;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tasknavi.DatabaseTestConfig;
import org.tasknavi.persistence.SampleDataUtil;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.AppTheme;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本物のメイン画面を両方のテーマで開き、背景とほぼ同じ色の文字（白に白・黒に黒）がないか全タブで確かめる。
 * テーマを切り替えたあとに古い色が残る部品も、ここで見つかる。
 */
class ThemeContrastTest {

    /** WCAG の「大きな文字・部品」の基準。薄い補足の文字もこれは超えてほしい。 */
    private static final double MIN_CONTRAST = 3.0;

    private static boolean originalDark;

    @BeforeAll
    static void setUp() throws Exception {
        originalDark = AppTheme.isDarkMode();
        DatabaseTestConfig.useIsolatedDatabase();
        SampleDataUtil.insertSampleTasksIfEmpty();
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @AfterAll
    static void restoreTheme() {
        AppTheme.setDarkMode(originalDark);
        AppTheme.applySwingDefaults();
    }

    @Test
    void everyTabIsReadableWhenStartedLight() throws Exception {
        checkStartingIn(false);
    }

    @Test
    void everyTabIsReadableWhenStartedDark() throws Exception {
        checkStartingIn(true);
    }

    private static void checkStartingIn(boolean dark) throws Exception {
        List<String> problems = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            AppTheme.setDarkMode(dark);
            AppTheme.applySwingDefaults();
            MainFrame frame = new MainFrame(TaskService.createDefault());
            try {
                frame.setSize(1400, 860);
                frame.setVisible(true);
                collectAcrossTabs(frame, dark ? "dark" : "light", problems);
                themeToggle(frame).doClick();
                collectAcrossTabs(frame, dark ? "dark→light" : "light→dark", problems);
            } finally {
                frame.dispose();
            }
        });
        assertTrue(problems.isEmpty(), "読みにくい文字があります:\n" + String.join("\n", problems));
    }

    private static void collectAcrossTabs(MainFrame frame, String theme, List<String> problems) {
        JTabbedPane tabs = findTabs(frame.getContentPane());
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setSelectedIndex(i);
            frame.validate();
            collect(frame.getContentPane(), theme + " / " + tabs.getTitleAt(i).trim(), problems);
        }
    }

    private static void collect(Container container, String where, List<String> problems) {
        for (Component child : container.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            String text = textOf(child);
            if (text != null && child.isEnabled()) {
                Color fg = child.getForeground();
                Color bg = effectiveBackground(child);
                double ratio = contrast(fg, bg);
                if (ratio < MIN_CONTRAST) {
                    String line = String.format("%s: %s「%s」 文字%s 背景%s 比%.1f",
                            where, child.getClass().getSimpleName(), text, rgb(fg), rgb(bg), ratio);
                    if (!problems.contains(line)) {
                        problems.add(line);
                    }
                }
            }
            if (child instanceof Container nested) {
                collect(nested, where, problems);
            }
        }
    }

    private static String textOf(Component c) {
        String text = null;
        if (c instanceof JLabel label) {
            text = label.getText();
        } else if (c instanceof AbstractButton button && button.getIcon() == null) {
            text = button.getText();
        } else if (c instanceof JTextComponent field && field.isEditable()) {
            text = field.getText();
        }
        if (text == null || text.isBlank() || c.getWidth() == 0 || c.getHeight() == 0) {
            return null;
        }
        return text.replaceAll("<[^>]+>", "").trim();
    }

    /** 自分で地を塗るボタンは自分の背景、それ以外は地を塗っている一番近い親の背景の上に文字が乗る。 */
    private static Color effectiveBackground(Component c) {
        if (paintsOwnFill(c)
                || c instanceof AbstractButton button && !button.isOpaque() && button.isContentAreaFilled()
                && !(button.getParent() instanceof JTabbedPane)) {
            return c.getBackground();
        }
        Component current = c;
        while (current != null) {
            if (current.isOpaque() || current instanceof JComponent == false) {
                return current.getBackground();
            }
            current = current.getParent();
        }
        return AppTheme.BACKGROUND;
    }

    private static boolean paintsOwnFill(Component c) {
        for (Class<?> type = c.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getSimpleName().equals("RoundedFillButton")) {
                return true;
            }
        }
        return false;
    }

    private static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static String rgb(Color c) {
        return c == null ? "null" : "(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")";
    }

    private static JTabbedPane findTabs(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JTabbedPane tabs) {
                return tabs;
            }
            if (child instanceof Container nested) {
                JTabbedPane found = findTabs(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JToggleButton themeToggle(MainFrame frame) {
        try {
            Field field = MainFrame.class.getDeclaredField("themeToggleButton");
            field.setAccessible(true);
            return (JToggleButton) field.get(frame);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
