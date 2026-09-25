package org.example.util;

import org.example.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskViewFilterTest {

    @Test
    void keywordIsCaseInsensitive() {
        Task a = task(1, "Alpha", null, Task.STATUS_NOT_STARTED);
        Task b = task(2, "Beta", null, Task.STATUS_NOT_STARTED);

        List<Task> filtered = TaskViewFilter.apply(
                List.of(a, b), "alp", null, null, new HashMap<>());

        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
    }

    @Test
    void statusAndProjectFiltersCombine() {
        Task project = task(10, "Root", null, Task.STATUS_NOT_STARTED);
        Task childOk = task(11, "ChildA", 10, Task.STATUS_IN_PROGRESS);
        Task childOther = task(12, "ChildB", 10, Task.STATUS_NOT_STARTED);
        Task outsider = task(20, "Other", null, Task.STATUS_IN_PROGRESS);

        Map<Integer, Set<Integer>> cache = new HashMap<>();
        List<Task> filtered = TaskViewFilter.apply(
                List.of(project, childOk, childOther, outsider),
                null,
                Task.STATUS_IN_PROGRESS,
                10,
                cache);

        assertEquals(List.of(11), filtered.stream().map(Task::getId).toList());
        assertEquals(1, cache.size());
    }

    @Test
    void nullCacheIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> TaskViewFilter.apply(List.of(), null, null, null, null));
    }

    private static Task task(int id, String name, Integer parentId, String status) {
        return new Task(id, name, parentId, parentId == null ? 1 : 2, 0, 0, status,
                LocalDate.now(), LocalDate.now());
    }
}
