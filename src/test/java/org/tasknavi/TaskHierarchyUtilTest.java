package org.tasknavi;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskHierarchyUtilTest {

    @Test
    void sortDepthFirstKeepsParentBeforeChildren() {
        Task root = new Task(1, "Root", null, 1, 0, 0, "未着手", LocalDate.now(), LocalDate.now());
        Task child = new Task(2, "Child", 1, 2, 0, 0, "未着手", LocalDate.now(), LocalDate.now());
        Task grand = new Task(3, "Grand", 2, 3, 0, 0, "未着手", LocalDate.now(), LocalDate.now());

        List<Task> sorted = TaskHierarchyUtil.sortDepthFirst(List.of(grand, root, child));
        assertEquals(List.of(1, 2, 3), sorted.stream().map(Task::getId).toList());
        assertEquals(root, TaskHierarchyUtil.findRootProject(grand, sorted));
    }
}
