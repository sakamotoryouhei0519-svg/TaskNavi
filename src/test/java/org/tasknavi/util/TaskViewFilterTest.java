package org.tasknavi.util;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;

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
    void keywordMatchesAssignee() {
        Task named = task(1, "Alpha", null, Task.STATUS_NOT_STARTED);
        named.setAssignee("Sato");
        Task other = task(2, "Beta", null, Task.STATUS_NOT_STARTED);
        other.setAssignee("Suzuki");

        List<Task> filtered = TaskViewFilter.apply(
                List.of(named, other), "sat", null, null, new HashMap<>());

        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
    }

    @Test
    void keywordMatchesMemo() {
        Task withMemo = task(1, "Alpha", null, Task.STATUS_NOT_STARTED);
        withMemo.setMemo("先方に見積もりを確認\n来週月曜まで");
        Task other = task(2, "Beta", null, Task.STATUS_NOT_STARTED);

        List<Task> filtered = TaskViewFilter.apply(
                List.of(withMemo, other), "見積", null, null, new HashMap<>());

        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
    }

    @Test
    void keywordMatchesComments() {
        Task withComment = task(1, "Alpha", null, Task.STATUS_NOT_STARTED);
        withComment.setComments(List.of("着手しました", "サーバーの申請は済んでいます"));
        Task other = task(2, "Beta", null, Task.STATUS_NOT_STARTED);

        List<Task> filtered = TaskViewFilter.apply(
                List.of(withComment, other), "申請", null, null, new HashMap<>());

        assertEquals(List.of(1), filtered.stream().map(Task::getId).toList());
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
