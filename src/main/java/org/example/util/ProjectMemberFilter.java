package org.example.util;

import org.example.Task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 指定プロジェクト配下（子孫含む）のタスク ID を収集する。
 */
public final class ProjectMemberFilter {
    private ProjectMemberFilter() {
    }

    public static Set<Integer> collectMemberIds(List<Task> tasks, int projectId) {
        Map<Integer, Task> taskMap = new HashMap<>();
        if (tasks != null) {
            for (Task task : tasks) {
                if (task != null) {
                    taskMap.put(task.getId(), task);
                }
            }
        }

        Set<Integer> memberIds = new HashSet<>();
        if (tasks == null) {
            return memberIds;
        }

        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            Task current = task;
            while (current != null) {
                if (current.getId() == projectId) {
                    memberIds.add(task.getId());
                    break;
                }
                Integer parentId = current.getParentId();
                if (parentId == null) {
                    break;
                }
                current = taskMap.get(parentId);
            }
        }
        return memberIds;
    }
}
