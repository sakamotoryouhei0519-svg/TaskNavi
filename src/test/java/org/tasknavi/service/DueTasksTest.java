package org.tasknavi.service;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Priority;
import org.tasknavi.model.Task;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DueTasksTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @Test
    void splitsByDeadline() {
        DueTasks due = DueTasks.of(List.of(
                task(1, "late", null, TODAY.minusDays(2), Task.STATUS_IN_PROGRESS),
                task(2, "today", null, TODAY, Task.STATUS_NOT_STARTED),
                task(3, "tomorrow", null, TODAY.plusDays(1), Task.STATUS_NOT_STARTED),
                task(4, "week", null, TODAY.plusDays(DueTasks.UPCOMING_DAYS), Task.STATUS_NOT_STARTED),
                task(5, "far", null, TODAY.plusDays(DueTasks.UPCOMING_DAYS + 1), Task.STATUS_NOT_STARTED)
        ), TODAY);

        assertEquals(List.of(1), ids(due.overdue()));
        assertEquals(List.of(2), ids(due.dueToday()));
        assertEquals(List.of(3, 4), ids(due.upcoming()));
        assertEquals(1, due.countDueOn(TODAY.plusDays(1)));
    }

    @Test
    void skipsCompletedUndatedAndParents() {
        DueTasks due = DueTasks.of(List.of(
                task(1, "project", null, TODAY, Task.STATUS_IN_PROGRESS),
                task(2, "child", 1, TODAY, Task.STATUS_IN_PROGRESS),
                task(3, "done", null, TODAY, Task.STATUS_COMPLETED),
                task(4, "undated", null, null, Task.STATUS_NOT_STARTED)
        ), TODAY);

        assertEquals(List.of(2), ids(due.dueToday()));
        assertTrue(due.overdue().isEmpty());
        assertTrue(due.upcoming().isEmpty());
    }

    @Test
    void sameDayIsOrderedByPriority() {
        Task low = task(1, "low", null, TODAY, Task.STATUS_NOT_STARTED);
        low.setPriority(Priority.LOW);
        Task high = task(2, "high", null, TODAY, Task.STATUS_NOT_STARTED);
        high.setPriority(Priority.HIGH);

        DueTasks due = DueTasks.of(List.of(low, high), TODAY);

        assertEquals(List.of(2, 1), ids(due.dueToday()));
    }

    @Test
    void mergeKeepsEachProjectsParentsApart() {
        DueTasks personal = DueTasks.of(List.of(
                task(1, "自分の作業", null, TODAY, Task.STATUS_NOT_STARTED)), TODAY);
        DueTasks shared = DueTasks.of(List.of(
                task(1, "共有の親", null, TODAY.minusDays(1), Task.STATUS_IN_PROGRESS),
                task(2, "共有の子", 1, TODAY.minusDays(1), Task.STATUS_IN_PROGRESS),
                task(5, "共有の明日", null, TODAY.plusDays(1), Task.STATUS_NOT_STARTED)), TODAY);

        DueTasks merged = DueTasks.merge(List.of(personal, shared));

        assertEquals(List.of("自分の作業"), names(merged.dueToday()), "別のプロジェクトの同じ番号を親と取り違えない");
        assertEquals(List.of("共有の子"), names(merged.overdue()));
        assertEquals(1, merged.countDueOn(TODAY.plusDays(1)));
    }

    @Test
    void onlyMineStillTreatsParentsOfOthersTasksAsParents() {
        Task parent = task(1, "親", null, TODAY, Task.STATUS_IN_PROGRESS);
        Task othersChild = task(2, "鈴木さんの子", 1, TODAY, Task.STATUS_IN_PROGRESS);
        othersChild.setAssignee("鈴木");
        Task mine = task(3, "自分の", null, TODAY, Task.STATUS_NOT_STARTED);
        mine.setAssignee("田中");
        TaskSource source = new TaskSource("共有", true, null, "田中");

        DueTasks due = DueTasks.of(List.of(parent, othersChild, mine), TODAY, source::isMine);

        assertEquals(List.of("自分の"), names(due.dueToday()), "子を持つ親は、子が自分の担当でなくても出さない");
    }

    @Test
    void emptyWhenNothingIsDue() {
        assertTrue(DueTasks.of(null, TODAY).isEmpty());
        assertTrue(DueTasks.of(List.of(), TODAY).isEmpty());
    }

    private static Task task(int id, String name, Integer parentId, LocalDate end, String status) {
        return new Task(id, name, parentId, parentId == null ? 1 : 2, 0, 0, status, TODAY.minusDays(10), end);
    }

    private static List<String> names(List<Task> tasks) {
        return tasks.stream().map(Task::getName).toList();
    }

    private static List<Integer> ids(List<Task> tasks) {
        return tasks.stream().map(Task::getId).toList();
    }
}
