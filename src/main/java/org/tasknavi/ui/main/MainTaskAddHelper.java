package org.tasknavi.ui.main;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.ErrorDialogUtil;
import org.tasknavi.ui.NoticeBar;
import org.tasknavi.ui.TaskEntryFactory;
import org.tasknavi.ui.taskdialog.TaskDialog;

import javax.swing.JFrame;
import javax.swing.SwingWorker;

/**
 * MainFrame からのタスク追加フロー。
 */
public final class MainTaskAddHelper {

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
                }
            }
        };
        worker.execute();
    }
}
