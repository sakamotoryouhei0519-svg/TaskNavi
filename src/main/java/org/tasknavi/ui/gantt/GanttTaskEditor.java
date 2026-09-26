package org.tasknavi.ui.gantt;

import org.tasknavi.Task;
import org.tasknavi.ui.taskdialog.TaskDialog;
import org.tasknavi.TaskService;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;
import java.time.LocalDate;

/**
 * ガントからのタスク編集ダイアログ。
 */
public final class GanttTaskEditor {

    private GanttTaskEditor() {
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
        taskService.updateTask(task);
    }
}
