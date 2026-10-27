package org.tasknavi.service;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskSourceTest {

    @Test
    void personalTasksAreAllMine() {
        TaskSource personal = new TaskSource("自分だけのタスク", true, null);

        assertTrue(personal.isMine(assignedTo("鈴木")));
    }

    @Test
    void sharedTasksAreMineWhenAssignedToMeOrToNoOne() {
        TaskSource shared = new TaskSource("新システム開発", true, null, "田中");

        assertTrue(shared.isMine(assignedTo("田中")));
        assertTrue(shared.isMine(assignedTo(" 田中 ")), "前後の空白は無視する");
        assertTrue(shared.isMine(assignedTo("")), "担当者が空なら自分にも出す");
        assertTrue(shared.isMine(assignedTo(null)));
        assertFalse(shared.isMine(assignedTo("鈴木")));
    }

    @Test
    void nameIsComparedIgnoringCase() {
        TaskSource shared = new TaskSource("Project", true, null, "Tanaka");

        assertTrue(shared.isMine(assignedTo("tanaka")));
    }

    private static Task assignedTo(String assignee) {
        Task task = new Task(1, "t", null, 1, 0, 0, Task.STATUS_NOT_STARTED, null, null);
        task.setAssignee(assignee);
        return task;
    }
}
