package org.tasknavi;

import org.tasknavi.ui.calendar.CalendarPanel;
import org.tasknavi.ui.gantt.GanttPanel;
import org.tasknavi.ui.kanban.KanbanPanel;
import org.tasknavi.ui.wbs.WbsPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * マージ直後スモーク: MainFrame が4タブを開き、切替・テーマ更新で落ちないこと。
 */
class MainFrameSmokeTest {

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @Test
    void mainFrameOpensTabsAndSurvivesThemeToggle() throws Exception {
        UserSession.login(new User(0, "smoke", "smoke@example.com", "USER", "smoke", LocalDateTime.now()));
        try {
            SwingUtilities.invokeAndWait(() -> {
                MainFrame frame = new MainFrame(new TaskService(new TaskDao()));
                try {
                    JTabbedPane tabs = getField(frame, "mainTabbedPane", JTabbedPane.class);
                    assertEquals(4, tabs.getTabCount());

                    assertTabContent(tabs, 0, WbsPanel.class);
                    assertTabContent(tabs, 1, KanbanPanel.class);
                    assertTabContent(tabs, 2, GanttPanel.class);
                    assertTabContent(tabs, 3, CalendarPanel.class);

                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        tabs.setSelectedIndex(i);
                        Component active = unwrap(tabs.getSelectedComponent());
                        assertTrue(active instanceof SearchablePanel, "tab " + i + " should be searchable");
                        ((SearchablePanel) active).applySearchFilter("smoke", null);
                        ((SearchablePanel) active).handleClearFilter();
                    }

                    AppTheme.setDarkMode(!AppTheme.isDarkMode());
                    AppTheme.setDarkMode(!AppTheme.isDarkMode());
                    frame.revalidate();
                    frame.repaint();
                } finally {
                    frame.dispose();
                }
            });
        } finally {
            UserSession.logout();
        }
    }

    private static void assertTabContent(JTabbedPane tabs, int index, Class<?> expectedType) {
        Component content = unwrap(tabs.getComponentAt(index));
        assertInstanceOf(expectedType, content, "tab " + index);
    }

    private static Component unwrap(Component component) {
        if (component instanceof JScrollPane scrollPane) {
            return scrollPane.getViewport().getView();
        }
        return component;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
