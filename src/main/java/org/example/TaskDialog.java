package org.example;

// --- 画面 UI (Swing) 関連のライブラリ ---
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// --- レイアウト・イベント関連のライブラリ ---
import java.awt.*;

// --- 日付処理関連のライブラリ ---
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 【タスク追加・編集用ダイアログクラス】
 * タスク名、担当者、開始日・終了日、進捗率を入力するためのモーダル画面です。
 */
public class TaskDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    // --- 入力フィールドコンポーネント ---
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"プロジェクト", "工程", "タスク"});
    private final JTextField nameField = new JTextField(20);
    private final JTextField assigneeField = new JTextField(20);
    private final JTextField startDateField = new JTextField(10);
    private final JTextField endDateField = new JTextField(10);
    private final JComboBox<String> priorityCombo = new JComboBox<>(Task.PRIORITY_OPTIONS);
    private final JSlider progressSlider = new JSlider(0, 100, 0);
    private final JLabel progressLabel = new JLabel("0%");
    private final JComboBox<String> parentProjectCombo = new JComboBox<>();
    private final Map<String, Task> parentTargetMap = new LinkedHashMap<>();

    private boolean confirmed = false; // 「保存」が押されたかどうかのフラグ

    private static boolean isProjectTask(Task task) {
        if (task == null) {
            return false;
        }
        return task.getLevel() == 1 || task.getParentId() == null;
    }

    /**
     * コンストラクタ（新規作成用）
     */
    public TaskDialog(Frame parent) {
        this(parent, null, null, null);
    }

    /**
     * コンストラクタ（新規作成用：種別と親プロジェクトを事前選択）
     */
    public TaskDialog(Frame parent, String defaultType, Integer defaultParentProjectId) {
        this(parent, null, defaultType, defaultParentProjectId);
    }

    /**
     * コンストラクタ（編集用：既存タスクを受け取る）
     */
    public TaskDialog(Frame parent, Task task) {
        this(parent, task, null, null);
    }

    private TaskDialog(Frame parent, Task task, String defaultType, Integer defaultParentProjectId) {
        super(parent, task == null ? "新規登録" : (isProjectTask(task) ? "プロジェクト編集" : "タスク編集"), true);
        setSize(470, 430);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // --- 1. メイン入力フォームパネルの作成 ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        formPanel.setBackground(AppTheme.PANEL_BG);

        // 入力フィールドのスタイリングを適用
        AppTheme.styleTextField(nameField);
        AppTheme.styleTextField(assigneeField);
        AppTheme.styleTextField(startDateField);
        AppTheme.styleTextField(endDateField);

        // コンボボックスのスタイリングを適用
        styleComboBox(typeCombo);
        styleComboBox(parentProjectCombo);
        styleComboBox(priorityCombo);

        // スライダーのスタイリングを適用
        progressSlider.setOpaque(false);
        progressSlider.setForeground(AppTheme.isDarkMode() ? new Color(96, 165, 250) : AppTheme.PRIMARY);

        // 進捗ラベルのスタイリングを適用
        progressLabel.setFont(AppTheme.FONT_MAIN);
        progressLabel.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        parentProjectCombo.setEnabled(task == null);
        if (task == null) {
            typeCombo.setSelectedItem(defaultType != null ? defaultType : "工程");
        } else {
            if (task.getLevel() == 1 || task.getParentId() == null) {
                typeCombo.setSelectedItem("プロジェクト");
            } else if (task.getLevel() == 2) {
                typeCombo.setSelectedItem("工程");
            } else {
                typeCombo.setSelectedItem("タスク");
            }
            typeCombo.setEnabled(false);
        }

        typeCombo.addActionListener(e -> updateParentProjectVisibility());
        updateParentProjectVisibility();
        applyDefaultParentProjectSelection(defaultParentProjectId);

        // 初期値（本日の日付）の設定
        LocalDate today = LocalDate.now();
        startDateField.setText(today.toString());
        endDateField.setText(today.plusDays(7).toString());

        // --- 2. フォーム項目の配置 ---
        addFormRow(formPanel, gbc, 0, "種別:", typeCombo);
        addFormRow(formPanel, gbc, 1, "親要素:", parentProjectCombo);
        addFormRow(formPanel, gbc, 2, "名前:", nameField);
        addFormRow(formPanel, gbc, 3, "担当者:", assigneeField);

        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        startPanel.setOpaque(false);
        startDateField.setColumns(10);
        AppTheme.styleTextField(startDateField);
        JButton btnStartCal = IconManager.getIconButton(IconManager.IconType.CALENDAR);
        btnStartCal.setPreferredSize(new Dimension(40, 36));
        btnStartCal.setMargin(new Insets(0, 2, 0, 2));
        btnStartCal.setToolTipText("開始日を選択");
        btnStartCal.setFocusPainted(false);
        btnStartCal.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        btnStartCal.setForeground(AppTheme.TEXT_PRIMARY);
        UiDebugUtil.logButtonState("TaskDialog.startDateButton", btnStartCal);
        btnStartCal.addActionListener(e -> showDatePickerDialog(startDateField));
        startPanel.add(startDateField);
        startPanel.add(btnStartCal);
        addFormRow(formPanel, gbc, 4, "開始日 (YYYY-MM-DD):", startPanel);

        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        endPanel.setOpaque(false);
        endDateField.setColumns(10);
        AppTheme.styleTextField(endDateField);
        JButton btnEndCal = IconManager.getIconButton(IconManager.IconType.CALENDAR);
        btnEndCal.setPreferredSize(new Dimension(40, 36));
        btnEndCal.setMargin(new Insets(0, 2, 0, 2));
        btnEndCal.setToolTipText("終了日を選択");
        btnEndCal.setFocusPainted(false);
        btnEndCal.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        btnEndCal.setForeground(AppTheme.TEXT_PRIMARY);
        UiDebugUtil.logButtonState("TaskDialog.endDateButton", btnEndCal);
        btnEndCal.addActionListener(e -> showDatePickerDialog(endDateField));
        endPanel.add(endDateField);
        endPanel.add(btnEndCal);
        addFormRow(formPanel, gbc, 5, "終了日 (YYYY-MM-DD):", endPanel);

        addFormRow(formPanel, gbc, 6, "優先度:", priorityCombo);

        progressSlider.setMajorTickSpacing(25);
        progressSlider.setPaintTicks(true);
        progressSlider.addChangeListener(e -> progressLabel.setText(progressSlider.getValue() + "%"));

        JPanel progressPanel = new JPanel(new BorderLayout(5, 0));
        progressPanel.setOpaque(false);
        progressPanel.add(progressSlider, BorderLayout.CENTER);
        progressPanel.add(progressLabel, BorderLayout.EAST);

        addFormRow(formPanel, gbc, 7, "進捗率:", progressPanel);

        if (task != null) {
            nameField.setText(task.getName());
            assigneeField.setText(task.getAssignee());
            if (task.getStartDate() != null) startDateField.setText(task.getStartDate().toString());
            if (task.getEndDate() != null) endDateField.setText(task.getEndDate().toString());
            priorityCombo.setSelectedItem(task.getPriority() != null ? task.getPriority() : Task.DEFAULT_PRIORITY);
            progressSlider.setValue(task.getProgress());
            progressLabel.setText(task.getProgress() + "%");
            if (task.getParentId() != null) {
                Task rootProject = findRootProject(task);
                if (rootProject != null) {
                    parentProjectCombo.setSelectedItem(rootProject.getName());
                }
            }
        } else {
            priorityCombo.setSelectedItem(Task.DEFAULT_PRIORITY);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(AppTheme.BACKGROUND);

        JButton saveButton = new JButton("保存");
        saveButton.setFont(AppTheme.FONT_MAIN);
        saveButton.setBackground(AppTheme.PRIMARY);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> handleSave());

        JButton cancelButton = new JButton("キャンセル");
        cancelButton.setFont(AppTheme.FONT_MAIN);
        cancelButton.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        cancelButton.setForeground(AppTheme.TEXT_PRIMARY);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // ダイアログ全体の背景色を設定
        setBackground(AppTheme.BACKGROUND);
    }

    private void rebuildParentTargetOptions() {
        parentTargetMap.clear();

        String selectedType = (String) typeCombo.getSelectedItem();
        if ("プロジェクト".equals(selectedType)) {
            parentTargetMap.put("親プロジェクトなし", null);
        } else if ("工程".equals(selectedType)) {
            for (Task candidate : TaskDao.getAllTasks()) {
                if (candidate == null) {
                    continue;
                }
                if (candidate.getLevel() == 1 || candidate.getParentId() == null) {
                    parentTargetMap.put(buildParentLabel(candidate), candidate);
                }
            }
        } else if ("タスク".equals(selectedType)) {
            for (Task candidate : TaskDao.getAllTasks()) {
                if (candidate == null) {
                    continue;
                }
                if (candidate.getLevel() == 2) {
                    Task rootProject = findRootProject(candidate);
                    String label = rootProject != null ? rootProject.getName() + " / " + candidate.getName() : candidate.getName();
                    parentTargetMap.put(label, candidate);
                }
            }
        }

        parentProjectCombo.setModel(new DefaultComboBoxModel<>(parentTargetMap.keySet().toArray(new String[0])));
        if (parentTargetMap.isEmpty()) {
            parentProjectCombo.setEnabled(false);
        }
    }

    private String buildParentLabel(Task task) {
        if (task == null) {
            return "親プロジェクトなし";
        }
        if (task.getLevel() == 1 || task.getParentId() == null) {
            return task.getName();
        }
        Task rootProject = findRootProject(task);
        if (rootProject != null && rootProject.getId() != task.getId()) {
            return rootProject.getName() + " / " + task.getName();
        }
        return task.getName();
    }

    private void applyDefaultParentProjectSelection(Integer defaultParentProjectId) {
        String selectedType = (String) typeCombo.getSelectedItem();
        if ("プロジェクト".equals(selectedType)) {
            parentProjectCombo.setSelectedItem("親プロジェクトなし");
            return;
        }

        if (defaultParentProjectId == null) {
            if (parentTargetMap.isEmpty()) {
                parentProjectCombo.setSelectedItem(null);
            } else {
                parentProjectCombo.setSelectedIndex(0);
            }
            return;
        }

        for (Map.Entry<String, Task> entry : parentTargetMap.entrySet()) {
            Task candidate = entry.getValue();
            if (candidate != null && candidate.getId() == defaultParentProjectId) {
                parentProjectCombo.setSelectedItem(entry.getKey());
                return;
            }
            if (candidate != null) {
                Task rootProject = findRootProject(candidate);
                if (rootProject != null && rootProject.getId() == defaultParentProjectId) {
                    parentProjectCombo.setSelectedItem(entry.getKey());
                    return;
                }
            }
        }

        if (parentTargetMap.isEmpty()) {
            parentProjectCombo.setSelectedItem(null);
        } else {
            parentProjectCombo.setSelectedIndex(0);
        }
    }

    private void updateParentProjectVisibility() {
        rebuildParentTargetOptions();

        String selectedType = (String) typeCombo.getSelectedItem();
        boolean isProject = "プロジェクト".equals(selectedType);

        if (isProject) {
            parentProjectCombo.setEnabled(false);
            parentProjectCombo.setSelectedItem("親プロジェクトなし");
            return;
        }

        if (parentTargetMap.isEmpty()) {
            parentProjectCombo.setEnabled(false);
            parentProjectCombo.setSelectedItem(null);
            return;
        }

        parentProjectCombo.setEnabled(true);
        if (parentProjectCombo.getSelectedItem() == null) {
            parentProjectCombo.setSelectedIndex(0);
        }
    }

    private Task findRootProject(Task task) {
        if (task == null) {
            return null;
        }
        if (task.getParentId() == null || task.getLevel() == 1) {
            return task;
        }
        for (Task candidate : TaskDao.getAllTasks()) {
            if (candidate.getId() == task.getParentId()) {
                return findRootProject(candidate);
            }
        }
        return task;
    }

    /**
     * フォーム行追加用のヘルパーメソッド
     */
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel label = new JLabel(labelText);
        label.setFont(AppTheme.FONT_MAIN);
        label.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        comp.setFont(AppTheme.FONT_MAIN);
        panel.add(comp, gbc);
    }

    /**
     * コンボボックスのスタイリング用ヘルパーメソッド
     */
    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(AppTheme.FONT_MAIN);
        combo.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        combo.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        // セルレンダラーを設定してドロップダウンリストもダークモード対応
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (AppTheme.isDarkMode()) {
                    if (isSelected) {
                        c.setBackground(new Color(96, 165, 250));
                        c.setForeground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(30, 41, 59));
                        c.setForeground(new Color(226, 232, 240));
                    }
                }
                return c;
            }
        });
    }

    /**
     * ポップアップカレンダーダイアログ（WBS画面と同じ実装）
     */
    private void showDatePickerDialog(JTextField targetField) {
        final LocalDate[] currentDate = {LocalDate.now()};
        try {
            if (!targetField.getText().trim().isEmpty()) {
                currentDate[0] = LocalDate.parse(targetField.getText().trim());
            }
        } catch (Exception ignored) {}

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "日付を選択", true);
        dialog.setLayout(new BorderLayout(5, 5));
        dialog.setSize(320, 280);
        dialog.setLocationRelativeTo(this);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnPrev = new JButton("＜");
        JButton btnNext = new JButton("＞");
        JLabel lblYearMonth = new JLabel("", SwingConstants.CENTER);
        lblYearMonth.setFont(new Font("SansSerif", Font.BOLD, 14));

        headerPanel.add(btnPrev);
        headerPanel.add(lblYearMonth);
        headerPanel.add(btnNext);

        JPanel calendarPanel = new JPanel(new GridLayout(0, 7, 2, 2));

        Runnable updateCalendar = () -> {
            calendarPanel.removeAll();
            lblYearMonth.setText(currentDate[0].getYear() + "年 " + currentDate[0].getMonthValue() + "月");

            String[] days = {"日", "月", "火", "水", "木", "金", "土"};
            for (int i = 0; i < 7; i++) {
                JLabel lblDay = new JLabel(days[i], SwingConstants.CENTER);
                lblDay.setFont(new Font("SansSerif", Font.BOLD, 12));
                if (i == 0) lblDay.setForeground(Color.RED);
                if (i == 6) lblDay.setForeground(Color.BLUE);
                calendarPanel.add(lblDay);
            }

            LocalDate firstOfMonth = currentDate[0].withDayOfMonth(1);
            int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
            int lengthOfMonth = currentDate[0].lengthOfMonth();

            for (int i = 0; i < startDayOfWeek; i++) {
                calendarPanel.add(new JLabel(""));
            }

            for (int day = 1; day <= lengthOfMonth; day++) {
                int selectedDay = day;
                JButton btnDay = new JButton(String.valueOf(day));
                btnDay.setMargin(new Insets(2, 2, 2, 2));
                btnDay.setFocusPainted(false);

                if (currentDate[0].getDayOfMonth() == day) {
                    btnDay.setBackground(new Color(200, 220, 255));
                }

                btnDay.addActionListener(e -> {
                    LocalDate selectedDate = currentDate[0].withDayOfMonth(selectedDay);
                    targetField.setText(selectedDate.toString());
                    dialog.dispose();
                });

                calendarPanel.add(btnDay);
            }

            calendarPanel.revalidate();
            calendarPanel.repaint();
        };

        btnPrev.addActionListener(e -> {
            currentDate[0] = currentDate[0].minusMonths(1);
            updateCalendar.run();
        });

        btnNext.addActionListener(e -> {
            currentDate[0] = currentDate[0].plusMonths(1);
            updateCalendar.run();
        });

        updateCalendar.run();

        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(calendarPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    /**
     * 保存ボタン押下時の入力チェック処理
     */
    private void handleSave() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "名前を入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!"プロジェクト".equals(typeCombo.getSelectedItem()) && getParentProjectId() == null) {
            JOptionPane.showMessageDialog(this, "工程・タスクは親プロジェクトまたは親工程を選択してから登録してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            LocalDate.parse(startDateField.getText().trim());
            LocalDate.parse(endDateField.getText().trim());
        } catch (Exception ignored) {
            JOptionPane.showMessageDialog(this, "日付は YYYY-MM-DD 形式で入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        confirmed = true;
        dispose();
    }

    // --- ゲッターメソッド（入力結果の取得用） ---
    public boolean isConfirmed() { return confirmed; }
    public String getEntryType() { return (String) typeCombo.getSelectedItem(); }
    public String getTaskName() { return nameField.getText().trim(); }
    public String getAssignee() { return assigneeField.getText().trim(); }
    public String getStartDate() { return startDateField.getText().trim(); }
    public String getEndDate() { return endDateField.getText().trim(); }
    public String getPriority() { return (String) priorityCombo.getSelectedItem(); }
    public int getProgress() { return progressSlider.getValue(); }
    public Integer getParentProjectId() {
        Task selectedParent = getSelectedParentTask();
        if (selectedParent == null) {
            return null;
        }
        Task rootProject = findRootProject(selectedParent);
        return rootProject != null ? rootProject.getId() : null;
    }

    public Integer getParentTaskId() {
        Task selectedParent = getSelectedParentTask();
        return selectedParent != null ? selectedParent.getId() : null;
    }

    private Task getSelectedParentTask() {
        Object selected = parentProjectCombo.getSelectedItem();
        if (selected == null) {
            return null;
        }
        return parentTargetMap.get(selected.toString());
    }
}