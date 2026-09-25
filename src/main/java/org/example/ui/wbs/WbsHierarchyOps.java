package org.example.ui.wbs;

import org.example.Task;
import org.example.TaskService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WBS 階層移動時のサイクル判定と子孫レベルの再計算。
 */
public final class WbsHierarchyOps {

    private WbsHierarchyOps() {
    }

    public static boolean wouldCreateCycle(TaskService taskService, int draggedTaskId, int targetTaskId) {
        Task current = taskService.getTaskById(targetTaskId);
        while (current != null) {
            if (current.getId() == draggedTaskId) {
                return true;
            }
            Integer parentId = current.getParentId();
            if (parentId == null) {
                break;
            }
            current = taskService.getTaskById(parentId);
        }
        return false;
    }

    public static void recalculateDescendantLevels(TaskService taskService, int rootTaskId, int baseLevel) {
        List<Task> allTasks = taskService.getAllTasks();
        Map<Integer, List<Task>> childrenMap = new HashMap<>();
        for (Task task : allTasks) {
            if (task.getParentId() != null) {
                childrenMap.computeIfAbsent(task.getParentId(), k -> new java.util.ArrayList<>()).add(task);
            }
        }
        updateDescendantLevelsRecursively(taskService, rootTaskId, baseLevel, childrenMap);
    }

    private static void updateDescendantLevelsRecursively(
            TaskService taskService,
            int parentId,
            int parentLevel,
            Map<Integer, List<Task>> childrenMap) {
        List<Task> children = childrenMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (Task child : children) {
            int nextLevel = parentLevel + 1;
            child.setLevel(nextLevel);
            taskService.updateTask(child);
            updateDescendantLevelsRecursively(taskService, child.getId(), nextLevel, childrenMap);
        }
    }
}
