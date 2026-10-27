package org.tasknavi.service;

import org.tasknavi.model.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 期限で分けた、今日見るべきタスク。
 * 子を持つタスクは子から状態が決まるので含めない。完了済みと終了日なしも含めない。
 */
public record DueTasks(List<Task> overdue, List<Task> dueToday, List<Task> upcoming) {

    /** 「近い」に入れる日数。明日から数えてこの日数先まで。 */
    public static final int UPCOMING_DAYS = 7;

    /** 期限の早い順、同じ日なら優先度の高い順。 */
    public static final Comparator<Task> ORDER = Comparator
            .comparing(Task::getEndDate)
            .thenComparing(task -> task.getPriority().ordinal())
            .thenComparing(task -> task.getName() != null ? task.getName() : "");

    public static DueTasks of(List<Task> tasks, LocalDate today) {
        List<Task> overdue = new ArrayList<>();
        List<Task> dueToday = new ArrayList<>();
        List<Task> upcoming = new ArrayList<>();
        if (tasks == null) {
            return new DueTasks(overdue, dueToday, upcoming);
        }

        Set<Integer> parentIds = new HashSet<>();
        for (Task task : tasks) {
            if (task != null && task.getParentId() != null) {
                parentIds.add(task.getParentId());
            }
        }

        LocalDate lastUpcoming = today.plusDays(UPCOMING_DAYS);
        for (Task task : tasks) {
            if (task == null || task.isCompleted() || task.getEndDate() == null
                    || parentIds.contains(task.getId())) {
                continue;
            }
            LocalDate end = task.getEndDate();
            if (end.isBefore(today)) {
                overdue.add(task);
            } else if (end.isEqual(today)) {
                dueToday.add(task);
            } else if (!end.isAfter(lastUpcoming)) {
                upcoming.add(task);
            }
        }
        overdue.sort(ORDER);
        dueToday.sort(ORDER);
        upcoming.sort(ORDER);
        return new DueTasks(List.copyOf(overdue), List.copyOf(dueToday), List.copyOf(upcoming));
    }

    /**
     * プロジェクトごとに分けたものを 1 つにまとめる。ID はプロジェクトごとの番号なので、
     * 全部のタスクをつないでから {@link #of} に渡すと親子を取り違える。先に分けてからまとめる。
     */
    public static DueTasks merge(List<DueTasks> parts) {
        List<Task> overdue = new ArrayList<>();
        List<Task> dueToday = new ArrayList<>();
        List<Task> upcoming = new ArrayList<>();
        for (DueTasks part : parts) {
            overdue.addAll(part.overdue());
            dueToday.addAll(part.dueToday());
            upcoming.addAll(part.upcoming());
        }
        overdue.sort(ORDER);
        dueToday.sort(ORDER);
        upcoming.sort(ORDER);
        return new DueTasks(List.copyOf(overdue), List.copyOf(dueToday), List.copyOf(upcoming));
    }

    public long countDueOn(LocalDate date) {
        return upcoming.stream().filter(task -> date.isEqual(task.getEndDate())).count();
    }

    public boolean isEmpty() {
        return overdue.isEmpty() && dueToday.isEmpty() && upcoming.isEmpty();
    }
}
