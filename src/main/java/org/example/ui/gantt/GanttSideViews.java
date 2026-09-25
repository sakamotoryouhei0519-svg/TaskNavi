package org.example.ui.gantt;

import org.example.AppTheme;
import org.example.Task;
import org.example.TaskHierarchyUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * ガントの行ヘッダー（タスク名）と列ヘッダー（日付）。
 */
public final class GanttSideViews {

    public interface Host {
        List<Task> visibleTasks();

        List<Task> cachedTasks();

        Integer selectedTaskId();

        int dayWidth();

        boolean manualVisibleWindow();

        LocalDate visibleStartDate();

        LocalDate visibleEndDate();

        void setVisibleWindow(LocalDate start, LocalDate end);

        int totalDays();

        void setTotalDays(int days);

        void registerTaskNameColumn(JComponent view);

        void registerDateHeader(JComponent view);
    }

    private GanttSideViews() {
    }

    public static JComponent createTaskNameColumn(Host host) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    List<Task> tasks = host.visibleTasks();
                    if (tasks.isEmpty()) {
                        return;
                    }
                    int currentY = 0;
                    for (Task task : tasks) {
                        GanttPainter.drawTaskNameCell(
                                g2, task, currentY, host.selectedTaskId(),
                                GanttMetrics.TASK_NAME_WIDTH, GanttMetrics.ROW_HEIGHT);
                        g2.setColor(new Color(230, 230, 230));
                        g2.drawLine(0, currentY + GanttMetrics.ROW_HEIGHT,
                                GanttMetrics.TASK_NAME_WIDTH, currentY + GanttMetrics.ROW_HEIGHT);
                        currentY += GanttMetrics.ROW_HEIGHT;
                    }
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public Dimension getPreferredSize() {
                int rowCount = host.visibleTasks().size();
                int height = Math.max(200, (rowCount * GanttMetrics.ROW_HEIGHT) + 20);
                return new Dimension(GanttMetrics.TASK_NAME_WIDTH, height);
            }
        };
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setOpaque(true);
        host.registerTaskNameColumn(panel);
        return panel;
    }

    public static JComponent createDateHeader(Host host) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    List<Task> tasks = host.visibleTasks();
                    if (tasks.isEmpty()) {
                        return;
                    }

                    List<Task> allSorted = TaskHierarchyUtil.sortDepthFirst(host.cachedTasks());
                    Map<Integer, List<Task>> childMap = TaskHierarchyUtil.buildChildrenMap(allSorted);
                    LocalDate[] visibleWindow = GanttDisplayDates.calculateVisibleDateWindow(
                            tasks, childMap, host.manualVisibleWindow(),
                            host.visibleStartDate(), host.visibleEndDate());
                    LocalDate minDate = visibleWindow[0];
                    LocalDate maxDate = visibleWindow[1];
                    host.setVisibleWindow(minDate, maxDate);

                    int days = (int) ChronoUnit.DAYS.between(minDate, maxDate) + 1;
                    days = Math.max(GanttMetrics.MIN_VISIBLE_DAYS, days);
                    host.setTotalDays(days);

                    GanttPainter.drawHeader(g2, minDate, 0, days, host.dayWidth(), GanttMetrics.HEADER_HEIGHT);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public Dimension getPreferredSize() {
                List<Task> tasks = host.visibleTasks();
                int days;
                if (tasks.isEmpty()) {
                    days = host.totalDays();
                } else {
                    Map<Integer, List<Task>> childMap = TaskHierarchyUtil.buildChildrenMap(
                            TaskHierarchyUtil.sortDepthFirst(host.cachedTasks()));
                    LocalDate[] visibleWindow = GanttDisplayDates.calculateVisibleDateWindow(
                            tasks, childMap, host.manualVisibleWindow(),
                            host.visibleStartDate(), host.visibleEndDate());
                    days = (int) ChronoUnit.DAYS.between(visibleWindow[0], visibleWindow[1]) + 1;
                    days = Math.max(GanttMetrics.MIN_VISIBLE_DAYS, days);
                }
                return new Dimension(Math.max(800, days * host.dayWidth()), GanttMetrics.HEADER_HEIGHT);
            }
        };
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setOpaque(true);
        host.registerDateHeader(panel);
        return panel;
    }
}
