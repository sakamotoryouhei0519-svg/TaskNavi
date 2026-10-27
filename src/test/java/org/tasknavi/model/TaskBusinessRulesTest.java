package org.tasknavi.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskBusinessRulesTest {

    @Test
    void resolveProgress_stringAndEnumOverloadsAgree() {
        assertEquals(0, TaskBusinessRules.resolveProgress(TaskStatus.NOT_STARTED, 40));
        assertEquals(0, TaskBusinessRules.resolveProgress(TaskStatus.NOT_STARTED.name(), 40));

        assertEquals(50, TaskBusinessRules.resolveProgress(TaskStatus.IN_PROGRESS, 0));
        assertEquals(50, TaskBusinessRules.resolveProgress(TaskStatus.IN_PROGRESS.name(), null));
        assertEquals(75, TaskBusinessRules.resolveProgress(TaskStatus.IN_PROGRESS, 75));

        assertEquals(100, TaskBusinessRules.resolveProgress(TaskStatus.COMPLETED, 10));
        assertEquals(0, TaskBusinessRules.resolveProgress((TaskStatus) null, null));
        assertEquals(42, TaskBusinessRules.resolveProgress((TaskStatus) null, 42));
    }
}
