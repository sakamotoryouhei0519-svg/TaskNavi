package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskStatusCodifyTest {

    @Test
    void normalizeStatusShouldPersistEnumNames() {
        assertEquals("NOT_STARTED", Task.normalizeStatus("未着手"));
        assertEquals("IN_PROGRESS", Task.normalizeStatus("進行中"));
        assertEquals("COMPLETED", Task.normalizeStatus("完了"));
        assertEquals("NOT_STARTED", Task.normalizeStatus("not_started"));
        assertEquals("IN_PROGRESS", Task.normalizeStatus("IN_PROGRESS"));
    }

    @Test
    void taskConstructorShouldStoreStatusCodes() {
        Task task = new Task(1, "t", null, 1, 0, 0, "完了", null, null);
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals(Task.STATUS_COMPLETED, task.getStatusCode());
    }

    @Test
    void priorityOptionsShouldBeCodes() {
        assertEquals("MEDIUM", Task.DEFAULT_PRIORITY);
        assertEquals("HIGH", Task.PRIORITY_OPTIONS[0]);
        assertEquals(Priority.HIGH, Priority.fromString("高"));
        assertEquals(Priority.HIGH, Priority.fromString("HIGH"));
    }
}
