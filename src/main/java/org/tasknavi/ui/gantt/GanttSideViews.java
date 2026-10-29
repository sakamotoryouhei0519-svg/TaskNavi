package org.tasknavi.ui.gantt;

import org.tasknavi.model.Task;
import org.tasknavi.model.TaskHierarchyUtil;
import org.tasknavi.ui.AppTheme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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

        Integer hoveredTaskId();

        void setHoveredTaskId(Integer taskId);

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
                    for (int row = 0; row < tasks.size(); row++) {
                        GanttPainter.drawTaskNameCell(
                                g2, tasks.get(row), row * GanttMetrics.ROW_HEIGHT, row,
                                host.selectedTaskId(), host.hoveredTaskId(),
                                GanttMetrics.TASK_NAME_WIDTH, GanttMetrics.ROW_HEIGHT);
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
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = e.getY() / GanttMetrics.ROW_HEIGHT;
                java.util.List<Task> tasks = host.visibleTasks();
                if (row < 0 || row >= tasks.size()) {
                    host.setHoveredTaskId(null);
                } else {
                    host.setHoveredTaskId(tasks.get(row).getId());
                }
            }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                host.setHoveredTaskId(null);
            }
        });
        host.registerTaskNameColumn(panel);
        return panel;
    }

    /** 名前の列の上（日付の見出しの左）の角。見出しと同じ地にして「タスク」と出す。 */
    public static JComponent createCorner() {
        return new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(AppTheme.PANEL_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(AppTheme.BORDER_COLOR);
                    g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                    g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(AppTheme.FONT_BOLD);
                    g2.setColor(AppTheme.TEXT_MUTED);
                    g2.drawString(org.tasknavi.AppMessages.get("gantt.corner.task", "タスク"), 12, getHeight() - 14);
                } finally {
                    g2.dispose();
                }
            }
        };
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

                    GanttPainter.drawHeader(g2, minDate, 0, days, host.dayWidth(), GanttMetrics.HEADER_HEIGHT,
                            getVisibleRect().x);
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

            @Override
            public void addNotify() {
                super.addNotify();
                // 月の名前は見えている左端について行くので、横に送るたびに全体を描き直す
                if (getParent() instanceof JViewport viewport) {
                    viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
                }
            }
        };
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setOpaque(true);
        host.registerDateHeader(panel);
        return panel;
    }
}
