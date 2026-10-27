package org.tasknavi.ui.taskdialog;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.DatePickerDialog;
import org.tasknavi.ui.IconManager;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.util.List;

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
            JComboBox<String> statusCombo,
            String statusHint,
            JTextArea memoArea,
            List<String> assigneeOptions,
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
        formPanel.add(assigneeRow(assigneeField, assigneeOptions), "wrap");

        JPanel startPanel = dateRow(
                startDateField,
                AppMessages.get("dialog.tooltip.select.start", "開始日を選択"),
                ownerForDatePicker);
        formPanel.add(labeled(AppMessages.get("dialog.label.startdate", "開始日 (YYYY-MM-DD)") + ":"), "align right");
        formPanel.add(startPanel, "wrap");

        JPanel endPanel = dateRow(
                endDateField,
                AppMessages.get("dialog.tooltip.select.end", "終了日を選択"),
                ownerForDatePicker);
        formPanel.add(labeled(AppMessages.get("dialog.label.enddate", "終了日 (YYYY-MM-DD)") + ":"), "align right");
        formPanel.add(endPanel, "wrap");

        formPanel.add(labeled(AppMessages.get("dialog.label.priority", "優先度") + ":"), "align right");
        formPanel.add(priorityCombo, "wrap");

        JPanel progressPanel = new JPanel(new MigLayout("fill, insets 0, gap 5", "[grow]"));
        progressPanel.setOpaque(false);
        progressPanel.add(statusCombo, "grow, wrap");
        if (statusHint != null && !statusHint.isBlank()) {
            JLabel hint = new JLabel(statusHint);
            hint.setFont(AppTheme.FONT_SMALL);
            hint.setForeground(AppTheme.TEXT_MUTED);
            progressPanel.add(hint, "grow");
        }

        formPanel.add(labeled(AppMessages.get("dialog.label.status", "状態") + ":"), "align right");
        formPanel.add(progressPanel, "wrap");

        memoArea.setLineWrap(true);
        memoArea.setWrapStyleWord(true);
        memoArea.setToolTipText(AppMessages.get("dialog.tooltip.memo", "Ctrl+Enter で保存"));
        memoArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        memoArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        JScrollPane memoScroll = AppTheme.createStyledTextAreaScroll(memoArea);
        memoScroll.setPreferredSize(new Dimension(280, 96));
        formPanel.add(labeled(AppMessages.get("dialog.label.memo", "メモ") + ":"), "align right top, gaptop 6");
        formPanel.add(memoScroll, "growx, wrap");

        return new FormParts(formPanel, parentLabel, startPanel, endPanel, progressPanel);
    }

    public record DialogButtons(JPanel panel, JButton saveButton) {
    }

    public static DialogButtons buildButtonPanel(Runnable onSave, Runnable onCancel) {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 10, gap 10, align right", "[][]"));
        buttonPanel.setBackground(AppTheme.BACKGROUND);

        JButton saveButton = new JButton(AppMessages.get("common.button.save", "保存"));
        JButton cancelButton = new JButton(AppMessages.get("common.button.cancel", "キャンセル"));
        AppTheme.styleDialogSecondaryButton(saveButton);
        AppTheme.styleDialogSecondaryButton(cancelButton);
        int textWidth = Math.max(
                saveButton.getFontMetrics(saveButton.getFont()).stringWidth(saveButton.getText()),
                cancelButton.getFontMetrics(cancelButton.getFont()).stringWidth(cancelButton.getText()));
        Dimension buttonSize = new Dimension(textWidth + 36, 36);
        saveButton.setPreferredSize(buttonSize);
        saveButton.setMinimumSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
        cancelButton.setMinimumSize(buttonSize);
        saveButton.addActionListener(e -> onSave.run());
        cancelButton.addActionListener(e -> onCancel.run());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        return new DialogButtons(buttonPanel, saveButton);
    }

    private static JLabel labeled(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        return label;
    }

    /** 候補があれば、押すと名前の一覧が出るボタンを横に置く。手で打つこともできる。 */
    private static JComponent assigneeRow(JTextField field, List<String> options) {
        if (options == null || options.isEmpty()) {
            return field;
        }
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 5", "[grow,fill][]"));
        panel.setOpaque(false);
        JButton pick = new JButton("▼");
        AppTheme.styleDialogSecondaryButton(pick);
        pick.setPreferredSize(new Dimension(40, 36));
        pick.setToolTipText(AppMessages.get("dialog.tooltip.assignee.pick", "メンバーから選ぶ"));
        pick.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            menu.setBackground(AppTheme.PANEL_BG);
            for (String name : options) {
                JMenuItem item = new JMenuItem(name);
                item.setFont(AppTheme.FONT_MAIN);
                item.setForeground(AppTheme.TEXT_PRIMARY);
                item.setBackground(AppTheme.PANEL_BG);
                item.addActionListener(ev -> field.setText(name));
                menu.add(item);
            }
            menu.show(pick, 0, pick.getHeight());
        });
        panel.add(field);
        panel.add(pick);
        return panel;
    }

    private static JPanel dateRow(JTextField field, String tooltip, Frame owner) {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 5, align left", "[][]"));
        panel.setOpaque(false);
        field.setColumns(10);
        AppTheme.styleTextField(field);

        JButton cal = new JButton(IconManager.createCalendarOutlineIcon(AppTheme.TEXT_PRIMARY));
        AppTheme.styleDialogSecondaryButton(cal);
        cal.setPreferredSize(new Dimension(40, 36));
        cal.setMargin(new Insets(0, 2, 0, 2));
        cal.setToolTipText(tooltip);
        cal.addActionListener(e -> new DatePickerDialog(owner, field));

        panel.add(field);
        panel.add(cal);
        return panel;
    }
}
