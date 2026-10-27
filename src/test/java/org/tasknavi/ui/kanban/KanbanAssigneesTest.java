package org.tasknavi.ui.kanban;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KanbanAssigneesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 27);

    @Test
    void optionsPutKnownMembersFirstAndMergeSpellingDifferences() {
        List<Task> tasks = List.of(task(1, "suzuki "), task(2, "佐藤"), task(3, ""), task(4, "Suzuki"));

        assertEquals(List.of("田中", "Suzuki", "佐藤"), KanbanAssignees.options(List.of("田中", "Suzuki"), tasks));
        assertEquals(List.of("Suzuki", "佐藤"), KanbanAssignees.options(List.of(), tasks));
    }

    @Test
    void filterMatchesTheSameNameOrOnlyUnassigned() {
        List<Task> tasks = List.of(task(1, "田中"), task(2, " 田中 "), task(3, null), task(4, "鈴木"));

        assertEquals(List.of(1, 2), ids(KanbanAssignees.filter(tasks, "田中")));
        assertEquals(List.of(3), ids(KanbanAssignees.filter(tasks, KanbanAssignees.UNASSIGNED)));
        assertEquals(List.of(1, 2, 3, 4), ids(KanbanAssignees.filter(tasks, null)));
    }

    @Test
    void groupsByNameKeepingOrderInsideAndPutsUnassignedLast() {
        List<Task> tasks = List.of(task(1, "鈴木"), task(2, ""), task(3, "田中"), task(4, "鈴木 "));

        Map<String, List<Task>> groups = KanbanAssignees.group(tasks);

        assertEquals(List.of("田中", "鈴木", KanbanAssignees.UNASSIGNED), new ArrayList<>(groups.keySet()));
        assertEquals(List.of(1, 4), ids(groups.get("鈴木")));
        assertEquals(List.of(2), ids(groups.get(KanbanAssignees.UNASSIGNED)));
    }

    private static Task task(int id, String assignee) {
        Task task = new Task(id, "t" + id, null, 1, id, 0, TaskStatus.NOT_STARTED, DAY, DAY);
        task.setAssignee(assignee);
        return task;
    }

    private static List<Integer> ids(List<Task> tasks) {
        return tasks.stream().map(Task::getId).toList();
    }
}
