package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.ui.taskdialog.TaskDialog;
import org.tasknavi.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.SwingWorker;

/**
 * MainFrame からのタスク追加フロー。
 */
public final class MainTaskAddHelper {

    private static final Logger logger = LoggerFactory.getLogger(MainTaskAddHelper.class);

    private MainTaskAddHelper() {
    }

    public static void showAndAdd(JFrame parent, TaskService taskService) {
        TaskDialog dialog = new TaskDialog(parent, taskService);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        if (TaskEntryFactory.requiresParent(dialog.getEntryType()) && dialog.getParentProjectId() == null) {
            NoticeBar.error(parent, AppMessages.get("dialog.error.parent.required"));
            return;
        }

        Task newTask = TaskEntryFactory.fromDialog(dialog, taskService.getAllTasks());
        if (newTask == null) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                taskService.addTask(newTask);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    NoticeBar.info(parent, AppMessages.get("notice.saved", "保存しました"));
                } catch (Exception e) {
                    ErrorDialogUtil.showBackgroundError(parent, e, AppMessages.get("mainframe.error.task.add"));
                    logger.error("タスク追加エラー", e);
                }
            }
        };
        worker.execute();
    }
}
