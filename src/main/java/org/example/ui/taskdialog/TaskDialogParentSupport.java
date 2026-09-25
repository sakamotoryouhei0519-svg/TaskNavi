package org.example.ui.taskdialog;

import org.example.AppMessages;
import org.example.EntryType;
import org.example.Task;
import org.example.TaskHierarchyUtil;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TaskDialog の親要素コンボ（選択肢構築・既定選択・表示切替）。
 */
public final class TaskDialogParentSupport {

    private TaskDialogParentSupport() {
    }

    public static String noneLabel() {
        return AppMessages.get("dialog.parent.none", "親プロジェクトなし");
    }

    public static LinkedHashMap<String, Task> buildOptions(String selectedType, List<Task> allTasks) {
        LinkedHashMap<String, Task> options = new LinkedHashMap<>();
        if (allTasks == null) {
            allTasks = List.of();
        }

        if (EntryType.PROJECT.name().equals(selectedType)) {
            options.put(noneLabel(), null);
            return options;
        }

        if (EntryType.PHASE.name().equals(selectedType)) {
            for (Task candidate : allTasks) {
                if (candidate == null) {
                    continue;
                }
                if (candidate.getLevel() == 1 || candidate.getParentId() == null) {
                    options.put(buildParentLabel(candidate, allTasks), candidate);
                }
            }
            return options;
        }

        if (EntryType.TASK.name().equals(selectedType)) {
            for (Task candidate : allTasks) {
                if (candidate == null) {
                    continue;
                }
                if (candidate.getLevel() == 2) {
                    Task rootProject = TaskHierarchyUtil.findRootProject(candidate, allTasks);
                    String label = rootProject != null
                            ? rootProject.getName() + " / " + candidate.getName()
                            : candidate.getName();
                    options.put(label, candidate);
                }
            }
        }
        return options;
    }

    public static String buildParentLabel(Task task, List<Task> allTasks) {
        if (task == null) {
            return noneLabel();
        }
        if (task.getLevel() == 1 || task.getParentId() == null) {
            return task.getName();
        }
        Task rootProject = TaskHierarchyUtil.findRootProject(task, allTasks);
        if (rootProject != null && rootProject.getId() != task.getId()) {
            return rootProject.getName() + " / " + task.getName();
        }
        return task.getName();
    }

    public static void applyModel(JComboBox<String> combo, Map<String, Task> options) {
        combo.setModel(new DefaultComboBoxModel<>(options.keySet().toArray(new String[0])));
        if (options.isEmpty()) {
            combo.setEnabled(false);
        }
    }

    public static void applyDefaultSelection(
            JComboBox<String> combo,
            Map<String, Task> options,
            String selectedType,
            Integer defaultParentProjectId,
            List<Task> allTasks) {
        if (EntryType.PROJECT.name().equals(selectedType)) {
            combo.setSelectedItem(noneLabel());
            return;
        }

        if (defaultParentProjectId == null) {
            if (options.isEmpty()) {
                combo.setSelectedItem(null);
            } else {
                combo.setSelectedIndex(0);
            }
            return;
        }

        for (Map.Entry<String, Task> entry : options.entrySet()) {
            Task candidate = entry.getValue();
            if (candidate != null && candidate.getId() == defaultParentProjectId) {
                combo.setSelectedItem(entry.getKey());
                return;
            }
            if (candidate != null) {
                Task rootProject = TaskHierarchyUtil.findRootProject(candidate, allTasks);
                if (rootProject != null && rootProject.getId() == defaultParentProjectId) {
                    combo.setSelectedItem(entry.getKey());
                    return;
                }
            }
        }

        if (options.isEmpty()) {
            combo.setSelectedItem(null);
        } else {
            combo.setSelectedIndex(0);
        }
    }

    public static void syncVisibility(
            JComboBox<String> typeCombo,
            JComboBox<String> parentCombo,
            Map<String, Task> options) {
        String selectedType = (String) typeCombo.getSelectedItem();
        boolean isProject = EntryType.PROJECT.name().equals(selectedType);

        if (isProject) {
            parentCombo.setEnabled(false);
            parentCombo.setSelectedItem(noneLabel());
            return;
        }

        if (options.isEmpty()) {
            parentCombo.setEnabled(false);
            parentCombo.setSelectedItem(null);
            return;
        }

        parentCombo.setEnabled(true);
        if (parentCombo.getSelectedItem() == null) {
            parentCombo.setSelectedIndex(0);
        }
    }

    public static Task selectedParent(JComboBox<String> combo, Map<String, Task> options) {
        if (combo == null || options == null) {
            return null;
        }
        Object selected = combo.getSelectedItem();
        if (selected == null) {
            return null;
        }
        return options.get(selected.toString());
    }

    public static Integer rootProjectId(Task selectedParent, List<Task> allTasks) {
        if (selectedParent == null) {
            return null;
        }
        Task root = TaskHierarchyUtil.findRootProject(selectedParent, allTasks);
        return root != null ? root.getId() : null;
    }
}
