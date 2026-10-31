package org.tasknavi.ui.gantt;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class GanttRowsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 31);

    @Test
    void keepsTheSameRowsWhileNothingChanges() {
        List<Task> source = List.of(task(1, "企画", null, 1), task(2, "設計", 1, 2));
        GanttRows rows = new GanttRows();

        rows.update(source, null, null, null);
        List<Task> first = rows.rows();
        rows.update(source, null, null, null);

        assertSame(first, rows.rows());
        assertEquals(List.of(2), rows.children().get(1).stream().map(Task::getId).toList());
    }

    @Test
    void rebuildsWhenTheKeywordChanges() {
        List<Task> source = List.of(task(1, "企画", null, 1), task(2, "設計", 1, 2));
        GanttRows rows = new GanttRows();
        rows.update(source, null, null, null);
        List<Task> all = rows.rows();

        rows.update(source, "設計", null, null);

        assertNotSame(all, rows.rows());
        assertEquals(List.of(2), rows.rows().stream().map(Task::getId).toList());
    }

    @Test
    void projectFilterIncludesTasksAddedAfterReload() {
        List<Task> before = new ArrayList<>(List.of(task(1, "企画", null, 1), task(2, "設計", 1, 2)));
        GanttRows rows = new GanttRows();
        rows.update(before, null, null, 1);

        List<Task> after = new ArrayList<>(before);
        after.add(task(3, "実装", 1, 2));
        rows.update(after, null, null, 1);

        assertEquals(List.of(1, 2, 3), rows.rows().stream().map(Task::getId).toList());
    }

    private static Task task(int id, String name, Integer parentId, int level) {
        return new Task(id, name, parentId, level, id, 0, Task.STATUS_NOT_STARTED, DAY, DAY);
    }
}
