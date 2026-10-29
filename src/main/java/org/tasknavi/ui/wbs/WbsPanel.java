package org.tasknavi.ui.wbs;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskHierarchyUtil;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.ProjectFilterDialog;
import org.tasknavi.ui.SearchablePanel;
import org.tasknavi.ui.UiConstants;
import org.tasknavi.ui.WidthTrackingPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WBS タブ。左側にツリー、右側に選択中の内容を表示する。編集は選んで Enter（MainFrame の共通の操作）で行う。
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
    private final WbsDetailForm detailForm;
    private final JPanel detailHolder;

    private Integer selectedTaskId = null;
    private int hoverRow = -1;
    private Integer currentFilterProjectId = null;
    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;

    private final Map<DefaultMutableTreeNode, Task> nodeTaskMap = new HashMap<>();
    private List<Task> taskCache;
    private final Set<Integer> expandedTaskIds = new HashSet<>();
    private boolean treeReady;
    private boolean restoringTree;
    private TreePath highlightDropPath;

    public WbsPanel(TaskService taskService) {
        this.taskService = taskService;

        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.BACKGROUND);
        setBorder(new EmptyBorder(10, 10, 10, 10));

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

            @Override
            public int hoverRow() {
                return hoverRow;
            }
        }));
        wbsTree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = wbsTree.getRowForLocation(e.getX(), e.getY());
                if (row != hoverRow) {
                    hoverRow = row;
                    wbsTree.repaint();
                }
            }
        });
        wbsTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (hoverRow != -1) {
                    hoverRow = -1;
                    wbsTree.repaint();
                }
            }
        });
        wbsTree.addTreeSelectionListener(e -> handleTreeSelection());
        configureTreeDragAndDrop();
        leftPanel.add(new JScrollPane(wbsTree), "grow");

        detailForm = new WbsDetailForm();
        detailHolder = new WidthTrackingPanel(new BorderLayout());
        detailHolder.setBackground(AppTheme.PANEL_BG);
        detailHolder.add(detailForm.getPanel(), BorderLayout.NORTH);

        JScrollPane rightScroll = new JScrollPane(detailHolder);
        rightScroll.setBorder(null);
        rightScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.getVerticalScrollBar().setUnitIncrement(72);
        rightScroll.addMouseWheelListener(e -> {
            int delta = (int) Math.round(e.getPreciseWheelRotation() * 100);
            if (delta == 0) {
                delta = e.getWheelRotation() * 100;
            }
            java.awt.Adjustable bar = rightScroll.getVerticalScrollBar();
            bar.setValue(bar.getValue() + delta);
            e.consume();
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightScroll);
        splitPane.setDividerLocation(UiConstants.SPLIT_DIVIDER_LOCATION);
        splitPane.setResizeWeight(0.28);
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
        if (treeReady && wbsTree.getRowCount() > 0) {
            expandedTaskIds.clear();
            expandedTaskIds.addAll(collectExpandedTaskIds());
        }

        WbsTreeModelBuilder.Result built = WbsTreeModelBuilder.build(
                taskSnapshot,
                currentFilterProjectId,
                currentSearchKeyword,
                currentStatusFilter);
        nodeTaskMap.clear();
        nodeTaskMap.putAll(built.getNodeTaskMap());

        restoringTree = true;
        try {
            wbsTree.setModel(new DefaultTreeModel(built.getRoot()));
            if (!treeReady) {
                for (int i = 0; i < wbsTree.getRowCount(); i++) {
                    wbsTree.expandRow(i);
                }
            } else {
                restoreExpansion(built.getRoot(), expandedTaskIds);
            }
            restoreSelection(selectedTaskId);
        } finally {
            restoringTree = false;
        }
        treeReady = true;
        reloadDetail();
    }

    private Set<Integer> collectExpandedTaskIds() {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < wbsTree.getRowCount(); i++) {
            TreePath path = wbsTree.getPathForRow(i);
            if (path == null || !wbsTree.isExpanded(path)) {
                continue;
            }
            Object last = path.getLastPathComponent();
            if (last instanceof DefaultMutableTreeNode node) {
                Task task = nodeTaskMap.get(node);
                if (task != null) {
                    ids.add(task.getId());
                }
            }
        }
        return ids;
    }

    private void restoreExpansion(DefaultMutableTreeNode node, Set<Integer> expandedIds) {
        Task task = nodeTaskMap.get(node);
        if (task != null && expandedIds.contains(task.getId())) {
            wbsTree.expandPath(new TreePath(node.getPath()));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            restoreExpansion((DefaultMutableTreeNode) node.getChildAt(i), expandedIds);
        }
    }

    private void restoreSelection(Integer taskId) {
        if (taskId == null) {
            return;
        }
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) wbsTree.getModel().getRoot();
        DefaultMutableTreeNode node = findNodeByTaskId(root, taskId);
        if (node == null) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        wbsTree.setSelectionPath(path);
        wbsTree.scrollPathToVisible(path);
    }

    private DefaultMutableTreeNode findNodeByTaskId(DefaultMutableTreeNode node, int taskId) {
        Task task = nodeTaskMap.get(node);
        if (task != null && task.getId() == taskId) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode found = findNodeByTaskId((DefaultMutableTreeNode) node.getChildAt(i), taskId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void handleTreeSelection() {
        if (restoringTree) {
            return;
        }
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
        reloadDetail();
    }

    private void reloadDetail() {
        if (selectedTaskId == null) {
            detailForm.clear();
            return;
        }
        Task task = taskService.getTaskById(selectedTaskId);
        if (task == null) {
            detailForm.clear();
            return;
        }
        Task rootProject = findRootProject(task);
        String stageName = "";
        if (task.getLevel() == 2) {
            stageName = task.getName();
        } else if (task.getLevel() >= 3 && task.getParentId() != null) {
            Task parentStage = taskService.getTaskById(task.getParentId());
            stageName = parentStage != null ? parentStage.getName() : "";
        }
        detailForm.show(
                task,
                rootProject != null ? rootProject.getName() : "",
                stageName,
                wbsCodeOfSelection());
    }

    private String wbsCodeOfSelection() {
        TreePath path = wbsTree.getSelectionPath();
        if (path == null) {
            return "";
        }
        String code = "";
        for (int i = 1; i < path.getPathCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(i);
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (parent == null) {
                continue;
            }
            code = WbsTreeModelBuilder.childCode(code, parent.getIndex(node) + 1);
        }
        return code;
    }

    private Task findRootProject(Task task) {
        return TaskHierarchyUtil.findRootProject(task, getCachedTasksSnapshot());
    }

    private List<Task> getCachedTasksSnapshot() {
        if (taskCache == null) {
            List<Task> loaded = taskService.getAllTasks();
            taskCache = loaded != null ? loaded : new ArrayList<>();
        }
        return new ArrayList<>(taskCache);
    }

    private void invalidateTaskCache() {
        taskCache = null;
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
        if (org.tasknavi.util.SearchFilterUtil.isUnchanged(currentSearchKeyword, currentStatusFilter, keyword, status)) {
            return;
        }
        currentSearchKeyword = org.tasknavi.util.SearchFilterUtil.normalizeKeyword(keyword);
        currentStatusFilter = org.tasknavi.util.SearchFilterUtil.normalizeStatus(status);
        refreshWbs();
    }

    public void updateTheme() {
        detailHolder.setBackground(AppTheme.PANEL_BG);
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

    @Override
    public void handleClearFilter() {
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
