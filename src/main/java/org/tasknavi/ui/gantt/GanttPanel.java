package org.tasknavi.ui.gantt;

import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.SearchablePanel;
import org.tasknavi.Task;
import org.tasknavi.TaskHierarchyUtil;
import org.tasknavi.TaskService;
import org.tasknavi.event.TaskEvent;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.ui.ProjectFilterDialog;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【ガントチャート画面パネルクラス】
 * WBS階層表示、日付ヘッダー、ドラッグ操作、進捗バー描画に対応。
 */
public class GanttPanel extends JPanel implements SearchablePanel {

    private static final long serialVersionUID = 1L;

    private final TaskService taskService;
    private final TaskEventListener taskEventListener = this::onTaskEvent;
    private final GanttDragSession dragSession = new GanttDragSession();
    private final GanttMouseController mouseController;

    private int dayWidth = GanttMetrics.DEFAULT_DAY_WIDTH;
    private int totalDays = GanttMetrics.MIN_VISIBLE_DAYS;
    private LocalDate visibleStartDate = LocalDate.now().minusDays(14);
    private LocalDate visibleEndDate = LocalDate.now().plusDays(30);
    private boolean manualVisibleWindow = false;

    private JTextField txtSearchKeyword;
    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;
    private Integer currentProjectFilterId = null;
    private final Map<Integer, Set<Integer>> projectMemberCache = new HashMap<>();

    private Task hoveredTask = null;
    private Integer selectedTaskId = null;
    private List<Task> cachedTasks = new ArrayList<>();

    private JComponent taskNameColumnView;
    private JComponent dateHeaderView;

    private final GanttSideViews.Host sideHost = new GanttSideViews.Host() {
        @Override
        public List<Task> visibleTasks() {
            return getVisibleTasks();
        }

        @Override
        public List<Task> cachedTasks() {
            return cachedTasks;
        }

        @Override
        public Integer selectedTaskId() {
            return selectedTaskId;
        }

        @Override
        public int dayWidth() {
            return dayWidth;
        }

        @Override
        public boolean manualVisibleWindow() {
            return manualVisibleWindow;
        }

        @Override
        public LocalDate visibleStartDate() {
            return visibleStartDate;
        }

        @Override
        public LocalDate visibleEndDate() {
            return visibleEndDate;
        }

        @Override
        public void setVisibleWindow(LocalDate start, LocalDate end) {
            visibleStartDate = start;
            visibleEndDate = end;
        }

        @Override
        public int totalDays() {
            return totalDays;
        }

        @Override
        public void setTotalDays(int days) {
            totalDays = days;
        }

        @Override
        public void registerTaskNameColumn(JComponent view) {
            taskNameColumnView = view;
        }

        @Override
        public void registerDateHeader(JComponent view) {
            dateHeaderView = view;
        }
    };

    public GanttPanel(TaskService taskService) {
        this.taskService = taskService;
        this.mouseController = new GanttMouseController(new GanttMouseController.Host() {
            @Override
            public GanttBarHit hitTest(MouseEvent e) {
                return GanttPanel.this.hitTest(e);
            }

            @Override
            public GanttDragSession dragSession() {
                return dragSession;
            }

            @Override
            public void setSelectedTaskId(Integer taskId) {
                selectedTaskId = taskId;
            }

            @Override
            public void setHoveredTask(Task task) {
                hoveredTask = task;
            }

            @Override
            public void setManualVisibleWindow(boolean manual) {
                manualVisibleWindow = manual;
            }

            @Override
            public LocalDate visibleStartDate() {
                return visibleStartDate;
            }

            @Override
            public LocalDate visibleEndDate() {
                return visibleEndDate;
            }

            @Override
            public void setVisibleWindow(LocalDate start, LocalDate end) {
                visibleStartDate = start;
                visibleEndDate = end;
            }

            @Override
            public int dayWidth() {
                return dayWidth;
            }

            @Override
            public int panelWidth() {
                return getWidth();
            }

            @Override
            public TaskService taskService() {
                return taskService;
            }

            @Override
            public Task resolveRootProjectForDrag(Task task) {
                return GanttPanel.this.resolveRootProjectForDrag(task);
            }

            @Override
            public void repaintPanel() {
                repaint();
            }

            @Override
            public void setCursor(Cursor cursor) {
                GanttPanel.this.setCursor(cursor);
            }

            @Override
            public void setToolTipText(String text) {
                GanttPanel.this.setToolTipText(text);
            }

            @Override
            public java.awt.Component component() {
                return GanttPanel.this;
            }
        });

        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(Math.max(800, totalDays * dayWidth), 800));

