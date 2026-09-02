package org.example;

// --- 画面 UI (Swing) 関連のライブラリ ---
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

// --- レイアウト・イベント関連のライブラリ ---
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.TransferHandler.TransferSupport;

// --- 日付処理関連のライブラリ ---
import java.time.LocalDate;

// --- コレクション関連のライブラリ ---
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【カンバン画面パネルクラス】
 * タスクのステータス（未着手・進行中・完了）ごとに 3 カラムのボードを構築し、
 * カード形式で直感的に進捗管理を行う画面です。
 */
public class KanbanPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final MainFrame mainFrame; // 状態更新後に画面全体をリフレッシュするための参照

    // --- 検索用コンポーネント ---
    private JTextField txtSearchKeyword;  // 検索欄は非表示のため、処理用として残す

    // 【重要】現在の検索条件を保持
    private String currentSearchKeyword = null;
    private String currentStatusFilter = null;
    private Integer currentProjectFilterId = null;
    private final Map<Integer, Set<Integer>> projectMemberCache = new HashMap<>();

    // 各ステータスカラム用のカード格納パネル
    private final JPanel todoColumn = createColumnPanel();
    private final JPanel inProgressColumn = createColumnPanel();
    private final JPanel doneColumn = createColumnPanel();
    private JLabel todoHeaderLabel;
    private JLabel inProgressHeaderLabel;
    private JLabel doneHeaderLabel;
    private Integer selectedTaskId = null;

    /**
     * コンストラクタ：カンバンボードの列レイアウトを構築します。
     */
    public KanbanPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        // 検索欄は非表示。カンバンは列一覧がメインのため、余白を減らして見やすくする
        setLayout(new BorderLayout(0, 10));
        setBackground(AppTheme.BACKGROUND);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // カラム用パネル
        JPanel columnsPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        columnsPanel.setBackground(AppTheme.BACKGROUND);

        // 3 つのカラム（未着手、進行中、完了）をそれぞれ構築して追加
        JPanel todoWrapper = buildColumnWrapper("未着手", todoColumn);
        JPanel inProgressWrapper = buildColumnWrapper("進行中", inProgressColumn);
        JPanel doneWrapper = buildColumnWrapper("完了", doneColumn);
        columnsPanel.add(todoWrapper);
        columnsPanel.add(inProgressWrapper);
        columnsPanel.add(doneWrapper);

        add(columnsPanel, BorderLayout.CENTER);

        // データベースからタスクを取得してカードを配置
        refreshKanban();
    }

    /**
     * 【カラム用枠線・ヘッダー構築メソッド】
     * 各列の見出し（タイトル）と、カードが並ぶスクロールエリアを生成します。
     * ヘッダーは「【 未着手 】 (件数)」の形式で表示し、後から件数を更新できるよう
     * ラベルをフィールドに保持しておきます。
     */
    private JPanel buildColumnWrapper(String title, JPanel cardsContainer) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(AppTheme.BACKGROUND);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createSoftPanelBorder(),
                new EmptyBorder(0, 0, 0, 0)
        ));
        cardsContainer.putClientProperty("kanban.status", title);

        JLabel headerLabel = new JLabel(formatColumnHeader(title, 0), SwingConstants.CENTER);
        headerLabel.setFont(AppTheme.FONT_HEADER);
        headerLabel.setOpaque(true);
        headerLabel.putClientProperty("kanban.column.title", title);
        updateHeaderLabelTheme(headerLabel, title);
        headerLabel.setBorder(new EmptyBorder(10, 12, 10, 12));
        wrapper.add(headerLabel, BorderLayout.NORTH);

        if ("未着手".equals(title)) {
            todoHeaderLabel = headerLabel;
        } else if ("進行中".equals(title)) {
            inProgressHeaderLabel = headerLabel;
        } else if ("完了".equals(title)) {
            doneHeaderLabel = headerLabel;
        }

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(null);
        scrollPane.setBackground(AppTheme.BACKGROUND);
        scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setTransferHandler(createColumnDropHandler(title));
        scrollPane.getViewport().setTransferHandler(createColumnDropHandler(title));
        wrapper.add(scrollPane, BorderLayout.CENTER);

        cardsContainer.setTransferHandler(createColumnDropHandler(title));
        return wrapper;
    }

    private String formatColumnHeader(String title, int count) {
        return "\u3010 " + title + " \u3011  (" + count + ")";
    }

    /**
     * 【カード格納用パネルの初期化】
     * 縦方向に上詰めでカードが並ぶように BoxLayout を設定します。
     */
    private JPanel createColumnPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AppTheme.BACKGROUND);
        panel.setBorder(new EmptyBorder(8, 8, 10, 8));
        return panel;
    }

    /**
     * 【カンバン画面のデータ再描画処理】
     * データベースから最新のタスクを取得し、該当するステータスの列へカードを読み込みます。
     */
    public void refreshKanban() {
        refreshKanban(TaskDao.getAllTasks());
    }

    public void refreshKanban(List<Task> taskSnapshot) {
        clearProjectMemberCache();
        todoColumn.removeAll();
        inProgressColumn.removeAll();
        doneColumn.removeAll();

        List<Task> tasks = taskSnapshot != null ? taskSnapshot : TaskDao.getAllTasks();
        if (tasks == null) {
            tasks = java.util.Collections.emptyList();
        }

        if (currentSearchKeyword != null && !currentSearchKeyword.trim().isEmpty()) {
            List<Task> filteredTasks = new java.util.ArrayList<>();
            String keyword = currentSearchKeyword.trim();
            for (Task task : tasks) {
                if (task.getName() != null && task.getName().contains(keyword)) {
                    filteredTasks.add(task);
                }
            }
            tasks = filteredTasks;
        }

        if (currentStatusFilter != null && !"すべて".equals(currentStatusFilter)) {
            List<Task> filteredTasks = new java.util.ArrayList<>();
            for (Task task : tasks) {
                if (currentStatusFilter.equals(task.getStatus())) {
                    filteredTasks.add(task);
                }
            }
            tasks = filteredTasks;
        }

        if (currentProjectFilterId != null) {
            Set<Integer> projectMemberIds = getProjectMemberIds(tasks, currentProjectFilterId);
            List<Task> filteredTasks = new java.util.ArrayList<>();
            for (Task task : tasks) {
                if (projectMemberIds.contains(task.getId())) {
                    filteredTasks.add(task);
                }
            }
            tasks = filteredTasks;
        }

        int todoCount = 0;
        int inProgressCount = 0;
        int doneCount = 0;

        for (Task task : tasks) {
            JPanel card = createTaskCard(task);
            String status = task.getStatus();
            if ("進行中".equals(status)) {
                inProgressColumn.add(card);
                inProgressColumn.add(Box.createVerticalStrut(8));
                inProgressCount++;
            } else if ("完了".equals(status)) {
                doneColumn.add(card);
                doneColumn.add(Box.createVerticalStrut(8));
                doneCount++;
            } else {
                todoColumn.add(card);
                todoColumn.add(Box.createVerticalStrut(8));
                todoCount++;
            }
        }

        if (todoHeaderLabel != null) {
            todoHeaderLabel.setText(formatColumnHeader("未着手", todoCount));
        }
        if (inProgressHeaderLabel != null) {
            inProgressHeaderLabel.setText(formatColumnHeader("進行中", inProgressCount));
        }
        if (doneHeaderLabel != null) {
            doneHeaderLabel.setText(formatColumnHeader("完了", doneCount));
        }

        revalidate();
        repaint();
    }

    public String getCurrentSearchKeyword() {
        return currentSearchKeyword;
    }

    public String getCurrentStatusFilter() {
        return currentStatusFilter;
    }

    public void applySearchFilter(String keyword, String status) {
        currentSearchKeyword = normalizeKeyword(keyword);
        currentStatusFilter = normalizeStatus(status);
        if (txtSearchKeyword != null) {
            txtSearchKeyword.setText(currentSearchKeyword != null ? currentSearchKeyword : "");
        }
        refreshKanban();
    }

    public void clearSearchFilter() {
        currentSearchKeyword = null;
        currentStatusFilter = null;
        currentProjectFilterId = null;
        clearProjectMemberCache();
        if (txtSearchKeyword != null) {
            txtSearchKeyword.setText("");
        }
        refreshKanban();
    }

    public void handleClearFilter() {
        clearSearchFilter();
    }

    public void handleExistingProject() {
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
            currentProjectFilterId = null;
        } else {
            for (Task p : projectList) {
                if (p.getName().equals(selected)) {
                    currentProjectFilterId = p.getId();
                    break;
                }
            }
        }
        clearProjectMemberCache();
        refreshKanban();
        }
    }

    private Set<Integer> getProjectMemberIds(List<Task> tasks, int projectId) {
        Set<Integer> cached = projectMemberCache.get(projectId);
        if (cached != null) {
            return cached;
        }

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task task : tasks) {
            if (task != null) {
                taskMap.put(task.getId(), task);
            }
        }

        Set<Integer> memberIds = new HashSet<>();
        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            Task current = task;
            while (current != null) {
                if (current.getId() == projectId) {
                    memberIds.add(task.getId());
                    break;
                }
                Integer parentId = current.getParentId();
                if (parentId == null) {
                    break;
                }
                current = taskMap.get(parentId);
            }
        }

        projectMemberCache.put(projectId, memberIds);
        return memberIds;
    }

    private void clearProjectMemberCache() {
        projectMemberCache.clear();
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
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return TaskDao.getTaskById(selectedTaskId);
    }

    public void deleteSelectedTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this, "削除するカードを選択してください。", "削除できません", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Task task = TaskDao.getTaskById(selectedTaskId);
        if (task == null) {
            return;
        }

        TaskDao.deleteTask(selectedTaskId);
        selectedTaskId = null;
        refreshKanban();
        JOptionPane.showMessageDialog(this, "「" + task.getName() + "」を削除しました。", "削除完了", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 【タスクカードの UI 生成メソッド】
     * 1 つのタスク情報を保持するカードパネル（枠・タイトル・担当者・進捗・操作ボタン）を生成します。
     */
    private JPanel createTaskCard(Task task) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(AppTheme.isDarkMode() ? new Color(42, 50, 61) : new Color(255, 255, 255));
        
        Color statusColor = AppTheme.getStatusColor(task.getStatus());
        Border normalBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(statusColor, 12, 1, 1, 1, 1),
                new EmptyBorder(10, 12, 10, 12)
        );
        Border hoverBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(statusColor, 12, 2, 2, 2, 2),
                new EmptyBorder(9, 11, 9, 11)
        );
        card.setBorder(normalBorder);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("ドラッグで列移動 / ダブルクリックで編集");
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                return new StringSelection(String.valueOf(task.getId()));
            }

            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }
        });

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    selectedTaskId = task.getId();
                    refreshKanban();
                    card.getTransferHandler().exportAsDrag(card, e, TransferHandler.MOVE);
                }
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedTaskId = task.getId();
                if (e.getClickCount() == 2) {
                    openEditDialog(task);
                }
                refreshKanban();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (selectedTaskId != null && selectedTaskId.equals(task.getId())) {
                    card.setBorder(hoverBorder);
                } else {
                    card.setBorder(hoverBorder);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (selectedTaskId != null && selectedTaskId.equals(task.getId())) {
                    card.setBorder(hoverBorder);
                } else {
                    card.setBorder(normalBorder);
                }
            }
        });

        if (selectedTaskId != null && selectedTaskId.equals(task.getId())) {
            card.setBorder(hoverBorder);
        }

        JLabel nameLabel = new JLabel(task.getName());
        nameLabel.setFont(AppTheme.FONT_HEADER);
        nameLabel.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        JLabel progressLabel = new JLabel(task.getProgress() + "%");
        progressLabel.setFont(AppTheme.FONT_MAIN);
        progressLabel.setForeground(AppTheme.isDarkMode() ? new Color(96, 165, 250) : AppTheme.PRIMARY);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(progressLabel, BorderLayout.EAST);

        String assigneeText = "\uD83D\uDC64 " + (task.getAssignee() != null ? task.getAssignee() : "未設定"); // 👤
        JLabel assigneeLabel = new JLabel(assigneeText);
        assigneeLabel.setFont(AppTheme.FONT_MAIN);
        assigneeLabel.setForeground(AppTheme.isDarkMode() ? new Color(148, 163, 184) : AppTheme.TEXT_MUTED);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JComponent progressBar = createCardProgressBar(task.getProgress(), task.getStatus());
        if (progressBar != null) {
            progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(progressBar);
            centerPanel.add(Box.createVerticalStrut(6));
        }
        assigneeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(assigneeLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);

        if (!"未着手".equals(task.getStatus())) {
            JButton prevBtn = new JButton("←");
            prevBtn.setFont(AppTheme.FONT_MAIN);
            prevBtn.setMargin(new Insets(2, 6, 2, 6));
            prevBtn.setFocusPainted(false);
            prevBtn.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1));
            prevBtn.addActionListener(e -> changeTaskStatus(task, getPrevStatus(task.getStatus())));
            buttonPanel.add(prevBtn);
        }

        if (!"完了".equals(task.getStatus())) {
            JButton nextBtn = new JButton("→");
            nextBtn.setFont(AppTheme.FONT_MAIN);
            nextBtn.setMargin(new Insets(2, 6, 2, 6));
            nextBtn.setFocusPainted(false);
            nextBtn.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1));
            nextBtn.addActionListener(e -> changeTaskStatus(task, getNextStatus(task.getStatus())));
            buttonPanel.add(nextBtn);
        }

        JPanel priorityIndicator = new JPanel();
        priorityIndicator.setPreferredSize(new Dimension(8, 0));
        String prio = task.getPriority() != null ? task.getPriority() : Task.DEFAULT_PRIORITY;
        priorityIndicator.setBackground(AppTheme.getPriorityColor(prio));
        card.add(priorityIndicator, BorderLayout.WEST);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        return card;
    }

    /**
     * 【カード内進捗バー生成】
     * 状態に応じた色でバーを表示します。未着手（進捗0%）はバーを表示しません。
     */
    private JComponent createCardProgressBar(int progress, String status) {
        if (progress <= 0) {
            return null;
        }
        Color fillColor = AppTheme.getStatusColor(status);
        JComponent bar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int trackHeight = 6;
                    int y = (getHeight() - trackHeight) / 2;
                    g2.setColor(new Color(230, 232, 236));
                    g2.fillRoundRect(0, y, getWidth(), trackHeight, trackHeight, trackHeight);
                    int fillWidth = (int) Math.round(getWidth() * Math.min(100, Math.max(0, progress)) / 100.0);
                    if (fillWidth > 0) {
                        g2.setColor(fillColor);
                        g2.fillRoundRect(0, y, fillWidth, trackHeight, trackHeight, trackHeight);
                    }
                } finally {
                    g2.dispose();
                }
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 10));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        return bar;
    }

    /**
     * ステータスを1段階進めるヘルパーメソッド
     */
    private String getNextStatus(String current) {
        if ("未着手".equals(current)) return "進行中";
        if ("進行中".equals(current)) return "完了";
        return "完了";
    }

    /**
     * ステータスを1段階戻すヘルパーメソッド
     */
    private String getPrevStatus(String current) {
        if ("完了".equals(current)) return "進行中";
        if ("進行中".equals(current)) return "未着手";
        return "未着手";
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
            updateHeaderLabelTheme(todoHeaderLabel, "未着手");
        }
        if (inProgressHeaderLabel != null) {
            updateHeaderLabelTheme(inProgressHeaderLabel, "進行中");
        }
        if (doneHeaderLabel != null) {
            updateHeaderLabelTheme(doneHeaderLabel, "完了");
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
                    moveTaskToStatus(taskId, status);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    private void moveTaskToStatus(int taskId, String targetStatus) {
        if (targetStatus == null || targetStatus.isBlank()) {
            return;
        }

        List<Task> tasks = TaskDao.getAllTasks();
        for (Task task : tasks) {
            if (task.getId() == taskId) {
                task.setStatus(targetStatus);
                task.setProgress(TaskBusinessRules.resolveProgress(targetStatus, task.getProgress()));
                TaskDao.updateTask(task);
                refreshKanban();
                if (mainFrame != null) {
                    mainFrame.refreshTaskViews();
                }
                return;
            }
        }
    }

    /**
     * タスクの詳細編集ダイアログを開く
     */
    private void openEditDialog(Task task) {
        java.awt.Frame parentFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        if (parentFrame == null) return;

        TaskDialog dialog = new TaskDialog(parentFrame, task);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            task.setName(dialog.getTaskName());
            task.setAssignee(dialog.getAssignee());
            task.setProgress(dialog.getProgress());
            task.setPriority(dialog.getPriority());
            task.setStartDate(LocalDate.parse(dialog.getStartDate()));
            task.setEndDate(LocalDate.parse(dialog.getEndDate()));

            String nextStatus = task.getStatus();
            if (task.getProgress() >= 100) {
                nextStatus = "完了";
            } else if (task.getProgress() > 0) {
                nextStatus = "進行中";
            } else {
                nextStatus = "未着手";
            }
            task.setStatus(nextStatus);

            TaskDao.updateTask(task);
            refreshKanban();
            if (mainFrame != null) {
                mainFrame.refreshTaskViews();
            }
        }
    }

    /**
     * 【ステータス更新処理】
     * ボタン押下時に DB のステータスと進捗率(progress)を自動連動して書き換え、全体画面を更新します。
     */
    private void changeTaskStatus(Task task, String newStatus) {
        if (task == null || newStatus == null) return;

        task.setStatus(newStatus);
        task.setProgress(TaskBusinessRules.resolveProgress(newStatus, task.getProgress()));

        TaskDao.updateTask(task);

        refreshKanban();
        if (mainFrame != null) {
            mainFrame.refreshTaskViews();
        }
    }

    // ==========================================
    //  【検索・フィルタ処理メソッド】
    // ==========================================

    /**
     * 【検索実行処理】
     * 入力された検索キーワードを適用してタスクリストを更新します。
     *
     * 【重要単語の解説】
     * - trim(): 文字列の前後の空白を削除するメソッド
     * - isEmpty(): 文字列が空かどうかを判定するメソッド
     * - refreshKanban(): カンバンボードを再描画するメソッド
     *
     * 【コードの読み方】
     * 1. テキストフィールドから検索キーワードを取得
     * 2. 条件をフィールドに保存
     * 3. refreshKanban()を呼んでカンバンを再描画
     */
    /**
     * 【検索実行処理】
     * 入力された検索キーワードを適用してタスクリストを更新します。
     */
    private void handleSearch() {
        // 【重要】getText(): テキストフィールドの内容を取得
        String keyword = txtSearchKeyword.getText().trim();

        // 【重要】検索キーワードが空の場合はnullに設定（検索条件なし）
        currentSearchKeyword = keyword.isEmpty() ? null : keyword;

        // 【重要】カンバンを再描画して検索結果を反映
        refreshKanban();

        // ⚠️【修正】一文字ごとのポップアップ警告（JOptionPane）を削除しました！
        // これにより、文字を入力するたびに邪魔されることなく、リアルタイムに画面が絞り込まれます。
    }


    /**
     * 【フィルタクリア処理】
     * 検索条件をクリアして全タスクを表示します。
     *
     * 【重要単語の解説】
     * - setText(): テキストフィールドの内容を設定するメソッド
     *
     * 【コードの読み方】
     * 1. 検索キーワードフィールドを空にする
     * 2. 保存されている条件をnullにリセット
     * 3. refreshKanban()を呼んで全タスクを表示
     */
}