package org.tasknavi.ui.wbs;

import org.tasknavi.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WBS ツリー表示用のプロジェクト絞り込み・検索フィルタ（祖先ノードも残す）。
 */
public final class WbsTreeFilter {

    private WbsTreeFilter() {
    }

    public static boolean isDescendantOrSelf(Task task, int projectId, Map<Integer, Task> taskMap) {
        Task current = task;
        while (current != null) {
            if (current.getId() == projectId) {
                return true;
            }
            if (current.getParentId() == null) {
                break;
            }
            current = taskMap.get(current.getParentId());
        }
        return false;
    }

    /**
     * キーワード／ステータスに一致するタスクと、その祖先を残して返す。
     */
    public static List<Task> filterKeepingAncestors(
            List<Task> tasks, String keyword, String status, Map<Integer, Task> taskMap) {
        Set<Integer> matchingTaskIds = new HashSet<>();

        for (Task task : tasks) {
            boolean matchesKeyword = keyword == null || keyword.isEmpty()
                    || (task.getName() != null && task.getName().toLowerCase().contains(keyword.toLowerCase()));
            boolean matchesStatus = status == null || status.isEmpty()
                    || (task.getStatusCode() != null && task.getStatusCode().equals(status));

            if (matchesKeyword && matchesStatus) {
                matchingTaskIds.add(task.getId());
            }
        }

        Set<Integer> includedTaskIds = new HashSet<>(matchingTaskIds);

        for (Integer taskId : matchingTaskIds) {
            Task current = taskMap.get(taskId);
            while (current != null && current.getParentId() != null) {
                Integer parentId = current.getParentId();
                if (includedTaskIds.contains(parentId)) {
                    break;
                }
                includedTaskIds.add(parentId);
                current = taskMap.get(parentId);
            }
        }

        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (includedTaskIds.contains(task.getId())) {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }
}
