package org.tasknavi.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class TaskCopyTest {

    @Test
    void copyKeepsEveryFieldAndIsIndependent() {
        Task original = new Task(7, "Design", 3, 2, 4, 60, Task.STATUS_IN_PROGRESS,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 20));
        original.setAssignee("Sato");
        original.setPriority(Priority.HIGH);

        Task copy = original.copy();
        original.setName("Renamed");

        assertNotSame(original, copy);
        assertEquals(7, copy.getId());
        assertEquals("Design", copy.getName());
        assertEquals(3, copy.getParentId());
        assertEquals(2, copy.getLevel());
        assertEquals(4, copy.getOrderIndex());
        assertEquals(60, copy.getProgress());
        assertEquals(Task.STATUS_IN_PROGRESS, copy.getStatusCode());
        assertEquals(LocalDate.of(2026, 1, 5), copy.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 20), copy.getEndDate());
        assertEquals("Sato", copy.getAssignee());
        assertEquals(Priority.HIGH, copy.getPriority());
    }
}
