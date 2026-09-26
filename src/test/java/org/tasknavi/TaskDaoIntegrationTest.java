package org.tasknavi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskDaoIntegrationTest {

    private final TaskDao taskDao = new TaskDao();

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @BeforeEach
    void setUp() {
        Database.initialize();
        DatabaseTestConfig.clearTasksTable();
    }

    @AfterEach
    void tearDown() {
        DatabaseTestConfig.clearTasksTable();
    }

    @Test
    void shouldPersistAndUpdateTaskHierarchy() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String projectName = "Project-" + unique;
        String childName = "Child-" + unique;

        Task project = new Task(0, projectName, null, 1, 0, 0, "未着手",
                LocalDate.now(), LocalDate.now().plusDays(10));
        int projectId = taskDao.addTask(project);
        assertTrue(projectId > 0);

        Task child = new Task(0, childName, projectId, 2, 0, 50, "進行中",
                LocalDate.now(), LocalDate.now().plusDays(5));
        child.setAssignee("Tester");
        child.setPriority("高");
        int childId = taskDao.addTask(child);
        assertTrue(childId > 0);

        Task loadedChild = taskDao.getTaskById(childId);
        assertNotNull(loadedChild);
        assertEquals(childName, loadedChild.getName());
        assertEquals(projectId, loadedChild.getParentId());

        loadedChild.setProgress(100);
        loadedChild.setStatus("完了");
        taskDao.updateTask(loadedChild);
        taskDao.updateParentProgress(projectId);

        Task loadedProject = taskDao.getTaskById(projectId);
        assertNotNull(loadedProject);
        assertTrue(loadedProject.getProgress() >= 0);

        taskDao.deleteTask(childId);
        taskDao.deleteTask(projectId);
        assertNull(taskDao.getTaskById(childId));
        assertNull(taskDao.getTaskById(projectId));
    }

    @Test
    void shouldFilterByDateRange() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(10);
        Task task = new Task(0, "DateTest-" + unique, null, 1, 0, 0, "未着手",
                start, end);
        int taskId = taskDao.addTask(task);
        assertTrue(taskId > 0);

        List<Task> results = taskDao.filterByDateRange(start, end);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(t -> t.getId() == taskId));

        taskDao.deleteTask(taskId);
    }

    @Test
    void shouldSearchCombinedRespectProjectHierarchy() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Task project = new Task(0, "Proj-" + unique, null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(10));
        int projectId = taskDao.addTask(project);
        assertTrue(projectId > 0);

        Task inProject = new Task(0, "Hit-" + unique, projectId, 2, 0, 0, Task.STATUS_IN_PROGRESS,
                LocalDate.now(), LocalDate.now().plusDays(3));
        inProject.setAssignee("Alice");
        int inId = taskDao.addTask(inProject);

        Task outside = new Task(0, "Hit-" + unique + "-out", null, 1, 0, 0, Task.STATUS_IN_PROGRESS,
                LocalDate.now(), LocalDate.now().plusDays(3));
        outside.setAssignee("Alice");
        int outId = taskDao.addTask(outside);

        List<Task> results = taskDao.searchCombined(
                "Hit-" + unique, "Alice", Task.STATUS_IN_PROGRESS, null, null, projectId);

        assertTrue(results.stream().anyMatch(t -> t.getId() == inId));
        assertTrue(results.stream().noneMatch(t -> t.getId() == outId));

        taskDao.deleteTask(inId);
        taskDao.deleteTask(outId);
        taskDao.deleteTask(projectId);
    }
}
