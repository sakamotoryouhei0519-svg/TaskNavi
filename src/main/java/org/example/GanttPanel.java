package org.example;

// --- 画面 UI (Swing) 関連のライブラリ ---
import javax.swing.*;

// --- レイアウト・イベント関連のライブラリ ---
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

// --- 日付処理・コレクション関連のライブラリ ---
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【ガントチャート画面パネルクラス】
 * WBS階層表示、2段構造日付ヘッダー、ドラッグ操作（半透明・マウス追従）、
 * およびバーの長さによる進行度（%）グラフィカル表現に対応しています。
 *
 * 【重要単語の解説】
 * - ガントチャート: タスクの期間を横棒で表現する図
 * - ドラッグ操作: マウスで要素を移動・リサイズする操作
 * - 半透明: 要素が透けて見える表示状態
 * - マウス追従: マウスの動きに合わせて要素が移動する機能
 *
 * 【コードの読み方】
 * 1. 定数で描画サイズを定義
 * 2. ドラッグ操作用のフィールドを定義
 * 3. paintComponentで描画処理を実装
 * 4. マウスイベントでドラッグ操作を処理
 */
public class GanttPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    // ==========================================
    //  【描画用のサイズ定数設定】
    // ==========================================
    // 【重要】static final: クラス全体で共通の定数（変更不可）
    private static final int TASK_NAME_WIDTH = 240; // 【重要】左側タスク領域の幅 (px)
    private static final int DEFAULT_DAY_WIDTH = 35; // 【重要】1日あたりのデフォルト横幅 (px)
    private int dayWidth = 35;           // 【重要】1日あたりの横幅 (px) - ズーム対応
    private static final int ROW_HEIGHT = 38;        // 【重要】1行の高さ (px)
    private static final int BAR_HEIGHT = 22;        // 【重要】ガントバーの高さ (px)
    private static final int HEADER_HEIGHT = 60;     // 【重要】ヘッダーの高さ (px)
    private int totalDays = 31;        // 【重要】表示日数（動的に計算）
    private LocalDate visibleStartDate = LocalDate.now().minusDays(14);
    private LocalDate visibleEndDate = LocalDate.now().plusDays(30);
    private boolean manualVisibleWindow = false;
    // 【重要】曜日表記用配列
    private static final String[] WEEKDAYS = {"月", "火", "水", "木", "金", "土", "日"};

    // ==========================================
    //  【ドラッグ操作用フィールド】
    // ==========================================
    private Task draggingTask = null;  // 【重要】ドラッグ中のタスク
    private int dragMode = 0;         // 【重要】ドラッグモード（0:なし、1:移動、2:右端リサイズ、3:左端リサイズ）
    private int dragStartX = 0;       // 【重要】ドラッグ開始時のX座標
    private int currentMouseX = 0;   // 【重要】現在のマウスX座標（追従計算用）
    private LocalDate originalStartDate = null;  // 【重要】ドラッグ開始時の開始日
    private LocalDate originalEndDate = null;    // 【重要】ドラッグ開始時の終了日
    private static final int RESIZE_HANDLE_WIDTH = 6; // 【重要】リサイズ反応領域(px)

    // ==========================================
    //  【検索・フィルタ用フィールド】
    // ==========================================
    private JTextField txtSearchKeyword;        // 互換用。ガントでは検索欄を表示せず、フィルタは現在の検索語だけで管理する。
    private String currentSearchKeyword = null; // 現在適用されている検索ワード（nullは検索なし）
    private String currentStatusFilter = null;   // ステータスでの絞り込み
    private Integer currentProjectFilterId = null;
    private final Map<Integer, Set<Integer>> projectMemberCache = new HashMap<>();

    // ==========================================
    //  【ツールチップ用フィールド】
    // ==========================================
    private Task hoveredTask = null; // ホバー中のタスク
    private Integer selectedTaskId = null;
    private List<Task> cachedTasks = new ArrayList<>();

    // ==========================================
    //  【固定ヘッダー（行ヘッダー／列ヘッダー）参照】
    // ==========================================
    // JScrollPane の setRowHeaderView / setColumnHeaderView に渡した
    // コンポーネントへの参照を保持しておき、タスク件数・表示期間・ズーム倍率が
    // 変わるたびに、こちら側からも確実に再レイアウトさせるために使います。
    // これが無いと、メイン描画側だけ幅・高さが更新されてヘッダー側は
    // 古いサイズのまま残り、スクロール時に日付やタスク行がズレてしまいます。
    private JComponent taskNameColumnView;
    private JComponent dateHeaderView;

    /**
     * 【コンストラクタ】
     * ガントチャートパネルの初期化を行います。
     *
     * 【重要単語の解説】
     * - setBackground(): 背景色を設定
     * - setPreferredSize(): 推奨サイズを設定
     * - Dimension: 幅と高さを表すクラス
     *
     * 【コードの読み方】
     * 1. 背景色を白に設定
     * 2. パネルのサイズを計算して設定
     * 3. マウスイベントリスナーを登録
     */
    public GanttPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(Math.max(800, totalDays * dayWidth), 800));
        refreshTaskCache();

        // マウス操作イベントの登録
        GanttMouseAdapter adapter = new GanttMouseAdapter();
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        addMouseWheelListener(e -> {
            if (isChartInteractionArea(e.getX(), e.getY())) {
                int rotation = e.getWheelRotation();
                if (rotation < 0) {
                    dayWidth = Math.min(80, dayWidth + 3);
                } else {
                    dayWidth = Math.max(24, dayWidth - 3);
                }
                notifyLayoutChanged();
                return;
            }

            scrollParentViewport(e);
        });

        ToolTipManager.sharedInstance().registerComponent(this);
        setToolTipText("");
    }

    public void setTasks(List<Task> tasks) {
        cachedTasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        clearProjectMemberCache();
        manualVisibleWindow = false;
        notifyLayoutChanged();
    }

    /**
     * 【固定ヘッダー再レイアウト通知】
     * タスク件数・表示期間・ズーム倍率（dayWidth）のいずれかが変わった直後に
     * 呼び出します。メインの描画領域（this）だけでなく、行ヘッダー（タスク名列）と
     * 列ヘッダー（カレンダー）にも再計算・再描画を強制することで、
     * スクロール時に「日付がズレる」「バーがヘッダーに被る」問題を防ぎます。
     */
    private void notifyLayoutChanged() {
        revalidate();
        repaint();
        if (taskNameColumnView != null) {
            taskNameColumnView.revalidate();
            taskNameColumnView.repaint();
        }
        if (dateHeaderView != null) {
            dateHeaderView.revalidate();
            dateHeaderView.repaint();
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (hoveredTask != null) {
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html><b>").append(hoveredTask.getName()).append("</b><br>");
            tooltip.append("担当者: ").append(hoveredTask.getAssignee() != null ? hoveredTask.getAssignee() : "未設定").append("<br>");
            tooltip.append("期間: ").append(hoveredTask.getStartDate()).append(" ～ ").append(hoveredTask.getEndDate()).append("<br>");
            tooltip.append("進捗: ").append(hoveredTask.getProgress()).append("%<br>");
           tooltip.append("状態: ").append(hoveredTask.getStatus() != null ? hoveredTask.getStatus() : "未設定").append("<br>");
           tooltip.append("優先度: ").append(hoveredTask.getPriority() != null ? hoveredTask.getPriority() : Task.DEFAULT_PRIORITY);
           tooltip.append("</html>");
           return tooltip.toString();
        }
        return null;
    }


    /**
     * 【ガントチャートの実際の描画処理】
     * このメソッドは、Swing の描画サイクルに合わせて画面に図を出力します。
     *
     * 画面の描画は「何を描きたいか」ではなく、paintComponent() というメソッドの中で
     * 1つずつ図形を組み立てるイメージです。ここでは以下を行っています。
     * - タスク一覧をソートして順番を安定化
     * - 検索ワードで対象を絞り込む
     * - 最古開始日〜最終終了日を基準に日付軸を決める
     * - 左のタスク名列と右のガントバーを描画する
     *
     * @param g Swing が渡してくれるグラフィックスオブジェクト
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (cachedTasks.isEmpty()) {
            refreshTaskCache();
        }

        if (cachedTasks.isEmpty()) {
            return;
        }

        List<Task> allSortedTasks = getSortedTasksForGantt(cachedTasks);
        List<Task> tasks = getVisibleTasks();
        Map<Integer, List<Task>> childMap = buildChildrenMap(allSortedTasks);

        LocalDate[] visibleWindow = calculateVisibleDateWindow(tasks, childMap);
        LocalDate minDate = visibleWindow[0];
        LocalDate maxDate = visibleWindow[1];
        visibleStartDate = minDate;
        visibleEndDate = maxDate;

        totalDays = (int) ChronoUnit.DAYS.between(minDate, maxDate) + 1;
        totalDays = Math.max(31, totalDays);

        int currentY = 0;

        g2.setColor(AppTheme.PANEL_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());

        drawDayGrid(g2, minDate, 0, tasks.size() * ROW_HEIGHT);

        for (Task task : tasks) {
            drawGanttBar(g2, task, minDate, currentY, childMap);
            g2.setColor(new Color(220, 225, 232));
            g2.drawLine(0, currentY + ROW_HEIGHT - 1, getWidth(), currentY + ROW_HEIGHT - 1);
            currentY += ROW_HEIGHT;
        }

        drawTodayLine(g2, minDate, currentY, 0);

        int preferredHeight = Math.max(200, (tasks.size() * ROW_HEIGHT) + 20);
        int preferredWidth = Math.max(800, totalDays * dayWidth);
        if (getPreferredSize().width != preferredWidth || getPreferredSize().height != preferredHeight) {
            setPreferredSize(new Dimension(preferredWidth, preferredHeight));
            revalidate();
        }
    }

    /**
     * 【WBS構造に基づく並び替え処理】
     */
    private List<Task> getSortedTasksForGantt(List<Task> allTasks) {
        Map<Integer, List<Task>> childrenMap = new HashMap<>();
        List<Task> rootTasks = new ArrayList<>();

        for (Task task : allTasks) {
            if (task.getParentId() == null || task.getParentId() == 0) {
                rootTasks.add(task);
            } else {
                childrenMap.computeIfAbsent(task.getParentId(), k -> new ArrayList<>()).add(task);
            }
        }

        List<Task> result = new ArrayList<>();
        for (Task root : rootTasks) {
            collectTasksRecursive(root, childrenMap, result);
        }
        return result;
    }

    private void collectTasksRecursive(Task current, Map<Integer, List<Task>> childrenMap, List<Task> result) {
        result.add(current);
        List<Task> children = childrenMap.get(current.getId());
        if (children != null) {
            for (Task child : children) {
                collectTasksRecursive(child, childrenMap, result);
            }
        }
    }

    /**
     * 【2段ヘッダー描画】
     */
    private void drawHeader(Graphics2D g2, LocalDate minDate, int offsetY) {
        LocalDate today = LocalDate.now();

        int currentMonthStartX = 0;
        LocalDate currentMonth = minDate;
        int monthDaysCount = 0;

        for (int i = 0; i < totalDays; i++) {
            LocalDate date = minDate.plusDays(i);
            if (!date.getMonth().equals(currentMonth.getMonth()) || i == totalDays - 1) {
                if (i == totalDays - 1 && date.getMonth().equals(currentMonth.getMonth())) {
                    monthDaysCount++;
                }

                int groupWidth = Math.max(1, monthDaysCount * dayWidth);
                g2.setColor(AppTheme.isDarkMode() ? new Color(30, 41, 59) : new Color(240, 243, 248));
                g2.fillRect(currentMonthStartX, offsetY, groupWidth, 22);
                g2.setColor(AppTheme.BORDER_COLOR);
                g2.drawRect(currentMonthStartX, offsetY, groupWidth, 22);

                g2.setColor(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                String yearMonthStr = currentMonth.format(DateTimeFormatter.ofPattern("yyyy年 M月"));
                g2.drawString(yearMonthStr, currentMonthStartX + 8, offsetY + 16);

                currentMonthStartX += groupWidth;
                currentMonth = date;
                monthDaysCount = 1;
            } else {
                monthDaysCount++;
            }
        }

        for (int i = 0; i < totalDays; i++) {
            int x = i * dayWidth;
            LocalDate date = minDate.plusDays(i);

            DayOfWeek dow = date.getDayOfWeek();
            boolean isSaturday = dow == DayOfWeek.SATURDAY;
            boolean isSunday = dow == DayOfWeek.SUNDAY;
            boolean isToday = date.equals(today);

            Color bgHeader;
            if (AppTheme.isDarkMode()) {
                if (isToday) {
                    bgHeader = new Color(71, 85, 105);
                } else if (isSaturday) {
                    bgHeader = new Color(30, 41, 59);
                } else if (isSunday) {
                    bgHeader = new Color(30, 41, 59);
                } else {
                    bgHeader = new Color(30, 41, 59);
                }
            } else {
                if (isToday) {
                    bgHeader = new Color(255, 243, 205);
                } else if (isSaturday) {
                    bgHeader = new Color(230, 240, 255);
                } else if (isSunday) {
                    bgHeader = new Color(255, 230, 230);
                } else {
                    bgHeader = new Color(250, 251, 253);
                }
            }

            g2.setColor(bgHeader);
            g2.fillRect(x, offsetY + 22, dayWidth, HEADER_HEIGHT - 22);
            g2.setColor(AppTheme.BORDER_COLOR);
            g2.drawRect(x, offsetY + 22, dayWidth, HEADER_HEIGHT - 22);

            Shape oldClip = g2.getClip();
            g2.clipRect(x, offsetY + 22, dayWidth, HEADER_HEIGHT - 22);

            if (dayWidth >= 18) {
                int dateFontSize = dayWidth >= 26 ? 10 : 9;
                int weekdayFontSize = dayWidth >= 28 ? 9 : 8;
                g2.setFont(new Font("SansSerif", Font.PLAIN, dateFontSize));
                if (AppTheme.isDarkMode()) {
                    g2.setColor(isSunday ? new Color(248, 113, 113) : (isSaturday ? new Color(96, 165, 250) : new Color(226, 232, 240)));
                } else {
                    g2.setColor(isSunday ? new Color(210, 50, 50) : (isSaturday ? new Color(30, 100, 200) : AppTheme.TEXT_PRIMARY));
                }
                String dayText = String.valueOf(date.getDayOfMonth());
                FontMetrics dayMetrics = g2.getFontMetrics();
                int dayTextX = x + Math.max(2, (dayWidth - dayMetrics.stringWidth(dayText)) / 2);
                g2.drawString(dayText, dayTextX, offsetY + 34);

                String dayOfWeekStr = WEEKDAYS[dow.getValue() - 1];
                g2.setFont(new Font("SansSerif", Font.PLAIN, weekdayFontSize));
                FontMetrics weekMetrics = g2.getFontMetrics();
                int weekTextX = x + Math.max(2, (dayWidth - weekMetrics.stringWidth(dayOfWeekStr)) / 2);
                g2.drawString(dayOfWeekStr, weekTextX, offsetY + 46);
                g2.setClip(oldClip);
            } else {
                g2.setClip(oldClip);
            }

            if (date.getDayOfMonth() == 1 && i > 0) {
                g2.setColor(AppTheme.isDarkMode() ? new Color(71, 85, 105) : new Color(180, 180, 180));
                g2.drawLine(x, offsetY, x, HEADER_HEIGHT);
            } else {
                g2.setColor(AppTheme.isDarkMode() ? new Color(71, 85, 105) : new Color(240, 240, 240));
                g2.drawLine(x, offsetY + HEADER_HEIGHT, x, HEADER_HEIGHT);
            }
        }
    }

    /**
     * 【当日（Today）縦線の描画】
     */
    private void drawTodayLine(Graphics2D g2, LocalDate minDate, int totalY, int offsetY) {
        LocalDate today = LocalDate.now();
        long daysFromMin = ChronoUnit.DAYS.between(minDate, today);

        if (daysFromMin >= 0 && daysFromMin < totalDays) {
            int todayX = (int) (daysFromMin * dayWidth) + (dayWidth / 2);
            g2.setColor(new Color(230, 50, 50, 180));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 4.0f}, 0.0f));
            g2.drawLine(todayX, offsetY, todayX, totalY);
            g2.setStroke(new BasicStroke(1.0f));
        }
    }

    private void drawDayGrid(Graphics2D g2, LocalDate minDate, int startY, int endY) {
        for (int i = 0; i < totalDays; i++) {
            int x = i * dayWidth;
            g2.setColor(new Color(220, 220, 220));
            g2.drawLine(x, startY, x, endY);
        }
    }

    /**
     * 【左側タスク名セル描画】
     */
    private void drawTaskNameCell(Graphics2D g2, Task task, int y) {
        Color rowBg;
        if (AppTheme.isDarkMode()) {
            rowBg = (selectedTaskId != null && task.getId() == selectedTaskId)
                    ? new Color(30, 41, 59)
                    : new Color(30, 41, 59);
        } else {
            rowBg = (selectedTaskId != null && task.getId() == selectedTaskId)
                    ? new Color(236, 244, 255)
                    : Color.WHITE;
        }
        g2.setColor(rowBg);
        g2.fillRect(0, y, TASK_NAME_WIDTH, ROW_HEIGHT);
        g2.setColor(AppTheme.isDarkMode() ? new Color(71, 85, 105) : new Color(226, 232, 240));
        g2.fillRect(0, y + ROW_HEIGHT - 1, TASK_NAME_WIDTH, 1);
        g2.setColor(AppTheme.BORDER_COLOR);
        g2.drawRect(0, y, TASK_NAME_WIDTH, ROW_HEIGHT);

        int indent = 10 + (Math.max(0, task.getLevel() - 1) * 15);

        g2.setFont(task.getLevel() == 1 ? AppTheme.FONT_HEADER : AppTheme.FONT_MAIN);
        g2.setColor(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
        g2.drawString(task.getName(), indent, y + 24);
    }

    /**
     * 【表示中タスク一覧の取得（検索フィルタ適用後）】
     * 左のタスク名列・上のカレンダー・右のバー領域が常に同じ並び順・同じ件数を
     * 参照できるよう、フィルタ処理を一箇所に集約します。
     */
    private List<Task> getVisibleTasks() {
        if (cachedTasks.isEmpty()) {
            refreshTaskCache();
        }
        List<Task> allSortedTasks = getSortedTasksForGantt(cachedTasks);
        List<Task> tasks = new ArrayList<>();
        Set<Integer> projectMemberIds = currentProjectFilterId == null
                ? null
                : getProjectMemberIds(allSortedTasks, currentProjectFilterId);

        for (Task task : allSortedTasks) {
            boolean matchesKeyword = currentSearchKeyword == null || task.getName().contains(currentSearchKeyword);
            boolean matchesStatus = currentStatusFilter == null || "すべて".equals(currentStatusFilter) || currentStatusFilter.equals(task.getStatus());
            boolean matchesProject = projectMemberIds == null || projectMemberIds.contains(task.getId());
            if (matchesKeyword && matchesStatus && matchesProject) {
                tasks.add(task);
            }
        }
        return tasks;
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
        notifyLayoutChanged();
    }

    public void clearSearchFilter() {
        currentSearchKeyword = null;
        currentStatusFilter = null;
        currentProjectFilterId = null;
        clearProjectMemberCache();
        if (txtSearchKeyword != null) {
            txtSearchKeyword.setText("");
        }
        notifyLayoutChanged();
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
            notifyLayoutChanged();
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

    public JComponent createTaskNameColumn() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    List<Task> tasks = getVisibleTasks();
                    if (tasks.isEmpty()) {
                        return;
                    }

                    int currentY = 0;
                    for (Task task : tasks) {
                        drawTaskNameCell(g2, task, currentY);
                        g2.setColor(new Color(230, 230, 230));
                        g2.drawLine(0, currentY + ROW_HEIGHT, TASK_NAME_WIDTH, currentY + ROW_HEIGHT);
                        currentY += ROW_HEIGHT;
                    }
                } finally {
                    g2.dispose();
                }
            }

            /**
             * 【重要】固定サイズをキャッシュせず、常に「今表示すべき行数」から
             * 動的に高さを算出します。これにより、右側のガントバー領域（メインの
             * JScrollPane ビューポート）と行数・スクロール量が完全に一致し、
             * スクロール後にタスク名とバーの行がズレる問題を防ぎます。
             */
            @Override
            public Dimension getPreferredSize() {
                int rowCount = getVisibleTasks().size();
                int height = Math.max(200, (rowCount * ROW_HEIGHT) + 20);
                return new Dimension(TASK_NAME_WIDTH, height);
            }
        };
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setOpaque(true);
        taskNameColumnView = panel;
        return panel;
    }
    public JComponent createDateHeader() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    List<Task> tasks = getVisibleTasks();
                    if (tasks.isEmpty()) {
                        return;
                    }

                    Map<Integer, List<Task>> childMap = buildChildrenMap(getSortedTasksForGantt(cachedTasks));
                    LocalDate[] visibleWindow = calculateVisibleDateWindow(tasks, childMap);
                    LocalDate minDate = visibleWindow[0];
                    LocalDate maxDate = visibleWindow[1];
                    visibleStartDate = minDate;
                    visibleEndDate = maxDate;
                    totalDays = (int) ChronoUnit.DAYS.between(minDate, maxDate) + 1;
                    totalDays = Math.max(31, totalDays);

                    drawHeader(g2, minDate, 0);
                } finally {
                    g2.dispose();
                }
            }

            /**
             * 【重要】カレンダーの横幅も、メイン描画側（paintComponent）が
             * 算出する totalDays / dayWidth と必ず一致させます。
             * ここがズレると、横スクロール時にカレンダーの列とバーの列が
             * 少しずつ食い違っていく（日付がズレて見える）原因になります。
             */
            @Override
            public Dimension getPreferredSize() {
                List<Task> tasks = getVisibleTasks();
                int days;
                if (tasks.isEmpty()) {
                    days = totalDays;
                } else {
                    Map<Integer, List<Task>> childMap = buildChildrenMap(getSortedTasksForGantt(cachedTasks));
                    LocalDate[] visibleWindow = calculateVisibleDateWindow(tasks, childMap);
                    days = (int) ChronoUnit.DAYS.between(visibleWindow[0], visibleWindow[1]) + 1;
                    days = Math.max(31, days);
                }
                return new Dimension(Math.max(800, days * dayWidth), HEADER_HEIGHT);
            }
        };
        panel.setBackground(AppTheme.PANEL_BG);
        panel.setOpaque(true);
        dateHeaderView = panel;
        return panel;
    }

    /**
     * 【右側ガントバー描画（進行度をバー内部の色の長さで表現）】
     */
    private Map<Integer, List<Task>> buildChildrenMap(List<Task> tasks) {
        Map<Integer, List<Task>> childrenMap = new HashMap<>();
        for (Task task : tasks) {
            if (task.getParentId() != null && task.getParentId() != 0) {
                childrenMap.computeIfAbsent(task.getParentId(), key -> new ArrayList<>()).add(task);
            }
        }
        return childrenMap;
    }

    private LocalDate[] calculateVisibleDateWindow(List<Task> tasks, Map<Integer, List<Task>> childrenMap) {
        List<Task> activeTasks = tasks.stream()
                .filter(task -> !Task.STATUS_COMPLETED.equals(task.getStatus()))
                .toList();

        if (activeTasks.isEmpty()) {
            activeTasks = tasks;
        }

        LocalDate minDate = activeTasks.stream()
                .map(task -> resolveDisplayStartDate(task, childrenMap))
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate maxDate = activeTasks.stream()
                .map(task -> resolveDisplayEndDate(task, childrenMap))
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(minDate.plusDays(30));

        LocalDate windowStart = trimEmptyMonthStart(minDate, maxDate, activeTasks, childrenMap);
        LocalDate windowEnd = trimEmptyMonthEnd(maxDate, windowStart, activeTasks, childrenMap);

        if (windowStart.isAfter(windowEnd)) {
            return new LocalDate[]{minDate, maxDate};
        }

        return new LocalDate[]{windowStart, windowEnd};
    }

    private LocalDate trimEmptyMonthStart(LocalDate start, LocalDate end, List<Task> tasks, Map<Integer, List<Task>> childrenMap) {
        LocalDate cursor = start.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            if (monthHasTask(cursor, tasks, childrenMap)) {
                return cursor.isBefore(start) ? cursor : start;
            }
            cursor = cursor.plusMonths(1).withDayOfMonth(1);
        }
        return start;
    }

    private LocalDate trimEmptyMonthEnd(LocalDate end, LocalDate start, List<Task> tasks, Map<Integer, List<Task>> childrenMap) {
        LocalDate cursor = end.withDayOfMonth(1);
        while (!cursor.isBefore(start.withDayOfMonth(1))) {
            if (monthHasTask(cursor, tasks, childrenMap)) {
                return cursor.isBefore(end.withDayOfMonth(1)) ? cursor.plusMonths(1).minusDays(1) : end;
            }
            cursor = cursor.minusMonths(1).withDayOfMonth(1);
        }
        return end;
    }

    private boolean monthHasTask(LocalDate monthStart, List<Task> tasks, Map<Integer, List<Task>> childrenMap) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        for (Task task : tasks) {
            LocalDate taskStart = resolveDisplayStartDate(task, childrenMap);
            LocalDate taskEnd = resolveDisplayEndDate(task, childrenMap);
            if (taskStart == null || taskEnd == null) {
                continue;
            }
            if (!(taskEnd.isBefore(monthStart) || taskStart.isAfter(monthEnd))) {
                return true;
            }
        }
        return false;
    }

    private LocalDate resolveDisplayStartDate(Task task, Map<Integer, List<Task>> childrenMap) {
        if (task == null) {
            return null;
        }

        List<Task> children = childrenMap.get(task.getId());
        if (children != null && !children.isEmpty()) {
            LocalDate minChildStart = children.stream()
                    .map(child -> resolveDisplayStartDate(child, childrenMap))
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(task.getStartDate());

            if (task.getStartDate() == null) {
                return minChildStart;
            }
            if (minChildStart == null) {
                return task.getStartDate();
            }
            return minChildStart.isBefore(task.getStartDate()) ? minChildStart : task.getStartDate();
        }
        return task.getStartDate();
    }

    private LocalDate resolveDisplayEndDate(Task task, Map<Integer, List<Task>> childrenMap) {
        if (task == null) {
            return null;
        }

        List<Task> children = childrenMap.get(task.getId());
        if (children != null && !children.isEmpty()) {
            LocalDate maxChildEnd = children.stream()
                    .map(child -> resolveDisplayEndDate(child, childrenMap))
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(task.getEndDate());

            if (task.getEndDate() == null) {
                return maxChildEnd;
            }
            if (maxChildEnd == null) {
                return task.getEndDate();
            }
            return maxChildEnd.isAfter(task.getEndDate()) ? maxChildEnd : task.getEndDate();
        }
        return task.getEndDate();
    }

    private void drawGanttBar(Graphics2D g2, Task task, LocalDate minDate, int y, Map<Integer, List<Task>> childrenMap) {
        LocalDate startToUse = resolveDisplayStartDate(task, childrenMap);
        LocalDate endToUse = resolveDisplayEndDate(task, childrenMap);
        if (startToUse == null || endToUse == null) {
            return;
        }

        boolean isDragging = (draggingTask != null && draggingTask.getId() == task.getId());
        boolean isSelected = (selectedTaskId != null && selectedTaskId.equals(task.getId()));

        if (isDragging && originalStartDate != null && originalEndDate != null) {
            int diffX = currentMouseX - dragStartX;
            long diffDays = Math.round((double) diffX / dayWidth);

            List<Task> allTasks = TaskDao.getAllTasks();
            Task rootProject = (draggingTask.getLevel() > 1) ? findRootProject(draggingTask, allTasks) : null;

            if (dragMode == 1) {
                long durationDays = ChronoUnit.DAYS.between(originalStartDate, originalEndDate);
                startToUse = originalStartDate.plusDays(diffDays);
                endToUse = startToUse.plusDays(durationDays);

                if (rootProject != null && rootProject.getStartDate() != null && rootProject.getEndDate() != null) {
                    if (startToUse.isBefore(rootProject.getStartDate())) {
                        startToUse = rootProject.getStartDate();
                        endToUse = startToUse.plusDays(durationDays);
                    }
                    if (endToUse.isAfter(rootProject.getEndDate())) {
                        endToUse = rootProject.getEndDate();
                        startToUse = endToUse.minusDays(durationDays);
                        if (startToUse.isBefore(rootProject.getStartDate())) {
                            startToUse = rootProject.getStartDate();
                        }
                    }
                }
            } else if (dragMode == 2) {
                endToUse = originalEndDate.plusDays(diffDays);
                if (endToUse.isBefore(startToUse)) {
                    endToUse = startToUse;
                }
                if (rootProject != null && rootProject.getEndDate() != null && endToUse.isAfter(rootProject.getEndDate())) {
                    endToUse = rootProject.getEndDate();
                }
            } else if (dragMode == 3) {
                startToUse = originalStartDate.plusDays(diffDays);
                if (startToUse.isAfter(endToUse)) {
                    startToUse = endToUse;
                }
                if (rootProject != null && rootProject.getStartDate() != null && startToUse.isBefore(rootProject.getStartDate())) {
                    startToUse = rootProject.getStartDate();
                }
            }
        }
        long startOffset = ChronoUnit.DAYS.between(minDate, startToUse);
        long duration = ChronoUnit.DAYS.between(startToUse, endToUse) + 1;

        if (startOffset < 0 || duration <= 0) return;

        int barX = (int) (startOffset * dayWidth);
        int barWidth = (int) (duration * dayWidth);
        int barY = y + (ROW_HEIGHT - BAR_HEIGHT) / 2;
        int barHeight = BAR_HEIGHT;

        // ドラッグ中のバーは半透明（アルファ値 0.55）にする
        Composite originalComposite = g2.getComposite();
        if (isDragging) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        }

        // 進捗率（0~100%）の取得
        int progress = Math.max(0, Math.min(100, task.getProgress()));

        // 【デザイン変更】状態別の色分けに変更（未着手:グレー、進行中:青、完了:緑）
        Color mainColor = AppTheme.getStatusColor(task.getStatus());
        Color bgBarColor = AppTheme.getStatusSoftColor(task.getStatus());

        java.awt.geom.RoundRectangle2D barShape = new java.awt.geom.RoundRectangle2D.Double(barX, barY, barWidth, barHeight, 8, 8);
        g2.setColor(bgBarColor);
        g2.fill(barShape);

        int progressWidth = (int) Math.round(barWidth * (progress / 100.0));
        if (progress > 0) {
            Shape oldClip = g2.getClip();
            g2.clip(barShape);
            g2.setColor(mainColor);
            g2.fillRoundRect(barX, barY, Math.max(progressWidth, 1), barHeight, 8, 8);
            g2.setClip(oldClip);
        }

        if (isDragging) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(255, 110, 0));
        } else if (isSelected) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(30, 120, 255));
        } else {
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(mainColor.darker());
        }
        g2.drawRoundRect(barX, barY, barWidth, barHeight, 8, 8);
        g2.setStroke(new BasicStroke(1.0f));

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        String progressText = progress + "%";
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(progressText);
        int textX = barX + Math.max(6, (barWidth - textWidth) / 2);
        int textY = barY + (barHeight + fm.getAscent()) / 2 - 2;

        if (progress == 0) {
            g2.setColor(mainColor);
            g2.drawString(progressText, textX, textY);
        } else {
            g2.setColor(Color.WHITE);
            g2.drawString(progressText, textX, textY);
        }

        if (isDragging) {
            g2.setComposite(originalComposite);
        }
    }

    public Task getSelectedTask() {
        if (selectedTaskId == null) {
            return null;
        }
        return TaskDao.getTaskById(selectedTaskId);
    }

    public void deleteSelectedTask() {
        if (selectedTaskId == null) {
            JOptionPane.showMessageDialog(this, "削除するバーを選択してください。", "削除できません", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Task task = TaskDao.getTaskById(selectedTaskId);
        if (task == null) {
            return;
        }

        TaskDao.deleteTask(selectedTaskId);
        selectedTaskId = null;
        refreshTaskCache();
        repaint();
        JOptionPane.showMessageDialog(this, "「" + task.getName() + "」を削除しました。", "削除完了", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTaskCache() {
        cachedTasks = TaskDao.getAllTasks();
    }

    private boolean isChartInteractionArea(int mouseX, int mouseY) {
        return mouseX >= 0 && mouseY >= 0;
    }

    private void scrollParentViewport(MouseWheelEvent e) {
        Container parent = getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        if (!(parent instanceof JScrollPane scrollPane)) {
            return;
        }

        if (e.isShiftDown()) {
            JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
            if (horizontal != null) {
                int delta = e.getWheelRotation() * 24;
                horizontal.setValue(horizontal.getValue() + delta);
            }
            return;
        }

        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        if (vertical != null) {
            int delta = e.getWheelRotation() * 24;
            vertical.setValue(vertical.getValue() + delta);
        }
    }

    /**
     * 【マウスドラッグ・リサイズ用インナークラス】
     */
    private class GanttMouseAdapter extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (cachedTasks.isEmpty()) {
                refreshTaskCache();
            }
            if (cachedTasks.isEmpty()) return;

            List<Task> tasks = getSortedTasksForGantt(cachedTasks);
            Map<Integer, List<Task>> childrenMap = buildChildrenMap(tasks);
            LocalDate[] visibleWindow = calculateVisibleDateWindow(tasks, childrenMap);
            LocalDate minDate = visibleWindow[0];

            int y = 0;
            int mouseX = e.getX();
            int mouseY = e.getY();

            boolean hitBar = false;
            for (Task task : tasks) {
                LocalDate displayStart = resolveDisplayStartDate(task, childrenMap);
                LocalDate displayEnd = resolveDisplayEndDate(task, childrenMap);
                if (displayStart == null || displayEnd == null) {
                    y += ROW_HEIGHT;
                    continue;
                }

                long startOffset = ChronoUnit.DAYS.between(minDate, displayStart);
                long duration = ChronoUnit.DAYS.between(displayStart, displayEnd) + 1;

                int barX = (int) (startOffset * dayWidth);
                int barWidth = (int) (duration * dayWidth);
                int barY = y + (ROW_HEIGHT - BAR_HEIGHT) / 2;
                int barHeight = BAR_HEIGHT;

                if (mouseY >= barY && mouseY <= barY + barHeight && mouseX >= barX && mouseX <= barX + barWidth) {
                    selectedTaskId = task.getId();
                    draggingTask = task;
                    dragStartX = mouseX;
                    currentMouseX = mouseX;
                    originalStartDate = displayStart;
                    originalEndDate = displayEnd;

                    if (mouseX <= barX + RESIZE_HANDLE_WIDTH) {
                        dragMode = 3; // 左端リサイズ
                    } else if (mouseX >= barX + barWidth - RESIZE_HANDLE_WIDTH) {
                        dragMode = 2; // 右端リサイズ
                    } else {
                        dragMode = 1; // 全体移動
                    }
                    hitBar = true;
                    repaint();
                    break;
                }
                y += ROW_HEIGHT;
            }
            if (!hitBar) {
                selectedTaskId = null;
                repaint();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (cachedTasks.isEmpty()) {
                refreshTaskCache();
            }
            if (cachedTasks.isEmpty()) return;

            List<Task> tasks = getSortedTasksForGantt(cachedTasks);
            Map<Integer, List<Task>> childrenMap = buildChildrenMap(tasks);
            LocalDate[] visibleWindow = calculateVisibleDateWindow(tasks, childrenMap);
            LocalDate minDate = visibleWindow[0];

            int y = 0;
            int mouseX = e.getX();
            int mouseY = e.getY();

            boolean hitBar = false;
            for (Task task : tasks) {
                LocalDate displayStart = resolveDisplayStartDate(task, childrenMap);
                LocalDate displayEnd = resolveDisplayEndDate(task, childrenMap);
                if (displayStart == null || displayEnd == null) {
                    y += ROW_HEIGHT;
                    continue;
                }

                long startOffset = ChronoUnit.DAYS.between(minDate, displayStart);
                long duration = ChronoUnit.DAYS.between(displayStart, displayEnd) + 1;

                int barX = (int) (startOffset * dayWidth);
                int barWidth = (int) (duration * dayWidth);
                int barY = y + (ROW_HEIGHT - BAR_HEIGHT) / 2;
                int barHeight = BAR_HEIGHT;

                if (mouseY >= barY && mouseY <= barY + barHeight && mouseX >= barX && mouseX <= barX + barWidth) {
                    selectedTaskId = task.getId();
                    hitBar = true;
                    if (e.getClickCount() == 2) {
                        openEditDialog(task);
                    }
                    repaint();
                    break;
                }
                y += ROW_HEIGHT;
            }
            if (!hitBar) {
                selectedTaskId = null;
                repaint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (draggingTask == null || dragMode == 0) return;

            currentMouseX = e.getX();
            manualVisibleWindow = true;

            int leftEdgeThreshold = 60;
            int rightEdgeThreshold = getWidth() - 60;

            if (e.getX() < leftEdgeThreshold && visibleStartDate != null) {
                visibleStartDate = visibleStartDate.minusDays(Math.max(1, (leftEdgeThreshold - e.getX()) / Math.max(1, dayWidth)));
            } else if (e.getX() > rightEdgeThreshold && visibleEndDate != null) {
                visibleEndDate = visibleEndDate.plusDays(Math.max(1, (e.getX() - rightEdgeThreshold) / Math.max(1, dayWidth)));
            }

            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (draggingTask != null && dragMode != 0) {
                int diffX = e.getX() - dragStartX;
                long diffDays = Math.round((double) diffX / dayWidth);

                if (visibleStartDate != null && visibleEndDate != null) {
                    manualVisibleWindow = true;
                }

                List<Task> allTasks = TaskDao.getAllTasks();
                Task rootProject = (draggingTask.getLevel() > 1) ? findRootProject(draggingTask, allTasks) : null;

                LocalDate newStart = originalStartDate;
                LocalDate newEnd = originalEndDate;

                if (dragMode == 1) { // 移動
                    long durationDays = ChronoUnit.DAYS.between(originalStartDate, originalEndDate);
                    newStart = originalStartDate.plusDays(diffDays);
                    newEnd = newStart.plusDays(durationDays);

                    if (rootProject != null && rootProject.getStartDate() != null && rootProject.getEndDate() != null) {
                        if (newStart.isBefore(rootProject.getStartDate())) {
                            newStart = rootProject.getStartDate();
                            newEnd = newStart.plusDays(durationDays);
                        }
                        if (newEnd.isAfter(rootProject.getEndDate())) {
                            newEnd = rootProject.getEndDate();
                            newStart = newEnd.minusDays(durationDays);
                            if (newStart.isBefore(rootProject.getStartDate())) {
                                newStart = rootProject.getStartDate();
                            }
                        }
                    }
                } else if (dragMode == 2) { // 右端リサイズ
                    newEnd = originalEndDate.plusDays(diffDays);
                    if (newEnd.isBefore(originalStartDate)) {
                        newEnd = originalStartDate;
                    }
                    if (rootProject != null && rootProject.getEndDate() != null && newEnd.isAfter(rootProject.getEndDate())) {
                        newEnd = rootProject.getEndDate();
                    }
                } else if (dragMode == 3) { // 左端リサイズ
                    newStart = originalStartDate.plusDays(diffDays);
                    if (newStart.isAfter(originalEndDate)) {
                        newStart = originalEndDate;
                    }
                    if (rootProject != null && rootProject.getStartDate() != null && newStart.isBefore(rootProject.getStartDate())) {
                        newStart = rootProject.getStartDate();
                    }
                }

                draggingTask.setStartDate(newStart);
                draggingTask.setEndDate(newEnd);

                // データベース更新
                TaskDao.updateTask(draggingTask);
                refreshTaskCache();

                manualVisibleWindow = false;
                visibleStartDate = null;
                visibleEndDate = null;

                draggingTask = null;
                dragMode = 0;
                originalStartDate = null;
                originalEndDate = null;
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (cachedTasks.isEmpty()) {
                refreshTaskCache();
            }
            if (cachedTasks.isEmpty()) return;

            List<Task> tasks = getSortedTasksForGantt(cachedTasks);
            Map<Integer, List<Task>> childrenMap = buildChildrenMap(tasks);
            LocalDate[] visibleWindow = calculateVisibleDateWindow(tasks, childrenMap);
            LocalDate minDate = visibleWindow[0];

            int y = 0;
            int mouseX = e.getX();
            int mouseY = e.getY();
            boolean isHoverOnBar = false;
            hoveredTask = null; // リセット

            for (Task task : tasks) {
                LocalDate displayStart = resolveDisplayStartDate(task, childrenMap);
                LocalDate displayEnd = resolveDisplayEndDate(task, childrenMap);
                if (displayStart == null || displayEnd == null) {
                    y += ROW_HEIGHT;
                    continue;
                }

                long startOffset = ChronoUnit.DAYS.between(minDate, displayStart);
                long duration = ChronoUnit.DAYS.between(displayStart, displayEnd) + 1;

                int barX = (int) (startOffset * dayWidth);
                int barWidth = (int) (duration * dayWidth);
                int barY = y + (ROW_HEIGHT - BAR_HEIGHT) / 2;
                int barHeight = BAR_HEIGHT;

                if (mouseY >= barY && mouseY <= barY + barHeight && mouseX >= barX && mouseX <= barX + barWidth) {
                    isHoverOnBar = true;
                    hoveredTask = task; // ホバー中のタスクを設定
                    if (mouseX <= barX + RESIZE_HANDLE_WIDTH || mouseX >= barX + barWidth - RESIZE_HANDLE_WIDTH) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                    // ツールチップを更新
                    setToolTipText(getToolTipText(e));
                    break;
                }
                y += ROW_HEIGHT;
            }

            if (!isHoverOnBar) {
                setCursor(Cursor.getDefaultCursor());
                setToolTipText(null);
            }
        }
    }

    /**
     * 対象タスクの最上位親プロジェクトを取得します。
     */
    private Task findRootProject(Task task, List<Task> allTasks) {
        if (task == null) return null;

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : allTasks) {
            taskMap.put(t.getId(), t);
        }

        Task current = task;
        while (current.getParentId() != null && current.getParentId() != 0) {
            Task parent = taskMap.get(current.getParentId());
            if (parent == null) break;
            current = parent;
        }
        return current;
    }
    /**
     * 【検索実行処理】
     * 入力された検索キーワードを適用してガントチャートを再描画します。
     */
    private void handleSearch() {
        if (txtSearchKeyword == null) {
            currentSearchKeyword = null;
            repaint();
            return;
        }
        String keyword = txtSearchKeyword.getText().trim();
        currentSearchKeyword = keyword.isEmpty() ? null : keyword;
        repaint();
    }

    /**
     * 【タスク編集ダイアログを開く】
     */
    private void openEditDialog(Task task) {
        java.awt.Frame parentFrame = (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this);
        if (parentFrame == null) return;

        TaskDialog dialog = new TaskDialog(parentFrame, task);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            task.setName(dialog.getTaskName());
            task.setAssignee(dialog.getAssignee());
            task.setProgress(dialog.getProgress());
            // 優先度をダイアログの選択値で上書き（null は Task#setPriority が扱う）
            task.setPriority(dialog.getPriority());
            task.setStartDate(LocalDate.parse(dialog.getStartDate()));
            task.setEndDate(LocalDate.parse(dialog.getEndDate()));

            TaskDao.updateTask(task);
            refreshTaskCache();
            repaint();
        }
    }

}