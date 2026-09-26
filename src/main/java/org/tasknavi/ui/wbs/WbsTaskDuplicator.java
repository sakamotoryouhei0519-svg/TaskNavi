package org.tasknavi.ui.wbs;

import org.tasknavi.Task;
import org.tasknavi.TaskService;

import java.util.List;

/**
 * タスクとその子孫を再帰的に複製する。
 */
public final class WbsTaskDuplicator {

    private WbsTaskDuplicator() {
    }

    public static Task duplicateRecursive(
            TaskService taskService, Task originalTask, String newName, Integer newParentId) {
        Task newTask = new Task(
                0,
                newName,
                newParentId,
                originalTask.getLevel(),
                originalTask.getOrderIndex(),
                originalTask.getProgress(),
                originalTask.getStatusCode(),
                originalTask.getStartDate(),
                originalTask.getEndDate()
        );
        newTask.setAssignee(originalTask.getAssignee());
        newTask.setPriority(originalTask.getPriority());

        int newTaskId = taskService.addTask(newTask);

        List<Task> allTasks = taskService.getAllTasks();
        for (Task child : allTasks) {
            if (child.getParentId() != null && child.getParentId() == originalTask.getId()) {
                duplicateRecursive(taskService, child, child.getName(), newTaskId);
            }
        }

        return newTask;
    }
}
