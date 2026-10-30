package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.service.TaskService;

import javax.swing.SwingWorker;
import java.awt.Component;

/**
 * カンバン・カレンダー共通の状態変更。保存は裏側で TaskService に任せる。
 */
public final class TaskStatusChanger {

    private TaskStatusChanger() {
    }

    public static void change(Component parent, TaskService taskService, int taskId, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            return;
        }
        SwingWorker<TaskService.StatusChange, Void> worker = new SwingWorker<>() {
            @Override
            protected TaskService.StatusChange doInBackground() {
                return taskService.changeStatus(taskId, newStatus);
            }

            @Override
            protected void done() {
                try {
                    if (get() == TaskService.StatusChange.HAS_CHILDREN) {
                        NoticeBar.info(parent, AppMessages.get(
                                "task.status.locked.by.children",
                                "子タスクがあるため、状態は子タスクから自動で決まります。"));
                    }
                } catch (Exception e) {
                    ErrorDialogUtil.showBackgroundError(parent, e,
                            AppMessages.get("kanban.status.update.error", "ステータス更新中に予期せぬエラーが発生しました。"));
                }
            }
        };
        worker.execute();
    }
}
