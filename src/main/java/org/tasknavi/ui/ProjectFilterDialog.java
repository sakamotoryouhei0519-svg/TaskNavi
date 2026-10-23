package org.tasknavi.ui;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.taskdialog.TaskDialogUiStyle;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 * プロジェクト絞り込みダイアログ（共通）。
 */
public final class ProjectFilterDialog {
    private ProjectFilterDialog() {
    }

    public record Result(boolean confirmed, Integer projectId) {
    }

    public static Result show(Component parent, TaskService taskService) {
        List<Task> allTasks = taskService.getAllTasks();
        List<Task> projectList = new ArrayList<>();
        for (Task t : allTasks) {
            if (t.getLevel() == 1 || t.getParentId() == null) {
                projectList.add(t);
            }
        }
        if (projectList.isEmpty()) {
            NoticeBar.info(
                    parent,
                    AppMessages.get("project.switch.no.projects", "登録されているプロジェクトがありません。")
            );
            return new Result(false, null);
        }

        String[] options = new String[projectList.size() + 1];
        options[0] = AppMessages.get("project.switch.all", "[すべてのプロジェクトを表示]");
        for (int i = 0; i < projectList.size(); i++) {
            options[i + 1] = projectList.get(i).getName();
        }

        JComboBox<String> projectCombo = new JComboBox<>(options);
        projectCombo.setSelectedIndex(0);
        TaskDialogUiStyle.styleComboBox(projectCombo);
        FontMetrics metrics = projectCombo.getFontMetrics(projectCombo.getFont());
        int optionWidth = 320;
        for (String option : options) {
            optionWidth = Math.max(optionWidth, metrics.stringWidth(option) + 48);
        }
        projectCombo.setPreferredSize(new Dimension(Math.min(optionWidth, 720), 36));

        JLabel selectLabel = new JLabel(AppMessages.get("project.switch.select", "表示するプロジェクトを選択してください:"));
        selectLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel content = new JPanel(new MigLayout("fillx, insets 0, gap 10, wrap", "[grow,fill]"));
        content.setOpaque(false);
        content.add(selectLabel);
        content.add(projectCombo, "growx, h 36!");

        boolean confirmed = AppDialog.showForm(
                parent,
                AppMessages.get("project.switch.title", "既存プロジェクト切替"),
                content);
        if (!confirmed) {
            return new Result(false, null);
        }

        String selected = (String) projectCombo.getSelectedItem();
        if (selected == null) {
            return new Result(false, null);
        }
        if (selected.equals(options[0])) {
            return new Result(true, null);
        }
        for (Task p : projectList) {
            if (p.getName().equals(selected)) {
                return new Result(true, p.getId());
            }
        }
        return new Result(false, null);
    }
}
