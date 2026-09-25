package org.example.ui.wbs;

import org.example.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WbsTreeFilterTest {

    @Test
    void isDescendantOrSelfDetectsAncestors() {
        Task root = task(1, "Root", null, 1);
        Task child = task(2, "Child", 1, 2);
        Task other = task(3, "Other", null, 1);

        Map<Integer, Task> map = Map.of(1, root, 2, child, 3, other);

        assertTrue(WbsTreeFilter.isDescendantOrSelf(child, 1, map));
        assertTrue(WbsTreeFilter.isDescendantOrSelf(root, 1, map));
        assertFalse(WbsTreeFilter.isDescendantOrSelf(other, 1, map));
    }

    @Test
    void filterKeepingAncestorsIncludesParentsOfMatches() {
        Task root = task(1, "Alpha Project", null, 1);
        Task stage = task(2, "Stage", 1, 2);
        Task match = task(3, "Special Task", 2, 3);
        Task other = task(4, "Noise", 1, 2);

        Map<Integer, Task> map = new HashMap<>();
        map.put(1, root);
        map.put(2, stage);
        map.put(3, match);
        map.put(4, other);

        List<Task> filtered = WbsTreeFilter.filterKeepingAncestors(
                List.of(root, stage, match, other), "special", null, map);

        assertEquals(List.of(1, 2, 3), filtered.stream().map(Task::getId).toList());
    }

    private static Task task(int id, String name, Integer parentId, int level) {
        return new Task(id, name, parentId, level, 0, 0, Task.STATUS_NOT_STARTED, LocalDate.now(), LocalDate.now());
    }
}
