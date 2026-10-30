package org.tasknavi.ui.kanban;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppException;
import org.tasknavi.AppMessages;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.ErrorDialogUtil;
import org.tasknavi.ui.ProjectFilterDialog;
import org.tasknavi.ui.SearchablePanel;
import org.tasknavi.ui.TaskDragData;
import org.tasknavi.ui.TaskStatusChanger;
import org.tasknavi.ui.WidthTrackingPanel;
import org.tasknavi.ui.taskdialog.TaskDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * カンバンタブ。未着手・進行中・完了の3列にカードを並べる。担当者で絞り込み、列の中を担当者ごとに分けられる。
 */
public class KanbanPanel extends JPanel implements SearchablePanel {
    private final TaskService taskService;
    private final TaskEventListener taskEventListener = event -> {
        logger.info("Kanban: タスク変更イベントを検知しました -> {}", event.getType());
        refreshKanban();
    };
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KanbanPanel.class);

    private static final long serialVersionUID = 1L;

    // --- 検索用コンポーネント ---
    // 現在の検索条件を保持
    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;
    private Integer currentProjectFilterId = null;
    private final Map<Integer, Set<Integer>> projectMemberCache = new HashMap<>();

    // 各ステータスカラム用のカード格納パネル
    private final JPanel todoColumn = createColumnPanel();
    private final JPanel inProgressColumn = createColumnPanel();
    private final JPanel doneColumn = createColumnPanel();
    // 各カラムのヘッダーラベル
    private JLabel todoHeaderLabel;
    private JLabel inProgressHeaderLabel;
    private JLabel doneHeaderLabel;
    // 選択中のタスクID
    private Integer selectedTaskId = null;

    private static final String PREF_BY_ASSIGNEE = "kanban.byAssignee";
    private static final String ALL_ASSIGNEES = "\u0000all";
    private final JComboBox<String> assigneeCombo = new JComboBox<>();
    /** null なら全員。 */
    private String assigneeFilter = null;
    private boolean fillingAssigneeCombo;

    private final KanbanCardFactory.Actions cardActions = new KanbanCardFactory.Actions() {
        @Override
        public Integer selectedTaskId() {
            return selectedTaskId;
        }

        @Override
        public void onSelect(Task task) {
            selectedTaskId = task.getId();
        }

        @Override
        public void onOpenEdit(Task task) {
            openEditDialog(task);
        }

        @Override
        public void onStatusChange(Task task, String newStatus) {
            if (task != null) {
                changeTaskStatus(task.getId(), newStatus);
            }
        }

        @Override
        public void onDrop(int taskId, Component source) {
            String status = KanbanColumnSupport.statusOf(source);
            if (status != null) {
                changeTaskStatus(taskId, status);
            }
        }

        @Override
        public void refresh() {
            refreshKanban();
        }
    };

    /**
     * コンストラクタ：カンバンボードの列レイアウトを構築します。
     */
    public KanbanPanel(TaskService taskService) {
        this.taskService = taskService; // 受け取ったサービスを保存

        setLayout(new BorderLayout(0, 10));
        setBackground(AppTheme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel columnsPanel = new JPanel(new MigLayout(
                "fill, insets 0, gap 12",
                "[grow,fill,0:0][grow,fill,0:0][grow,fill,0:0]",
                "[grow,fill]"));
        columnsPanel.setBackground(AppTheme.BACKGROUND);

        // 3 つのカラム（未着手、進行中、完了）をそれぞれ構築して追加
        JPanel todoWrapper = buildColumnWrapper(Task.STATUS_NOT_STARTED, AppMessages.get("kanban.column.todo"), todoColumn);
        JPanel inProgressWrapper = buildColumnWrapper(Task.STATUS_IN_PROGRESS, AppMessages.get("kanban.column.inprogress"), inProgressColumn);
        JPanel doneWrapper = buildColumnWrapper(Task.STATUS_COMPLETED, AppMessages.get("kanban.column.done"), doneColumn);

        columnsPanel.add(todoWrapper, "grow, push");
        columnsPanel.add(inProgressWrapper, "grow, push");
        columnsPanel.add(doneWrapper, "grow, push");

        add(buildAssigneeBar(), BorderLayout.NORTH);
        add(columnsPanel, BorderLayout.CENTER);

        // データベースからタスクを取得してカードを配置
        refreshKanban();

        TaskEventBus.getInstance().register(taskEventListener);
    }

    @Override
    public void removeNotify() {
        TaskEventBus.getInstance().unregister(taskEventListener);
        super.removeNotify();
    }

    /** 担当者での絞り込みと、列の中を担当者ごとに分ける切り替え。 */
    private JPanel buildAssigneeBar() {
        JPanel bar = new JPanel(new MigLayout("insets 0, gap 8", "[][]16[]"));
        bar.setOpaque(false);
        JLabel label = new JLabel(AppMessages.get("kanban.assignee.label", "担当者") + ":");
        label.setFont(AppTheme.FONT_MAIN);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        AppTheme.styleComboBox(assigneeCombo);
        assigneeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, assigneeLabel((String) value), index,
                        isSelected, cellHasFocus);
            }
        });
        assigneeCombo.addActionListener(e -> {
            if (fillingAssigneeCombo) {
                return;
            }
            Object selected = assigneeCombo.getSelectedItem();
            assigneeFilter = selected == null || ALL_ASSIGNEES.equals(selected) ? null : (String) selected;
            refreshKanban();
        });

        JCheckBox byAssignee = new JCheckBox(AppMessages.get("kanban.byAssignee", "担当者ごとに分ける"), byAssignee());
        byAssignee.setFont(AppTheme.FONT_MAIN);
        byAssignee.setForeground(AppTheme.TEXT_PRIMARY);
        byAssignee.setOpaque(false);
        byAssignee.setFocusable(false);
        byAssignee.setToolTipText(AppMessages.get("kanban.byAssignee.tooltip", "各列の中を担当者ごとに分けて並べます"));
        byAssignee.addActionListener(e -> {
            Preferences.userNodeForPackage(KanbanPanel.class).putBoolean(PREF_BY_ASSIGNEE, byAssignee.isSelected());
            refreshKanban();
        });

        bar.add(label);
        bar.add(assigneeCombo, "wmin 160");
        bar.add(byAssignee);
        return bar;
    }

    private static boolean byAssignee() {
        return Preferences.userNodeForPackage(KanbanPanel.class).getBoolean(PREF_BY_ASSIGNEE, false);
    }

    private static String assigneeLabel(String value) {
        if (value == null || ALL_ASSIGNEES.equals(value)) {
            return AppMessages.get("kanban.assignee.all", "全員");
        }
        return KanbanAssignees.UNASSIGNED.equals(value) ? AppMessages.get("kanban.assignee.none", "担当なし") : value;
    }

    /** 候補を入れ直す。選んでいた人がいなくなったら「全員」に戻す。 */
    private void fillAssigneeCombo(List<Task> allTasks) {
        List<String> names = KanbanAssignees.options(taskService.assigneeOptions(), allTasks);
        List<String> items = new ArrayList<>();
        items.add(ALL_ASSIGNEES);
        items.addAll(names);
        items.add(KanbanAssignees.UNASSIGNED);
        if (assigneeFilter != null && !items.contains(assigneeFilter)) {
            assigneeFilter = null;
        }
        fillingAssigneeCombo = true;
        try {
            assigneeCombo.setModel(new DefaultComboBoxModel<>(items.toArray(String[]::new)));
            assigneeCombo.setSelectedItem(assigneeFilter != null ? assigneeFilter : ALL_ASSIGNEES);
        } finally {
            fillingAssigneeCombo = false;
        }
    }

    private JLabel groupHeader(String assignee, int count) {
        JLabel header = new JLabel(assigneeLabel(assignee) + "  (" + count + ")");
        header.setFont(AppTheme.FONT_BOLD);
        header.setForeground(AppTheme.TEXT_MUTED);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
                new EmptyBorder(0, 2, 2, 2)));
        return header;
    }

    private JPanel columnPanel(KanbanColumnSupport.Column column) {
        return switch (column) {
            case IN_PROGRESS -> inProgressColumn;
            case DONE -> doneColumn;
            default -> todoColumn;
        };
    }

    /** 1列分。見出しと、カードが並ぶスクロール欄。 */
    private JPanel buildColumnWrapper(String status, String title, JPanel cardsContainer) {
        JPanel wrapper = new JPanel(new MigLayout("fill, insets 0, gap 8", "[grow]", "[][grow]"));
        wrapper.setBackground(AppTheme.BACKGROUND);
        wrapper.setMinimumSize(new Dimension(0, 0));
        wrapper.putClientProperty("kanban.status", status);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createSoftPanelBorder(),
                new EmptyBorder(0, 0, 0, 0)
        ));
        cardsContainer.putClientProperty("kanban.status", status);

        JLabel headerLabel = new JLabel(KanbanColumnSupport.formatHeader(title, 0), SwingConstants.CENTER);
        headerLabel.setFont(AppTheme.FONT_HEADER);
        headerLabel.setOpaque(true);
        headerLabel.putClientProperty("kanban.column.status", status);
        updateHeaderLabelTheme(headerLabel, status);
        headerLabel.setBorder(new EmptyBorder(10, 12, 10, 12));

        wrapper.add(headerLabel, "grow, wrap");

        if (Task.STATUS_NOT_STARTED.equals(status)) {
            todoHeaderLabel = headerLabel;
        } else if (Task.STATUS_IN_PROGRESS.equals(status)) {
            inProgressHeaderLabel = headerLabel;
        } else if (Task.STATUS_COMPLETED.equals(status)) {
            doneHeaderLabel = headerLabel;
        }

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(null);
        scrollPane.setBackground(AppTheme.BACKGROUND);
        scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setTransferHandler(createColumnDropHandler(status));
        scrollPane.getViewport().setTransferHandler(createColumnDropHandler(status));

        wrapper.add(scrollPane, "grow");

        cardsContainer.setTransferHandler(createColumnDropHandler(status));
        KanbanColumnSupport.attachDrop(wrapper, createColumnDropHandler(status));
        return wrapper;
    }

    private JPanel createColumnPanel() {
        JPanel panel = new WidthTrackingPanel(new MigLayout("fillx, insets 8 8 10 8, wrap", "[grow,fill]", "[]8"));
        panel.setBackground(AppTheme.BACKGROUND);
        return panel;
    }

    public void refreshKanban() {
        refreshKanban(taskService.getAllTasks());
    }

    public void refreshKanban(List<Task> taskSnapshot) {
        projectMemberCache.clear();
        todoColumn.removeAll();
        inProgressColumn.removeAll();
        doneColumn.removeAll();

        List<Task> allTasks = taskSnapshot != null ? taskSnapshot : taskService.getAllTasks();
        fillAssigneeCombo(allTasks);
        List<Task> tasks = KanbanAssignees.filter(KanbanTaskFilter.apply(
                allTasks,
                currentSearchKeyword,
                currentStatusFilter,
                currentProjectFilterId,
                projectMemberCache), assigneeFilter);

        Map<String, List<Task>> groups = byAssignee()
                ? KanbanAssignees.group(tasks)
                : Collections.singletonMap(null, tasks);
        Map<KanbanColumnSupport.Column, Integer> counts = new EnumMap<>(KanbanColumnSupport.Column.class);
        for (Map.Entry<String, List<Task>> group : groups.entrySet()) {
            Map<KanbanColumnSupport.Column, List<Task>> byColumn = new EnumMap<>(KanbanColumnSupport.Column.class);
            for (Task task : group.getValue()) {
                byColumn.computeIfAbsent(KanbanColumnSupport.columnFor(task), key -> new ArrayList<>()).add(task);
            }
            byColumn.forEach((column, columnTasks) -> {
                JPanel target = columnPanel(column);
                if (group.getKey() != null) {
                    target.add(groupHeader(group.getKey(), columnTasks.size()), "wmin 0, gaptop 4");
                }
                for (Task task : columnTasks) {
                    target.add(KanbanCardFactory.create(task, cardActions), "wmin 0");
                }
                counts.merge(column, columnTasks.size(), Integer::sum);
            });
        }
        int todoCount = counts.getOrDefault(KanbanColumnSupport.Column.TODO, 0);
        int inProgressCount = counts.getOrDefault(KanbanColumnSupport.Column.IN_PROGRESS, 0);
        int doneCount = counts.getOrDefault(KanbanColumnSupport.Column.DONE, 0);

        KanbanColumnSupport.attachDrop(todoColumn, createColumnDropHandler(Task.STATUS_NOT_STARTED));
        KanbanColumnSupport.attachDrop(inProgressColumn, createColumnDropHandler(Task.STATUS_IN_PROGRESS));
        KanbanColumnSupport.attachDrop(doneColumn, createColumnDropHandler(Task.STATUS_COMPLETED));

        if (todoHeaderLabel != null) {
            todoHeaderLabel.setText(KanbanColumnSupport.formatHeader(AppMessages.get("kanban.column.todo"), todoCount));
        }
        if (inProgressHeaderLabel != null) {
            inProgressHeaderLabel.setText(KanbanColumnSupport.formatHeader(AppMessages.get("kanban.column.inprogress"), inProgressCount));
        }
        if (doneHeaderLabel != null) {
            doneHeaderLabel.setText(KanbanColumnSupport.formatHeader(AppMessages.get("kanban.column.done"), doneCount));
        }

        revalidate();
        repaint();
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
        refreshKanban();
    }

    public void clearSearchFilter() {
        currentSearchKeyword = null;
        currentStatusFilter = null;
        currentProjectFilterId = null;
        assigneeFilter = null;
        projectMemberCache.clear();
        refreshKanban();
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
        refreshKanban();
    }

    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return taskService.getTaskById(selectedTaskId);
    }

    private void updateHeaderLabelTheme(JLabel headerLabel, String title) {
        Color headerBg;
        if (AppTheme.isDarkMode()) {
            headerBg = AppTheme.controlBackground();
        } else {
            headerBg = AppTheme.getStatusSoftColor(title);
        }
        headerLabel.setBackground(headerBg);
        headerLabel.setForeground(AppTheme.textStrong());
    }

    public void updateTheme() {
        if (todoHeaderLabel != null) {
            updateHeaderLabelTheme(todoHeaderLabel, Task.STATUS_NOT_STARTED);
        }
        if (inProgressHeaderLabel != null) {
            updateHeaderLabelTheme(inProgressHeaderLabel, Task.STATUS_IN_PROGRESS);
        }
        if (doneHeaderLabel != null) {
            updateHeaderLabelTheme(doneHeaderLabel, Task.STATUS_COMPLETED);
        }
        refreshKanban();
    }

    private TransferHandler createColumnDropHandler(String status) {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop()) {
                    return false;
                }
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                OptionalInt taskId = TaskDragData.taskId(support);
                if (taskId.isEmpty()) {
                    return false;
                }
                changeTaskStatus(taskId.getAsInt(), status);
                return true;
            }
        };
    }

    private void changeTaskStatus(int taskId, String newStatus) {
        TaskStatusChanger.change(this, taskService, taskId, newStatus);
    }

    private void openEditDialog(Task task) {
        java.awt.Frame parentFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        if (parentFrame == null) return;

        TaskDialog dialog = new TaskDialog(parentFrame, taskService, task);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            task.setName(dialog.getTaskName());
            task.setAssignee(dialog.getAssignee());
            task.setMemo(dialog.getMemo());
            task.setProgress(dialog.getProgress());
            task.setPriority(dialog.getPriority());
            task.setStartDate(LocalDate.parse(dialog.getStartDate()));
            task.setEndDate(LocalDate.parse(dialog.getEndDate()));

            task.setStatus(org.tasknavi.util.TaskStatusCycle.fromProgress(task.getProgress()));

            try {
                taskService.updateTask(task);
            } catch (AppException e) {
                ErrorDialogUtil.showError(this, e.getUserMessage());
            }
        }
    }
}
