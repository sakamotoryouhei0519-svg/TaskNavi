package org.tasknavi.ui.calendar;

import org.tasknavi.AppException;
import org.tasknavi.AppMessages;
import org.tasknavi.ui.ErrorDialogUtil;
import org.tasknavi.ui.NoticeBar;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskBusinessRules;
import org.tasknavi.model.TaskHierarchyUtil;
import org.tasknavi.ui.taskdialog.TaskDialog;
import org.tasknavi.ui.TaskEntryFactory;
import org.tasknavi.service.TaskService;
import org.tasknavi.util.TaskStatusCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Frame;
import java.time.LocalDate;

/**
 * カレンダーからのタスク追加・編集・ステータス更新。
 */
public final class CalendarTaskEditor {

    private static final Logger logger = LoggerFactory.getLogger(CalendarTaskEditor.class);

    private CalendarTaskEditor() {
    }

    public static void openAddForDate(Component parent, TaskService taskService, LocalDate selectedDate) {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(parent);
        TaskDialog dialog = new TaskDialog(frame, taskService);
        if (selectedDate != null) {
            dialog.setStartDate(selectedDate.toString());
            dialog.setEndDate(selectedDate.toString());
        }
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        if (TaskEntryFactory.requiresParent(dialog.getEntryType()) && dialog.getParentProjectId() == null) {
            NoticeBar.error(parent, AppMessages.get("dialog.error.parent.required",
                    "工程・タスクは親プロジェクトまたは親工程を選択してから登録してください。"));
            return;
        }

        try {
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
                    } catch (Exception ex) {
                        ErrorDialogUtil.showBackgroundError(parent, ex,
                                AppMessages.get("mainframe.error.task.add", "タスク追加中にエラーが発生しました。"));
                        logger.error("カレンダーからのタスク追加エラー", ex);
                    }
                }
            };
            worker.execute();
        } catch (Exception ex) {
            ErrorDialogUtil.showError(parent,
                    AppMessages.get("calendar.error.date.format", "日付の形式が正しくありません。"));
        }
    }

    public static void openEdit(Component parent, TaskService taskService, Task task) {
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

        task.setName(dialog.getTaskName());
        task.setAssignee(dialog.getAssignee());
        task.setProgress(dialog.getProgress());
        task.setPriority(dialog.getPriority());
        task.setStartDate(LocalDate.parse(dialog.getStartDate()));
        task.setEndDate(LocalDate.parse(dialog.getEndDate()));
        task.setStatus(TaskStatusCycle.fromProgress(task.getProgress()));
        try {
            taskService.updateTask(task);
            NoticeBar.info(parent, AppMessages.get("notice.saved", "保存しました"));
        } catch (AppException e) {
            ErrorDialogUtil.showError(parent, e.getUserMessage());
        }
    }

    public static void changeStatus(Component parent, TaskService taskService, Task task, String newStatus) {
        if (task == null || newStatus == null) {
            return;
        }
        if (TaskHierarchyUtil.hasChildren(task.getId(), taskService.getAllTasks())) {
            return;
        }
        task.setStatus(newStatus);
        task.setProgress(TaskBusinessRules.resolveProgress(newStatus, task.getProgress()));

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
                } catch (Exception e) {
                    ErrorDialogUtil.showBackgroundError(parent, e,
                            AppMessages.get("kanban.status.update.error", "ステータス更新中に予期せぬエラーが発生しました。"));
                    logger.error("カレンダーステータス更新エラー", e);
                }
            }
        };
        worker.execute();
    }
}
