package org.tasknavi.ui.kanban;

import org.tasknavi.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KanbanTaskFilterTest {

    @Test
    void applyFiltersByKeywordAndStatus() {
        Task a = task(1, "Alpha");
        a.setStatus(Task.STATUS_IN_PROGRESS);
        Task b = task(2, "Beta");
        b.setStatus(Task.STATUS_NOT_STARTED);

        List<Task> filtered = KanbanTaskFilter.apply(
                List.of(a, b), "Alp", Task.STATUS_IN_PROGRESS, null, new HashMap<>());

        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
    }

    @Test
    void keywordMatchIsCaseInsensitive() {
        Task a = task(1, "Alpha");
        List<Task> filtered = KanbanTaskFilter.apply(
                List.of(a), "alp", null, null, new HashMap<>());
        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
    }

    @Test
    void columnForMapsStatus() {
        Task todo = task(1, "T");
        todo.setStatus(Task.STATUS_NOT_STARTED);
        Task doing = task(2, "D");
        doing.setStatus(Task.STATUS_IN_PROGRESS);
        Task done = task(3, "C");
        done.setStatus(Task.STATUS_COMPLETED);

        assertEquals(KanbanColumnSupport.Column.TODO, KanbanColumnSupport.columnFor(todo));
        assertEquals(KanbanColumnSupport.Column.IN_PROGRESS, KanbanColumnSupport.columnFor(doing));
        assertEquals(KanbanColumnSupport.Column.DONE, KanbanColumnSupport.columnFor(done));
    }

    @Test
    void formatHeaderIncludesCount() {
        assertEquals("【 Todo 】  (3)", KanbanColumnSupport.formatHeader("Todo", 3));
    }

    private static Task task(int id, String name) {
        return new Task(id, name, null, 1, 0, 0, Task.STATUS_NOT_STARTED, LocalDate.now(), LocalDate.now());
    }
}
