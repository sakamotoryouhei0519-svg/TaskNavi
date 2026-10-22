package org.tasknavi.service;

import org.tasknavi.AppException;
import org.tasknavi.model.Task;
import org.tasknavi.model.User;
import org.tasknavi.persistence.TaskDao;
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

    /** 本番起動用。ログイン中の人のタスクだけを扱う。UI からは {@code TaskDao} を直接 new しない。 */
    public static TaskService createDefault() {
        User user = UserSession.getCurrentUser();
        if (user == null || user.getId() <= 0) {
            throw AppException.validation("ログインが必要です。", "ログインしてから操作してください。");
        }
        return new TaskService(new TaskDao(user.getId()));
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

    /** 兄弟の並びを1回で保存し、成功したあとだけ画面へ知らせる。 */
    public void updateSiblingOrder(List<Task> orderedTasks) {
        taskDao.updateSiblingOrder(orderedTasks);
        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.REFRESH_ALL, null));
    }

    public void updateTask(Task task) {
        updateTasks(List.of(task));
    }

    /**
     * 複数タスクを1回の保存として扱う。全部成功したあとでだけ変更を通知する。
     */
    public void updateTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        taskDao.updateTasks(tasks);
        for (Task task : tasks) {
            TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_UPDATED, task));
        }
    }

    public int addTask(Task task) {
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
