package org.tasknavi.persistence;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskHierarchySupportTest {

    @Test
    void collectDescendantIdsIncludesSubtree() {
        Task root = task(1, null, 1);
        Task child = task(2, 1, 2);
        Task grand = task(3, 2, 3);

        Set<Integer> ids = new HashSet<>();
        TaskHierarchySupport.collectDescendantIds(1, List.of(root, child, grand), ids);
        assertEquals(Set.of(1, 2, 3), ids);
    }

    @Test
    void rollupAveragesChildProgress() {
        Task parent = task(1, null, 1);
        Task c1 = task(2, 1, 2);
        c1.setStatus(Task.STATUS_IN_PROGRESS);
        c1.setProgress(50);
        Task c2 = task(3, 1, 2);
        c2.setStatus(Task.STATUS_COMPLETED);
        c2.setProgress(100);

        Optional<Task> rolled = TaskHierarchySupport.rollupParentProgress(1, List.of(parent, c1, c2));
        assertTrue(rolled.isPresent());
        assertEquals(75, rolled.get().getProgress());
        assertEquals(Task.STATUS_IN_PROGRESS, rolled.get().getStatusCode());
    }

    @Test
    void rollupSetsCompletedWhenAllChildrenDone() {
        Task parent = task(1, null, 1);
        Task c1 = task(2, 1, 2);
        c1.setStatus(Task.STATUS_COMPLETED);
        c1.setProgress(100);
        Task c2 = task(3, 1, 2);
        c2.setStatus(Task.STATUS_COMPLETED);
        c2.setProgress(100);

        Optional<Task> rolled = TaskHierarchySupport.rollupParentProgress(1, List.of(parent, c1, c2));
        assertTrue(rolled.isPresent());
        assertEquals(Task.STATUS_COMPLETED, rolled.get().getStatusCode());
        assertEquals(100, rolled.get().getProgress());
    }

    @Test
    void rollupAncestorsUpdatesPhaseAndProject() {
        Task project = task(1, null, 1);
        Task phase = task(2, 1, 2);
        Task leaf = task(3, 2, 3);
        leaf.setStatus(Task.STATUS_COMPLETED);
        leaf.setProgress(100);

        TaskHierarchySupport.rollupAncestors(3, List.of(project, phase, leaf));

        assertEquals(100, phase.getProgress());
        assertEquals(Task.STATUS_COMPLETED, phase.getStatusCode());
        assertEquals(100, project.getProgress());
        assertEquals(Task.STATUS_COMPLETED, project.getStatusCode());
    }

    @Test
    void isValidHierarchyChecksLevels() {
        Task project = task(1, null, 1);
        Task phase = task(2, 1, 2);
        assertTrue(TaskHierarchySupport.isValidHierarchy(project, id -> null));
        assertTrue(TaskHierarchySupport.isValidHierarchy(phase, id -> project));
        assertFalse(TaskHierarchySupport.isValidHierarchy(task(3, 1, 3), id -> project));
    }

    private static Task task(int id, Integer parentId, int level) {
        return new Task(id, "T" + id, parentId, level, 0, 0, Task.STATUS_NOT_STARTED, LocalDate.now(), LocalDate.now());
    }
}
