package org.tasknavi.persistence;

import org.tasknavi.AppException;
import org.tasknavi.DatabaseTestConfig;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.event.TaskEvent;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TaskDaoIntegrationTest {

    private TaskDao taskDao;

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @BeforeEach
    void setUp() {
        Database.initialize();
        DatabaseTestConfig.clearTasksTable();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        int ownerId = DatabaseTestConfig.insertUser("owner-" + unique, "owner-" + unique + "@example.com");
        taskDao = new TaskDao(ownerId);
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
    void parentProgressFollowsChildrenAndIgnoresManualEdit() {
        Task project = new Task(0, "P", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(10));
        int projectId = taskDao.addTask(project);
        Task phase = new Task(0, "Ph", projectId, 2, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(5));
        int phaseId = taskDao.addTask(phase);
        Task leaf = new Task(0, "Leaf", phaseId, 3, 0, 50, Task.STATUS_IN_PROGRESS,
                LocalDate.now(), LocalDate.now().plusDays(2));
        taskDao.addTask(leaf);

        assertEquals(50, taskDao.getTaskById(phaseId).getProgress());
        assertEquals(50, taskDao.getTaskById(projectId).getProgress());

        Task loadedProject = taskDao.getTaskById(projectId);
        loadedProject.setName("P2");
        loadedProject.setProgress(0);
        loadedProject.setStatus(Task.STATUS_NOT_STARTED);
        taskDao.updateTask(loadedProject);

        Task after = taskDao.getTaskById(projectId);
        assertEquals("P2", after.getName());
        assertEquals(50, after.getProgress());
        assertEquals(Task.STATUS_IN_PROGRESS, after.getStatusCode());
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

    @Test
    void updateTaskRejectsBlankName() {
        Task task = new Task(0, "Name-" + UUID.randomUUID().toString().substring(0, 8), null, 1, 0, 0,
                Task.STATUS_NOT_STARTED, LocalDate.now(), LocalDate.now().plusDays(1));
        int id = taskDao.addTask(task);
        Task loaded = taskDao.getTaskById(id);
        assertNotNull(loaded);
        loaded.setName(" ");

        assertThrows(AppException.class, () -> taskDao.updateTask(loaded));

        Task still = taskDao.getTaskById(id);
        assertNotNull(still);
        assertFalse(still.getName().isBlank());
        taskDao.deleteTask(id);
    }

    @Test
    void serviceUpdateFailureDoesNotPublishEvent() {
        TaskService service = new TaskService(taskDao);
        Task task = new Task(0, "Event-" + UUID.randomUUID().toString().substring(0, 8), null, 1, 0, 0,
                Task.STATUS_NOT_STARTED, LocalDate.now(), LocalDate.now().plusDays(1));
        int id = taskDao.addTask(task);
        Task loaded = taskDao.getTaskById(id);
        assertNotNull(loaded);
        loaded.setName(" ");

        AtomicInteger updates = new AtomicInteger();
        TaskEventListener listener = event -> {
            if (event.getType() == TaskEvent.Type.TASK_UPDATED) {
                updates.incrementAndGet();
            }
        };
        TaskEventBus.getInstance().register(listener);
        try {
            assertThrows(AppException.class, () -> service.updateTask(loaded));
            assertEquals(0, updates.get());
        } finally {
            TaskEventBus.getInstance().unregister(listener);
            taskDao.deleteTask(id);
        }
    }

    @Test
    void updateTasksSavesProjectNameAndChildTogether() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Task project = new Task(0, "Project-" + unique, null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(10));
        int projectId = taskDao.addTask(project);
        Task child = new Task(0, "Child-" + unique, projectId, 2, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(3));
        int childId = taskDao.addTask(child);

        Task loadedProject = taskDao.getTaskById(projectId);
        Task loadedChild = taskDao.getTaskById(childId);
        loadedProject.setName("Renamed-" + unique);
        loadedChild.setName("ChildRenamed-" + unique);
        taskDao.updateTasks(List.of(loadedProject, loadedChild));

        assertEquals("Renamed-" + unique, taskDao.getTaskById(projectId).getName());
        assertEquals("ChildRenamed-" + unique, taskDao.getTaskById(childId).getName());

        taskDao.deleteTask(childId);
        taskDao.deleteTask(projectId);
    }

    @Test
    void updateTasksWritesNothingWhenOneTaskIsInvalid() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String projectName = "Project-" + unique;
        String childName = "Child-" + unique;
        Task project = new Task(0, projectName, null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(10));
        int projectId = taskDao.addTask(project);
        Task child = new Task(0, childName, projectId, 2, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(3));
        int childId = taskDao.addTask(child);

        Task loadedProject = taskDao.getTaskById(projectId);
        Task loadedChild = taskDao.getTaskById(childId);
        loadedProject.setName("Renamed-" + unique);
        loadedChild.setName(" ");

        TaskService service = new TaskService(taskDao);
        AtomicInteger updates = new AtomicInteger();
        TaskEventListener listener = event -> {
            if (event.getType() == TaskEvent.Type.TASK_UPDATED) {
                updates.incrementAndGet();
            }
        };
        TaskEventBus.getInstance().register(listener);
        try {
            assertThrows(AppException.class, () -> service.updateTasks(List.of(loadedProject, loadedChild)));
            assertEquals(0, updates.get());
        } finally {
            TaskEventBus.getInstance().unregister(listener);
        }

        assertEquals(projectName, taskDao.getTaskById(projectId).getName());
        assertEquals(childName, taskDao.getTaskById(childId).getName());
        taskDao.deleteTask(childId);
        taskDao.deleteTask(projectId);
    }

    @Test
    void anotherUserCannotSeeOrChangeTasks() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        int otherId = DatabaseTestConfig.insertUser("other-" + unique, "other-" + unique + "@example.com");
        TaskDao otherDao = new TaskDao(otherId);

        int id = taskDao.addTask(new Task(0, "Mine-" + unique, null, 1, 0, 0, "未着手",
                LocalDate.now(), LocalDate.now().plusDays(1)));

        assertNull(otherDao.getTaskById(id));
        assertTrue(otherDao.getAllTasks().isEmpty());
        assertThrows(AppException.class, () -> otherDao.deleteTask(id));

        Task stored = taskDao.getTaskById(id);
        assertNotNull(stored);
        stored.setName("Stolen-" + unique);
        assertThrows(AppException.class, () -> otherDao.updateTask(stored));
        assertEquals("Mine-" + unique, taskDao.getTaskById(id).getName());
    }

    @Test
    void unownedTasksAreClaimedByTheFirstUserOnly() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO tasks (name, owner_id) VALUES ('Legacy-" + unique + "', NULL)");
        }

        int firstId = DatabaseTestConfig.insertUser("first-" + unique, "first-" + unique + "@example.com");
        int secondId = DatabaseTestConfig.insertUser("second-" + unique, "second-" + unique + "@example.com");
        SampleDataUtil.prepareLedgerForUser(firstId);
        SampleDataUtil.prepareLedgerForUser(secondId);

        TaskDao firstDao = new TaskDao(firstId);
        TaskDao secondDao = new TaskDao(secondId);
        assertTrue(firstDao.getAllTasks().stream().anyMatch(task -> ("Legacy-" + unique).equals(task.getName())));
        assertTrue(secondDao.getAllTasks().stream().noneMatch(task -> ("Legacy-" + unique).equals(task.getName())));
    }
}
