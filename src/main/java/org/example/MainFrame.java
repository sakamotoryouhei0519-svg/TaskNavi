package org.example;

// --- 画面 UI (Swing) 関連のライブラリ ---

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

// --- レイアウト・イベント関連のライブラリ ---
// --- 日付処理・コレクション関連のライブラリ ---

/**
 * 【メイン画面クラス】
 * 上部に「WBS」「カンバン」「ガントチャート」のタブを配置し、
 * 各画面を独立して切り替えるメインフレームです。
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTabbedPane mainTabbedPane;
    private final WbsPanel wbsPanel;
    private final KanbanPanel kanbanPanel;
    private final GanttPanel ganttPanel;
    private JScrollPane ganttScrollPane;
    private final JToggleButton themeToggleButton;
    private JLabel userStatusLabel;
    private JPanel headerPanel;
    private JPanel toolbarPanel;
    private volatile boolean refreshInProgress = false;

    public MainFrame() {
        setTitle("TaskNavi - 福祉現場向け ポータブルWBS・カンバンシステム");
        setSize(UiConstants.WINDOW_WIDTH_MAIN, UiConstants.WINDOW_HEIGHT_MAIN);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(AppTheme.BACKGROUND);

        AppTheme.setDarkMode(AppTheme.isDarkMode());
        themeToggleButton = createThemeToggleButton();
        themeToggleButton.setSelected(AppTheme.isDarkMode());
        headerPanel = createHeaderPanel();
        toolbarPanel = createToolBarPanel();

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(headerPanel);
        topContainer.add(toolbarPanel);

        wbsPanel = (WbsPanel) createWbsTabPanel();
        kanbanPanel = (KanbanPanel) createKanbanTabPanel();
        ganttPanel = (GanttPanel) createGanttChartTabPanel();

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(AppTheme.FONT_HEADER);
        mainTabbedPane.setOpaque(false);
        mainTabbedPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER_COLOR));

        mainTabbedPane.addTab("  " + IconManager.getIcon(IconManager.IconType.TAB_WBS) + "  WBS  ", wbsPanel);
        mainTabbedPane.addTab("  " + IconManager.getIcon(IconManager.IconType.TAB_KANBAN) + "  カンバン  ", kanbanPanel);
        ganttScrollPane = createGanttScrollPane(ganttPanel);
        mainTabbedPane.addTab("  " + IconManager.getIcon(IconManager.IconType.TAB_GANTT) + "  ガントチャート  ", ganttScrollPane);
        applyTabbedPaneStyle();

        mainTabbedPane.addChangeListener(e -> {
            updateTabTextColors();
            refreshTaskViews();
        });
        updateTabTextColors();

        contentPane.add(topContainer, BorderLayout.NORTH);
        contentPane.add(mainTabbedPane, BorderLayout.CENTER);

        add(contentPane);
        applyThemeToWindow();

        refreshTaskViews();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleArea.setOpaque(false);

        JLabel logoIcon = IconManager.getIconLabel(IconManager.IconType.LOGO);
        logoIcon.setFont(new Font("SansSerif", Font.PLAIN, 24));

        JLabel logoLabel = new JLabel("TaskNavi");
        logoLabel.setFont(AppTheme.FONT_TITLE);
        logoLabel.setForeground(AppTheme.PRIMARY);

        JLabel subtitleLabel = new JLabel("福祉現場向け ポータブルWBS・カンバンシステム");
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);

        titleArea.add(logoIcon);
        titleArea.add(logoLabel);
        titleArea.add(subtitleLabel);

        User currentUser = UserSession.getCurrentUser();
        String currentUsername = currentUser != null ? currentUser.getDisplayNameOrUsername() : "ゲスト";

        userStatusLabel = new JLabel(IconManager.getIcon(IconManager.IconType.USER) + " " + currentUsername);
        userStatusLabel.setFont(AppTheme.FONT_MAIN);
        userStatusLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JButton logoutButton = createHeaderIconButton("ログアウト", IconManager.IconType.LOGOUT);
        logoutButton.addActionListener(e -> handleLogout());

        JButton exportButton = createHeaderIconButton("エクスポート", IconManager.IconType.EXPORT);
        exportButton.addActionListener(e -> handleExport());

        JButton importButton = createHeaderIconButton("インポート", IconManager.IconType.IMPORT);
        importButton.addActionListener(e -> handleImport());

        Object themeIcon = IconManager.getIcon(IconManager.IconType.THEME);
        if (themeIcon instanceof String) {
            themeToggleButton.setText((String) themeIcon);
        } else {
            themeToggleButton.setIcon((ImageIcon) themeIcon);
        }
        themeToggleButton.setBackground(Color.WHITE);
        themeToggleButton.setForeground(AppTheme.TEXT_PRIMARY);
        themeToggleButton.setContentAreaFilled(false);
        themeToggleButton.setOpaque(false);
        themeToggleButton.setFocusPainted(false);
        themeToggleButton.setBorderPainted(false);
        themeToggleButton.setBorder(new EmptyBorder(4, 6, 4, 6));
        themeToggleButton.setPreferredSize(new Dimension(40, 36));
        themeToggleButton.setMinimumSize(new Dimension(40, 36));

        JPanel rightUserArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightUserArea.setOpaque(false);
        rightUserArea.add(userStatusLabel);
        rightUserArea.add(importButton);
        rightUserArea.add(exportButton);
        rightUserArea.add(themeToggleButton);
        rightUserArea.add(logoutButton);

        panel.add(titleArea, BorderLayout.WEST);
        panel.add(rightUserArea, BorderLayout.EAST);

        return panel;
    }

    /**
     * 【ヘッダー用アウトラインボタン生成】
     * パディング・フォント・枠線（2px）を大きく調整したボタンを作成します。
     */
    private JButton createHeaderIconButton(String text, IconManager.IconType iconType) {
        JButton button = IconManager.getIconButton(iconType, text);
        button.putClientProperty("app.button.variant", "header");
        button.setFont(AppTheme.FONT_MAIN);
        button.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 2, 2, 2, 2));
        button.setMargin(new Insets(6, 12, 6, 12));
        return button;
    }

    private JToggleButton createThemeToggleButton() {
        Object themeIcon = IconManager.getIcon(IconManager.IconType.THEME);
        JToggleButton toggle;
        if (themeIcon instanceof String) {
            toggle = new JToggleButton((String) themeIcon, false);
        } else {
            toggle = new JToggleButton((ImageIcon) themeIcon, false);
        }
        toggle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        toggle.setFocusPainted(false);
        toggle.setBorderPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setPreferredSize(new Dimension(40, 36));
        toggle.addActionListener(e -> {
            AppTheme.setDarkMode(toggle.isSelected());
            applyThemeToWindow();
        });
        return toggle;
    }

    private void applyThemeToWindow() {
        if (themeToggleButton != null) {
            Object themeIcon = IconManager.getIcon(IconManager.IconType.THEME);
            if (themeIcon instanceof String) {
                themeToggleButton.setText((String) themeIcon);
                themeToggleButton.setIcon(null);
            } else {
                themeToggleButton.setText("");
                themeToggleButton.setIcon((ImageIcon) themeIcon);
            }
            themeToggleButton.setBackground(Color.WHITE);
            themeToggleButton.setForeground(AppTheme.TEXT_PRIMARY);
            themeToggleButton.setContentAreaFilled(false);
            themeToggleButton.setOpaque(false);
            themeToggleButton.setFocusPainted(false);
            themeToggleButton.setBorderPainted(false);
            themeToggleButton.setBorder(new EmptyBorder(4, 6, 4, 6));
            themeToggleButton.setPreferredSize(new Dimension(40, 36));
            themeToggleButton.setMinimumSize(new Dimension(40, 36));
        }

        if (userStatusLabel != null) {
            userStatusLabel.setForeground(AppTheme.TEXT_PRIMARY);
        }

        if (wbsPanel != null) {
            wbsPanel.updateTheme();
        }

        if (kanbanPanel != null) {
            kanbanPanel.updateTheme();
        }

        if (headerPanel != null) {
            headerPanel.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        }
        if (toolbarPanel != null) {
            toolbarPanel.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        }

        Container contentPane = getContentPane();
        if (contentPane != null) {
            contentPane.setBackground(AppTheme.BACKGROUND);
            applyThemeToContainer(contentPane);
        }

        if (mainTabbedPane != null) {
            mainTabbedPane.setBackground(AppTheme.BACKGROUND);
            mainTabbedPane.setForeground(AppTheme.TEXT_PRIMARY);
            applyTabbedPaneStyle();
            updateTabTextColors();
        }

        revalidate();
        repaint();
    }

    private void updateTabTextColors() {
        if (mainTabbedPane == null) {
            return;
        }
        int selected = mainTabbedPane.getSelectedIndex();
        for (int i = 0; i < mainTabbedPane.getTabCount(); i++) {
            mainTabbedPane.setForegroundAt(i, i == selected ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED);
        }
    }

    private void applyTabbedPaneStyle() {
        if (mainTabbedPane == null) {
            return;
        }

        mainTabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                tabInsets = new Insets(14, 18, 14, 18);
                tabAreaInsets = new Insets(0, 10, 0, 10);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(AppTheme.PANEL_BG);
                    g2.fillRoundRect(x + 2, y + 1, w - 4, h - 2, 12, 12);
                    if (isSelected) {
                        g2.setColor(AppTheme.PANEL_BG);
                        g2.fillRoundRect(x + 2, y + 1, w - 4, h - 2, 12, 12);
                    }
                } finally {
                    g2.dispose();
                }
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                if (!isSelected) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(AppTheme.PRIMARY);
                    g2.fillRect(x + 12, y + h - 3, w - 24, 3);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(AppTheme.BORDER_COLOR);
                    g2.fillRect(0, 0, mainTabbedPane.getWidth(), 1);
                } finally {
                    g2.dispose();
                }
            }
        });
    }

    private void applyThemeToContainer(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JPanel panel) {
                panel.setBackground(AppTheme.PANEL_BG);
            } else if (child instanceof JLabel label) {
                label.setForeground(AppTheme.TEXT_PRIMARY);
            } else if (child instanceof JButton button) {
                if (child == themeToggleButton) {
                    continue;
                }

                Object variant = button.getClientProperty("app.button.variant");
                Color dangerColor = new Color(220, 38, 38);
                if ("toolbar-primary".equals(variant)) {
                    button.setBackground(AppTheme.PRIMARY);
                    button.setForeground(Color.WHITE);
                    button.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY, 8, 2, 2, 2, 2));
                    continue;
                }
                if ("toolbar-outline-danger".equals(variant)) {
                    button.setBackground(Color.WHITE);
                    button.setForeground(dangerColor);
                    button.setBorder(AppTheme.createRoundedBorder(dangerColor, 8, 2, 2, 2, 2));
                    continue;
                }
                if ("toolbar-outline-primary".equals(variant)) {
                    button.setBackground(Color.WHITE);
                    button.setForeground(AppTheme.PRIMARY);
                    button.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY, 8, 2, 2, 2, 2));
                    continue;
                }
                if ("toolbar-outline".equals(variant) || "header".equals(variant) || "secondary".equals(variant) || "toolbar".equals(variant)) {
                    button.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
                    button.setForeground(AppTheme.TEXT_PRIMARY);
                    button.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 2, 2, 2, 2));
                    continue;
                }
                if ("primary".equals(variant)) {
                    button.setBackground(AppTheme.PRIMARY);
                    button.setForeground(Color.WHITE);
                    button.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY_DARK, 12, 2, 2, 2, 2));
                    continue;
                }

                button.setBackground(AppTheme.PANEL_BG);
                button.setForeground(AppTheme.TEXT_PRIMARY);
            } else if (child instanceof JRadioButton radio) {
                radio.setBackground(AppTheme.isDarkMode() ? AppTheme.PANEL_BG : Color.WHITE);
                radio.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
                radio.setOpaque(false);
            } else if (child instanceof JComboBox<?> combo) {
                combo.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
                combo.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
            } else if (child instanceof JTabbedPane tabbedPane) {
                tabbedPane.setBackground(AppTheme.BACKGROUND);
                tabbedPane.setForeground(AppTheme.TEXT_PRIMARY);
            } else if (child instanceof JTextField field) {
                AppTheme.styleTextField(field);
            } else if (child instanceof JScrollPane scrollPane) {
                scrollPane.setBackground(AppTheme.BACKGROUND);
                if (scrollPane.getViewport() != null) {
                    scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
                }
            } else if (child instanceof JTree tree) {
                tree.setBackground(AppTheme.PANEL_BG);
                tree.setForeground(AppTheme.TEXT_PRIMARY);
                tree.repaint();
            } else if (child instanceof JTable table) {
                table.setBackground(AppTheme.PANEL_BG);
                table.setForeground(AppTheme.TEXT_PRIMARY);
                table.setGridColor(AppTheme.BORDER_COLOR);
            }

            if (child instanceof Container nestedContainer) {
                applyThemeToContainer(nestedContainer);
            }
        }
    }

    private JPanel createToolBarPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
                new EmptyBorder(8, 20, 8, 20)
        ));

        JButton addTaskButton = createToolbarButton("追加", IconManager.IconType.ADD, AppTheme.PRIMARY, Color.WHITE, AppTheme.PRIMARY);
        addTaskButton.putClientProperty("app.button.variant", "toolbar-primary");
        addTaskButton.addActionListener(e -> handleAddTask());
        addTaskButton.setToolTipText("新規登録");

        JButton searchButton = createToolbarButton("検索", IconManager.IconType.SEARCH, Color.WHITE, AppTheme.TEXT_PRIMARY, AppTheme.BORDER_COLOR);
        searchButton.putClientProperty("app.button.variant", "toolbar-outline");
        searchButton.addActionListener(e -> handleSearchFromToolbar());
        searchButton.setToolTipText("検索／フィルタを開く");

        Color dangerColor = new Color(220, 38, 38);
        JButton deleteButton = createToolbarButton("削除", IconManager.IconType.DELETE, Color.WHITE, dangerColor, dangerColor);
        deleteButton.putClientProperty("app.button.variant", "toolbar-outline-danger");
        deleteButton.addActionListener(e -> handleDeleteSelectedItem());
        deleteButton.setToolTipText("選択中のタスク／プロジェクトを削除");

        JButton refreshButton = createToolbarButton("更新", IconManager.IconType.REFRESH, Color.WHITE, AppTheme.PRIMARY, AppTheme.PRIMARY);
        refreshButton.putClientProperty("app.button.variant", "toolbar-outline-primary");
        refreshButton.addActionListener(e -> refreshTaskViews());

        panel.add(addTaskButton);
        panel.add(searchButton);
        panel.add(deleteButton);
        panel.add(refreshButton);

        return panel;
    }

    private JPanel createWbsTabPanel() {
        return new WbsPanel();
    }

    private JPanel createKanbanTabPanel() {
        return new KanbanPanel(this);
    }

    private JPanel createGanttChartTabPanel() {
        return new GanttPanel();
    }

    private JScrollPane createGanttScrollPane(GanttPanel chart) {
        JScrollPane scrollPane = new JScrollPane(chart);
        scrollPane.setBorder(null);
        scrollPane.setRowHeaderView(chart.createTaskNameColumn());
        scrollPane.setColumnHeaderView(chart.createDateHeader());
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new JPanel());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JButton createToolbarButton(String text, IconManager.IconType iconType, Color bg, Color fg, Color borderColor) {
        Object icon = IconManager.getIcon(iconType);
        JButton btn;
        if (icon instanceof ImageIcon) {
            btn = new AppTheme.RoundedFillButton(text, 8);
            btn.setIcon((ImageIcon) icon);
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        } else {
            btn = new AppTheme.RoundedFillButton(icon + "  " + text, 8);
        }
        btn.putClientProperty("app.button.variant", "toolbar");
        btn.setFont(AppTheme.FONT_HEADER);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(true);
        btn.setBorder(AppTheme.createRoundedBorder(borderColor, 8, 2, 2, 2, 2));
        btn.setMargin(new Insets(4, 12, 4, 12));
        btn.setRolloverEnabled(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setPreferredSize(new Dimension(Math.max(100, btn.getFontMetrics(btn.getFont()).stringWidth(btn.getText()) + 36), 36));
        btn.setMinimumSize(new Dimension(100, 36));
        return btn;
    }

    private void handleSearchFromToolbar() {
        if (mainTabbedPane == null) {
            return;
        }

        int selectedIndex = mainTabbedPane.getSelectedIndex();
        if (selectedIndex == 0 && wbsPanel != null) {
            WbsSearchDialog dialog = new WbsSearchDialog((Frame) SwingUtilities.getWindowAncestor(this), wbsPanel,
                    wbsPanel.getCurrentSearchKeyword(), wbsPanel.getCurrentStatusFilter());
            boolean ok = dialog.showDialog();
            if (ok) {
                WbsSearchDialog.SearchCriteria criteria = dialog.getCriteria();
                wbsPanel.applySearchCriteria(criteria.getKeyword(), criteria.getStatus());
            }
            return;
        }

        String initialKeyword = null;
        String initialStatus = "すべて";
        if (selectedIndex == 1 && kanbanPanel != null) {
            initialKeyword = kanbanPanel.getCurrentSearchKeyword();
            initialStatus = kanbanPanel.getCurrentStatusFilter() != null ? kanbanPanel.getCurrentStatusFilter() : "すべて";
        } else if (selectedIndex == 2 && ganttPanel != null) {
            initialKeyword = ganttPanel.getCurrentSearchKeyword();
            initialStatus = ganttPanel.getCurrentStatusFilter() != null ? ganttPanel.getCurrentStatusFilter() : "すべて";
        }

        Runnable projectAction = null;
        Runnable clearAction = null;
        if (selectedIndex == 1 && kanbanPanel != null) {
            projectAction = kanbanPanel::handleExistingProject;
            clearAction = kanbanPanel::handleClearFilter;
        } else if (selectedIndex == 2 && ganttPanel != null) {
            projectAction = ganttPanel::handleExistingProject;
            clearAction = ganttPanel::handleClearFilter;
        }

        TaskSearchDialog dialog = new TaskSearchDialog((Frame) SwingUtilities.getWindowAncestor(this), initialKeyword, initialStatus, projectAction, clearAction);
        boolean ok = dialog.showDialog();
        if (!ok) {
            return;
        }

        TaskSearchDialog.SearchCriteria criteria = dialog.getCriteria();
        if (selectedIndex == 1 && kanbanPanel != null) {
            kanbanPanel.applySearchFilter(criteria.getKeyword(), criteria.getStatus());
        } else if (selectedIndex == 2 && ganttPanel != null) {
            ganttPanel.applySearchFilter(criteria.getKeyword(), criteria.getStatus());
        }
    }

    private void handleAddTask() {
        TaskDialog dialog = new TaskDialog(this);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            String entryType = dialog.getEntryType();
            if (!"プロジェクト".equals(entryType) && dialog.getParentProjectId() == null) {
                JOptionPane.showMessageDialog(this, "工程・タスクは親プロジェクトまたは親工程を選択してから登録してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate start = LocalDate.parse(dialog.getStartDate());
            LocalDate end = LocalDate.parse(dialog.getEndDate());

            Integer parentProjectId = dialog.getParentProjectId();
            Integer parentId = dialog.getParentTaskId();
            int level = 1;

            if ("工程".equals(entryType) && parentProjectId != null) {
                level = 2;
                parentId = parentProjectId;
            } else if ("タスク".equals(entryType) && parentId != null) {
                Task parentTask = TaskDao.getTaskById(parentId);
                level = (parentTask != null && parentTask.getLevel() == 2) ? 3 : 2;
                if (parentTask != null && parentTask.getLevel() == 1) {
                    parentId = parentTask.getId();
                    level = 2;
                }
            } else if ("タスク".equals(entryType) && parentProjectId != null) {
                parentId = parentProjectId;
                level = 2;
            }

            Task newTask = new Task(
                    0,
                    dialog.getTaskName(),
                    parentId,
                    level,
                    dialog.getProgress(),
                    "未着手",
                    start,
                    end
            );
            newTask.setAssignee(dialog.getAssignee());
            newTask.setPriority(dialog.getPriority() != null ? dialog.getPriority() : Task.DEFAULT_PRIORITY);

            TaskDao.addTask(newTask);
            refreshTaskViews();
        }
    }

    private void handleDeleteSelectedItem() {
        Task selectedTask = null;
        String selectionSource = "WBS";

        if (mainTabbedPane != null) {
            int selectedIndex = mainTabbedPane.getSelectedIndex();
            if (selectedIndex == 1 && kanbanPanel != null) {
                selectedTask = kanbanPanel.getSelectedTask();
                selectionSource = "カンバン";
            } else if (selectedIndex == 2 && ganttPanel != null) {
                selectedTask = ganttPanel.getSelectedTask();
                selectionSource = "ガントチャート";
            }
        }

        if (selectedTask == null && wbsPanel != null) {
            selectedTask = wbsPanel.getSelectedTask();
            selectionSource = "WBS";
        }

        if (selectedTask == null) {
            JOptionPane.showMessageDialog(this, "削除する項目を選択してください。\nWBSツリー、カンバンカード、ガントバーのいずれかを選んでください。", "削除できません", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "「" + selectedTask.getName() + "」を削除してもよろしいですか？\n※配下の子タスクも削除されます。",
                selectionSource + "の削除確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (mainTabbedPane != null && mainTabbedPane.getSelectedIndex() == 1 && kanbanPanel != null) {
                kanbanPanel.deleteSelectedTask();
            } else if (mainTabbedPane != null && mainTabbedPane.getSelectedIndex() == 2 && ganttPanel != null) {
                ganttPanel.deleteSelectedTask();
            } else if (wbsPanel != null) {
                wbsPanel.deleteSelectedTask();
            }
            refreshTaskViews();
        }
    }

    public void refreshTaskViews() {
        if (refreshInProgress) {
            return;
        }

        refreshInProgress = true;
        SwingWorker<List<Task>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Task> doInBackground() {
                return TaskDao.getAllTasks();
            }

            @Override
            protected void done() {
                try {
                    List<Task> tasks = get();
                    if (wbsPanel != null) {
                        wbsPanel.refreshWbs(tasks);
                    }
                    if (kanbanPanel != null) {
                        kanbanPanel.refreshKanban(tasks);
                    }
                    if (ganttPanel != null) {
                        ganttPanel.setTasks(tasks);
                        if (ganttScrollPane != null) {
                            ganttScrollPane.revalidate();
                            ganttScrollPane.repaint();
                        }
                    }
                } catch (Exception e) {
                    org.example.util.Logger.error("画面更新中にエラー", e);
                    if (wbsPanel != null) wbsPanel.refreshWbs();
                    if (kanbanPanel != null) kanbanPanel.refreshKanban();
                    if (ganttPanel != null) ganttPanel.repaint();
                } finally {
                    refreshInProgress = false;
                }
            }
        };

        worker.execute();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "ログアウトしてログイン画面に戻りますか？",
                "ログアウト確認",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            UserSession.logout();
            new LoginFrame().setVisible(true);
            this.dispose();
        }
    }

    private void handleExport() {
        List<Task> tasks = TaskDao.getAllTasks();
        if (tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "エクスポートするタスクがありません。", "情報", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] options = {"Excel対応CSV (BOM付き)", "Excel対応TSV (タブ区切り)", "JSON形式", "従来形式CSV"};
        String selectedFormat = (String) JOptionPane.showInputDialog(
                this,
                "エクスポート形式を選択してください:",
                "エクスポート形式選択",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selectedFormat == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("エクスポート先を選択");

        String extension = ".csv";
        if (selectedFormat.equals("Excel対応TSV (タブ区切り)")) {
            extension = ".tsv";
        } else if (selectedFormat.equals("JSON形式")) {
            extension = ".json";
        }

        fileChooser.setSelectedFile(new File("tasknavi_export" + extension));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();

        try {
            if (selectedFormat.equals("Excel対応CSV (BOM付き)")) {
                CsvUtil.exportToCsvWithBom(file, tasks);
                JOptionPane.showMessageDialog(this, "Excel対応CSVとしてエクスポートしました。", "完了", JOptionPane.INFORMATION_MESSAGE);
            } else if (selectedFormat.equals("Excel対応TSV (タブ区切り)")) {
                CsvUtil.exportToTsv(file, tasks);
                JOptionPane.showMessageDialog(this, "Excel対応TSVとしてエクスポートしました。", "完了", JOptionPane.INFORMATION_MESSAGE);
            } else if (selectedFormat.equals("JSON形式")) {
                CsvUtil.exportToJson(file, tasks);
                JOptionPane.showMessageDialog(this, "JSON形式としてエクスポートしました。", "完了", JOptionPane.INFORMATION_MESSAGE);
            } else {
                CsvUtil.exportToCsv(file, tasks);
                JOptionPane.showMessageDialog(this, "従来形式CSVとしてエクスポートしました。", "完了", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "エクスポート中にエラーが発生しました: " + e.getMessage(), "エラー", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void handleImport() {
        String[] options = {"JSON形式", "従来形式CSV"};
        String selectedFormat = (String) JOptionPane.showInputDialog(
                this,
                "インポート形式を選択してください:",
                "インポート形式選択",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selectedFormat == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("インポート元ファイルを選択");

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();

        try {
            List<Task> importedTasks;
            if (selectedFormat.equals("JSON形式")) {
                importedTasks = CsvUtil.importFromJson(file);
            } else {
                importedTasks = CsvUtil.importFromCsv(file);
            }

            if (importedTasks.isEmpty()) {
                JOptionPane.showMessageDialog(this, "インポートするタスクがありませんでした。", "情報", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int importMode = JOptionPane.showConfirmDialog(
                    this,
                    importedTasks.size() + "件のタスクが見つかりました。\n既存のタスクをすべて削除してインポートしますか？\n（いいえを選択すると追加インポート）",
                    "インポート方法",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );

            if (importMode == JOptionPane.CANCEL_OPTION) {
                return;
            }

            TaskDao.importTasks(importedTasks, importMode == JOptionPane.YES_OPTION);

            refreshTaskViews();
            JOptionPane.showMessageDialog(this, importedTasks.size() + "件のタスクをインポートしました。", "完了", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "インポート中にエラーが発生しました: " + e.getMessage(), "エラー", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}