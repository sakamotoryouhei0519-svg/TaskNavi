package org.tasknavi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * タスクの親子階層を横断する共通ユーティリティ。
 */
public final class TaskHierarchyUtil {
    private TaskHierarchyUtil() {
    }

    public static List<Task> sortDepthFirst(List<Task> allTasks) {
        Map<Integer, List<Task>> childrenMap = buildChildrenMap(allTasks);
        List<Task> rootTasks = new ArrayList<>();

        for (Task task : allTasks) {
            if (task.getParentId() == null || task.getParentId() == 0) {
                rootTasks.add(task);
            }
        }

        List<Task> result = new ArrayList<>();
        for (Task root : rootTasks) {
            collectTasksRecursive(root, childrenMap, result);
        }
        return result;
    }

    public static Map<Integer, List<Task>> buildChildrenMap(List<Task> tasks) {
        Map<Integer, List<Task>> childrenMap = new HashMap<>();
        for (Task task : tasks) {
            if (task.getParentId() != null && task.getParentId() != 0) {
                childrenMap.computeIfAbsent(task.getParentId(), key -> new ArrayList<>()).add(task);
            }
        }
        return childrenMap;
    }

    public static Task findRootProject(Task task, List<Task> allTasks) {
        if (task == null) {
            return null;
        }

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : allTasks) {
            taskMap.put(t.getId(), t);
        }

        Task current = task;
        while (current.getParentId() != null && current.getParentId() != 0) {
            Task parent = taskMap.get(current.getParentId());
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return current;
    }

    private static void collectTasksRecursive(Task current, Map<Integer, List<Task>> childrenMap, List<Task> result) {
        result.add(current);
        List<Task> children = childrenMap.get(current.getId());
        if (children != null) {
            for (Task child : children) {
                collectTasksRecursive(child, childrenMap, result);
            }
        }
    }
}
