package org.tasknavi.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppException;
import org.tasknavi.model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自分だけのタスク（tasks テーブル）の保存先。SQL の中身は同じパッケージのクラスに分けている。
 */
public class TaskDao implements TaskStore {

    private static final Logger logger = LoggerFactory.getLogger(TaskDao.class);

    private final int ownerId;

    public TaskDao(int ownerId) {
        if (ownerId <= 0) {
            throw new IllegalArgumentException("ownerId must be positive");
        }
        this.ownerId = ownerId;
    }

    public int addTask(Task task) {
        validateTask(task);
        int id = TaskCrudDao.add(task, ownerId);
        Integer parentId = task.getParentId();
        if (parentId != null && parentId > 0) {
            updateParentProgress(parentId);
        }
        return id;
    }

    public List<Task> getAllTasks() {
        return TaskCrudDao.getAll(ownerId);
    }

    public Task getTaskById(int taskId) {
        return TaskCrudDao.getById(taskId, ownerId);
    }

    public void updateTask(Task task) {
        updateTasks(List.of(task));
    }

    /**
     * 複数タスクをまとめて保存する。検証に失敗した場合は1件も書き込まない。
     */
    public void updateTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (Task task : tasks) {
            validateTask(task);
        }
        TaskCrudDao.updateAll(tasks, ownerId);
    }

    /** @return 消したタスク（子を含む）。{@link #restoreTasks} に渡せば元に戻せる */
    public List<Task> deleteTask(int taskId) {
        if (taskId <= 0) {
            throw AppException.validation("削除対象のIDが不正です: " + taskId, "削除対象を特定できませんでした。");
        }

        List<Task> allTasks = getAllTasks();
        boolean exists = allTasks.stream().anyMatch(t -> t.getId() == taskId);
        if (!exists) {
            throw AppException.notFound("タスクID: " + taskId);
        }

        Integer parentId = null;
        for (Task task : allTasks) {
            if (task.getId() == taskId) {
                parentId = task.getParentId();
                break;
            }
        }

        Set<Integer> targetIds = new HashSet<>();
        TaskHierarchySupport.collectDescendantIds(taskId, allTasks, targetIds);
        TaskCrudDao.deleteIds(targetIds, ownerId);
        if (parentId != null && parentId > 0 && !targetIds.contains(parentId)) {
            updateParentProgress(parentId);
        }
        logger.info("タスクID: {} を削除しました。対象件数={}", taskId, targetIds.size());

        List<Task> deleted = new ArrayList<>();
        for (Task task : allTasks) {
            if (targetIds.contains(task.getId())) {
                deleted.add(task);
            }
        }
        return deleted;
    }

    /**
     * {@link #deleteTask} で消したタスクを元の ID のまま戻す。
     * 消したあとで親が消されていたら、行き場がないので戻さない。
     */
    public void restoreTasks(List<Task> deletedTasks) {
        if (deletedTasks == null || deletedTasks.isEmpty()) {
            return;
        }
        Set<Integer> restoringIds = new HashSet<>();
        for (Task task : deletedTasks) {
            restoringIds.add(task.getId());
        }
        Set<Integer> outsideParentIds = new HashSet<>();
        for (Task task : deletedTasks) {
            Integer parentId = task.getParentId();
            if (parentId != null && parentId > 0 && !restoringIds.contains(parentId)) {
                outsideParentIds.add(parentId);
            }
        }
        for (Integer parentId : outsideParentIds) {
            if (getTaskById(parentId) == null) {
                throw AppException.businessRule(
                        "復元先の親タスクがありません: " + parentId,
                        "親のタスクが削除されているため、元に戻せません。");
            }
        }

        TaskCrudDao.insertWithIds(deletedTasks, ownerId);
        for (Integer parentId : outsideParentIds) {
            updateParentProgress(parentId);
        }
        logger.info("削除したタスクを元に戻しました。対象件数={}", deletedTasks.size());
    }

    /**
     * タスクをまとめて取り込む。ID・親 ID は {@code tasks} の中だけで通じる番号として扱い、保存時に振り直す。
     * 1 件でも不正なら何も書き込まない。
     */
    public void importTasks(List<Task> tasks, boolean replaceExisting) {
        if (tasks == null) {
            return;
        }
        Map<Integer, Task> byImportId = new HashMap<>();
        List<Task> valid = new ArrayList<>();
        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            if (byImportId.putIfAbsent(task.getId(), task) != null) {
                throw AppException.businessRule(
                        "取り込みデータの ID が重複しています: " + task.getId(),
                        "取り込むデータに同じ ID のタスクが複数あります（ID: " + task.getId() + "）。");
            }
            valid.add(task);
        }
        for (Task task : valid) {
            try {
                TaskWriteValidator.validate(task, byImportId::get);
            } catch (AppException e) {
                throw AppException.businessRule(
                        "インポートデータに不正な値があります: " + task.getName(),
                        "「" + task.getName() + "」: " + e.getUserMessage());
            }
        }
        if (valid.isEmpty()) {
            return;
        }
        TaskImportDao.importTasks(valid, replaceExisting, ownerId);
    }

    public void updateParentProgress(int parentId) {
        updateParentProgress(parentId, getAllTasks());
    }

    private void updateParentProgress(int parentId, List<Task> allTasks) {
        for (Task rolled : TaskHierarchySupport.rollupAncestors(parentId, allTasks)) {
            TaskCrudDao.updateProgressAndStatus(rolled, ownerId);
        }
    }

    public void validateTask(Task task) {
        TaskWriteValidator.validate(task, this::getTaskById);
    }

    public void updateTaskOrderIndex(int taskId, int newOrderIndex) {
        TaskCrudDao.updateOrderIndex(taskId, newOrderIndex, ownerId);
    }

    public void updateSiblingOrder(List<Task> orderedTasks) {
        if (orderedTasks == null || orderedTasks.isEmpty()) {
            return;
        }
        Map<Integer, Integer> orderByTaskId = new HashMap<>();
        for (Task task : orderedTasks) {
            orderByTaskId.put(task.getId(), task.getOrderIndex());
        }
        TaskCrudDao.updateOrderIndexes(orderByTaskId, ownerId);
    }
}
