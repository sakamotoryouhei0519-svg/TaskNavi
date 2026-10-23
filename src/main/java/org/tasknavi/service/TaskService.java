package org.tasknavi.service;

import org.tasknavi.AppException;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskBusinessRules;
import org.tasknavi.model.TaskHierarchyUtil;
import org.tasknavi.model.User;
import org.tasknavi.persistence.TaskDao;
import org.tasknavi.event.TaskEvent;
import org.tasknavi.event.TaskEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * タスクのユースケースをまとめるサービス層。
 */
public class TaskService {
    private final TaskDao taskDao;
    private final Object snapshotLock = new Object();
    private List<Task> snapshot;
    private long snapshotVersion;

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

    /**
     * 全件を返す。タスクの書き込みは必ずこのクラスを通るので、次の書き込みまでは前に読んだ結果を使い回す。
     * 受け取った側が書き換えても共有の結果が壊れないよう、毎回複製して渡す。
     */
    public List<Task> getAllTasks() {
        long version;
        synchronized (snapshotLock) {
            if (snapshot != null) {
                return copyOf(snapshot);
            }
            version = snapshotVersion;
        }
        List<Task> loaded = taskDao.getAllTasks();
        if (loaded == null) {
            loaded = new ArrayList<>();
        }
        synchronized (snapshotLock) {
            if (version == snapshotVersion) {
                snapshot = copyOf(loaded);
            }
        }
        return loaded;
    }

    /** データベースから読み直して、全画面に描き直させる。 */
    public void reload() {
        invalidateSnapshot();
        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.REFRESH_ALL, null));
    }

    public Task getTaskById(int id) {
        return taskDao.getTaskById(id);
    }

    public void updateTaskOrderIndex(int id, int index) {
        try {
            taskDao.updateTaskOrderIndex(id, index);
        } finally {
            invalidateSnapshot();
        }

        Task task = taskDao.getTaskById(id);
        if (task != null) {
            TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_UPDATED, task));
        }
    }

    /** 兄弟の並びを1回で保存し、成功したあとだけ画面へ知らせる。 */
    public void updateSiblingOrder(List<Task> orderedTasks) {
        try {
            taskDao.updateSiblingOrder(orderedTasks);
        } finally {
            invalidateSnapshot();
        }
        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.REFRESH_ALL, null));
    }

    public void updateTask(Task task) {
        updateTasks(List.of(task));
    }

    public enum StatusChange {
        CHANGED,
        HAS_CHILDREN,
        NOT_FOUND
    }

    /**
     * 状態を1件変え、進捗率も状態に合わせる。子がいるタスクは子の状態から決まるので変えない。
     */
    public StatusChange changeStatus(int taskId, String newStatus) {
        Objects.requireNonNull(newStatus, "newStatus");
        List<Task> allTasks = getAllTasks();
        Task task = null;
        for (Task candidate : allTasks) {
            if (candidate.getId() == taskId) {
                task = candidate;
                break;
            }
        }
        if (task == null) {
            return StatusChange.NOT_FOUND;
        }
        if (TaskHierarchyUtil.hasChildren(taskId, allTasks)) {
            return StatusChange.HAS_CHILDREN;
        }
        task.setStatus(newStatus);
        task.setProgress(TaskBusinessRules.resolveProgress(newStatus, task.getProgress()));
        updateTask(task);
        return StatusChange.CHANGED;
    }

    /**
     * 複数タスクを1回の保存として扱う。全部成功したあとでだけ変更を通知する。
     */
    public void updateTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        try {
            taskDao.updateTasks(tasks);
        } finally {
            invalidateSnapshot();
        }
        for (Task task : tasks) {
            TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_UPDATED, task));
        }
    }

    public int addTask(Task task) {
        int newId;
        try {
            newId = taskDao.addTask(task);
        } finally {
            invalidateSnapshot();
        }

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

        try {
            taskDao.deleteTask(taskId);
        } finally {
            invalidateSnapshot();
        }
        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.TASK_DELETED, deleted));
    }

    /**
     * タスクの一括インポート処理を行います。
     *
     * @param tasks         インポートするタスクのリスト
     * @param clearExisting 既存のタスクを全削除して置き換える場合は true
     */
    public void importTasks(List<Task> tasks, boolean clearExisting) {
        try {
            taskDao.importTasks(tasks, clearExisting);
        } finally {
            invalidateSnapshot();
        }
        TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.REFRESH_ALL, null));
    }

    private void invalidateSnapshot() {
        synchronized (snapshotLock) {
            snapshot = null;
            snapshotVersion++;
        }
    }

    private static List<Task> copyOf(List<Task> tasks) {
        List<Task> copies = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            copies.add(task != null ? task.copy() : null);
        }
        return copies;
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
