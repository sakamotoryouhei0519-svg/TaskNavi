package org.example.ui.gantt;

import org.example.Task;
import org.example.TaskHierarchyUtil;
import org.example.util.TaskViewFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ガント表示用の絞り込み（階層順ソート後に共通フィルタを適用）。
 */
public final class GanttTaskFilter {

    private GanttTaskFilter() {
    }

    public static List<Task> visibleTasks(
            List<Task> cachedTasks,
            String searchKeyword,
            String statusFilter,
            Integer projectFilterId,
            Map<Integer, Set<Integer>> projectMemberCache) {
        List<Task> sorted = TaskHierarchyUtil.sortDepthFirst(
                cachedTasks != null ? cachedTasks : List.of());
        return TaskViewFilter.apply(sorted, searchKeyword, statusFilter, projectFilterId, projectMemberCache);
    }
}
