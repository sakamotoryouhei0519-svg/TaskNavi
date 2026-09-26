package org.tasknavi;

import org.tasknavi.event.TaskEvent;
import org.tasknavi.event.TaskEventBus;

import java.util.List;

/**
 * タスクのユースケースをまとめるサービス層。
 */
public class TaskService {
    private final TaskDao taskDao;

    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    /** 本番起動用。UI からは {@code TaskDao} を直接 new せず、これを使う。 */
    public static TaskService createDefault() {
        return new TaskService(new TaskDao());
    }

    public List<Task> getAllTasks() {
        return taskDao.getAllTasks();
    }

    public Task getTaskById(int id) {
        return taskDao.getTaskById(id);
    }

    public void updateTaskOrderIndex(int id, int index) {
        taskDao.updateTaskOrderIndex(id, index);

        Task task = taskDao.getTaskById(id);
        if (task != null) {
            TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_UPDATED, task));
        }
    }

    public void updateTask(Task task) {
        taskDao.validateTask(task);
        taskDao.updateTask(task);

        if (task.getParentId() != null && task.getParentId() != 0) {
            taskDao.updateParentProgress(task.getParentId());
        }

        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_UPDATED, task));
    }

    public int addTask(Task task) {
        taskDao.validateTask(task);
        int newId = taskDao.addTask(task);

        Task created = taskDao.getTaskById(newId);
        if (created == null) {
            Task fallback = copyTaskWithId(task, newId);
            TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_CREATED, fallback));
        } else {
            TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_CREATED, created));
        }

        return newId;
    }

    public void deleteTask(int taskId) {
        Task existing = taskDao.getTaskById(taskId);
        Task deleted = existing != null
                ? existing
                : new Task(taskId, "Deleted Task", null, 0, 0, 0, Task.STATUS_NOT_STARTED, null, null);

        taskDao.deleteTask(taskId);
        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_DELETED, deleted));
    }

    /**
     * タスクの一括インポート処理を行います。
     *
     * @param tasks         インポートするタスクのリスト
     * @param clearExisting 既存のタスクを全削除して置き換える場合は true
     */
    public void importTasks(List<Task> tasks, boolean clearExisting) {
        taskDao.importTasks(tasks, clearExisting);
        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.REFRESH_ALL, null));
    }

    private static Task copyTaskWithId(Task source, int newId) {
        Task copy = new Task(
                newId,
                source.getName(),
                source.getParentId(),
                source.getLevel(),
                source.getOrderIndex(),
                source.getProgress(),
                source.getStatusCode(),
                source.getStartDate(),
                source.getEndDate()
        );
        copy.setAssignee(source.getAssignee());
        copy.setPriority(source.getPriority());
        return copy;
    }
}
