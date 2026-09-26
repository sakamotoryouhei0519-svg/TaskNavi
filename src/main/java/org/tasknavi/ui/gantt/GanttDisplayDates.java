package org.tasknavi.ui.gantt;

import org.tasknavi.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ガント表示用の開始・終了日と表示期間の算出。
 */
public final class GanttDisplayDates {
    private GanttDisplayDates() {
    }

    public static LocalDate resolveDisplayStartDate(Task task, Map<Integer, List<Task>> childrenMap) {
        if (task == null) {
            return null;
        }

        List<Task> children = childrenMap.get(task.getId());
        if (children != null && !children.isEmpty()) {
            LocalDate minChildStart = children.stream()
                    .map(child -> resolveDisplayStartDate(child, childrenMap))
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(task.getStartDate());

            if (task.getStartDate() == null) {
                return minChildStart;
            }
            if (minChildStart == null) {
                return task.getStartDate();
            }
            return minChildStart.isBefore(task.getStartDate()) ? minChildStart : task.getStartDate();
        }
        return task.getStartDate();
    }

    public static LocalDate resolveDisplayEndDate(Task task, Map<Integer, List<Task>> childrenMap) {
        if (task == null) {
            return null;
        }

        List<Task> children = childrenMap.get(task.getId());
        if (children != null && !children.isEmpty()) {
            LocalDate maxChildEnd = children.stream()
                    .map(child -> resolveDisplayEndDate(child, childrenMap))
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(task.getEndDate());

            if (task.getEndDate() == null) {
                return maxChildEnd;
            }
            if (maxChildEnd == null) {
                return task.getEndDate();
            }
            return maxChildEnd.isAfter(task.getEndDate()) ? maxChildEnd : task.getEndDate();
        }
        return task.getEndDate();
    }

    public static LocalDate[] calculateVisibleDateWindow(
            List<Task> tasks,
            Map<Integer, List<Task>> childrenMap,
            boolean manualVisibleWindow,
            LocalDate visibleStart,
            LocalDate visibleEnd
    ) {
        LocalDate[] manual = GanttDateWindow.manualWindowOrNull(manualVisibleWindow, visibleStart, visibleEnd);
        if (manual != null) {
            return manual;
        }

        List<Task> activeTasks = tasks.stream()
                .filter(task -> !Task.STATUS_COMPLETED.equals(task.getStatus()))
                .toList();

        if (activeTasks.isEmpty()) {
            activeTasks = tasks;
        }

        LocalDate minDate = activeTasks.stream()
                .map(task -> resolveDisplayStartDate(task, childrenMap))
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate maxDate = activeTasks.stream()
                .map(task -> resolveDisplayEndDate(task, childrenMap))
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(minDate.plusDays(30));

        LocalDate windowStart = trimEmptyMonthStart(minDate, maxDate, activeTasks, childrenMap);
        LocalDate windowEnd = trimEmptyMonthEnd(maxDate, windowStart, activeTasks, childrenMap);

        if (windowStart.isAfter(windowEnd)) {
            return new LocalDate[]{minDate, maxDate};
        }

        return new LocalDate[]{windowStart, windowEnd};
    }

    private static LocalDate trimEmptyMonthStart(
            LocalDate start,
            LocalDate end,
            List<Task> tasks,
            Map<Integer, List<Task>> childrenMap
    ) {
        LocalDate cursor = start.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            if (monthHasTask(cursor, tasks, childrenMap)) {
                return cursor.isBefore(start) ? cursor : start;
            }
            cursor = cursor.plusMonths(1).withDayOfMonth(1);
        }
        return start;
    }

    private static LocalDate trimEmptyMonthEnd(
            LocalDate end,
            LocalDate start,
            List<Task> tasks,
            Map<Integer, List<Task>> childrenMap
    ) {
        LocalDate cursor = end.withDayOfMonth(1);
        while (!cursor.isBefore(start.withDayOfMonth(1))) {
            if (monthHasTask(cursor, tasks, childrenMap)) {
                return cursor.isBefore(end.withDayOfMonth(1)) ? cursor.plusMonths(1).minusDays(1) : end;
            }
            cursor = cursor.minusMonths(1).withDayOfMonth(1);
        }
        return end;
    }

    private static boolean monthHasTask(
            LocalDate monthStart,
            List<Task> tasks,
            Map<Integer, List<Task>> childrenMap
    ) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        for (Task task : tasks) {
            LocalDate taskStart = resolveDisplayStartDate(task, childrenMap);
            LocalDate taskEnd = resolveDisplayEndDate(task, childrenMap);
            if (taskStart == null || taskEnd == null) {
                continue;
            }
            if (!(taskEnd.isBefore(monthStart) || taskStart.isAfter(monthEnd))) {
                return true;
            }
        }
        return false;
    }
}
