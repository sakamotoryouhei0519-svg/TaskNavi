package org.example;

import org.example.util.SearchFilterUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntryTypeAndFilterTest {

    @Test
    void entryTypeFromStringAcceptsCodeAndLabel() {
        assertEquals(EntryType.PROJECT, EntryType.fromString("PROJECT"));
        assertEquals(EntryType.PHASE, EntryType.fromString("工程"));
        assertEquals(EntryType.TASK, EntryType.fromString("タスク"));
    }

    @Test
    void filterAllIsCodeNotDisplayLabel() {
        assertEquals("ALL", AppMessages.FILTER_ALL);
        assertTrue(AppMessages.isFilterAll("ALL"));
        assertTrue(AppMessages.isFilterAll("すべて"));
        assertNull(SearchFilterUtil.normalizeStatus(AppMessages.FILTER_ALL));
        assertEquals("すべて", AppMessages.statusDisplay(AppMessages.FILTER_ALL));
    }

    @Test
    void statusFieldIsEnum() {
        Task task = new Task(1, "t", null, 1, 0, 0, "進行中", null, null);
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals("IN_PROGRESS", task.getStatusCode());
        task.setStatus(TaskStatus.COMPLETED);
        assertTrue(task.isCompleted());
    }
}