        TaskEventBus.getInstance().register(taskEventListener);
        refreshTaskCache();

        addMouseListener(mouseController);
        addMouseMotionListener(mouseController);
        addMouseWheelListener(e -> {
            Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);
            if (p.x >= 0 && p.y >= 0) {
                dayWidth = GanttMetrics.adjustDayWidth(dayWidth, e.getWheelRotation());
                notifyLayoutChanged();
                return;
            }
            GanttViewportSupport.scrollParentViewport(this, e);
        });

        ToolTipManager.sharedInstance().registerComponent(this);
        setToolTipText("");
    }

    private MouseEvent translateEvent(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);
        return new MouseEvent(
                this, e.getID(), e.getWhen(), e.getModifiersEx(),
                p.x, p.y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(translateEvent(e));
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(translateEvent(e));
    }

    private void onTaskEvent(TaskEvent event) {
        refreshTaskCache();
        notifyLayoutChanged();
    }

    public void setTasks(List<Task> tasks) {
        cachedTasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        projectMemberCache.clear();
        manualVisibleWindow = false;
        notifyLayoutChanged();
    }

    @Override
    public void updateTheme() {
        setBackground(AppTheme.BACKGROUND);
        notifyLayoutChanged();
    }

    private void notifyLayoutChanged() {
        syncPreferredSize();
        revalidate();
        repaint();
        if (taskNameColumnView != null) {
            taskNameColumnView.revalidate();
            taskNameColumnView.repaint();
        }
        if (dateHeaderView != null) {
            dateHeaderView.revalidate();
            dateHeaderView.repaint();
        }
    }

    private void syncPreferredSize() {
        List<Task> tasks = getVisibleTasks();
        List<Task> allSortedTasks = TaskHierarchyUtil.sortDepthFirst(cachedTasks);
        Map<Integer, List<Task>> childMap = TaskHierarchyUtil.buildChildrenMap(allSortedTasks);
        LocalDate[] visibleWindow = GanttDisplayDates.calculateVisibleDateWindow(
                tasks, childMap, manualVisibleWindow, visibleStartDate, visibleEndDate);
        int days = Math.max(GanttMetrics.MIN_VISIBLE_DAYS,
                (int) ChronoUnit.DAYS.between(visibleWindow[0], visibleWindow[1]) + 1);
        totalDays = days;
        visibleStartDate = visibleWindow[0];
        visibleEndDate = visibleWindow[1];

        Dimension next = new Dimension(
                Math.max(800, days * dayWidth),
                Math.max(200, (tasks.size() * GanttMetrics.ROW_HEIGHT) + 20));
        if (!next.equals(getPreferredSize())) {
            setPreferredSize(next);
        }
    }

    @Override
    public void removeNotify() {
        TaskEventBus.getInstance().unregister(taskEventListener);
        super.removeNotify();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        return GanttTooltips.forTask(hoveredTask);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (cachedTasks.isEmpty()) {
            refreshTaskCache();
        }
        if (cachedTasks.isEmpty()) {
            g2.dispose();
            return;
        }

        List<Task> allSortedTasks = TaskHierarchyUtil.sortDepthFirst(cachedTasks);
        List<Task> tasks = getVisibleTasks();
        Map<Integer, List<Task>> childMap = TaskHierarchyUtil.buildChildrenMap(allSortedTasks);

        LocalDate[] visibleWindow = GanttDisplayDates.calculateVisibleDateWindow(
                tasks, childMap, manualVisibleWindow, visibleStartDate, visibleEndDate);
        LocalDate minDate = visibleWindow[0];
        visibleStartDate = minDate;
        visibleEndDate = visibleWindow[1];
        totalDays = Math.max(GanttMetrics.MIN_VISIBLE_DAYS,
                (int) ChronoUnit.DAYS.between(minDate, visibleWindow[1]) + 1);

        g2.setColor(AppTheme.PANEL_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());
        GanttPainter.drawDayGrid(g2, minDate, 0, tasks.size() * GanttMetrics.ROW_HEIGHT, totalDays, dayWidth);

        int currentY = 0;
        for (Task task : tasks) {
            boolean dragging = dragSession.isDragging(task);
            LocalDate dragStart = null;
            LocalDate dragEnd = null;
            if (dragging) {
                GanttBarDragMath.Result preview = dragSession.previewDates(dayWidth, this::resolveRootProjectForDrag);
                if (preview != null) {
                    dragStart = preview.getStart();
                    dragEnd = preview.getEnd();
                }
            }
            GanttPainter.drawBar(
                    g2, task, minDate, currentY, childMap, dayWidth,
                    GanttMetrics.ROW_HEIGHT, GanttMetrics.BAR_HEIGHT,
                    dragging,
                    selectedTaskId != null && selectedTaskId.equals(task.getId()),
                    dragStart, dragEnd);
            g2.setColor(new Color(220, 225, 232));
            g2.drawLine(0, currentY + GanttMetrics.ROW_HEIGHT - 1, getWidth(), currentY + GanttMetrics.ROW_HEIGHT - 1);
            currentY += GanttMetrics.ROW_HEIGHT;
        }

        GanttPainter.drawTodayLine(g2, minDate, currentY, 0, totalDays, dayWidth);
        g2.dispose();
    }

    private List<Task> getVisibleTasks() {
        if (cachedTasks.isEmpty()) {
            refreshTaskCache();
        }
        return GanttTaskFilter.visibleTasks(
                cachedTasks, currentSearchKeyword, currentStatusFilter,
                currentProjectFilterId, projectMemberCache);
    }

    private GanttBarHit hitTest(MouseEvent e) {
        if (cachedTasks.isEmpty()) {
            refreshTaskCache();
        }
        if (cachedTasks.isEmpty()) {
            return null;
        }
        List<Task> allSorted = TaskHierarchyUtil.sortDepthFirst(cachedTasks);
        List<Task> tasks = getVisibleTasks();
        Map<Integer, List<Task>> childrenMap = TaskHierarchyUtil.buildChildrenMap(allSorted);
        LocalDate[] visibleWindow = GanttDisplayDates.calculateVisibleDateWindow(
                tasks, childrenMap, manualVisibleWindow, visibleStartDate, visibleEndDate);
        return GanttBarHit.find(
                tasks, childrenMap, visibleWindow[0], e.getX(), e.getY(),
                dayWidth, GanttMetrics.ROW_HEIGHT, GanttMetrics.BAR_HEIGHT, GanttMetrics.RESIZE_HANDLE_WIDTH);
    }

    private Task resolveRootProjectForDrag(Task task) {
        if (task == null || task.getLevel() <= 1) {
            return null;
        }
        return TaskHierarchyUtil.findRootProject(task, taskService.getAllTasks());
    }

    @Override
    public String getCurrentSearchKeyword() {
        return currentSearchKeyword;
    }

    @Override
    public String getCurrentStatusFilter() {
        return currentStatusFilter;
    }

    @Override
    public void applySearchFilter(String keyword, String status) {
        currentSearchKeyword = org.tasknavi.util.SearchFilterUtil.normalizeKeyword(keyword);
        currentStatusFilter = org.tasknavi.util.SearchFilterUtil.normalizeStatus(status);
        if (txtSearchKeyword != null) {
            txtSearchKeyword.setText(currentSearchKeyword != null ? currentSearchKeyword : "");
        }
        notifyLayoutChanged();
    }

    public void clearSearchFilter() {
        currentSearchKeyword = null;
        currentStatusFilter = null;
        currentProjectFilterId = null;
        projectMemberCache.clear();
        if (txtSearchKeyword != null) {
            txtSearchKeyword.setText("");
        }
        notifyLayoutChanged();
    }

    @Override
    public void handleClearFilter() {
        clearSearchFilter();
    }

    @Override
    public void handleExistingProject() {
        ProjectFilterDialog.Result result = ProjectFilterDialog.show(this, taskService);
        if (!result.confirmed()) {
            return;
        }
        currentProjectFilterId = result.projectId();
        projectMemberCache.clear();
        notifyLayoutChanged();
    }

    public JComponent createTaskNameColumn() {
        return GanttSideViews.createTaskNameColumn(sideHost);
    }

    public JComponent createDateHeader() {
        return GanttSideViews.createDateHeader(sideHost);
    }

    @Override
    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return taskService.getTaskById(selectedTaskId);
    }

    public void deleteSelectedTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this,
                    AppMessages.get("gantt.delete.select", "削除するバーを選択してください。"),
                    AppMessages.get("message.delete.blocked.title", "削除できません"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Task task = taskService.getTaskById(selectedTaskId);
        if (task == null) {
            return;
        }

        taskService.deleteTask(selectedTaskId);
        selectedTaskId = null;
        JOptionPane.showMessageDialog(this,
                AppMessages.format("message.deleted.named", "「{0}」を削除しました。", task.getName()),
                AppMessages.get("message.delete.done.title", "削除完了"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTaskCache() {
        cachedTasks = taskService.getAllTasks();
    }
}
