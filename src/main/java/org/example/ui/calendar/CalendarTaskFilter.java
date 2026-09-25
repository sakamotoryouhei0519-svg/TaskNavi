package org.example.ui.calendar;

import org.example.Task;
import org.example.util.TaskViewFilter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * カレンダー表示用のタスク絞り込みと日付マッチ。
 */
public final class CalendarTaskFilter {

    private CalendarTaskFilter() {
    }

    public static List<Task> applyFilters(
            List<Task> source,
            String searchKeyword,
            String statusFilter,
            Integer projectFilterId,
            Map<Integer, Set<Integer>> projectMemberCache) {
        return TaskViewFilter.apply(source, searchKeyword, statusFilter, projectFilterId, projectMemberCache);
    }

    /**
     * 指定日がタスクの開始〜終了に含まれるもの（開始または終了のみでも可）。
     */
    public static List<Task> tasksForDate(List<Task> tasks, LocalDate date) {
        if (tasks == null || date == null) {
            return Collections.emptyList();
        }
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            LocalDate start = task.getStartDate();
            LocalDate end = task.getEndDate();
            if (start != null && end != null) {
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    result.add(task);
                }
            } else if (start != null && date.equals(start)) {
                result.add(task);
            } else if (end != null && date.equals(end)) {
                result.add(task);
            }
        }
        return result;
    }
}
