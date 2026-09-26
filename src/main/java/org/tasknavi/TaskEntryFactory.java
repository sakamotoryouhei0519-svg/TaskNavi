package org.tasknavi;

import org.tasknavi.ui.taskdialog.TaskDialog;

import java.time.LocalDate;
import java.util.List;

/**
 * TaskDialog の入力から新規 Task を組み立てる共通ロジック。
 */
public final class TaskEntryFactory {
    private TaskEntryFactory() {
    }

    public static boolean requiresParent(String entryType) {
        return !EntryType.PROJECT.name().equals(entryType);
    }


    public static Task fromDialog(TaskDialog dialog, List<Task> allTasksForLookup) {
        if (dialog == null) {
            return null;
        }

        String entryType = dialog.getEntryType();
        if (requiresParent(entryType) && dialog.getParentProjectId() == null) {
            return null;
        }

        LocalDate start = LocalDate.parse(dialog.getStartDate());
        LocalDate end = LocalDate.parse(dialog.getEndDate());

        Integer parentProjectId = dialog.getParentProjectId();
        Integer parentId = dialog.getParentTaskId();
        int level = 1;

        if (EntryType.PHASE.name().equals(entryType) && parentProjectId != null) {
            level = 2;
            parentId = parentProjectId;
        } else if (EntryType.TASK.name().equals(entryType) && parentId != null) {
            Task parentTask = findById(allTasksForLookup, parentId);
            level = (parentTask != null && parentTask.getLevel() == 2) ? 3 : 2;
            if (parentTask != null && parentTask.getLevel() == 1) {
                parentId = parentTask.getId();
                level = 2;
            }
        } else if (EntryType.TASK.name().equals(entryType) && parentProjectId != null) {
            parentId = parentProjectId;
            level = 2;
        }

        Task newTask = new Task(
                0,
                dialog.getTaskName(),
                parentId,
                level,
                0,
                dialog.getProgress(),
                Task.STATUS_NOT_STARTED,
                start,
                end
        );
        newTask.setAssignee(dialog.getAssignee());
        newTask.setPriority(dialog.getPriority() != null ? dialog.getPriority() : Task.DEFAULT_PRIORITY);
        return newTask;
    }

    private static Task findById(List<Task> tasks, int id) {
        if (tasks == null) {
            return null;
        }
        for (Task task : tasks) {
            if (task != null && task.getId() == id) {
                return task;
            }
        }
        return null;
    }
}
