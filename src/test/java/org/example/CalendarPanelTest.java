package org.example;

import org.example.ui.calendar.CalendarPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

import org.example.util.DatabaseUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【CalendarPanel 単体テスト】
 * カレンダー画面の初期化、表示切替、フィルタリング、タスク抽出の動作を検証します。
 */
class CalendarPanelTest {

    private TaskDao taskDao;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        Database.initialize();
        taskDao = new TaskDao();
        taskService = new TaskService(taskDao);
    }

    @AfterEach
    void tearDown() {
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM tasks");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCalendarPanelInitializationAndDefaultState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CalendarPanel calendarPanel = new CalendarPanel(taskService);

            // デフォルトで月表示であること
            CalendarPanel.ViewMode mode = getField(calendarPanel, "currentViewMode", CalendarPanel.ViewMode.class);
            assertEquals(CalendarPanel.ViewMode.MONTH, mode);

            // 基準日と選択日が今日であること
            LocalDate currentDate = getField(calendarPanel, "currentDate", LocalDate.class);
            LocalDate selectedDate = getField(calendarPanel, "selectedDate", LocalDate.class);
            assertEquals(LocalDate.now(), currentDate);
            assertEquals(LocalDate.now(), selectedDate);

            // 期間タイトルが現在年・月を含むこと
            JLabel lblPeriodTitle = getField(calendarPanel, "lblPeriodTitle", JLabel.class);
            assertNotNull(lblPeriodTitle);
            assertTrue(lblPeriodTitle.getText().contains(String.valueOf(LocalDate.now().getYear())));
            assertTrue(lblPeriodTitle.getText().contains(String.valueOf(LocalDate.now().getMonthValue())));
        });
    }

    @Test
    void testSearchAndClearFilter() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Task project = new Task(0, "Project-" + unique, null, 1, 0, 0, "未着手", LocalDate.now(), LocalDate.now().plusDays(10));
        int projectId = taskDao.addTask(project);
        Task taskA = new Task(0, "TaskA-" + unique, projectId, 2, 0, 50, "進行中", LocalDate.now(), LocalDate.now().plusDays(3));
        int taskAId = taskDao.addTask(taskA);

        SwingUtilities.invokeAndWait(() -> {
            CalendarPanel calendarPanel = new CalendarPanel(taskService);

            // 検索フィルタ適用
            calendarPanel.applySearchFilter("TaskA-" + unique, Task.STATUS_IN_PROGRESS);
            assertEquals("TaskA-" + unique, calendarPanel.getCurrentSearchKeyword());
            assertEquals(Task.STATUS_IN_PROGRESS, calendarPanel.getCurrentStatusFilter());

            // フィルタクリア
            calendarPanel.handleClearFilter();
            assertNull(calendarPanel.getCurrentSearchKeyword());
            assertNull(calendarPanel.getCurrentStatusFilter());
        });

        taskDao.deleteTask(projectId);
    }

    @Test
    void testThemeUpdate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CalendarPanel calendarPanel = new CalendarPanel(taskService);
            assertDoesNotThrow(calendarPanel::updateTheme);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new AssertionError("Field not found: " + fieldName, e);
        }
    }
}
