package org.tasknavi.ui.wbs;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppException;
import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.ErrorDialogUtil;
import org.tasknavi.SearchablePanel;
import org.tasknavi.Task;
import org.tasknavi.TaskBusinessRules;
import org.tasknavi.TaskHierarchyUtil;
import org.tasknavi.TaskService;
import org.tasknavi.UiConstants;
import org.tasknavi.UiLabels;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.ui.ProjectFilterDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【WBS画面パネルクラス】
 * 左側にツリー構造と操作ボタン、右側にタスク編集フォームを配置した左右分割UIです。
 */
public class WbsPanel extends JPanel implements SearchablePanel {
    private static final long serialVersionUID = 1L;

    private final TaskService taskService;
    private final TaskEventListener taskEventListener = event -> {
        logger.info("WBS: タスク変更イベントを検知しました -> {}", event.getType());
        invalidateTaskCache();
        refreshWbs();
    };
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WbsPanel.class);

    private final JTree wbsTree;
    private final JTextField txtSearchKeyword;
    private final JComboBox<String> comboStatusFilter;
    private final WbsDetailForm detailForm;

    private Integer selectedTaskId = null;
    private Integer selectedRootProjectId = null;
    private Integer currentFilterProjectId = null;
    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;

    private final Map<DefaultMutableTreeNode, Task> nodeTaskMap = new HashMap<>();
    private List<Task> taskCache = new ArrayList<>();
    private TreePath highlightDropPath;

    public WbsPanel(TaskService taskService) {
        this.taskService = taskService;

        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.BACKGROUND);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        txtSearchKeyword = new JTextField(12);
        txtSearchKeyword.setToolTipText(AppMessages.get("wbs.search.tooltip", "タスク名で検索"));
        AppTheme.styleTextField(txtSearchKeyword);
        txtSearchKeyword.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                handleSearch();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                handleSearch();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                handleSearch();
            }
        });

        comboStatusFilter = new JComboBox<>(new String[]{
                AppMessages.FILTER_ALL,
                Task.STATUS_NOT_STARTED,
                Task.STATUS_IN_PROGRESS,
                Task.STATUS_COMPLETED
        });
        comboStatusFilter.setFont(AppTheme.FONT_SMALL);
        comboStatusFilter.setPreferredSize(new Dimension(90, UiConstants.FIELD_HEIGHT_COMPACT));
        AppTheme.styleComboBox(comboStatusFilter);
        UiLabels.installStatusRenderer(comboStatusFilter);
        comboStatusFilter.addActionListener(e -> handleSearch());

        JPanel leftPanel = new JPanel(new MigLayout("fill, insets 0, gap 5", "[grow]", "[grow]"));
        leftPanel.setBackground(AppTheme.PANEL_BG);
        leftPanel.setPreferredSize(new Dimension(UiConstants.PANEL_WIDTH_LEFT, 0));
        wbsTree = new JTree();
        wbsTree.setFont(AppTheme.FONT_MAIN);
        wbsTree.setRowHeight(UiConstants.TREE_ROW_HEIGHT);
        wbsTree.setShowsRootHandles(true);
        wbsTree.setRootVisible(false);
        wbsTree.setCellRenderer(new WbsTreeCellRenderer(new WbsTreeCellRenderer.Context() {
            @Override
            public Task taskFor(DefaultMutableTreeNode node) {
                return nodeTaskMap.get(node);
            }

            @Override
            public TreePath highlightDropPath() {
                return highlightDropPath;
            }
        }));
        wbsTree.addTreeSelectionListener(e -> handleTreeSelection());
        configureTreeDragAndDrop();
        leftPanel.add(new JScrollPane(wbsTree), "grow");

        detailForm = new WbsDetailForm(this::handleSaveTask, this::clearForm);

        JScrollPane rightScroll = new JScrollPane(detailForm.getPanel());
        rightScroll.setBorder(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightScroll);
        splitPane.setDividerLocation(UiConstants.SPLIT_DIVIDER_LOCATION);
        add(splitPane, BorderLayout.CENTER);

        refreshWbs();
        detailForm.updateTheme();

        TaskEventBus.getInstance().register(taskEventListener);
    }

    public void showCreateEntryMenu(Component invoker) {
        showCreateEntryMenu(invoker, true);
    }

    public void showCreateEntryMenu(Component invoker, boolean useSelectedParent) {
        Task selected = selectedTaskId != null ? taskService.getTaskById(selectedTaskId) : null;
        WbsEntryCreator.openFromSelection(
                this,
                taskService,
                selected,
                this::findRootProject,
                useSelectedParent);
    }

    public void refreshWbs() {
        refreshWbs(getCachedTasksSnapshot());
    }

    public void refreshWbs(List<Task> taskSnapshot) {
        WbsTreeModelBuilder.Result built = WbsTreeModelBuilder.build(
                taskSnapshot,
                currentFilterProjectId,
                currentSearchKeyword,
                currentStatusFilter);
        nodeTaskMap.clear();
        nodeTaskMap.putAll(built.getNodeTaskMap());
        wbsTree.setModel(new DefaultTreeModel(built.getRoot()));
        for (int i = 0; i < wbsTree.getRowCount(); i++) {
            wbsTree.expandRow(i);
        }
    }

    private void handleTreeSelection() {
        TreePath path = wbsTree.getSelectionPath();
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Task task = nodeTaskMap.get(selectedNode);
        if (task == null) {
            return;
        }

        selectedTaskId = task.getId();
        Task rootProject = findRootProject(task);
        selectedRootProjectId = rootProject != null ? rootProject.getId() : null;

        Task parentStage = null;
        if (task.getLevel() >= 3 && task.getParentId() != null) {
            parentStage = taskService.getTaskById(task.getParentId());
        }
        detailForm.load(task, rootProject, parentStage);
    }

    private Task findRootProject(Task task) {
        return TaskHierarchyUtil.findRootProject(task, getCachedTasksSnapshot());
    }

    private List<Task> getCachedTasksSnapshot() {
        if (taskCache.isEmpty()) {
            taskCache = taskService.getAllTasks();
            if (taskCache == null) {
                taskCache = new ArrayList<>();
            }
        }
        return new ArrayList<>(taskCache);
    }

    private void invalidateTaskCache() {
        taskCache.clear();
    }

    @Override
    public void handleExistingProject() {
        ProjectFilterDialog.Result result = ProjectFilterDialog.show(this, taskService);
        if (!result.confirmed()) {
            return;
        }
        currentFilterProjectId = result.projectId();
        refreshWbs();
    }

    private void configureTreeDragAndDrop() {
        wbsTree.setDragEnabled(true);
        wbsTree.setDropMode(DropMode.ON_OR_INSERT);
        wbsTree.setAutoscrolls(true);
        wbsTree.setTransferHandler(new WbsTreeTransferHandler(new WbsTreeTransferHandler.Host() {
            @Override
            public Map<DefaultMutableTreeNode, Task> nodeTaskMap() {
                return nodeTaskMap;
            }

            @Override
            public TaskService taskService() {
                return taskService;
            }

            @Override
            public JTree tree() {
                return wbsTree;
            }

            @Override
            public Component dialogParent() {
                return WbsPanel.this;
            }

            @Override
            public void setHighlightDropPath(TreePath path) {
                highlightDropPath = path;
            }

            @Override
            public void clearDropHighlight() {
                WbsPanel.this.clearDropHighlight();
            }
        }));
    }

    private void clearDropHighlight() {
        highlightDropPath = null;
        if (wbsTree != null) {
            wbsTree.repaint();
        }
    }

    public String getCurrentSearchKeyword() {
        return currentSearchKeyword;
    }

    public String getCurrentStatusFilter() {
        return currentStatusFilter;
    }

    @Override
    public void applySearchFilter(String keyword, String status) {
        currentSearchKeyword = org.tasknavi.util.SearchFilterUtil.normalizeKeyword(keyword);
        currentStatusFilter = org.tasknavi.util.SearchFilterUtil.normalizeStatus(status);
        txtSearchKeyword.setText(currentSearchKeyword != null ? currentSearchKeyword : "");
        comboStatusFilter.setSelectedItem(currentStatusFilter != null ? currentStatusFilter : AppMessages.FILTER_ALL);
        handleSearch();
    }

    public void updateTheme() {
        detailForm.updateTheme();
        wbsTree.setBackground(AppTheme.PANEL_BG);
        wbsTree.setForeground(AppTheme.TEXT_PRIMARY);
        wbsTree.repaint();
    }

    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return taskService.getTaskById(selectedTaskId);
    }

    public void deleteSelectedTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this,
                    AppMessages.get("wbs.delete.select", "削除するタスクを左側のツリーから選択してください。"));
            return;
        }

        Task task = taskService.getTaskById(selectedTaskId);
        if (task == null) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                taskService.deleteTask(selectedTaskId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    clearForm();
                    JOptionPane.showMessageDialog(WbsPanel.this,
                            AppMessages.format("message.deleted.named", "「{0}」を削除しました。", task.getName()));
                } catch (AppException e) {
                    ErrorDialogUtil.showError(WbsPanel.this, e.getUserMessage());
                } catch (Exception e) {
                    ErrorDialogUtil.showError(WbsPanel.this,
                            AppMessages.get("mainframe.error.delete", "削除中にエラーが発生しました。"));
                    logger.error("タスク削除時の予期せぬエラー", e);
                }
            }
        };
        worker.execute();
    }

    private void clearForm() {
        selectedTaskId = null;
        selectedRootProjectId = null;
        detailForm.clear();
    }

    private void handleSaveTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this,
                    AppMessages.get("wbs.edit.select", "左側のツリーから編集対象のタスクを選択してください。"));
            return;
        }

        Task task = taskService.getTaskById(selectedTaskId);
        if (task == null) {
            return;
        }

        WbsDetailForm.Values values = detailForm.read();
        LocalDate startDate = WbsFormValidator.parseOptionalDate(values.startDateText());
        LocalDate endDate = WbsFormValidator.parseOptionalDate(values.endDateText());
        if ((values.startDateText() != null && !values.startDateText().trim().isEmpty() && startDate == null)
                || (values.endDateText() != null && !values.endDateText().trim().isEmpty() && endDate == null)) {
            JOptionPane.showMessageDialog(this,
                    AppMessages.get("wbs.error.date.format", "日付の形式は YYYY-MM-DD で入力してください。"));
            return;
        }

        String nameToValidate = WbsFormValidator.nameForLevel(
                task.getLevel(), values.projectName(), values.stageName(), values.taskName());
        Task rootProject = findRootProject(task);
        String validationError = WbsFormValidator.validate(
                nameToValidate, values.startDateText(), values.endDateText(), rootProject);
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, validationError,
                    AppMessages.get("wbs.error.date.range.title", "日付範囲エラー"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedRootProjectId != null && !values.projectName().isEmpty()) {
            Task rootProjectTask = taskService.getTaskById(selectedRootProjectId);
            if (rootProjectTask != null && !rootProjectTask.getName().equals(values.projectName())) {
                rootProjectTask.setName(values.projectName());
                taskService.updateTask(rootProjectTask);
            }
        }

        if (task.getLevel() == 1) {
            task.setName(values.projectName());
        } else if (task.getLevel() == 2) {
            task.setName(values.stageName());
        } else {
            task.setName(values.taskName());
        }

        task.setAssignee(values.assignee());
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setPriority(values.priorityCode());
        task.setStatus(values.statusCode());
        task.setProgress(TaskBusinessRules.resolveProgress(task.getStatusCode(), task.getProgress()));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                taskService.updateTask(task);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(WbsPanel.this,
                            AppMessages.get("wbs.message.updated", "更新しました。"));
                } catch (AppException e) {
                    ErrorDialogUtil.showError(WbsPanel.this, e.getUserMessage());
                } catch (Exception e) {
                    ErrorDialogUtil.showError(WbsPanel.this,
                            AppMessages.get("wbs.error.update", "更新中に予期せぬエラーが発生しました。"));
                    logger.error("タスク保存時の予期せぬエラー", e);
                }
            }
        };
        worker.execute();
    }

    private void handleSearch() {
        String keyword = txtSearchKeyword.getText().trim();
        String status = (String) comboStatusFilter.getSelectedItem();
        currentSearchKeyword = keyword.isEmpty() ? null : keyword;
        currentStatusFilter = AppMessages.isFilterAll(status) ? null : status;
        refreshWbs();
    }

    @Override
    public void handleClearFilter() {
        txtSearchKeyword.setText("");
        comboStatusFilter.setSelectedIndex(0);
        currentFilterProjectId = null;
        currentSearchKeyword = null;
        currentStatusFilter = null;
        refreshWbs();
    }

    @Override
    public void removeNotify() {
        TaskEventBus.getInstance().unregister(taskEventListener);
        super.removeNotify();
    }
}
