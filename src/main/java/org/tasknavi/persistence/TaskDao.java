package org.tasknavi.persistence;

import org.tasknavi.AppException;
import org.tasknavi.model.Task;
import org.tasknavi.util.ProjectMemberFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【データアクセスオブジェクト (DAO) クラス】
 * tasks テーブルの入口。SQL の中身は同じパッケージのクラスに分けている。
 */
public class TaskDao {

    private static final Logger logger = LoggerFactory.getLogger(TaskDao.class);

    private final int ownerId;
    private final TaskSearchDao searchDao;

    public TaskDao(int ownerId) {
        if (ownerId <= 0) {
            throw new IllegalArgumentException("ownerId must be positive");
        }
        this.ownerId = ownerId;
        this.searchDao = new TaskSearchDao(ownerId, this::getAllTasks);
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

    public boolean updateTaskStatus(int taskId, String newStatus) {
        return TaskCrudDao.updateStatus(taskId, newStatus, ownerId);
    }

    public void deleteTask(int taskId) {
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
    }

    public void importTasks(List<Task> tasks, boolean replaceExisting) {
        TaskImportDao.importTasks(tasks, replaceExisting, ownerId, this::validateTask);
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

    public boolean isValidHierarchy(Task task) {
        return TaskHierarchySupport.isValidHierarchy(task, this::getTaskById);
    }

    public List<Task> searchByName(String keyword) {
        return searchDao.searchByName(keyword);
    }

    public List<Task> searchByAssignee(String assignee) {
        return searchDao.searchByAssignee(assignee);
    }

    public List<Task> filterByStatus(String status) {
        return searchDao.filterByStatus(status);
    }

    public List<Task> filterByDateRange(LocalDate startDate, LocalDate endDate) {
        return searchDao.filterByDateRange(startDate, endDate);
    }

    public List<Task> searchCombined(String keyword, String assignee, String status,
                                     LocalDate startDate, LocalDate endDate, Integer projectId) {
        List<Task> results = searchDao.searchCombined(keyword, assignee, status, startDate, endDate);
        if (projectId == null) {
            return results;
        }
        Set<Integer> memberIds = ProjectMemberFilter.collectMemberIds(getAllTasks(), projectId);
        List<Task> filtered = new ArrayList<>();
        for (Task task : results) {
            if (memberIds.contains(task.getId())) {
                filtered.add(task);
            }
        }
        return filtered;
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

    public void reorderSiblingTasks(Integer parentId) {
        List<Task> changed = TaskReorderSupport.reorderSiblings(parentId, getAllTasks());
        updateSiblingOrder(changed);
    }
}
