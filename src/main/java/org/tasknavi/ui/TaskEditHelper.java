package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;
import org.tasknavi.ui.taskdialog.TaskDialog;
import org.tasknavi.util.TaskStatusCycle;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 編集ダイアログを開き、確定されたら裏側で TaskService.updateTask に保存する。
 */
public final class TaskEditHelper {

    private TaskEditHelper() {
    }

    public static void openAndSave(Component parent, TaskService taskService, Task task) {
        if (task == null) {
            return;
        }
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(parent);
        if (frame == null) {
            return;
        }

        TaskDialog dialog = new TaskDialog(frame, taskService, task);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(dialog.getStartDate());
            endDate = LocalDate.parse(dialog.getEndDate());
        } catch (DateTimeParseException ex) {
            ErrorDialogUtil.showError(parent,
                    AppMessages.get("wbs.error.date.format", "日付の形式は YYYY-MM-DD で入力してください。"));
            return;
        }

        task.setName(dialog.getTaskName());
        task.setAssignee(dialog.getAssignee());
        task.setMemo(dialog.getMemo());
        task.setProgress(dialog.getProgress());
        task.setPriority(dialog.getPriority());
        task.setStartDate(startDate);
        task.setEndDate(endDate);
        task.setStatus(TaskStatusCycle.fromProgress(task.getProgress()));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                taskService.updateTask(task);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    NoticeBar.info(parent, AppMessages.get("notice.saved", "保存しました"));
                } catch (Exception e) {
                    ErrorDialogUtil.showBackgroundError(parent, e,
                            AppMessages.get("wbs.error.update", "更新中に予期せぬエラーが発生しました。"));
                }
            }
        };
        worker.execute();
    }
}
