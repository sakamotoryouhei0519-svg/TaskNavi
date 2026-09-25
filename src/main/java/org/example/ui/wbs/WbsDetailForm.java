package org.example.ui.wbs;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.ui.DatePickerDialog;
import org.example.IconManager;
import org.example.Task;
import org.example.UiConstants;
import org.example.UiLabels;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;

/**
 * WBS 右側の詳細編集フォーム。
 */
public final class WbsDetailForm {

    public record Values(
            String projectName,
            String stageName,
            String taskName,
            String assignee,
            String startDateText,
            String endDateText,
            String priorityCode,
            String statusCode
    ) {
    }

    private final JTextField txtProjectName = new JTextField();
    private final JTextField txtStageName = new JTextField();
    private final JTextField txtTaskName = new JTextField();
    private final JTextField txtAssignee = new JTextField();
    private final JTextField txtStartDate = new JTextField(8);
    private final JTextField txtEndDate = new JTextField(8);
    private final JComboBox<String> comboPriority = new JComboBox<>(Task.PRIORITY_OPTIONS);
    private final JRadioButton rbNotStarted =
            new JRadioButton(AppMessages.statusDisplay(Task.STATUS_NOT_STARTED), true);
    private final JRadioButton rbInProgress =
            new JRadioButton(AppMessages.statusDisplay(Task.STATUS_IN_PROGRESS));
    private final JRadioButton rbCompleted =
            new JRadioButton(AppMessages.statusDisplay(Task.STATUS_COMPLETED));

    private final JPanel rootPanel;

