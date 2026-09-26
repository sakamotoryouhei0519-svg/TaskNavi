package org.tasknavi.ui.kanban;

import org.tasknavi.Task;
import org.tasknavi.util.TaskViewFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * カンバン表示用の絞り込み（共通 {@link TaskViewFilter} への委譲）。
 */
public final class KanbanTaskFilter {

    private KanbanTaskFilter() {
    }

    public static List<Task> apply(
            List<Task> source,
            String searchKeyword,
            String statusFilter,
            Integer projectFilterId,
            Map<Integer, Set<Integer>> projectMemberCache) {
        return TaskViewFilter.apply(source, searchKeyword, statusFilter, projectFilterId, projectMemberCache);
    }
}
