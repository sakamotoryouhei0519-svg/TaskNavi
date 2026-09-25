package org.example.ui.taskdialog;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.DatePickerDialog;
import org.example.IconManager;
import org.example.UiDebugUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;

/**
 * TaskDialog のフォーム／ボタンパネル組み立て。
 */
public final class TaskDialogFormFactory {

    public record FormParts(
            JPanel formPanel,
            JLabel parentLabel,
            JPanel startPanel,
            JPanel endPanel,
            JPanel progressPanel
    ) {
    }

    private TaskDialogFormFactory() {
    }

    public static FormParts buildForm(
            JComboBox<String> typeCombo,
            JComboBox<String> parentProjectCombo,
            JTextField nameField,
            JTextField assigneeField,
            JTextField startDateField,
            JTextField endDateField,
            JComboBox<String> priorityCombo,
            JSlider progressSlider,
            JLabel progressLabel,
            Frame ownerForDatePicker) {

        JPanel formPanel = new JPanel(new MigLayout("fill, insets 15 20 15 20, wrap", "[right][grow,fill]", "[]8"));
        formPanel.setBackground(AppTheme.PANEL_BG);

        JLabel typeLabel = labeled(AppMessages.get("dialog.label.type", "種別") + ":");
        formPanel.add(typeLabel, "align right");
        formPanel.add(typeCombo, "wrap");

        JLabel parentLabel = labeled(AppMessages.get("dialog.label.parent", "親要素") + ":");
        formPanel.add(parentLabel, "align right");
        formPanel.add(parentProjectCombo, "wrap");

        formPanel.add(labeled(AppMessages.get("dialog.label.name", "名前") + ":"), "align right");
        formPanel.add(nameField, "wrap");
        formPanel.add(labeled(AppMessages.get("dialog.label.assignee", "担当者") + ":"), "align right");
        formPanel.add(assigneeField, "wrap");

        JPanel startPanel = dateRow(
                startDateField,
                AppMessages.get("dialog.tooltip.select.start", "開始日を選択"),
                "TaskDialog.startDateButton",
                ownerForDatePicker);
        formPanel.add(labeled(AppMessages.get("dialog.label.startdate", "開始日 (YYYY-MM-DD)") + ":"), "align right");
        formPanel.add(startPanel, "wrap");

        JPanel endPanel = dateRow(
                endDateField,
                AppMessages.get("dialog.tooltip.select.end", "終了日を選択"),
                "TaskDialog.endDateButton",
                ownerForDatePicker);
        formPanel.add(labeled(AppMessages.get("dialog.label.enddate", "終了日 (YYYY-MM-DD)") + ":"), "align right");
        formPanel.add(endPanel, "wrap");

        formPanel.add(labeled(AppMessages.get("dialog.label.priority", "優先度") + ":"), "align right");
        formPanel.add(priorityCombo, "wrap");

        progressSlider.setMajorTickSpacing(25);
        progressSlider.setPaintTicks(true);
        progressSlider.addChangeListener(e -> progressLabel.setText(progressSlider.getValue() + "%"));

        JPanel progressPanel = new JPanel(new MigLayout("fill, insets 0, gap 5", "[grow][]"));
        progressPanel.setOpaque(false);
        progressPanel.add(progressSlider, "grow");
        progressPanel.add(progressLabel);

        formPanel.add(labeled(AppMessages.get("dialog.label.progress", "進捗率") + ":"), "align right");
        formPanel.add(progressPanel, "wrap");

        return new FormParts(formPanel, parentLabel, startPanel, endPanel, progressPanel);
    }

    public static JPanel buildButtonPanel(Runnable onSave, Runnable onCancel) {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 10, gap 10, align right", "[][]"));
        buttonPanel.setBackground(AppTheme.BACKGROUND);

        JButton saveButton = new JButton(AppMessages.get("common.button.save", "保存"));
        saveButton.setFont(AppTheme.FONT_MAIN);
        saveButton.setBackground(AppTheme.PRIMARY);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> onSave.run());

        JButton cancelButton = new JButton(AppMessages.get("common.button.cancel", "キャンセル"));
        cancelButton.setFont(AppTheme.FONT_MAIN);
        cancelButton.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        cancelButton.setForeground(AppTheme.TEXT_PRIMARY);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> onCancel.run());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        return buttonPanel;
    }

    private static JLabel labeled(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        return label;
    }

    private static JPanel dateRow(JTextField field, String tooltip, String debugName, Frame owner) {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 5, align left", "[][]"));
        panel.setOpaque(false);
        field.setColumns(10);
        AppTheme.styleTextField(field);

        JButton cal = new JButton(IconManager.createCalendarOutlineIcon(AppTheme.TEXT_PRIMARY));
        cal.setPreferredSize(new Dimension(40, 36));
        cal.setMargin(new Insets(0, 2, 0, 2));
        cal.setToolTipText(tooltip);
        cal.setFocusPainted(false);
        cal.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        cal.setForeground(AppTheme.TEXT_PRIMARY);
        cal.setBorderPainted(false);
        cal.setContentAreaFilled(false);
        cal.setOpaque(false);
        UiDebugUtil.logButtonState(debugName, cal);
        cal.addActionListener(e -> new DatePickerDialog(owner, field));

        panel.add(field);
        panel.add(cal);
        return panel;
    }
}
