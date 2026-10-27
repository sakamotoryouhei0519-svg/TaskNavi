package org.tasknavi.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.AppException;
import org.tasknavi.DatabaseTestConfig;
import org.tasknavi.model.Task;
import org.tasknavi.persistence.Database;
import org.tasknavi.persistence.TaskDao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskServiceUndoDeleteTest {

    private TaskDao taskDao;
    private TaskService service;
    private int projectId;
    private int doneChildId;
    private int openChildId;

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @BeforeEach
    void setUp() {
        Database.initialize();
        DatabaseTestConfig.clearTasksTable();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        int ownerId = DatabaseTestConfig.insertUser("undo-" + unique, "undo-" + unique + "@example.com");
        taskDao = new TaskDao(ownerId);
        service = new TaskService(taskDao);

        projectId = taskDao.addTask(new Task(0, "Project", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(10)));
        doneChildId = taskDao.addTask(new Task(0, "Done", projectId, 2, 0, 100, Task.STATUS_COMPLETED,
                LocalDate.now(), LocalDate.now().plusDays(3)));
        openChildId = taskDao.addTask(new Task(0, "Open", projectId, 2, 1, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(3)));
    }

    @AfterEach
    void tearDown() {
        DatabaseTestConfig.clearTasksTable();
    }

    @Test
    void restoresWholeSubtreeWithOriginalIds() {
        List<Task> removed = service.deleteTask(projectId);
        assertEquals(3, removed.size());
        assertNull(taskDao.getTaskById(projectId));

        service.restoreDeleted(removed);

        Task open = taskDao.getTaskById(openChildId);
        assertNotNull(taskDao.getTaskById(projectId));
        assertNotNull(taskDao.getTaskById(doneChildId));
        assertEquals("Open", open.getName());
        assertEquals(projectId, open.getParentId());
        assertEquals(3, service.getAllTasks().size());
    }

    @Test
    void parentProgressFollowsDeleteAndRestore() {
        int before = taskDao.getTaskById(projectId).getProgress();

        List<Task> removed = service.deleteTask(openChildId);
        assertEquals(100, taskDao.getTaskById(projectId).getProgress());

        service.restoreDeleted(removed);
        assertEquals(before, taskDao.getTaskById(projectId).getProgress());
    }

    @Test
    void refusesWhenParentWasDeletedAfterwards() {
        List<Task> removedChild = service.deleteTask(openChildId);
        service.deleteTask(projectId);

        assertThrows(AppException.class, () -> service.restoreDeleted(removedChild));
        assertNull(taskDao.getTaskById(openChildId));
    }
}
