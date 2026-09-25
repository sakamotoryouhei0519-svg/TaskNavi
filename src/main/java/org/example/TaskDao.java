package org.example;

import org.example.persistence.TaskCrudDao;
import org.example.persistence.TaskHierarchySupport;
import org.example.persistence.TaskImportDao;
import org.example.persistence.TaskReorderSupport;
import org.example.persistence.TaskSearchDao;
import org.example.persistence.TaskWriteValidator;
import org.example.util.ProjectMemberFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 【データアクセスオブジェクト (DAO) クラス】
 * tasks テーブルへの公開 API。実装は persistence パッケージへ委譲する。
 */
public class TaskDao {

    private static final Logger logger = LoggerFactory.getLogger(TaskDao.class);

    private final TaskSearchDao searchDao = new TaskSearchDao(this::getAllTasks);

    public int addTask(Task task) {
        try {
            validateTask(task);
        } catch (AppException e) {
            logger.error("タスク追加エラー: " + e.getUserMessage(), e);
            return -1;
        }
        return TaskCrudDao.add(task);
    }

    public List<Task> getAllTasks() {
        return TaskCrudDao.getAll();
    }

    public Task getTaskById(int taskId) {
        return TaskCrudDao.getById(taskId);
    }

    public void updateTask(Task task) {
        try {
            validateTask(task);
        } catch (AppException e) {
            logger.error("タスク更新エラー: " + e.getUserMessage(), e);
            return;
        }
        TaskCrudDao.update(task);
        if (task.getParentId() != null && task.getParentId() != 0) {
            updateParentProgress(task.getParentId(), getAllTasks());
        }
    }

    public boolean updateTaskStatus(int taskId, String newStatus) {
        return TaskCrudDao.updateStatus(taskId, newStatus);
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

        Set<Integer> targetIds = new HashSet<>();
        TaskHierarchySupport.collectDescendantIds(taskId, allTasks, targetIds);
        TaskCrudDao.deleteIds(targetIds);
        logger.info("タスクID: {} を削除しました。対象件数={}", taskId, targetIds.size());
    }

    public void importTasks(List<Task> tasks, boolean replaceExisting) {
        TaskImportDao.importTasks(tasks, replaceExisting, this::validateTask);
    }

    public void updateParentProgress(int parentId) {
        updateParentProgress(parentId, getAllTasks());
    }

    private void updateParentProgress(int parentId, List<Task> allTasks) {
        TaskHierarchySupport.rollupParentProgress(parentId, allTasks)
                .ifPresent(TaskCrudDao::updateProgressAndStatus);
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
        TaskCrudDao.updateOrderIndex(taskId, newOrderIndex);
    }

    public void reorderSiblingTasks(Integer parentId) {
        TaskReorderSupport.reorderSiblings(parentId, getAllTasks(), TaskCrudDao::updateOrderIndex);
    }
}
