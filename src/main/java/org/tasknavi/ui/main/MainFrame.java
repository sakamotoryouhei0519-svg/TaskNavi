package org.tasknavi.ui.main;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppVersion;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;
import org.tasknavi.team.TeamNotice;
import org.tasknavi.team.TeamProject;
import org.tasknavi.team.TeamSettings;
import org.tasknavi.ui.AppIcon;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.IconManager;
import org.tasknavi.ui.SearchablePanel;
import org.tasknavi.ui.TaskDataIoHelper;
import org.tasknavi.ui.TaskDeleteHelper;
import org.tasknavi.ui.TaskEditHelper;
import org.tasknavi.ui.UiConstants;
import org.tasknavi.ui.calendar.CalendarPanel;
import org.tasknavi.ui.gantt.GanttMetrics;
import org.tasknavi.ui.gantt.GanttPanel;
import org.tasknavi.ui.kanban.KanbanPanel;
import org.tasknavi.ui.team.ProjectSwitcher;
import org.tasknavi.ui.team.TeamSession;
import org.tasknavi.ui.today.DueReminder;
import org.tasknavi.ui.today.TodayPanel;
import org.tasknavi.ui.wbs.WbsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * メインの窓。ヘッダーとツールバー、5つのタブ（今日・WBS・カンバン・ガント・カレンダー）をつなぐ。
 * ヘッダーなどの部品は同じパッケージの Main* クラスに分けている。
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String PREF_LAST_TAB = "tasknavi.main.lastTabKey";
    private static final String TAB_TODAY = "today";

    private final TaskService taskService;
    private final JTabbedPane mainTabbedPane;
    private final List<String> tabKeys = new ArrayList<>();
    private DueReminder dueReminder;
    private final JToggleButton themeToggleButton;
    private final MainGlobalSearchBar globalSearchBar;
    private final TeamSession teamSession;
    private final ProjectSwitcher projectSwitcher;
    private final ProjectSources projectSources;
    private boolean started;
    private JLabel userStatusLabel;
    private JPanel headerPanel;
    private JPanel toolbarPanel;

    public MainFrame(TaskService taskService) {
        this.taskService = taskService;
        this.globalSearchBar = new MainGlobalSearchBar(this::getActiveComponent);

        String profile = TeamSettings.profile();
        setTitle(AppMessages.get("mainframe.title.full") + "  (" + AppVersion.current() + ")"
                + (profile.isEmpty() ? "" : " [" + profile + "]"));
        setIconImages(AppIcon.windowIcons());
        setMinimumSize(new Dimension(UiConstants.WINDOW_MIN_WIDTH_MAIN, UiConstants.WINDOW_MIN_HEIGHT_MAIN));
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(UiConstants.WINDOW_WIDTH_MAIN, screen.width),
                Math.min(UiConstants.WINDOW_HEIGHT_MAIN, screen.height));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new MigLayout("fill, insets 0, gap 0", "[grow]", "[][grow]"));
        contentPane.setBackground(AppTheme.BACKGROUND);

        AppTheme.setDarkMode(AppTheme.isDarkMode());
        themeToggleButton = createThemeToggleButton();
        themeToggleButton.setSelected(AppTheme.isDarkMode());

        teamSession = new TeamSession(TeamSettings.userDefault(), this);
        projectSwitcher = new ProjectSwitcher(teamSession);
        projectSources = new ProjectSources(taskService, teamSession);
        teamSession.setListener(new TeamSession.Listener() {
            @Override
            public void projectChanged(TeamProject opened) {
                projectSources.invalidate();
                taskService.useStore(opened != null ? teamSession.storeOf(opened) : null);
                projectSwitcher.refresh();
                refreshUserName();
            }

            @Override
            public void otherProjectsChanged() {
                projectSources.invalidate();
                taskService.reload();
                refreshUserName();
            }

            @Override
            public void dataChanged() {
                if (!taskService.isPersonal()) {
                    taskService.reload();
                }
                projectSwitcher.refresh();
            }

            @Override
            public void statusChanged() {
                projectSwitcher.refresh();
                refreshUserName();
            }

            @Override
            public void noticesArrived(Map<String, List<TeamNotice>> byProject) {
                if (dueReminder != null) {
                    dueReminder.showNotices(byProject);
                }
            }
        });

        MainHeaderBar.Result header = MainHeaderBar.create(themeToggleButton, projectSwitcher.component(),
                teamSession.displayName(), new MainHeaderBar.Actions() {
            @Override
            public void onImport() {
                handleImport();
            }

            @Override
            public void onExport() {
                handleExport();
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

        TodayPanel todayPanel = new TodayPanel(taskService, projectSources::all);
        WbsPanel wbsPanel = new WbsPanel(taskService);
        KanbanPanel kanbanPanel = new KanbanPanel(taskService);
        GanttPanel ganttChartPanel = new GanttPanel(taskService);
        CalendarPanel calendarPanel = new CalendarPanel(taskService);

        JScrollPane ganttScroll = new JScrollPane(ganttChartPanel);
        ganttScroll.setColumnHeaderView(ganttChartPanel.createDateHeader());
        ganttScroll.setRowHeaderView(ganttChartPanel.createTaskNameColumn());
        ganttScroll.getViewport().setBackground(AppTheme.BACKGROUND);
        ganttScroll.setBorder(null);
        ganttScroll.getVerticalScrollBar().setUnitIncrement(GanttMetrics.ROW_HEIGHT * 3);
        ganttScroll.getHorizontalScrollBar().setUnitIncrement(80);

        addMainTab(TAB_TODAY, AppMessages.get("mainframe.tab.today"), IconManager.IconType.TAB_TODAY, todayPanel);
        addMainTab("wbs", AppMessages.get("mainframe.tab.wbs"), IconManager.IconType.TAB_WBS, wbsPanel);
        addMainTab("kanban", AppMessages.get("mainframe.tab.kanban"), IconManager.IconType.TAB_KANBAN, kanbanPanel);
        addMainTab("gantt", AppMessages.get("mainframe.tab.gantt"), IconManager.IconType.TAB_GANTT, ganttScroll);
        addMainTab("calendar", AppMessages.get("mainframe.tab.calendar"), IconManager.IconType.TAB_CALENDAR, calendarPanel);

        mainTabbedPane.setSelectedIndex(loadLastTabIndex());

        mainTabbedPane.addChangeListener(e -> {
            MainTabChrome.updateTabTextColors(mainTabbedPane);
            globalSearchBar.syncActive();
            MainThemeSupport.updateViewTheme(getActiveComponent());
            saveLastTab(mainTabbedPane.getSelectedIndex());
        });
        MainTabChrome.updateTabTextColors(mainTabbedPane);

        contentPane.add(topContainer, "grow, wrap");
        contentPane.add(mainTabbedPane, "grow");

        add(contentPane);
        applyThemeToWindow();
        installShortcuts();
    }

    private void installShortcuts() {
        InputMap input = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getRootPane().getActionMap();

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        actions.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                globalSearchBar.focusSearch();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "addTask");
        actions.put("addTask", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAdd();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editSelected");
        actions.put("editSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (typingFocused()) {
                    return;
                }
                Component selectedComp = getActiveComponent();
                Task selectedTask = selectedComp instanceof SearchablePanel targetPanel
                        ? targetPanel.getSelectedTask()
                        : null;
                if (selectedTask != null) {
                    TaskEditHelper.openAndSave(selectedComp, serviceForSelection(selectedComp), selectedTask);
                }
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
        actions.put("deleteSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (typingFocused()) {
                    return;
                }
                Component selectedComp = getActiveComponent();
                Task selectedTask = selectedComp instanceof SearchablePanel targetPanel
                        ? targetPanel.getSelectedTask()
                        : null;
                if (selectedTask == null) {
                    return;
                }
                TaskDeleteHelper.confirmAndDelete(MainFrame.this, serviceForSelection(selectedComp), selectedTask);
            }
        });
    }

    private static boolean typingFocused() {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focus instanceof javax.swing.text.JTextComponent || focus instanceof JComboBox<?>;
    }

    /** タブを足しても前に開いていた画面に戻れるよう、番号ではなく名前で覚える。 */
    private int loadLastTabIndex() {
        String saved = Preferences.userNodeForPackage(MainFrame.class).get(PREF_LAST_TAB, TAB_TODAY);
        int index = tabKeys.indexOf(saved);
        return Math.max(index, 0);
    }

    private void saveLastTab(int index) {
        if (index < 0 || index >= tabKeys.size()) {
            return;
        }
        Preferences.userNodeForPackage(MainFrame.class).put(PREF_LAST_TAB, tabKeys.get(index));
    }

    /**
     * 前回開いていた共有プロジェクトと最近のプロジェクトを開き、開き終わったら期限の通知を始める
     * （開く前に数えると、共有プロジェクトの分が抜けるため）。
     * テストで通知が出たり共有フォルダを読みに行ったりしないよう、コンストラクタでは始めない。
     */
    public void startDueReminders() {
        if (started) {
            return;
        }
        started = true;
        teamSession.start(() -> {
            if (!isDisplayable() || dueReminder != null) {
                return;
            }
            dueReminder = new DueReminder(taskService, projectSources::all, this::showTodayTab);
            dueReminder.start();
        });
    }

    private void showTodayTab() {
        if (getExtendedState() == Frame.ICONIFIED) {
            setExtendedState(Frame.NORMAL);
        }
        toFront();
        mainTabbedPane.setSelectedIndex(tabKeys.indexOf(TAB_TODAY));
    }

    @Override
    public void dispose() {
        if (dueReminder != null) {
            dueReminder.stop();
            dueReminder = null;
        }
        teamSession.stop();
        super.dispose();
    }

    private void addMainTab(String key, String title, IconManager.IconType iconType, Component component) {
        tabKeys.add(key);
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
        toggle.addActionListener(e -> {
            AppTheme.setDarkMode(toggle.isSelected());
            applyThemeToWindow();
        });
        return toggle;
    }

    private void applyThemeToWindow() {
        if (themeToggleButton != null) {
            MainHeaderBar.styleThemeToggle(themeToggleButton);
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
            skip.add(projectSwitcher.component());
            MainThemeSupport.applyToContainer(contentPane, skip, themeToggleButton);
        }
        projectSwitcher.refresh();

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
        addTaskButton.addActionListener(e -> handleAdd());
        addTaskButton.setToolTipText(AppMessages.get("mainframe.tooltip.add"));

        JButton deleteButton = MainToolbarButtons.create(
                AppMessages.get("mainframe.button.delete"), IconManager.IconType.DELETE,
                AppTheme.controlBackground(), AppTheme.danger(), AppTheme.danger());
        deleteButton.putClientProperty("app.button.variant", "toolbar-outline-danger");
        deleteButton.addActionListener(e -> handleDeleteSelectedItem());
        deleteButton.setToolTipText(AppMessages.get("mainframe.tooltip.delete"));

        JButton refreshButton = MainToolbarButtons.create(
                AppMessages.get("mainframe.button.refresh"), IconManager.IconType.REFRESH,
                AppTheme.controlBackground(), AppTheme.PRIMARY, AppTheme.PRIMARY);
        refreshButton.putClientProperty("app.button.variant", "toolbar-outline-primary");
        refreshButton.addActionListener(e -> {
            teamSession.reload();
            taskService.reload();
        });

        leftActionGroup.add(addTaskButton);
        leftActionGroup.add(deleteButton);
        leftActionGroup.add(refreshButton);

        JPanel rightSearchGroup = new JPanel(new MigLayout("insets 0, gap 6", "[][]"));
        rightSearchGroup.setOpaque(false);
        rightSearchGroup.add(globalSearchBar.createFilterSummaryLabel(), "wmin 0");
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

    private void handleAdd() {
        MainTaskAddHelper.showAndAdd(this, taskService);
    }

    private void handleDeleteSelectedItem() {
        Component selectedComp = getActiveComponent();
        Task selectedTask = selectedComp instanceof SearchablePanel targetPanel
                ? targetPanel.getSelectedTask()
                : null;
        TaskDeleteHelper.confirmAndDelete(this, serviceForSelection(selectedComp), selectedTask);
    }

    /** 「今日」で開いていないプロジェクトのタスクを選んでいれば、そのプロジェクトの入口。 */
    private TaskService serviceForSelection(Component selectedComp) {
        TaskService selected = selectedComp instanceof SearchablePanel panel ? panel.selectedTaskService() : null;
        return selected != null ? selected : taskService;
    }

    private void refreshUserName() {
        if (userStatusLabel != null) {
            MainHeaderBar.showUserName(userStatusLabel, teamSession.displayName());
        }
    }

    private void handleExport() {
        TaskDataIoHelper.exportTasks(this, taskService, globalSearchBar.keyword(), globalSearchBar.status());
    }

    private void handleImport() {
        TaskDataIoHelper.importTasks(this, taskService);
    }
}

