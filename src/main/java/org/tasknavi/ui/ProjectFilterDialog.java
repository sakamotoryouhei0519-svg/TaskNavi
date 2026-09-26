package org.tasknavi.ui;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.Task;
import org.tasknavi.TaskService;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
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
            JOptionPane.showMessageDialog(
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
        AppTheme.styleComboBox(projectCombo);

        JPanel dlgPanel = new JPanel(new MigLayout("fillx, insets 10, gap 10", "[grow]", "[][]"));
        dlgPanel.add(new JLabel(AppMessages.get("project.switch.select", "表示するプロジェクトを選択してください:")), "wrap");
        dlgPanel.add(projectCombo, "growx, h 32!");

        int result = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(parent),
                dlgPanel,
                AppMessages.get("project.switch.title", "既存プロジェクト切替"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
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
