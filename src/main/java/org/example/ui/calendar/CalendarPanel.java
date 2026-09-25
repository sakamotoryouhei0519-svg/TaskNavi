package org.example.ui.calendar;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.SearchablePanel;
import org.example.Task;
import org.example.TaskService;
import org.example.UiConstants;
import org.example.event.TaskEventBus;
import org.example.event.TaskEventListener;
import org.example.ui.ProjectFilterDialog;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【カレンダー画面パネルクラス】
 * 月／週表示切替と日別タスク一覧を持つカレンダービュー。
 */
public class CalendarPanel extends JPanel implements SearchablePanel {

    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CalendarPanel.class);

    public enum ViewMode {
        MONTH,
        WEEK;

        public String getLabel() {
            return this == MONTH
                    ? AppMessages.get("calendar.view.month", "月表示")
                    : AppMessages.get("calendar.view.week", "週表示");
        }
    }

    private final TaskService taskService;
    private final TaskEventListener taskEventListener = event -> {
        logger.info("Calendar: タスク変更イベントを検知しました -> {}", event.getType());
        refreshCalendar();
    };

    private LocalDate currentDate;
    private LocalDate selectedDate;
    private ViewMode currentViewMode = ViewMode.MONTH;

    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;
    private Integer currentProjectFilterId = null;
    private final Map<Integer, Set<Integer>> projectMemberCache = new HashMap<>();

    private Integer selectedTaskId = null;

    private JLabel lblPeriodTitle;
    private JPanel calendarContainer;
    private JPanel dailyTasksContainer;
    private JLabel lblDailyHeader;
    private JLabel lblDailyCountBadge;
    private CalendarNavToolbar.Result navToolbar;
    private JSplitPane mainSplitPane;

    public CalendarPanel(TaskService taskService) {
        this.taskService = taskService;
        this.currentDate = LocalDate.now();
        this.selectedDate = LocalDate.now();

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BACKGROUND);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(UiConstants.WINDOW_WIDTH_MAIN - 380);
        mainSplitPane.setResizeWeight(1.0);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setBorder(null);
        mainSplitPane.setBackground(AppTheme.BACKGROUND);
        mainSplitPane.setDividerSize(6);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBackground(AppTheme.BACKGROUND);
        leftPanel.setBorder(new EmptyBorder(12, 14, 12, 6));
        leftPanel.setMinimumSize(new Dimension(480, 300));

        navToolbar = CalendarNavToolbar.create(new CalendarNavToolbar.Actions() {
            @Override
            public boolean isMonthView() {
                return currentViewMode == ViewMode.MONTH;
            }

            @Override
            public void onPrevious() {
                navigatePeriod(-1);
            }

            @Override
            public void onNext() {
                navigatePeriod(1);
            }

            @Override
            public void onToday() {
                currentDate = LocalDate.now();
                selectedDate = LocalDate.now();
                refreshCalendar();
            }

            @Override
            public void onMonthView() {
                if (currentViewMode != ViewMode.MONTH) {
                    currentViewMode = ViewMode.MONTH;
                    navToolbar.updateViewSwitchStyle(true);
                    refreshCalendar();
                }
            }

            @Override
            public void onWeekView() {
                if (currentViewMode != ViewMode.WEEK) {
                    currentViewMode = ViewMode.WEEK;
                    navToolbar.updateViewSwitchStyle(false);
                    refreshCalendar();
                }
            }
        });
        lblPeriodTitle = navToolbar.periodTitle();
        leftPanel.add(navToolbar.panel(), BorderLayout.NORTH);

        calendarContainer = new JPanel(new BorderLayout());
        calendarContainer.setBackground(AppTheme.BACKGROUND);
        leftPanel.add(calendarContainer, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(AppTheme.BACKGROUND);
        rightPanel.setBorder(new EmptyBorder(12, 6, 12, 14));
        rightPanel.setMinimumSize(new Dimension(300, 300));
        rightPanel.setPreferredSize(new Dimension(350, 600));

        CalendarDailySideSupport.Header dailyHeader = CalendarDailySideSupport.createHeader(
                () -> CalendarTaskEditor.openAddForDate(this, taskService, selectedDate));
        lblDailyHeader = dailyHeader.titleLabel();
        lblDailyCountBadge = dailyHeader.countBadge();
        rightPanel.add(dailyHeader.panel(), BorderLayout.NORTH);

        dailyTasksContainer = new JPanel(new MigLayout("fillx, insets 4 2 12 2, wrap", "[grow,fill]", "[]8"));
        dailyTasksContainer.setBackground(AppTheme.BACKGROUND);

        JScrollPane dailyScroll = new JScrollPane(dailyTasksContainer);
        dailyScroll.setBorder(null);
        dailyScroll.setBackground(AppTheme.BACKGROUND);
        dailyScroll.getViewport().setBackground(AppTheme.BACKGROUND);
        dailyScroll.getVerticalScrollBar().setUnitIncrement(16);
        dailyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightPanel.add(dailyScroll, BorderLayout.CENTER);

        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        add(mainSplitPane, BorderLayout.CENTER);

        refreshCalendar();
        TaskEventBus.getInstance().register(taskEventListener);
    }

    @Override
    public void removeNotify() {
        TaskEventBus.getInstance().unregister(taskEventListener);
        super.removeNotify();
    }

    private void navigatePeriod(int delta) {
        if (currentViewMode == ViewMode.MONTH) {
            currentDate = currentDate.plusMonths(delta);
        } else {
            currentDate = currentDate.plusWeeks(delta);
        }
        refreshCalendar();
    }

    public void refreshCalendar() {
        List<Task> allTasks = CalendarTaskFilter.applyFilters(
                taskService.getAllTasks(),
                currentSearchKeyword,
                currentStatusFilter,
                currentProjectFilterId,
                projectMemberCache);

        updatePeriodTitle();

        calendarContainer.removeAll();
        JComponent viewComponent = currentViewMode == ViewMode.MONTH
                ? CalendarMonthViewBuilder.build(currentDate, selectedDate, allTasks, calendarMonthActions())
                : CalendarWeekViewBuilder.build(
                        currentDate, selectedDate, allTasks, calendarCardActions(), date -> {
                            selectedDate = date;
                            refreshCalendar();
                        });

        JScrollPane scrollPane = new JScrollPane(viewComponent);
        scrollPane.setBorder(null);
        scrollPane.setBackground(AppTheme.BACKGROUND);
        scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        if (currentViewMode == ViewMode.WEEK) {
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        }
        calendarContainer.add(scrollPane, BorderLayout.CENTER);

        CalendarDailySideSupport.updateTasks(
                lblDailyHeader, lblDailyCountBadge, dailyTasksContainer,
                selectedDate, allTasks, calendarCardActions());

        revalidate();
        repaint();
    }

    private void updatePeriodTitle() {
        if (lblPeriodTitle == null) {
            return;
        }
        if (currentViewMode == ViewMode.MONTH) {
            lblPeriodTitle.setText(CalendarPeriodLabels.forMonth(currentDate));
        } else {
            lblPeriodTitle.setText(CalendarPeriodLabels.forWeek(currentDate));
        }
    }

    private CalendarTaskCardFactory.Actions calendarCardActions() {
        return new CalendarTaskCardFactory.Actions() {
            @Override
            public Integer selectedTaskId() {
                return selectedTaskId;
            }

            @Override
            public void onTaskClicked(Task task, boolean doubleClick) {
                selectedTaskId = task.getId();
                if (doubleClick) {
                    CalendarTaskEditor.openEdit(CalendarPanel.this, taskService, task);
                }
            }

            @Override
            public void onStatusChange(Task task, String newStatus) {
                CalendarTaskEditor.changeStatus(CalendarPanel.this, taskService, task, newStatus);
            }

            @Override
            public void refresh() {
                refreshCalendar();
            }
        };
    }

    private CalendarMonthCellFactory.Actions calendarMonthActions() {
        return new CalendarMonthCellFactory.Actions() {
            @Override
            public LocalDate selectedDate() {
                return selectedDate;
            }

            @Override
            public Integer selectedTaskId() {
                return selectedTaskId;
            }

            @Override
            public void onDateSelected(LocalDate date) {
                selectedDate = date;
                refreshCalendar();
            }

            @Override
            public void onTaskClicked(Task task, boolean doubleClick) {
                selectedTaskId = task.getId();
                if (doubleClick) {
                    CalendarTaskEditor.openEdit(CalendarPanel.this, taskService, task);
                }
            }
        };
    }

    @Override
    public void applySearchFilter(String keyword, String status) {
        this.currentSearchKeyword = org.example.util.SearchFilterUtil.normalizeKeyword(keyword);
        this.currentStatusFilter = org.example.util.SearchFilterUtil.normalizeStatus(status);
        refreshCalendar();
    }

    @Override
    public void handleClearFilter() {
        this.currentSearchKeyword = null;
        this.currentStatusFilter = null;
        this.currentProjectFilterId = null;
        this.projectMemberCache.clear();
        refreshCalendar();
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
    public void handleExistingProject() {
        ProjectFilterDialog.Result result = ProjectFilterDialog.show(this, taskService);
        if (!result.confirmed()) {
            return;
        }
        currentProjectFilterId = result.projectId();
        projectMemberCache.clear();
        refreshCalendar();
    }

    @Override
    public void updateTheme() {
        setBackground(AppTheme.BACKGROUND);
        if (lblPeriodTitle != null) {
            lblPeriodTitle.setForeground(AppTheme.TEXT_PRIMARY);
        }
        if (navToolbar != null) {
            navToolbar.updateViewSwitchStyle(currentViewMode == ViewMode.MONTH);
        }
        refreshCalendar();
    }

    @Override
    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return taskService.getTaskById(selectedTaskId);
    }
}
