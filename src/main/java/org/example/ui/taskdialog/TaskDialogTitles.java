package org.example.ui.taskdialog;

import org.example.AppMessages;
import org.example.Task;

/**
 * TaskDialog のタイトル判定。
 */
public final class TaskDialogTitles {

    private TaskDialogTitles() {
    }

    public static boolean isProjectTask(Task task) {
        if (task == null) {
            return false;
        }
        return task.getLevel() == 1 || task.getParentId() == null;
    }

    public static String forTask(Task task) {
        if (task == null) {
            return AppMessages.get("dialog.task.new", "新規登録");
        }
        return isProjectTask(task)
                ? AppMessages.get("dialog.task.edit.project", "プロジェクト編集")
                : AppMessages.get("dialog.task.edit.task", "タスク編集");
    }
}
