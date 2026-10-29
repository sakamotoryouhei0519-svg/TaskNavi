package org.tasknavi.ui.main;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tasknavi.DatabaseTestConfig;
import org.tasknavi.persistence.TaskDao;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.SearchablePanel;
import org.tasknavi.ui.calendar.CalendarPanel;
import org.tasknavi.ui.gantt.GanttPanel;
import org.tasknavi.ui.kanban.KanbanPanel;
import org.tasknavi.ui.today.TodayPanel;
import org.tasknavi.ui.wbs.WbsPanel;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * マージ直後スモーク: MainFrame が5タブを開き、切替・テーマ更新で落ちないこと。
 */
class MainFrameSmokeTest {

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @Test
    void mainFrameOpensTabsAndSurvivesThemeToggle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame(new TaskService(new TaskDao()));
            try {
                JTabbedPane tabs = getField(frame, "mainTabbedPane", JTabbedPane.class);
                assertEquals(5, tabs.getTabCount());

                assertTabContent(tabs, 0, TodayPanel.class);
                assertTabContent(tabs, 1, WbsPanel.class);
                assertTabContent(tabs, 2, KanbanPanel.class);
                assertTabContent(tabs, 3, GanttPanel.class);
                assertTabContent(tabs, 4, CalendarPanel.class);

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
