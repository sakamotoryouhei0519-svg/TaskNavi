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
import org.tasknavi.ui.TaskStatusChanger;
import org.tasknavi.ui.WidthTrackingPanel;
import org.tasknavi.ui.taskdialog.TaskDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * カンバンタブ。未着手・進行中・完了の3列にカードを並べる。
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

        List<Task> tasks = KanbanTaskFilter.apply(
                taskSnapshot != null ? taskSnapshot : taskService.getAllTasks(),
                currentSearchKeyword,
                currentStatusFilter,
                currentProjectFilterId,
                projectMemberCache);

        int todoCount = 0;
        int inProgressCount = 0;
        int doneCount = 0;

        for (Task task : tasks) {
            JPanel card = KanbanCardFactory.create(task, cardActions);
            switch (KanbanColumnSupport.columnFor(task)) {
                case IN_PROGRESS -> {
                    inProgressColumn.add(card, "wmin 0");
                    inProgressCount++;
                }
                case DONE -> {
                    doneColumn.add(card, "wmin 0");
                    doneCount++;
                }
                default -> {
                    todoColumn.add(card, "wmin 0");
                    todoCount++;
                }
            }
        }

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
            headerBg = new Color(30, 41, 59);
        } else {
            headerBg = AppTheme.getStatusSoftColor(title);
        }
        headerLabel.setBackground(headerBg);
        headerLabel.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
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

                try {
                    String taskIdText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    int taskId = Integer.parseInt(taskIdText);
                    changeTaskStatus(taskId, status);
                    return true;
                } catch (Exception e) {
                    return false;
                }
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
