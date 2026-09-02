package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【WBS画面パネルクラス】
 * 左側にツリー構造と操作ボタン、右側にタスク編集フォームを配置した左右分割UIです。
 */
public class WbsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    // --- 左側エリアコンポーネント ---
    private final JTree wbsTree;

    // --- 検索・フィルタ用コンポーネント ---
    private final JTextField txtSearchKeyword;
    private final JComboBox<String> comboStatusFilter;

    // --- 右側フォームコンポーネント ---
    private final JTextField txtProjectName;
    private final JTextField txtStageName;
    private final JTextField txtTaskName;
    private final JTextField txtAssignee;
    private final JTextField txtStartDate;
    private final JTextField txtEndDate;
    private final JComboBox<String> comboPriority;
    private final JRadioButton rbNotStarted;
    private final JRadioButton rbInProgress;
    private final JRadioButton rbCompleted;

    private Integer selectedTaskId = null;
    private Integer selectedRootProjectId = null;
    private Integer currentFilterProjectId = null;
    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;

    private final Map<DefaultMutableTreeNode, Task> nodeTaskMap = new HashMap<>();
    private List<Task> taskCache = new ArrayList<>();
    private TreePath highlightDropPath;

    public WbsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.BACKGROUND);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        txtSearchKeyword = new JTextField(12);
        txtSearchKeyword.setToolTipText("タスク名で検索");
        AppTheme.styleTextField(txtSearchKeyword);
        txtSearchKeyword.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { handleSearch(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { handleSearch(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { handleSearch(); }
        });

        comboStatusFilter = new JComboBox<>(new String[]{"すべて", "未着手", "進行中", "完了"});
        comboStatusFilter.setFont(AppTheme.FONT_SMALL);
        comboStatusFilter.setPreferredSize(new Dimension(90, UiConstants.FIELD_HEIGHT_COMPACT));
        comboStatusFilter.setBackground(Color.WHITE);
        comboStatusFilter.setBorder(AppTheme.createFieldBorder());
        comboStatusFilter.addActionListener(e -> handleSearch());

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(AppTheme.PANEL_BG);
        leftPanel.setPreferredSize(new Dimension(UiConstants.PANEL_WIDTH_LEFT, 0));
        leftPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        wbsTree = new JTree();
        wbsTree.setFont(AppTheme.FONT_MAIN);
        wbsTree.setRowHeight(UiConstants.TREE_ROW_HEIGHT);
        wbsTree.setShowsRootHandles(true);
        wbsTree.setRootVisible(false);

        wbsTree.setCellRenderer(new WbsTreeCellRenderer());
        wbsTree.addTreeSelectionListener(e -> handleTreeSelection());
        wbsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                clearDropHighlight();
            }
        });
        configureTreeDragAndDrop();

        JScrollPane scrollTree = new JScrollPane(wbsTree);
        scrollTree.setBorder(BorderFactory.createEmptyBorder());

        JPanel leftContentPanel = new JPanel(new BorderLayout());
        leftContentPanel.setBackground(AppTheme.PANEL_BG);
        leftContentPanel.add(scrollTree, BorderLayout.CENTER);

        leftPanel.add(leftContentPanel, BorderLayout.CENTER);
        leftPanel.add(Box.createVerticalStrut(0), BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(AppTheme.PANEL_BG);
        rightPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(12, 8, 12, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- プロジェクト名入力欄（高さ 36px）---
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("プロジェクト名"), gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1.0;
        txtProjectName = new JTextField();
        AppTheme.styleTextField(txtProjectName);
        txtProjectName.setPreferredSize(new Dimension(0, 36));
        txtProjectName.setMinimumSize(new Dimension(0, 36));
        formPanel.add(txtProjectName, gbc);

        // --- 工程名入力欄（高さ 36px）---
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("工程名"), gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1.0;
        txtStageName = new JTextField();
        AppTheme.styleTextField(txtStageName);
        txtStageName.setPreferredSize(new Dimension(0, 36));
        txtStageName.setMinimumSize(new Dimension(0, 36));
        formPanel.add(txtStageName, gbc);

        // --- タスク名入力欄（高さ 36px）---
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("タスク名"), gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 1.0;
        txtTaskName = new JTextField();
        AppTheme.styleTextField(txtTaskName);
        txtTaskName.setPreferredSize(new Dimension(0, 36));
        txtTaskName.setMinimumSize(new Dimension(0, 36));
        formPanel.add(txtTaskName, gbc);

        // --- 担当者入力欄（高さ 36px）---
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        formPanel.add(new JLabel("担当者"), gbc);
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 1.0;
        txtAssignee = new JTextField();
        AppTheme.styleTextField(txtAssignee);
        txtAssignee.setPreferredSize(new Dimension(0, 36));
        txtAssignee.setMinimumSize(new Dimension(0, 36));
        formPanel.add(txtAssignee, gbc);

        // --- 優先度・日付 ---
        JPanel priorityDateRow = new JPanel(new GridBagLayout());
        priorityDateRow.setOpaque(false);
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.anchor = GridBagConstraints.NORTHWEST;
        pgbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel priorityCol = new JPanel();
        priorityCol.setLayout(new BoxLayout(priorityCol, BoxLayout.Y_AXIS));
        priorityCol.setOpaque(false);
        JLabel priorityLabel = new JLabel("優先度");
        priorityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        priorityCol.add(priorityLabel);
        priorityCol.add(Box.createVerticalStrut(8));
        comboPriority = new JComboBox<>(Task.PRIORITY_OPTIONS);
        comboPriority.setFont(AppTheme.FONT_MAIN);
        comboPriority.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        comboPriority.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
        comboPriority.setBorder(AppTheme.createFieldBorder());
        comboPriority.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboPriority.setMaximumSize(new Dimension(110, 36));
        comboPriority.setPreferredSize(new Dimension(110, 36));
        comboPriority.setMinimumSize(new Dimension(110, 36));
        priorityCol.add(comboPriority);

        pgbc.gridx = 0; pgbc.gridy = 0; pgbc.weightx = 0; pgbc.insets = new Insets(0, 0, 0, 16);
        priorityDateRow.add(priorityCol, pgbc);

        JPanel dateCol = new JPanel();
        dateCol.setLayout(new BoxLayout(dateCol, BoxLayout.Y_AXIS));
        dateCol.setOpaque(false);
        JLabel dateLabel = new JLabel("日付");
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateCol.add(dateLabel);
        dateCol.add(Box.createVerticalStrut(8));

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.setOpaque(false);
        datePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        datePanel.setPreferredSize(new Dimension(0, 36));

        // --- 開始・終了日付フィールド（高さ 36px）---
        txtStartDate = new JTextField(8);
        AppTheme.styleTextField(txtStartDate);
        txtStartDate.setPreferredSize(new Dimension(110, 36));
        txtStartDate.setMinimumSize(new Dimension(80, 36));

        JButton btnSelectStart = IconManager.getIconButton(IconManager.IconType.CALENDAR);
        btnSelectStart.setPreferredSize(new Dimension(40, 36));
        btnSelectStart.setMargin(new Insets(0, 2, 0, 2));
        btnSelectStart.setToolTipText("開始日を選択");
        btnSelectStart.setFocusPainted(false);
        UiDebugUtil.logButtonState("WbsPanel.startDateButton", btnSelectStart);
        btnSelectStart.addActionListener(e -> showDatePickerDialog(txtStartDate));

        txtEndDate = new JTextField(8);
        AppTheme.styleTextField(txtEndDate);
        txtEndDate.setPreferredSize(new Dimension(110, 36));
        txtEndDate.setMinimumSize(new Dimension(80, 36));

        JButton btnSelectEnd = IconManager.getIconButton(IconManager.IconType.CALENDAR);
        btnSelectEnd.setPreferredSize(new Dimension(40, 36));
        btnSelectEnd.setMargin(new Insets(0, 2, 0, 2));
        btnSelectEnd.setToolTipText("終了日を選択");
        btnSelectEnd.setFocusPainted(false);
        UiDebugUtil.logButtonState("WbsPanel.endDateButton", btnSelectEnd);
        btnSelectEnd.addActionListener(e -> showDatePickerDialog(txtEndDate));

        datePanel.add(txtStartDate);
        datePanel.add(btnSelectStart);
        datePanel.add(new JLabel("〜"));
        datePanel.add(txtEndDate);
        datePanel.add(btnSelectEnd);
        dateCol.add(datePanel);

        pgbc.gridx = 1; pgbc.gridy = 0; pgbc.weightx = 1.0; pgbc.insets = new Insets(0, 0, 0, 0);
        priorityDateRow.add(dateCol, pgbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(priorityDateRow, gbc);

        gbc.gridx = 0; gbc.gridy = 14; gbc.weightx = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("状態を選択"), gbc);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        statusPanel.setOpaque(false);
        rbNotStarted = new JRadioButton("未着手", true);
        rbInProgress = new JRadioButton("進行中");
        rbCompleted = new JRadioButton("完了");

        updateRadioButtonTheme();

        ButtonGroup statusGroup = new ButtonGroup();
        statusGroup.add(rbNotStarted);
        statusGroup.add(rbInProgress);
        statusGroup.add(rbCompleted);

        statusPanel.add(rbNotStarted);
        statusPanel.add(rbInProgress);
        statusPanel.add(rbCompleted);

        gbc.gridx = 0; gbc.gridy = 15; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(statusPanel, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        rightPanel.add(formPanel, BorderLayout.NORTH);

        JPanel rightBottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightBottomPanel.setOpaque(false);
        JButton btnSave = AppTheme.createPrimaryButton(IconManager.getIcon(IconManager.IconType.CHECK) + " 完了");
        btnSave.setPreferredSize(new Dimension(UiConstants.BUTTON_WIDTH_COMPACT, UiConstants.BUTTON_HEIGHT));
        btnSave.addActionListener(e -> handleSaveTask());

        JButton btnCancel = AppTheme.createSecondaryButton(IconManager.getIcon(IconManager.IconType.CROSS) + " キャンセル");
        btnCancel.setPreferredSize(new Dimension(UiConstants.BUTTON_WIDTH_COMPACT, UiConstants.BUTTON_HEIGHT));
        btnCancel.addActionListener(e -> clearForm());

        rightBottomPanel.add(btnSave);
        rightBottomPanel.add(btnCancel);
        rightPanel.add(rightBottomPanel, BorderLayout.SOUTH);

        JScrollPane rightScroll = new JScrollPane(rightPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightScroll.getVerticalScrollBar().setUnitIncrement(UiConstants.SCROLL_UNIT_INCREMENT);
        rightScroll.setBorder(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightScroll);
        splitPane.setDividerLocation(UiConstants.SPLIT_DIVIDER_LOCATION);
        add(splitPane, BorderLayout.CENTER);

        refreshWbs();
        updateRadioButtonTheme();
    }

    private void showDatePickerDialog(JTextField targetField) {
        final LocalDate[] currentDate = {LocalDate.now()};
        try {
            if (!targetField.getText().trim().isEmpty()) {
                currentDate[0] = LocalDate.parse(targetField.getText().trim());
            }
        } catch (Exception ignored) {}

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "日付を選択", true);
        dialog.setLayout(new BorderLayout(5, 5));
        dialog.setSize(UiConstants.DIALOG_WIDTH_SMALL, UiConstants.DIALOG_HEIGHT_SMALL);
        dialog.setLocationRelativeTo(this);

        // テーマカラーの取得
        Color bgColor = AppTheme.isDarkMode() ? AppTheme.PANEL_BG : Color.WHITE;
        Color textColor = AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY;
        Color btnBg = AppTheme.isDarkMode() ? new Color(51, 65, 85) : new Color(240, 240, 240);

        dialog.getContentPane().setBackground(bgColor);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        headerPanel.setOpaque(false);
        JButton btnPrev = new JButton("＜");
        JButton btnNext = new JButton("＞");
        JLabel lblYearMonth = new JLabel("", SwingConstants.CENTER);
        lblYearMonth.setForeground(textColor);
        lblYearMonth.setFont(new Font("SansSerif", Font.BOLD, 14));

        headerPanel.add(btnPrev);
        headerPanel.add(lblYearMonth);
        headerPanel.add(btnNext);

        JPanel calendarPanel = new JPanel(new GridLayout(0, 7, UiConstants.CALENDAR_GRID_GAP, UiConstants.CALENDAR_GRID_GAP));
        calendarPanel.setOpaque(false);

        Runnable updateCalendar = () -> {
            calendarPanel.removeAll();
            lblYearMonth.setText(currentDate[0].getYear() + "年 " + currentDate[0].getMonthValue() + "月");

            String[] days = {"日", "月", "火", "水", "木", "金", "土"};
            for (int i = 0; i < 7; i++) {
                JLabel lblDay = new JLabel(days[i], SwingConstants.CENTER);
                lblDay.setFont(new Font("SansSerif", Font.BOLD, 12));
                lblDay.setForeground(i == 0 ? Color.RED : (i == 6 ? Color.BLUE : textColor));
                calendarPanel.add(lblDay);
            }

            LocalDate firstOfMonth = currentDate[0].withDayOfMonth(1);
            int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
            int lengthOfMonth = currentDate[0].lengthOfMonth();

            for (int i = 0; i < startDayOfWeek; i++) calendarPanel.add(new JLabel(""));

            for (int day = 1; day <= lengthOfMonth; day++) {
                int selectedDay = day;
                JButton btnDay = new JButton(String.valueOf(day));
                btnDay.setBackground(btnBg);
                btnDay.setForeground(textColor);
                btnDay.setBorder(AppTheme.createFieldBorder());
                btnDay.setFocusPainted(false);

                if (currentDate[0].getDayOfMonth() == day) {
                    btnDay.setBackground(AppTheme.PRIMARY);
                    btnDay.setForeground(Color.WHITE);
                }

                btnDay.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { btnDay.setBackground(AppTheme.PRIMARY_DARK); }
                    public void mouseExited(MouseEvent e) {
                        btnDay.setBackground(currentDate[0].getDayOfMonth() == selectedDay ? AppTheme.PRIMARY : btnBg);
                    }
                });

                btnDay.addActionListener(e -> {
                    targetField.setText(currentDate[0].withDayOfMonth(selectedDay).toString());
                    dialog.dispose();
                });
                calendarPanel.add(btnDay);
            }
            calendarPanel.revalidate();
            calendarPanel.repaint();
        };

        btnPrev.addActionListener(e -> { currentDate[0] = currentDate[0].minusMonths(1); updateCalendar.run(); });
        btnNext.addActionListener(e -> { currentDate[0] = currentDate[0].plusMonths(1); updateCalendar.run(); });

        updateCalendar.run();
        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(calendarPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    public void showCreateEntryMenu(Component invoker) {
        showCreateEntryMenu(invoker, true);
    }

    public void showCreateEntryMenu(Component invoker, boolean useSelectedParent) {
        Task selectedParentTask = null;
        if (useSelectedParent && selectedTaskId != null) {
            selectedParentTask = TaskDao.getTaskById(selectedTaskId);
        }

        Integer defaultParentProjectId = null;
        if (selectedParentTask != null) {
            Task rootProject = findRootProject(selectedParentTask);
            if (rootProject != null) {
                defaultParentProjectId = rootProject.getId();
            }
        }

        openSharedTaskDialog("タスク", defaultParentProjectId);
    }

    public void refreshWbs() {
        refreshWbs(getCachedTasksSnapshot());
    }

    public void refreshWbs(List<Task> taskSnapshot) {
        List<Task> baseTasks = taskSnapshot != null ? taskSnapshot : new ArrayList<>();
        if (baseTasks == null) {
            baseTasks = new ArrayList<>();
        }

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : baseTasks) {
            taskMap.put(t.getId(), t);
        }

        Set<Integer> allowedTaskIds = new HashSet<>();
        for (Task task : baseTasks) {
            if (currentFilterProjectId == null || isDescendantOrSelf(task, currentFilterProjectId, taskMap)) {
                allowedTaskIds.add(task.getId());
            }
        }

        List<Task> projectFilteredTasks = new ArrayList<>();
        for (Task task : baseTasks) {
            if (allowedTaskIds.contains(task.getId())) {
                projectFilteredTasks.add(task);
            }
        }

        List<Task> allTasks;
        String keyword = (currentSearchKeyword != null && !currentSearchKeyword.trim().isEmpty())
                ? currentSearchKeyword : null;
        String status = ("すべて".equals(currentStatusFilter) || currentStatusFilter == null)
                ? null : currentStatusFilter;

        if (keyword != null || status != null) {
            allTasks = filterTasksInMemory(projectFilteredTasks, keyword, status, taskMap);
        } else {
            allTasks = projectFilteredTasks;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("全タスクプロジェクト");
        nodeTaskMap.clear();

        taskMap.clear();
        for (Task t : allTasks) {
            taskMap.put(t.getId(), t);
        }

        Map<Integer, List<Task>> childrenMap = new HashMap<>();
        List<Task> rootTasks = new ArrayList<>();

        for (Task task : allTasks) {
            if (task.getParentId() == null || !taskMap.containsKey(task.getParentId())) {
                rootTasks.add(task);
            } else {
                childrenMap.computeIfAbsent(task.getParentId(), k -> new ArrayList<>()).add(task);
            }
        }

        int projectIndex = 1;
        for (Task rootTask : rootTasks) {
            String numberPrefix = projectIndex + ". ";
            DefaultMutableTreeNode projectNode = buildTreeNodeRecursive(rootTask, numberPrefix, childrenMap);
            root.add(projectNode);
            projectIndex++;
        }

        wbsTree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < wbsTree.getRowCount(); i++) {
            wbsTree.expandRow(i);
        }
    }

    private DefaultMutableTreeNode buildTreeNodeRecursive(Task currentTask, String numberPrefix, Map<Integer, List<Task>> childrenMap) {
        String displayText = numberPrefix + currentTask.getName();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(displayText);
        nodeTaskMap.put(node, currentTask);

        List<Task> children = childrenMap.get(currentTask.getId());
        if (children != null) {
            int childIndex = 1;
            for (Task child : children) {
                String childPrefix = numberPrefix.trim() + "." + childIndex + " ";
                DefaultMutableTreeNode childNode = buildTreeNodeRecursive(child, childPrefix, childrenMap);
                node.add(childNode);
                childIndex++;
            }
        }
        return node;
    }

    private boolean isDescendantOrSelf(Task task, int projectId, Map<Integer, Task> taskMap) {
        Task current = task;
        while (current != null) {
            if (current.getId() == projectId) {
                return true;
            }
            if (current.getParentId() == null) {
                break;
            }
            current = taskMap.get(current.getParentId());
        }
        return false;
    }

    private void handleTreeSelection() {
        TreePath path = wbsTree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Task task = nodeTaskMap.get(selectedNode);

        if (task != null) {
            selectedTaskId = task.getId();

            Task rootProject = findRootProject(task);
            selectedRootProjectId = (rootProject != null) ? rootProject.getId() : null;

            txtProjectName.setText(rootProject != null ? rootProject.getName() : "");

            if (task.getLevel() == 1) {
                txtStageName.setText("");
                txtTaskName.setText("");
            } else if (task.getLevel() == 2) {
                txtStageName.setText(task.getName());
                txtTaskName.setText("");
            } else {
                Task parentStage = task.getParentId() != null ? TaskDao.getTaskById(task.getParentId()) : null;
                txtStageName.setText(parentStage != null ? parentStage.getName() : "");
                txtTaskName.setText(task.getName());
            }

            txtAssignee.setText(task.getAssignee() != null ? task.getAssignee() : "");
            txtStartDate.setText(task.getStartDate() != null ? task.getStartDate().toString() : "");
            txtEndDate.setText(task.getEndDate() != null ? task.getEndDate().toString() : "");

            String priority = task.getPriority() != null ? task.getPriority() : Task.DEFAULT_PRIORITY;
            comboPriority.setSelectedItem(priority);

            String status = task.getStatus();
            if ("進行中".equals(status)) {
                rbInProgress.setSelected(true);
            } else if ("完了".equals(status)) {
                rbCompleted.setSelected(true);
            } else {
                rbNotStarted.setSelected(true);
            }
        }
    }

    private Task findRootProject(Task task) {
        if (task == null) return null;
        if (task.getLevel() == 1 || task.getParentId() == null) {
            return task;
        }
        Task parent = TaskDao.getTaskById(task.getParentId());
        return findRootProject(parent);
    }

    private List<Task> getCachedTasksSnapshot() {
        if (taskCache.isEmpty()) {
            taskCache = TaskDao.getAllTasks();
            if (taskCache == null) {
                taskCache = new ArrayList<>();
            }
        }
        return new ArrayList<>(taskCache);
    }

    private void invalidateTaskCache() {
        taskCache.clear();
    }

    private String validateTaskInput(String name, String startDateText, String endDateText, Task rootProject) {
        LocalDate startDate = parseOptionalDate(startDateText);
        LocalDate endDate = parseOptionalDate(endDateText);

        if (startDateText != null && !startDateText.trim().isEmpty() && startDate == null) {
            return "日付の形式は YYYY-MM-DD で入力してください。";
        }
        if (endDateText != null && !endDateText.trim().isEmpty() && endDate == null) {
            return "日付の形式は YYYY-MM-DD で入力してください。";
        }

        TaskBusinessRules.ValidationResult result = TaskBusinessRules.validateTaskDraft(name, startDate, endDate, rootProject);
        return result.isValid() ? null : result.getMessage();
    }

    private LocalDate parseOptionalDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateText.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void handleCreateEntry(String type) {
        handleCreateEntry(type, true);
    }

    private void handleCreateEntry(String type, boolean useSelectedParent) {
        Task selectedParentTask = null;
        if (useSelectedParent && selectedTaskId != null) {
            selectedParentTask = TaskDao.getTaskById(selectedTaskId);
        }

        String defaultType = "プロジェクト".equals(type) ? "プロジェクト" : "工程".equals(type) ? "工程" : "タスク";
        Integer defaultParentProjectId = null;
        if (selectedParentTask != null) {
            Task rootProject = findRootProject(selectedParentTask);
            if (rootProject != null) {
                defaultParentProjectId = rootProject.getId();
            }
        }

        if (!"プロジェクト".equals(defaultType) && defaultParentProjectId == null) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "工程・タスクを登録するには、親プロジェクトを選択してから追加してください。\nプロジェクトとして新規登録しますか？",
                    "親プロジェクト未選択",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                openSharedTaskDialog("プロジェクト", null);
            }
            return;
        }

        openSharedTaskDialog(defaultType, defaultParentProjectId);
    }

    private void openSharedTaskDialog(String defaultType, Integer defaultParentProjectId) {
        TaskDialog dialog = new TaskDialog((Frame) SwingUtilities.getWindowAncestor(this), defaultType, defaultParentProjectId);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        if (!"プロジェクト".equals(dialog.getEntryType()) && dialog.getParentProjectId() == null) {
            JOptionPane.showMessageDialog(this, "工程・タスクは親プロジェクトまたは親工程を選択してから登録してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            LocalDate start = LocalDate.parse(dialog.getStartDate());
            LocalDate end = LocalDate.parse(dialog.getEndDate());
            Integer parentProjectId = dialog.getParentProjectId();
            Integer parentId = dialog.getParentTaskId();
            int level = 1;

            if ("工程".equals(dialog.getEntryType()) && parentProjectId != null) {
                parentId = parentProjectId;
                level = 2;
            } else if ("タスク".equals(dialog.getEntryType()) && parentId != null) {
                Task parentTask = TaskDao.getTaskById(parentId);
                if (parentTask != null && parentTask.getLevel() == 2) {
                    level = 3;
                } else if (parentTask != null && parentTask.getLevel() == 1) {
                    level = 2;
                } else {
                    level = 2;
                }
            } else if ("タスク".equals(dialog.getEntryType()) && parentProjectId != null) {
                parentId = parentProjectId;
                level = 2;
            }

            Task newTask = new Task(0, dialog.getTaskName(), parentId, level, dialog.getProgress(), "未着手", start, end);
            newTask.setAssignee(dialog.getAssignee());
            newTask.setPriority(dialog.getPriority() != null ? dialog.getPriority() : Task.DEFAULT_PRIORITY);
            TaskDao.addTask(newTask);
            invalidateTaskCache();
            refreshWbs();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "日付の形式は YYYY-MM-DD で入力してください。", "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    void handleExistingProject() {
        List<Task> allTasks = TaskDao.getAllTasks();
        List<Task> projectList = new ArrayList<>();

        for (Task t : allTasks) {
            if (t.getLevel() == 1 || t.getParentId() == null) {
                projectList.add(t);
            }
        }

        if (projectList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "登録されているプロジェクトがありません。");
            return;
        }

        String[] options = new String[projectList.size() + 1];
        options[0] = "[すべてのプロジェクトを表示]";
        for (int i = 0; i < projectList.size(); i++) {
            options[i + 1] = projectList.get(i).getName();
        }

        JComboBox<String> projectCombo = new JComboBox<>(options);
        projectCombo.setSelectedIndex(0);
        JOptionPane optionPane = new JOptionPane(
                new Object[]{"表示するプロジェクトを選択してください:", projectCombo},
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
        );
        JDialog dialog = optionPane.createDialog(this, "既存プロジェクト切替");
        Window owner = SwingUtilities.getWindowAncestor(this);
        dialog.setLocationRelativeTo(owner != null ? owner : this);
        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();
        if (!(selectedValue instanceof Integer) || ((Integer) selectedValue) != JOptionPane.OK_OPTION) {
            return;
        }

        String selected = (String) projectCombo.getSelectedItem();
        if (selected != null) {
            if (selected.equals(options[0])) {
                currentFilterProjectId = null;
            } else {
                for (Task p : projectList) {
                    if (p.getName().equals(selected)) {
                        currentFilterProjectId = p.getId();
                        break;
                    }
                }
            }
            refreshWbs();
        }
    }

    private void showCreateEntryDialog(String defaultType, Task parentTask) {
        Integer defaultParentProjectId = null;
        if (parentTask != null) {
            Task rootProject = findRootProject(parentTask);
            if (rootProject != null) {
                defaultParentProjectId = rootProject.getId();
            }
        }
        openSharedTaskDialog(defaultType != null ? defaultType : "プロジェクト", defaultParentProjectId);
    }

    private void configureTreeDragAndDrop() {
        wbsTree.setDragEnabled(true);
        wbsTree.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        wbsTree.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                JTree tree = (JTree) c;
                TreePath path = tree.getSelectionPath();
                if (path == null) {
                    return null;
                }
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Task task = nodeTaskMap.get(node);
                if (task == null) {
                    return null;
                }
                return new StringSelection(String.valueOf(task.getId()));
            }

            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop()) {
                    clearDropHighlight();
                    return false;
                }
                if (!(support.getComponent() instanceof JTree)) {
                    clearDropHighlight();
                    return false;
                }
                if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    clearDropHighlight();
                    return false;
                }
                if (!(support.getDropLocation() instanceof JTree.DropLocation dropLocation)) {
                    clearDropHighlight();
                    return false;
                }
                TreePath targetPath = dropLocation.getPath();
                if (targetPath == null) {
                    clearDropHighlight();
                    return false;
                }
                DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
                Task targetTask = nodeTaskMap.get(targetNode);
                if (targetTask == null) {
                    clearDropHighlight();
                    return false;
                }

                try {
                    String draggedIdText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    int draggedId = Integer.parseInt(draggedIdText);
                    if (draggedId == targetTask.getId()) {
                        clearDropHighlight();
                        return false;
                    }
                    boolean valid = !wouldCreateCycle(draggedId, targetTask.getId());
                    highlightDropPath = valid ? targetPath : null;
                    if (wbsTree != null) {
                        wbsTree.repaint();
                    }
                    support.setShowDropLocation(valid);
                    return valid;
                } catch (Exception e) {
                    clearDropHighlight();
                    return false;
                }
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    String draggedIdText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    int draggedId = Integer.parseInt(draggedIdText);
                    JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                    TreePath targetPath = dropLocation.getPath();
                    if (targetPath == null) {
                        return false;
                    }

                    DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
                    Task targetTask = nodeTaskMap.get(targetNode);
                    Task movedTask = TaskDao.getTaskById(draggedId);
                    if (targetTask == null || movedTask == null) {
                        return false;
                    }

                    if (movedTask.getLevel() == 1 && targetTask.getLevel() == 1) {
                        clearDropHighlight();
                        return false;
                    }

                    movedTask.setParentId(targetTask.getId());
                    movedTask.setLevel(targetTask.getLevel() + 1);
                    TaskDao.updateTask(movedTask);
                    recalculateDescendantLevels(movedTask.getId(), movedTask.getLevel());
                    clearDropHighlight();
                    invalidateTaskCache();
                    refreshWbs();
                    return true;
                } catch (Exception e) {
                    clearDropHighlight();
                    JOptionPane.showMessageDialog(WbsPanel.this, "タスクの移動に失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });
    }

    private void clearDropHighlight() {
        highlightDropPath = null;
        if (wbsTree != null) {
            wbsTree.repaint();
        }
    }

    private boolean wouldCreateCycle(int draggedTaskId, int targetTaskId) {
        Task current = TaskDao.getTaskById(targetTaskId);
        while (current != null) {
            if (current.getId() == draggedTaskId) {
                return true;
            }
            Integer parentId = current.getParentId();
            if (parentId == null) {
                break;
            }
            current = TaskDao.getTaskById(parentId);
        }
        return false;
    }

    private void recalculateDescendantLevels(int rootTaskId, int baseLevel) {
        List<Task> allTasks = TaskDao.getAllTasks();
        Map<Integer, List<Task>> childrenMap = new HashMap<>();
        for (Task task : allTasks) {
            if (task.getParentId() != null) {
                childrenMap.computeIfAbsent(task.getParentId(), k -> new ArrayList<>()).add(task);
            }
        }
        updateDescendantLevelsRecursively(rootTaskId, baseLevel, childrenMap, allTasks);
    }

    private void updateDescendantLevelsRecursively(int parentId, int parentLevel, Map<Integer, List<Task>> childrenMap, List<Task> allTasks) {
        List<Task> children = childrenMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (Task child : children) {
            int nextLevel = parentLevel + 1;
            child.setLevel(nextLevel);
            TaskDao.updateTask(child);
            updateDescendantLevelsRecursively(child.getId(), nextLevel, childrenMap, allTasks);
        }
    }

    public String getCurrentSearchKeyword() {
        return currentSearchKeyword;
    }

    public String getCurrentStatusFilter() {
        return currentStatusFilter;
    }

    public void applySearchCriteria(String keyword, String status) {
        currentSearchKeyword = normalizeKeyword(keyword);
        currentStatusFilter = normalizeStatus(status);
        txtSearchKeyword.setText(currentSearchKeyword != null ? currentSearchKeyword : "");
        comboStatusFilter.setSelectedItem(currentStatusFilter != null ? currentStatusFilter : "すべて");
        handleSearch();
    }

    private String normalizeKeyword(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String value) {
        if (value == null || "すべて".equals(value)) {
            return null;
        }
        String trimmed = value == null ? null : value.trim();
        return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
    }

    private void updateRadioButtonTheme() {
        Color bgColor = AppTheme.isDarkMode() ? AppTheme.PANEL_BG : Color.WHITE;
        Color fgColor = AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY;

        rbNotStarted.setBackground(bgColor);
        rbNotStarted.setForeground(fgColor);
        rbNotStarted.setOpaque(false);
        rbInProgress.setBackground(bgColor);
        rbInProgress.setForeground(fgColor);
        rbInProgress.setOpaque(false);
        rbCompleted.setBackground(bgColor);
        rbCompleted.setForeground(fgColor);
        rbCompleted.setOpaque(false);
    }

    public void updateTheme() {
        updateRadioButtonTheme();
        wbsTree.setBackground(AppTheme.PANEL_BG);
        wbsTree.setForeground(AppTheme.TEXT_PRIMARY);
        wbsTree.repaint();
    }

    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return TaskDao.getTaskById(selectedTaskId);
    }

    public void deleteSelectedTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this, "削除するタスクを左側のツリーから選択してください。");
            return;
        }

        Task task = TaskDao.getTaskById(selectedTaskId);
        if (task == null) {
            return;
        }

        TaskDao.deleteTask(selectedTaskId);
        invalidateTaskCache();
        clearForm();
        refreshWbs();
        JOptionPane.showMessageDialog(this, "「" + task.getName() + "」を削除しました。");
    }

    private void handleDuplicateTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this, "複製するタスクを左側のツリーから選択してください。");
            return;
        }

        Task originalTask = TaskDao.getTaskById(selectedTaskId);
        if (originalTask == null) return;

        String newName = JOptionPane.showInputDialog(
                this,
                "複製するタスク名を入力してください：",
                originalTask.getName() + " (コピー)"
        );

        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        Task duplicatedTask = duplicateTaskRecursive(originalTask, newName, originalTask.getParentId());

        if (duplicatedTask != null) {
            invalidateTaskCache();
            refreshWbs();
            JOptionPane.showMessageDialog(this, "タスクを複製しました。");
        }
    }

    private Task duplicateTaskRecursive(Task originalTask, String newName, Integer newParentId) {
        Task newTask = new Task(
                0,
                newName,
                newParentId,
                originalTask.getLevel(),
                originalTask.getProgress(),
                originalTask.getStatus(),
                originalTask.getStartDate(),
                originalTask.getEndDate()
        );
        newTask.setAssignee(originalTask.getAssignee());
        newTask.setPriority(originalTask.getPriority());

        int newTaskId = TaskDao.addTask(newTask);
        if (newTaskId == -1) {
            return null;
        }
        newTask.setId(newTaskId);

        List<Task> allTasks = TaskDao.getAllTasks();
        for (Task child : allTasks) {
            if (child.getParentId() != null && child.getParentId() == originalTask.getId()) {
                duplicateTaskRecursive(child, child.getName(), newTaskId);
            }
        }

        return newTask;
    }

    private void clearForm() {
        selectedTaskId = null;
        selectedRootProjectId = null;
        txtProjectName.setText("");
        txtStageName.setText("");
        txtTaskName.setText("");
        txtAssignee.setText("");
        txtStartDate.setText("");
        txtEndDate.setText("");
        comboPriority.setSelectedItem(Task.DEFAULT_PRIORITY);
        rbNotStarted.setSelected(true);
    }

    private void handleSaveTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this, "左側のツリーから編集対象のタスクを選択してください。");
            return;
        }

        Task task = TaskDao.getTaskById(selectedTaskId);
        if (task == null) return;

        LocalDate startDate = null;
        LocalDate endDate = null;

        try {
            if (!txtStartDate.getText().trim().isEmpty()) {
                startDate = LocalDate.parse(txtStartDate.getText().trim());
            }
            if (!txtEndDate.getText().trim().isEmpty()) {
                endDate = LocalDate.parse(txtEndDate.getText().trim());
            }
        } catch (Exception ignored) {
            JOptionPane.showMessageDialog(this, "日付の形式は YYYY-MM-DD で入力してください。");
            return;
        }

        String updatedProjectName = txtProjectName.getText().trim();
        String updatedStageName = txtStageName.getText().trim();
        String updatedTaskName = txtTaskName.getText().trim();
        String nameToValidate;

        if (task.getLevel() == 1) {
            nameToValidate = updatedProjectName;
        } else if (task.getLevel() == 2) {
            nameToValidate = updatedStageName;
        } else {
            nameToValidate = updatedTaskName;
        }

        Task rootProject = findRootProject(task);

        String validationError = validateTaskInput(nameToValidate, txtStartDate.getText(), txtEndDate.getText(), rootProject);
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, validationError, "日付範囲エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedRootProjectId != null && !updatedProjectName.isEmpty()) {
            Task rootProjectTask = TaskDao.getTaskById(selectedRootProjectId);
            if (rootProjectTask != null && !rootProjectTask.getName().equals(updatedProjectName)) {
                rootProjectTask.setName(updatedProjectName);
                TaskDao.updateTask(rootProjectTask);
            }
        }

        if (task.getLevel() == 1) {
            task.setName(updatedProjectName);
        } else if (task.getLevel() == 2) {
            task.setName(updatedStageName);
        } else {
            task.setName(updatedTaskName);
        }

        task.setAssignee(txtAssignee.getText().trim());
        task.setStartDate(startDate);
        task.setEndDate(endDate);

        String selectedPriority = (String) comboPriority.getSelectedItem();
        task.setPriority(selectedPriority != null ? selectedPriority : Task.DEFAULT_PRIORITY);

        if (rbCompleted.isSelected()) {
            task.setStatus("完了");
        } else if (rbInProgress.isSelected()) {
            task.setStatus("進行中");
        } else {
            task.setStatus("未着手");
        }
        task.setProgress(TaskBusinessRules.resolveProgress(task.getStatus(), task.getProgress()));

        TaskDao.updateTask(task);
        invalidateTaskCache();
        refreshWbs();
        JOptionPane.showMessageDialog(this, "更新しました。");
    }

    private class WbsTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;
        private final Icon projectIcon;
        private final Icon taskIcon;

        public WbsTreeCellRenderer() {
            projectIcon = UIManager.getIcon("Tree.openIcon");
            taskIcon = UIManager.getIcon("Tree.leafIcon");
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            setOpaque(false);
            setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));
            setBackgroundSelectionColor(new Color(0, 0, 0, 0));

            if (AppTheme.isDarkMode()) {
                setForeground(new Color(226, 232, 240));
            } else {
                setForeground(AppTheme.TEXT_PRIMARY);
            }

            if (value instanceof DefaultMutableTreeNode node) {
                Task task = nodeTaskMap.get(node);
                if (task != null) {
                    if (task.getLevel() == 1) {
                        setIcon(projectIcon != null ? projectIcon : getDefaultOpenIcon());
                    } else {
                        setIcon(taskIcon != null ? taskIcon : getDefaultLeafIcon());
                        String status = task.getStatus();
                        setForeground(AppTheme.getStatusColor(status));
                    }
                }
            }

            if (highlightDropPath != null && highlightDropPath.getLastPathComponent() == value) {
                setOpaque(true);
                Task task = nodeTaskMap.get((DefaultMutableTreeNode) value);
                String status = task != null ? task.getStatus() : "未着手";
                setBackground(AppTheme.getStatusSoftColor(status));
                setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.PRIMARY));
                setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
            } else {
                setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
                setOpaque(false);
            }
            return this;
        }
    }

    private boolean validateTaskDatesWithinProject(Task parentProject, LocalDate taskStart, LocalDate taskEnd) {
        String errorMessage = TaskBusinessRules.validateProjectDateRange(parentProject, taskStart, taskEnd);
        if (errorMessage != null) {
            JOptionPane.showMessageDialog(this, errorMessage, "日付範囲エラー", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void handleSearch() {
        String keyword = txtSearchKeyword.getText().trim();
        String status = (String) comboStatusFilter.getSelectedItem();

        currentSearchKeyword = keyword.isEmpty() ? null : keyword;
        currentStatusFilter = "すべて".equals(status) ? null : status;

        refreshWbs();
    }

    void handleClearFilter() {
        txtSearchKeyword.setText("");
        comboStatusFilter.setSelectedIndex(0);
        currentFilterProjectId = null;
        currentSearchKeyword = null;
        currentStatusFilter = null;
        refreshWbs();
    }

    private List<Task> filterTasksInMemory(List<Task> tasks, String keyword, String status, Map<Integer, Task> taskMap) {
        Set<Integer> matchingTaskIds = new HashSet<>();

        for (Task task : tasks) {
            boolean matchesKeyword = keyword == null || keyword.isEmpty()
                    || (task.getName() != null && task.getName().toLowerCase().contains(keyword.toLowerCase()));
            boolean matchesStatus = status == null || status.isEmpty()
                    || (task.getStatus() != null && task.getStatus().equals(status));

            if (matchesKeyword && matchesStatus) {
                matchingTaskIds.add(task.getId());
            }
        }

        Set<Integer> includedTaskIds = new HashSet<>(matchingTaskIds);

        for (Integer taskId : matchingTaskIds) {
            Task current = taskMap.get(taskId);
            while (current != null && current.getParentId() != null) {
                Integer parentId = current.getParentId();
                if (includedTaskIds.contains(parentId)) {
                    break;
                }
                includedTaskIds.add(parentId);
                current = taskMap.get(parentId);
            }
        }

        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (includedTaskIds.contains(task.getId())) {
                filteredTasks.add(task);
            }
        }

        return filteredTasks;
    }
}