package org.tasknavi.ui.gantt;

import org.tasknavi.event.TaskEvent;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskHierarchyUtil;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.ProjectFilterDialog;
import org.tasknavi.ui.SearchablePanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ガントタブ。左に WBS の階層、右に日付と進捗のバーを描き、バーのドラッグで期間を変える。
 * 描画・クリック判定・ドラッグは同じパッケージの Gantt* クラスに分けている。
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
    private boolean centeredOnToday = false;

    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;
    private Integer currentProjectFilterId = null;
    private final GanttRows rows = new GanttRows();

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
        public Map<Integer, List<Task>> childrenMap() {
            return getChildrenMap();
        }

        @Override
        public Integer selectedTaskId() {
            return selectedTaskId;
        }

        @Override
        public Integer hoveredTaskId() {
            return hoveredTask == null ? null : hoveredTask.getId();
        }

        @Override
        public void setHoveredTaskId(Integer taskId) {
            Task match = null;
            if (taskId != null) {
                for (Task task : getVisibleTasks()) {
                    if (taskId.equals(task.getId())) {
                        match = task;
                        break;
                    }
                }
            }
            setHoveredTask(match);
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
                GanttPanel.this.setHoveredTask(task);
            }

            @Override
            public void setHoveredTaskAtY(int y) {
                List<Task> tasks = getVisibleTasks();
                int row = y / GanttMetrics.ROW_HEIGHT;
                Task task = row >= 0 && row < tasks.size() ? tasks.get(row) : null;
                GanttPanel.this.setHoveredTask(task);
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

        setBackground(AppTheme.PANEL_BG);
        setPreferredSize(new Dimension(Math.max(800, totalDays * dayWidth), 800));
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && !centeredOnToday) {
                SwingUtilities.invokeLater(this::centerOnToday);
            }
        });

        TaskEventBus.getInstance().register(taskEventListener);
        refreshTaskCache();

        addMouseListener(mouseController);
        addMouseMotionListener(mouseController);
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                dayWidth = GanttMetrics.adjustDayWidth(dayWidth, e.getWheelRotation());
                notifyLayoutChanged();
            } else {
                GanttViewportSupport.scrollParentViewport(this, e);
            }
            e.consume();
        });

        ToolTipManager.sharedInstance().registerComponent(this);
        setToolTipText("");
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // 進み具合の数字は見えている範囲に合わせて動くので、横に送るたびに全体を描き直す
        if (getParent() instanceof JViewport viewport) {
            viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        }
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

    /** 最初に見えたときに一度だけ、今日の列が見えている幅の真ん中に来るよう横に送る。 */
    void centerOnToday() {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        int extent = viewport == null ? 0 : viewport.getExtentSize().width;
        if (extent <= 0) {
            return;
        }
        syncPreferredSize();
        revalidate();
        viewport.getParent().validate();
        long offset = ChronoUnit.DAYS.between(visibleStartDate, LocalDate.now());
        if (offset < 0 || offset >= totalDays) {
            return;
        }
        centeredOnToday = true;
        int x = GanttViewportSupport.centeredScrollX(offset, dayWidth, extent, getPreferredSize().width);
        viewport.setViewPosition(new Point(x, viewport.getViewPosition().y));
    }

    private void onTaskEvent(TaskEvent event) {
        refreshTaskCache();
        notifyLayoutChanged();
    }

    public void setTasks(List<Task> tasks) {
        cachedTasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
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
        LocalDate[] visibleWindow = GanttDisplayDates.calculateVisibleDateWindow(
                tasks, getChildrenMap(), manualVisibleWindow, visibleStartDate, visibleEndDate);
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

    private void setHoveredTask(Task task) {
        Integer before = hoveredTask == null ? null : hoveredTask.getId();
        hoveredTask = task;
        Integer after = hoveredTask == null ? null : hoveredTask.getId();
        if (java.util.Objects.equals(before, after)) {
            return;
        }
        repaint();
        if (taskNameColumnView != null) {
            taskNameColumnView.repaint();
        }
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

        List<Task> tasks = getVisibleTasks();
        Map<Integer, List<Task>> childMap = getChildrenMap();

        LocalDate[] visibleWindow = GanttDisplayDates.calculateVisibleDateWindow(
                tasks, childMap, manualVisibleWindow, visibleStartDate, visibleEndDate);
        LocalDate minDate = visibleWindow[0];
        visibleStartDate = minDate;
        visibleEndDate = visibleWindow[1];
        totalDays = Math.max(GanttMetrics.MIN_VISIBLE_DAYS,
                (int) ChronoUnit.DAYS.between(minDate, visibleWindow[1]) + 1);

        g2.setColor(AppTheme.PANEL_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());
        int rowsHeight = tasks.size() * GanttMetrics.ROW_HEIGHT;
        Rectangle clip = g2.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(0, 0, getWidth(), getHeight());
        }
        int[] shown = GanttBarGeometry.cellRange(clip.y, clip.y + clip.height, GanttMetrics.ROW_HEIGHT, tasks.size());
        GanttPainter.drawRowStripes(g2, shown[0], shown[1], GanttMetrics.ROW_HEIGHT, getWidth());
        Integer hoverId = hoveredTask == null ? null : hoveredTask.getId();
        for (int row = shown[0]; row < shown[1]; row++) {
            Integer id = tasks.get(row).getId();
            boolean selected = selectedTaskId != null && selectedTaskId.equals(id);
            boolean hovered = hoverId != null && hoverId.equals(id);
            if (selected || hovered) {
                g2.setColor(selected ? AppTheme.selectedFill() : AppTheme.hoverFill());
                g2.fillRect(0, row * GanttMetrics.ROW_HEIGHT, getWidth(), GanttMetrics.ROW_HEIGHT);
            }
        }
        GanttPainter.overlayDayColumns(g2, minDate, rowsHeight, totalDays, dayWidth);

        Rectangle visible = getVisibleRect();
        for (int row = shown[0]; row < shown[1]; row++) {
            Task task = tasks.get(row);
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
                    g2, task, minDate, row * GanttMetrics.ROW_HEIGHT, childMap, dayWidth,
                    GanttMetrics.ROW_HEIGHT, GanttMetrics.BAR_HEIGHT,
                    dragging,
                    selectedTaskId != null && selectedTaskId.equals(task.getId()),
                    dragStart, dragEnd,
                    visible.x, visible.x + visible.width);
        }

        GanttPainter.drawTodayLine(g2, minDate, rowsHeight, 0, totalDays, dayWidth);
        g2.dispose();
    }

    private List<Task> getVisibleTasks() {
        if (cachedTasks.isEmpty()) {
            refreshTaskCache();
        }
        rows.update(cachedTasks, currentSearchKeyword, currentStatusFilter, currentProjectFilterId);
        return rows.rows();
    }

    private Map<Integer, List<Task>> getChildrenMap() {
        getVisibleTasks();
        return rows.children();
    }

    private GanttBarHit hitTest(MouseEvent e) {
        if (cachedTasks.isEmpty()) {
            refreshTaskCache();
        }
        if (cachedTasks.isEmpty()) {
            return null;
        }
        List<Task> tasks = getVisibleTasks();
        Map<Integer, List<Task>> childrenMap = getChildrenMap();
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
        return TaskHierarchyUtil.findRootProject(task, cachedTasks);
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
        if (org.tasknavi.util.SearchFilterUtil.isUnchanged(currentSearchKeyword, currentStatusFilter, keyword, status)) {
            return;
        }
        currentSearchKeyword = org.tasknavi.util.SearchFilterUtil.normalizeKeyword(keyword);
        currentStatusFilter = org.tasknavi.util.SearchFilterUtil.normalizeStatus(status);
        notifyLayoutChanged();
    }

    public void clearSearchFilter() {
        currentSearchKeyword = null;
        currentStatusFilter = null;
        currentProjectFilterId = null;
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
        notifyLayoutChanged();
    }

    public JComponent createTaskNameColumn() {
        return GanttSideViews.createTaskNameColumn(sideHost);
    }

    public JComponent createDateHeader() {
        return GanttSideViews.createDateHeader(sideHost);
    }

    public JComponent createCorner() {
        return GanttSideViews.createCorner();
    }

    @Override
    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return taskService.getTaskById(selectedTaskId);
    }

    private void refreshTaskCache() {
        cachedTasks = taskService.getAllTasks();
    }
}
