package org.tasknavi.persistence;

import org.tasknavi.Task;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * タスク階層の検証・子孫収集・親進捗ロールアップ。
 */
public final class TaskHierarchySupport {

    private TaskHierarchySupport() {
    }

    public static void collectDescendantIds(int rootTaskId, List<Task> allTasks, Set<Integer> targetIds) {
        if (allTasks == null || allTasks.isEmpty()) {
            return;
        }
        targetIds.add(rootTaskId);
        for (Task task : allTasks) {
            if (task.getParentId() != null && task.getParentId() == rootTaskId) {
                collectDescendantIds(task.getId(), allTasks, targetIds);
            }
        }
    }

    /**
     * 子タスクから親の進捗・ステータスを再計算する。更新不要なら empty。
     */
    public static Optional<Task> rollupParentProgress(int parentId, List<Task> allTasks) {
        if (allTasks == null || allTasks.isEmpty()) {
            return Optional.empty();
        }

        Task parent = null;
        int totalProgress = 0;
        int childCount = 0;

        for (Task task : allTasks) {
            if (task.getId() == parentId) {
                parent = task;
            }
            if (task.getParentId() != null && task.getParentId() == parentId) {
                totalProgress += task.getProgress();
                childCount++;
            }
        }

        if (parent == null || childCount == 0) {
            return Optional.empty();
        }

        int avgProgress = (int) Math.round((double) totalProgress / childCount);
        parent.setProgress(avgProgress);

        boolean allChildrenCompleted = true;
        for (Task task : allTasks) {
            if (task.getParentId() != null && task.getParentId() == parentId) {
                if (!task.isCompleted()) {
                    allChildrenCompleted = false;
                    break;
                }
            }
        }

        if (allChildrenCompleted) {
            parent.setStatus(Task.STATUS_COMPLETED);
            parent.setProgress(100);
        } else if (avgProgress > 0) {
            parent.setStatus(Task.STATUS_IN_PROGRESS);
        } else {
            parent.setStatus(Task.STATUS_NOT_STARTED);
        }
        return Optional.of(parent);
    }

    public static boolean isValidHierarchy(Task task, Function<Integer, Task> parentLookup) {
        if (task == null) {
            return false;
        }
        if (task.getLevel() < 1 || task.getLevel() > 3) {
            return false;
        }
        if (task.getParentId() == null || task.getParentId() == 0) {
            return task.getLevel() == 1;
        }

        Task parent = parentLookup.apply(task.getParentId());
        if (parent == null) {
            return false;
        }
        if (task.getLevel() == 2) {
            return parent.getLevel() == 1;
        }
        if (task.getLevel() == 3) {
            return parent.getLevel() == 2;
        }
        return false;
    }
}
