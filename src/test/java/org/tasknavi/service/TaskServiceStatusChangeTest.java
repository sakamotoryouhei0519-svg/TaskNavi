package org.tasknavi.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.DatabaseTestConfig;
import org.tasknavi.event.TaskEvent;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Task;
import org.tasknavi.persistence.Database;
import org.tasknavi.persistence.TaskDao;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskServiceStatusChangeTest {

    private TaskDao taskDao;
    private TaskService service;
    private int projectId;
    private int childId;
    private final AtomicInteger updates = new AtomicInteger();
    private final TaskEventListener listener = event -> {
        if (event.getType() == TaskEvent.Type.TASK_UPDATED) {
            updates.incrementAndGet();
        }
    };

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @BeforeEach
    void setUp() {
        Database.initialize();
        DatabaseTestConfig.clearTasksTable();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        taskDao = new TaskDao();
        service = new TaskService(taskDao);

        projectId = taskDao.addTask(new Task(0, "Project-" + unique, null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(10)));
        childId = taskDao.addTask(new Task(0, "Child-" + unique, projectId, 2, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(3)));
        TaskEventBus.getInstance().register(listener);
    }

    @AfterEach
    void tearDown() {
        TaskEventBus.getInstance().unregister(listener);
        DatabaseTestConfig.clearTasksTable();
    }

    @Test
    void leafTaskTakesNewStatusAndMatchingProgress() throws Exception {
        assertEquals(TaskService.StatusChange.CHANGED, service.changeStatus(childId, Task.STATUS_COMPLETED));
        flushEvents();

        Task child = taskDao.getTaskById(childId);
        assertEquals(Task.STATUS_COMPLETED, child.getStatusCode());
        assertEquals(100, child.getProgress());
        assertEquals(100, taskDao.getTaskById(projectId).getProgress());
        assertEquals(1, updates.get());
    }

    @Test
    void taskWithChildrenIsLeftAlone() throws Exception {
        assertEquals(TaskService.StatusChange.HAS_CHILDREN, service.changeStatus(projectId, Task.STATUS_COMPLETED));
        flushEvents();

        Task project = taskDao.getTaskById(projectId);
        assertEquals(Task.STATUS_NOT_STARTED, project.getStatusCode());
        assertEquals(0, project.getProgress());
        assertEquals(0, updates.get());
    }

    @Test
    void unknownTaskIsReported() throws Exception {
        assertEquals(TaskService.StatusChange.NOT_FOUND, service.changeStatus(childId + 1000, Task.STATUS_COMPLETED));
        flushEvents();

        assertEquals(0, updates.get());
    }

    private static void flushEvents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }
}
