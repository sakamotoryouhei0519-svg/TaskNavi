package org.tasknavi.ui.gantt;

import org.tasknavi.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GanttTaskFilterTest {

    @Test
    void filtersByKeywordAndStatus() {
        Task a = new Task(1, "Alpha", null, 1, 0, 0, Task.STATUS_IN_PROGRESS,
                LocalDate.now(), LocalDate.now());
        Task b = new Task(2, "Beta", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now());

        List<Task> filtered = GanttTaskFilter.visibleTasks(
                List.of(a, b), "Alp", Task.STATUS_IN_PROGRESS, null, new HashMap<>());

        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
    }
}
