package org.tasknavi.ui.calendar;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;
import org.tasknavi.util.TaskStatusCycle;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarTaskFilterTest {

    @Test
    void tasksForDateIncludesRangeAndSingleDay() {
        Task ranged = task(1, "A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5));
        Task startOnly = task(2, "B", LocalDate.of(2026, 9, 3), null);
        Task outside = task(3, "C", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));

        List<Task> matched = CalendarTaskFilter.tasksForDate(
                List.of(ranged, startOnly, outside), LocalDate.of(2026, 9, 3));

        assertEquals(List.of(1, 2), matched.stream().map(Task::getId).toList());
    }

    @Test
    void tasksForDateLeavesOutProjectsAndPhasesThatHaveChildren() {
        LocalDate day = LocalDate.of(2026, 9, 3);
        Task project = task(1, "P", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 12, 25));
        Task phase = new Task(2, "Phase", 1, 2, 0, 0, Task.STATUS_NOT_STARTED, day.minusDays(30), day.plusDays(30));
        Task work = new Task(3, "Work", 2, 3, 0, 0, Task.STATUS_NOT_STARTED, day, day);
        Task emptyProject = task(4, "New", day, day.plusDays(10));

        List<Task> matched = CalendarTaskFilter.tasksForDate(List.of(project, phase, work, emptyProject), day);

        assertEquals(List.of(3, 4), matched.stream().map(Task::getId).toList());
    }

    @Test
    void applyFiltersByKeywordAndStatus() {
        Task a = task(1, "Alpha", LocalDate.now(), LocalDate.now());
        a.setStatus(Task.STATUS_IN_PROGRESS);
        Task b = task(2, "Beta", LocalDate.now(), LocalDate.now());
        b.setStatus(Task.STATUS_NOT_STARTED);

        List<Task> filtered = CalendarTaskFilter.applyFilters(
                List.of(a, b), "alp", Task.STATUS_IN_PROGRESS, null, new HashMap<>());

        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
    }

    @Test
    void statusCycleMovesForwardAndBackward() {
        assertEquals(Task.STATUS_IN_PROGRESS, TaskStatusCycle.next(Task.STATUS_NOT_STARTED));
        assertEquals(Task.STATUS_COMPLETED, TaskStatusCycle.next(Task.STATUS_IN_PROGRESS));
        assertEquals(Task.STATUS_IN_PROGRESS, TaskStatusCycle.previous(Task.STATUS_COMPLETED));
        assertEquals(Task.STATUS_COMPLETED, TaskStatusCycle.fromProgress(100));
    }

    private static Task task(int id, String name, LocalDate start, LocalDate end) {
        return new Task(id, name, null, 1, 0, 0, Task.STATUS_NOT_STARTED, start, end);
    }
}
