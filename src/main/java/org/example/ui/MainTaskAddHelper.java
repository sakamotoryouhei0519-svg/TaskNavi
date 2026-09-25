package org.example.ui;

import org.example.AppMessages;
import org.example.ErrorDialogUtil;
import org.example.Task;
import org.example.ui.taskdialog.TaskDialog;
import org.example.TaskEntryFactory;
import org.example.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
            JOptionPane.showMessageDialog(
                    parent,
                    AppMessages.get("dialog.error.parent.required"),
                    AppMessages.get("dialog.error.input"),
                    JOptionPane.ERROR_MESSAGE);
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
                } catch (Exception e) {
                    ErrorDialogUtil.showError(parent, AppMessages.get("mainframe.error.task.add"));
                    logger.error("タスク追加エラー", e);
                }
            }
        };
        worker.execute();
    }
}
