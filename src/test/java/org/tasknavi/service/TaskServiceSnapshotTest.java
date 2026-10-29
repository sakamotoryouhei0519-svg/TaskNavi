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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceSnapshotTest {

    private final AtomicInteger reads = new AtomicInteger();
    private TaskService service;
    private String unique;

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @BeforeEach
    void setUp() {
        Database.initialize();
        DatabaseTestConfig.clearTasksTable();
        unique = UUID.randomUUID().toString().substring(0, 8);
        TaskDao countingDao = new TaskDao() {
            @Override
            public List<Task> getAllTasks() {
                reads.incrementAndGet();
                return super.getAllTasks();
            }
        };
        countingDao.addTask(project("First"));
        service = new TaskService(countingDao);
    }

    @AfterEach
    void tearDown() {
        DatabaseTestConfig.clearTasksTable();
    }

    @Test
    void repeatedReadsHitTheDatabaseOnce() {
        int before = reads.get();

        service.getAllTasks();
        service.getAllTasks();
        service.getAllTasks();

        assertEquals(before + 1, reads.get());
    }

    @Test
    void writeMakesTheNextReadFresh() {
        assertEquals(1, service.getAllTasks().size());

        service.addTask(project("Second"));
        int before = reads.get();
        List<Task> afterAdd = service.getAllTasks();

        assertEquals(before + 1, reads.get());
        assertEquals(2, afterAdd.size());
    }

    @Test
    void failedWriteAlsoMakesTheNextReadFresh() {
        Task task = service.getAllTasks().get(0);
        task.setName(" ");

        assertThrows(AppException.class, () -> service.updateTask(task));
        int before = reads.get();
        service.getAllTasks();

        assertEquals(before + 1, reads.get());
    }

    @Test
    void changesByCallersDoNotLeakIntoTheSharedResult() {
        service.getAllTasks().get(0).setName("changed by a screen");

        String name = service.getAllTasks().get(0).getName();

        assertTrue(name.startsWith("First-"));
    }

    @Test
    void reloadRereadsTheDatabase() {
        service.getAllTasks();

        service.reload();
        int before = reads.get();
        service.getAllTasks();

        assertEquals(before + 1, reads.get());
    }

    private Task project(String prefix) {
        return new Task(0, prefix + "-" + unique, null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(5));
    }
}
