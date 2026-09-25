package org.example;

import net.miginfocom.swing.MigLayout;
import org.example.event.TaskEventBus;
import org.example.ui.MainGlobalSearchBar;
import org.example.ui.MainHeaderBar;
import org.example.ui.MainTabChrome;
import org.example.ui.MainTaskAddHelper;
import org.example.ui.MainThemeSupport;
import org.example.ui.MainToolbarButtons;
import org.example.ui.TaskDataIoHelper;
import org.example.ui.TaskDeleteHelper;
import org.example.ui.calendar.CalendarPanel;
import org.example.ui.gantt.GanttPanel;
import org.example.ui.kanban.KanbanPanel;
import org.example.ui.wbs.WbsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * 【メイン画面クラス】
 * アプリケーションのメインウィンドウであり、ヘッダー（ツールバー）と
 * 各ビュー（WBS、カンバン、ガントチャート、カレンダー）を保持するコンテナとして機能します。
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final TaskService taskService;
    private final JTabbedPane mainTabbedPane;
    private final JToggleButton themeToggleButton;
    private final MainGlobalSearchBar globalSearchBar;
    private JLabel userStatusLabel;
    private JPanel headerPanel;
    private JPanel toolbarPanel;

    public MainFrame(TaskService taskService) {
        this.taskService = taskService;
        this.globalSearchBar = new MainGlobalSearchBar(this::getActiveComponent);

        setTitle(AppMessages.get("mainframe.title.full"));
        setSize(UiConstants.WINDOW_WIDTH_MAIN, UiConstants.WINDOW_HEIGHT_MAIN);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new MigLayout("fill, insets 0, gap 0", "[grow]", "[][grow]"));
        contentPane.setBackground(AppTheme.BACKGROUND);

        AppTheme.setDarkMode(AppTheme.isDarkMode());
        themeToggleButton = createThemeToggleButton();
        themeToggleButton.setSelected(AppTheme.isDarkMode());

        MainHeaderBar.Result header = MainHeaderBar.create(themeToggleButton, new MainHeaderBar.Actions() {
            @Override
            public void onImport() {
                handleImport();
            }

            @Override
            public void onExport() {
                handleExport();
            }

            @Override
            public void onLogout() {
                handleLogout();
            }
        });
        headerPanel = header.panel();
        userStatusLabel = header.userStatusLabel();
        toolbarPanel = createToolBarPanel();

        JPanel topContainer = new JPanel(new MigLayout("fill, insets 0, gap 0", "[grow]", "[][grow]"));
        topContainer.setBackground(AppTheme.BACKGROUND);
        topContainer.add(headerPanel, "grow, wrap");
        topContainer.add(toolbarPanel, "grow");

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(AppTheme.FONT_HEADER);
        mainTabbedPane.setOpaque(false);
        mainTabbedPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER_COLOR));
        mainTabbedPane.setFocusable(false);

        WbsPanel wbsPanel = new WbsPanel(taskService);
        KanbanPanel kanbanPanel = new KanbanPanel(taskService);
        GanttPanel ganttChartPanel = new GanttPanel(taskService);
        CalendarPanel calendarPanel = new CalendarPanel(taskService);

        JScrollPane ganttScroll = new JScrollPane(ganttChartPanel);
        ganttScroll.setColumnHeaderView(ganttChartPanel.createDateHeader());
        ganttScroll.setRowHeaderView(ganttChartPanel.createTaskNameColumn());
        ganttScroll.getViewport().setBackground(AppTheme.BACKGROUND);
        ganttScroll.setBorder(null);

        addMainTab(AppMessages.get("mainframe.tab.wbs"), IconManager.IconType.TAB_WBS, wbsPanel);
        addMainTab(AppMessages.get("mainframe.tab.kanban"), IconManager.IconType.TAB_KANBAN, kanbanPanel);
        addMainTab(AppMessages.get("mainframe.tab.gantt"), IconManager.IconType.TAB_GANTT, ganttScroll);
        addMainTab(AppMessages.get("mainframe.tab.calendar"), IconManager.IconType.TAB_CALENDAR, calendarPanel);

        mainTabbedPane.setSelectedIndex(3);

        mainTabbedPane.addChangeListener(e -> {
            MainTabChrome.updateTabTextColors(mainTabbedPane);
            globalSearchBar.syncActive();
            MainThemeSupport.updateViewTheme(getActiveComponent());
        });
        MainTabChrome.updateTabTextColors(mainTabbedPane);

        contentPane.add(topContainer, "grow, wrap");
        contentPane.add(mainTabbedPane, "grow");

        add(contentPane);
        applyThemeToWindow();
    }

    private void addMainTab(String title, IconManager.IconType iconType, Component component) {
        mainTabbedPane.addTab("  " + title + "  ", component);
        int tabIndex = mainTabbedPane.getTabCount() - 1;
        mainTabbedPane.setIconAt(tabIndex, MainTabChrome.createTabIcon(iconType, AppTheme.TEXT_MUTED));
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
        if (headerPanel != null) {
            headerPanel.setBackground(AppTheme.PANEL_BG);
        }
        if (toolbarPanel != null) {
            toolbarPanel.setBackground(AppTheme.PANEL_BG);
        }

        globalSearchBar.updateTheme();

        Container contentPane = getContentPane();
        if (contentPane != null) {
            contentPane.setBackground(AppTheme.BACKGROUND);
            Set<Component> skip = new HashSet<>();
            if (globalSearchBar.searchContainer() != null) {
                skip.add(globalSearchBar.searchContainer());
            }
            if (globalSearchBar.searchField() != null) {
                skip.add(globalSearchBar.searchField());
            }
            if (globalSearchBar.filterButton() != null) {
                skip.add(globalSearchBar.filterButton());
            }
            MainThemeSupport.applyToContainer(contentPane, skip, themeToggleButton);
        }

        if (mainTabbedPane != null) {
            for (int i = 0; i < mainTabbedPane.getTabCount(); i++) {
                MainThemeSupport.updateViewTheme(mainTabbedPane.getComponentAt(i));
            }
            mainTabbedPane.setBackground(AppTheme.BACKGROUND);
            mainTabbedPane.setForeground(AppTheme.TEXT_PRIMARY);
            MainTabChrome.applyTabbedPaneStyle(mainTabbedPane);
            MainTabChrome.updateTabTextColors(mainTabbedPane);
        }

        revalidate();
        repaint();
    }

    private JPanel createToolBarPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 6 18 6 18, gap 8", "[left][grow,right]", "[]"));
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
                new EmptyBorder(0, 0, 0, 0)
        ));

        JPanel leftActionGroup = new JPanel(new MigLayout("insets 0, gap 8", "[][][]"));
        leftActionGroup.setOpaque(false);

        JButton addTaskButton = MainToolbarButtons.create(
                AppMessages.get("mainframe.button.add"), IconManager.IconType.ADD,
                AppTheme.PRIMARY, Color.WHITE, AppTheme.PRIMARY);
        addTaskButton.putClientProperty("app.button.variant", "toolbar-primary");
        addTaskButton.addActionListener(e -> MainTaskAddHelper.showAndAdd(this, taskService));
        addTaskButton.setToolTipText(AppMessages.get("mainframe.tooltip.add"));

        Color dangerColor = new Color(220, 38, 38);
        JButton deleteButton = MainToolbarButtons.create(
                AppMessages.get("mainframe.button.delete"), IconManager.IconType.DELETE,
                Color.WHITE, dangerColor, dangerColor);
        deleteButton.putClientProperty("app.button.variant", "toolbar-outline-danger");
        deleteButton.addActionListener(e -> handleDeleteSelectedItem());
        deleteButton.setToolTipText(AppMessages.get("mainframe.tooltip.delete"));

        JButton refreshButton = MainToolbarButtons.create(
                AppMessages.get("mainframe.button.refresh"), IconManager.IconType.REFRESH,
                Color.WHITE, AppTheme.PRIMARY, AppTheme.PRIMARY);
        refreshButton.putClientProperty("app.button.variant", "toolbar-outline-primary");
        refreshButton.addActionListener(e ->
                TaskEventBus.getInstance().post(
                        new org.example.event.TaskEvent(org.example.event.TaskEvent.Type.REFRESH_ALL, null)));

        leftActionGroup.add(addTaskButton);
        leftActionGroup.add(deleteButton);
        leftActionGroup.add(refreshButton);

        JPanel rightSearchGroup = new JPanel(new MigLayout("insets 0, gap 6", "[][]"));
        rightSearchGroup.setOpaque(false);
        rightSearchGroup.add(globalSearchBar.createSearchBox());
        rightSearchGroup.add(globalSearchBar.createFilterButton());

        panel.add(leftActionGroup, "left");
        panel.add(rightSearchGroup, "right");
        return panel;
    }

    private Component getActiveComponent() {
        if (mainTabbedPane == null) {
            return null;
        }
        Component selectedComp = mainTabbedPane.getSelectedComponent();
        if (selectedComp instanceof JScrollPane scrollPane) {
            return scrollPane.getViewport().getView();
        }
        return selectedComp;
    }

    private void handleDeleteSelectedItem() {
        Component selectedComp = getActiveComponent();
        Task selectedTask = selectedComp instanceof SearchablePanel targetPanel
                ? targetPanel.getSelectedTask()
                : null;
        TaskDeleteHelper.confirmAndDelete(this, taskService, selectedTask);
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                AppMessages.get("mainframe.logout.confirm"),
                AppMessages.get("mainframe.logout.confirm.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            UserSession.logout();
            this.dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }

    private void handleExport() {
        TaskDataIoHelper.exportTasks(this, taskService);
    }

    private void handleImport() {
        TaskDataIoHelper.importTasks(this, taskService);
    }
}