    public WbsDetailForm(Runnable onSave, Runnable onCancel) {
        UiLabels.installPriorityRenderer(comboPriority);
        ButtonGroup statusGroup = new ButtonGroup();
        statusGroup.add(rbNotStarted);
        statusGroup.add(rbInProgress);
        statusGroup.add(rbCompleted);

        AppTheme.styleTextField(txtProjectName);
        AppTheme.styleTextField(txtStageName);
        AppTheme.styleTextField(txtTaskName);
        AppTheme.styleTextField(txtAssignee);
        AppTheme.styleTextField(txtStartDate);
        AppTheme.styleTextField(txtEndDate);

        JButton btnSelectStart = createCalendarButton();
        btnSelectStart.addActionListener(e -> openDatePicker(txtStartDate));
        JButton btnSelectEnd = createCalendarButton();
        btnSelectEnd.addActionListener(e -> openDatePicker(txtEndDate));

        comboPriority.setPreferredSize(new Dimension(110, UiConstants.FIELD_HEIGHT_COMPACT));
        AppTheme.styleComboBox(comboPriority);

        JPanel formPanel = new JPanel(new MigLayout("fillx, insets 12 8 12 8", "[left, grow]", "[]8[]8[]8[]8[]8[]8[]8[]"));
        formPanel.setOpaque(false);

        formPanel.add(new JLabel(AppMessages.get("wbs.label.project.name", "プロジェクト名")), "wrap");
        formPanel.add(txtProjectName, "growx, wrap, h 36!");
        formPanel.add(new JLabel(AppMessages.get("wbs.label.stage.name", "工程名")), "wrap");
        formPanel.add(txtStageName, "growx, wrap, h 36!");
        formPanel.add(new JLabel(AppMessages.get("wbs.label.task.name", "タスク名")), "wrap");
        formPanel.add(txtTaskName, "growx, wrap, h 36!");
        formPanel.add(new JLabel(AppMessages.get("label.assignee", "担当者")), "wrap");
        formPanel.add(txtAssignee, "growx, wrap, h 36!");
        formPanel.add(new JLabel(AppMessages.get("label.priority", "優先度")), "wrap");
        formPanel.add(comboPriority, "w 110!, h 36!, wrap");
        formPanel.add(new JLabel(AppMessages.get("label.date", "日付")), "wrap");

        JPanel datePanel = new JPanel(new MigLayout("insets 0, gap 5", "[][][][][]"));
        datePanel.setOpaque(false);
        datePanel.add(txtStartDate, "w 120!, h 36!");
        datePanel.add(btnSelectStart);
        datePanel.add(new JLabel("〜"));
        datePanel.add(txtEndDate, "w 120!, h 36!");
        datePanel.add(btnSelectEnd);
        formPanel.add(datePanel, "wrap");

        formPanel.add(new JLabel(AppMessages.get("wbs.label.status.select", "状態を選択")), "wrap");
        JPanel statusPanel = new JPanel(new MigLayout("insets 0, gap 15", "[][][]"));
        statusPanel.setOpaque(false);
        statusPanel.add(rbNotStarted);
        statusPanel.add(rbInProgress);
        statusPanel.add(rbCompleted);
        formPanel.add(statusPanel, "wrap");

        JPanel bottom = new JPanel(new MigLayout("insets 0, gap 10, align right", "[][]"));
        bottom.setOpaque(false);
        JButton btnSave = AppTheme.createPrimaryButton(
                IconManager.getIcon(IconManager.IconType.CHECK) + " " + AppMessages.get("wbs.button.done", "完了"));
        btnSave.addActionListener(e -> onSave.run());
        JButton btnCancel = AppTheme.createSecondaryButton(
                IconManager.getIcon(IconManager.IconType.CROSS) + " " + AppMessages.get("common.button.cancel", "キャンセル"));
        btnCancel.addActionListener(e -> onCancel.run());
        bottom.add(btnSave, "w 110!, h 36!");
        bottom.add(btnCancel, "w 110!, h 36!");

        rootPanel = new JPanel(new MigLayout("fill, insets 0, gap 10", "[grow]", "[grow][]"));
        rootPanel.setBackground(AppTheme.PANEL_BG);
        rootPanel.add(formPanel, "grow, wrap");
        rootPanel.add(bottom, "grow");
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public void clear() {
        txtProjectName.setText("");
        txtStageName.setText("");
        txtTaskName.setText("");
        txtAssignee.setText("");
        txtStartDate.setText("");
        txtEndDate.setText("");
        comboPriority.setSelectedItem(Task.DEFAULT_PRIORITY);
        rbNotStarted.setSelected(true);
    }

    public void load(Task task, Task rootProject, Task parentStage) {
        if (task == null) {
            clear();
            return;
        }
        txtProjectName.setText(rootProject != null ? rootProject.getName() : "");
        if (task.getLevel() == 1) {
            txtStageName.setText("");
            txtTaskName.setText("");
        } else if (task.getLevel() == 2) {
            txtStageName.setText(task.getName());
            txtTaskName.setText("");
        } else {
            txtStageName.setText(parentStage != null ? parentStage.getName() : "");
            txtTaskName.setText(task.getName());
        }
        txtAssignee.setText(task.getAssignee() != null ? task.getAssignee() : "");
        txtStartDate.setText(task.getStartDate() != null ? task.getStartDate().toString() : "");
        txtEndDate.setText(task.getEndDate() != null ? task.getEndDate().toString() : "");
        comboPriority.setSelectedItem(task.getPriorityCode());
        if (task.isInProgress()) {
            rbInProgress.setSelected(true);
        } else if (task.isCompleted()) {
            rbCompleted.setSelected(true);
        } else {
            rbNotStarted.setSelected(true);
        }
    }

    public Values read() {
        String priority = (String) comboPriority.getSelectedItem();
        String status;
        if (rbCompleted.isSelected()) {
            status = Task.STATUS_COMPLETED;
        } else if (rbInProgress.isSelected()) {
            status = Task.STATUS_IN_PROGRESS;
        } else {
            status = Task.STATUS_NOT_STARTED;
        }
        return new Values(
                txtProjectName.getText().trim(),
                txtStageName.getText().trim(),
                txtTaskName.getText().trim(),
                txtAssignee.getText().trim(),
                txtStartDate.getText(),
                txtEndDate.getText(),
                priority != null ? priority : Task.DEFAULT_PRIORITY,
                status
        );
    }

    public void updateTheme() {
        Color bgColor = AppTheme.isDarkMode() ? AppTheme.PANEL_BG : Color.WHITE;
        Color fgColor = AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY;
        for (JRadioButton rb : new JRadioButton[]{rbNotStarted, rbInProgress, rbCompleted}) {
            rb.setBackground(bgColor);
            rb.setForeground(fgColor);
            rb.setOpaque(false);
        }
        rootPanel.setBackground(AppTheme.PANEL_BG);
    }

    private static JButton createCalendarButton() {
        JButton button = new JButton(IconManager.createCalendarOutlineIcon(AppTheme.TEXT_PRIMARY));
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setPreferredSize(new Dimension(30, 36));
        button.setMinimumSize(new Dimension(30, 36));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void openDatePicker(JTextField field) {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(rootPanel);
        new DatePickerDialog(frame, field);
    }
}
