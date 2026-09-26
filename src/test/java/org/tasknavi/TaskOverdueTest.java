package org.tasknavi;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskOverdueTest {

    @Test
    void incompletePastEndDateIsOverdue() {
        Task task = new Task(1, "Late", null, 1, 0, 10, Task.STATUS_IN_PROGRESS,
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));
        assertTrue(task.isOverdue());
    }

    @Test
    void completedTaskIsNeverOverdue() {
        Task task = new Task(1, "Done", null, 1, 0, 100, Task.STATUS_COMPLETED,
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));
        assertFalse(task.isOverdue());
    }

    @Test
    void futureEndDateIsNotOverdue() {
        Task task = new Task(1, "Soon", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(3));
        assertFalse(task.isOverdue());
    }
}
