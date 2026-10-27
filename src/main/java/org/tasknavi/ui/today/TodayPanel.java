package org.tasknavi.ui.today;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppMessages;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Task;
import org.tasknavi.service.DueTasks;
import org.tasknavi.service.TaskService;
import org.tasknavi.service.TaskSource;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.ProjectFilterDialog;
import org.tasknavi.ui.SearchablePanel;
import org.tasknavi.ui.TaskEditHelper;
import org.tasknavi.ui.TaskStatusChanger;
import org.tasknavi.util.SearchFilterUtil;
import org.tasknavi.util.TaskViewFilter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * 期限切れ・今日まで・7日以内の未完了タスクを並べる画面。
 * 自分だけのタスクと、最近開いた共有プロジェクトのタスクをまとめて出すので、毎朝ここを開けば今日やることが分かる。
 * 共有プロジェクトは、初めは自分の担当だけを出す（右上の切り替えで全員分にできる）。
 */
public class TodayPanel extends JPanel implements SearchablePanel {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TodayPanel.class);
    private static final Color OVERDUE_COLOR = new Color(220, 38, 38);
    private static final Color TODAY_COLOR = new Color(234, 88, 12);
    private static final String PREF_ONLY_MINE = "today.onlyMine";

    /** 1 行分。ID はプロジェクトごとの番号なので、どのプロジェクトのタスクかと組で扱う。 */
    private record Entry(TaskSource source, Task task, Task parent) {
    }

    private final TaskService taskService;
    private final transient Supplier<List<TaskSource>> sources;
    private final JPanel listPanel = new JPanel(new MigLayout("fillx, insets 16 20 20 20, wrap", "[grow,fill]", ""));
    private final TaskEventListener taskEventListener = event -> refreshToday();
    private final Map<Integer, Set<Integer>> projectMemberCache = new HashMap<>();

    private String currentSearchKeyword;
    private String currentStatusFilter;
    private Integer currentProjectFilterId;
    private transient TaskService selectedService;
    private Integer selectedTaskId;

    public TodayPanel(TaskService taskService) {
        this(taskService, () -> List.of(new TaskSource(
                AppMessages.get("project.personal", "自分だけのタスク"), true, taskService)));
    }

    /** @param sources 並べるプロジェクト。今選んでいるものを含める */
    public TodayPanel(TaskService taskService, Supplier<List<TaskSource>> sources) {
        this.taskService = taskService;
        this.sources = sources;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BACKGROUND);

        listPanel.setBackground(AppTheme.BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        refreshToday();
        TaskEventBus.getInstance().register(taskEventListener);
    }

    @Override
    public void removeNotify() {
        TaskEventBus.getInstance().unregister(taskEventListener);
        super.removeNotify();
    }

    public void refreshToday() {
        LocalDate today = LocalDate.now();
        List<TaskSource> all = sources.get();
        boolean showProject = all.size() > 1;
        List<Entry> overdue = new ArrayList<>();
        List<Entry> dueToday = new ArrayList<>();
        List<Entry> upcoming = new ArrayList<>();
        for (TaskSource source : all) {
            collect(source, today, overdue, dueToday, upcoming);
        }
        Comparator<Entry> order = Comparator.comparing(Entry::task, DueTasks.ORDER);
        overdue.sort(order);
        dueToday.sort(order);
        upcoming.sort(order);

        listPanel.removeAll();
        listPanel.setBackground(AppTheme.BACKGROUND);

        JLabel title = new JLabel(AppMessages.format("today.title", "今日 {0}",
                today.format(DateTimeFormatter.ofPattern("M/d (E)", AppMessages.getLocale()))));
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        JPanel titleRow = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        titleRow.setOpaque(false);
        titleRow.add(title);
        if (all.stream().anyMatch(source -> source.myName() != null)) {
            titleRow.add(onlyMineToggle());
        }
        listPanel.add(titleRow, "gapbottom 8");

        int shown = 0;
        shown += addSection(AppMessages.get("today.section.overdue", "期限切れ"), OVERDUE_COLOR,
                overdue, today, showProject);
        shown += addSection(AppMessages.get("today.section.today", "今日まで"), TODAY_COLOR,
                dueToday, today, showProject);
        shown += addSection(AppMessages.format("today.section.upcoming", "{0}日以内", DueTasks.UPCOMING_DAYS),
                AppTheme.PRIMARY, upcoming, today, showProject);

        if (shown == 0) {
            JLabel empty = new JLabel(AppMessages.get("today.empty", "期限が近いタスクはありません。"));
            empty.setFont(AppTheme.FONT_MAIN);
            empty.setForeground(AppTheme.TEXT_MUTED);
            listPanel.add(empty, "gaptop 12");
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    /**
     * 1 プロジェクト分を期限で分ける。親子の判定に全件が要るので、検索と絞り込みは全件に当ててから見る。
     * 「既存プロジェクト」の絞り込みは今選んでいるプロジェクトの親タスクで選ぶので、ほかのプロジェクトは出さない。
     */
    private void collect(TaskSource source, LocalDate today,
                         List<Entry> overdue, List<Entry> dueToday, List<Entry> upcoming) {
        if (currentProjectFilterId != null && !source.current()) {
            return;
        }
        List<Task> tasks;
        try {
            tasks = source.service().getAllTasks();
        } catch (RuntimeException e) {
            logger.warn("「{}」のタスクを読めませんでした", source.name(), e);
            return;
        }
        Set<Integer> visibleIds = visibleIds(tasks, source.current() ? currentProjectFilterId : null);
        Map<Integer, Task> byId = new HashMap<>();
        for (Task task : tasks) {
            byId.put(task.getId(), task);
        }
        DueTasks due = DueTasks.of(tasks, today, onlyMine() ? source::isMine : task -> true);
        Function<Task, Entry> entry = task -> new Entry(source, task, byId.get(task.getParentId()));
        due.overdue().stream().filter(task -> visibleIds.contains(task.getId())).map(entry).forEach(overdue::add);
        due.dueToday().stream().filter(task -> visibleIds.contains(task.getId())).map(entry).forEach(dueToday::add);
        due.upcoming().stream().filter(task -> visibleIds.contains(task.getId())).map(entry).forEach(upcoming::add);
    }

    private Set<Integer> visibleIds(List<Task> allTasks, Integer projectFilterId) {
        projectMemberCache.clear();
        Set<Integer> ids = new HashSet<>();
        for (Task task : TaskViewFilter.apply(allTasks, currentSearchKeyword, currentStatusFilter,
                projectFilterId, projectMemberCache)) {
            ids.add(task.getId());
        }
        return ids;
    }

    private int addSection(String title, Color accent, List<Entry> entries, LocalDate today, boolean showProject) {
        if (entries.isEmpty()) {
            return 0;
        }
        JLabel header = new JLabel(title + "  " + entries.size());
        header.setFont(AppTheme.FONT_HEADER);
        header.setForeground(accent);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, accent),
                new EmptyBorder(4, 2, 4, 2)));
        listPanel.add(header, "gaptop 12, gapbottom 4");

        for (Entry entry : entries) {
            listPanel.add(createRow(entry, today, showProject));
        }
        return entries.size();
    }

    private boolean isSelected(Entry entry) {
        return selectedTaskId != null && selectedService == entry.source().service()
                && selectedTaskId == entry.task().getId();
    }

    private JPanel createRow(Entry entry, LocalDate today, boolean showProject) {
        Task task = entry.task();
        TaskService service = entry.source().service();
        Color baseBg = AppTheme.isDarkMode() ? new Color(42, 50, 61) : Color.WHITE;

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(isSelected(entry) ? AppTheme.selectedFill() : baseBg);
        row.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 10, 1, 1, 1, 1),
                new EmptyBorder(8, 0, 8, 12)));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setToolTipText(tooltip(task));

        JPanel priorityStrip = new JPanel();
        priorityStrip.setBackground(AppTheme.getPriorityColor(task.getPriorityCode()));
        priorityStrip.setPreferredSize(new Dimension(4, 1));
        row.add(priorityStrip, BorderLayout.WEST);

        JLabel nameLabel = new JLabel(task.getName());
        nameLabel.setFont(AppTheme.FONT_HEADER);
        nameLabel.setForeground(AppTheme.TEXT_PRIMARY);
        JPanel nameRow = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        nameRow.setOpaque(false);
        nameRow.add(nameLabel, "wmin 0");
        if (showProject) {
            nameRow.add(projectTag(entry.source()));
        }

        JLabel detailLabel = new JLabel(describe(task, entry.parent(), today));
        detailLabel.setFont(AppTheme.FONT_MAIN);
        detailLabel.setForeground(task.isOverdue() ? OVERDUE_COLOR : AppTheme.TEXT_MUTED);

        JPanel text = new JPanel(new MigLayout("insets 0, gap 2, wrap", "[grow,fill]", ""));
        text.setOpaque(false);
        text.add(nameRow, "wmin 0");
        text.add(detailLabel);
        row.add(text, BorderLayout.CENTER);

        JButton doneButton = new JButton(AppMessages.get("today.button.done", "完了"));
        AppTheme.styleDialogSecondaryButton(doneButton);
        doneButton.setFont(AppTheme.FONT_BOLD);
        doneButton.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                new EmptyBorder(5, 14, 5, 14)));
        doneButton.setFocusable(false);
        doneButton.addActionListener(e ->
                TaskStatusChanger.change(this, service, task.getId(), Task.STATUS_COMPLETED));
        JPanel buttonHolder = new JPanel(new MigLayout("insets 0, aligny center", "[]", "[]"));
        buttonHolder.setOpaque(false);
        buttonHolder.add(doneButton);
        row.add(buttonHolder, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                selectedService = service;
                selectedTaskId = task.getId();
                if (e.getClickCount() == 2) {
                    TaskEditHelper.openAndSave(TodayPanel.this, service, service.getTaskById(task.getId()));
                }
                refreshToday();
            }
        });
        return row;
    }

    /** 共有プロジェクトで、自分の担当（と担当者が空のもの）だけを出すか。期限の通知も同じ設定で数える。 */
    static boolean onlyMine() {
        return Preferences.userNodeForPackage(TodayPanel.class).getBoolean(PREF_ONLY_MINE, true);
    }

    private JCheckBox onlyMineToggle() {
        JCheckBox toggle = new JCheckBox(AppMessages.get("today.onlyMine", "自分の担当だけ"), onlyMine());
        toggle.setFont(AppTheme.FONT_MAIN);
        toggle.setForeground(AppTheme.TEXT_PRIMARY);
        toggle.setOpaque(false);
        toggle.setFocusable(false);
        toggle.setToolTipText(AppMessages.get("today.onlyMine.tooltip",
                "共有プロジェクトでは、担当者が自分の表示名か空のタスクだけを出します"));
        toggle.addActionListener(e -> {
            Preferences.userNodeForPackage(TodayPanel.class).putBoolean(PREF_ONLY_MINE, toggle.isSelected());
            refreshToday();
        });
        return toggle;
    }

    private static JLabel projectTag(TaskSource source) {
        JLabel tag = new JLabel(source.name());
        tag.setFont(AppTheme.FONT_SMALL);
        tag.setForeground(source.current() ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED);
        tag.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(source.current() ? AppTheme.PRIMARY : AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                new EmptyBorder(0, 6, 0, 6)));
        return tag;
    }

    private static String tooltip(Task task) {
        String hint = AppMessages.get("today.tooltip.row", "ダブルクリックで編集");
        String memo = task.getMemo();
        if (memo.isBlank()) {
            return hint;
        }
        String escaped = memo.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html>" + escaped + "<br><br><i>" + hint + "</i></html>";
    }

    private static String describe(Task task, Task parent, LocalDate today) {
        StringBuilder sb = new StringBuilder();
        if (parent != null && parent.getName() != null) {
            sb.append(parent.getName()).append("  ·  ");
        }
        String assignee = task.getAssignee();
        if (assignee != null && !assignee.isBlank()) {
            sb.append(assignee).append("  ·  ");
        }
        sb.append(AppMessages.statusDisplay(task.getStatusCode())).append("  ·  ");
        long days = ChronoUnit.DAYS.between(today, task.getEndDate());
        String endText = task.getEndDate().format(DateTimeFormatter.ofPattern("M/d (E)", AppMessages.getLocale()));
        if (days < 0) {
            sb.append(AppMessages.format("today.detail.late", "{0} 期限・{1}日遅れ", endText, -days));
        } else if (days == 0) {
            sb.append(AppMessages.get("today.detail.today", "今日まで"));
        } else {
            sb.append(AppMessages.format("today.detail.left", "{0} 期限・あと{1}日", endText, days));
        }
        return sb.toString();
    }

    @Override
    public void applySearchFilter(String keyword, String status) {
        if (SearchFilterUtil.isUnchanged(currentSearchKeyword, currentStatusFilter, keyword, status)) {
            return;
        }
        currentSearchKeyword = SearchFilterUtil.normalizeKeyword(keyword);
        currentStatusFilter = SearchFilterUtil.normalizeStatus(status);
        refreshToday();
    }

    @Override
    public void handleClearFilter() {
        currentSearchKeyword = null;
        currentStatusFilter = null;
        currentProjectFilterId = null;
        refreshToday();
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
        refreshToday();
    }

    @Override
    public void updateTheme() {
        setBackground(AppTheme.BACKGROUND);
        refreshToday();
    }

    @Override
    public Task getSelectedTask() {
        TaskService service = selectedService != null ? selectedService : taskService;
        return selectedTaskId != null ? service.getTaskById(selectedTaskId) : null;
    }

    @Override
    public TaskService selectedTaskService() {
        return selectedService;
    }
}
