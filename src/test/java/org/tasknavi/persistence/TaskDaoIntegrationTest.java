package org.tasknavi.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.AppException;
import org.tasknavi.DatabaseTestConfig;
import org.tasknavi.event.TaskEvent;
import org.tasknavi.event.TaskEventBus;
import org.tasknavi.event.TaskEventListener;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;

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
        taskDao = new TaskDao();
    }

    @AfterEach
    void tearDown() {
        DatabaseTestConfig.clearTasksTable();
    }

    @Test
    void memoIsSavedUpdatedAndCleared() {
        Task task = new Task(0, "Memo", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(1));
        task.setMemo("1行目\n2行目");
        int id = taskDao.addTask(task);
        assertEquals("1行目\n2行目", taskDao.getTaskById(id).getMemo());

        Task loaded = taskDao.getTaskById(id);
        loaded.setMemo("書き換え");
        taskDao.updateTask(loaded);
        assertEquals("書き換え", taskDao.getTaskById(id).getMemo());

        loaded.setMemo("  ");
        taskDao.updateTask(loaded);
        assertEquals("", taskDao.getTaskById(id).getMemo());
    }

    @Test
    void memoSurvivesDeleteAndRestore() {
        Task task = new Task(0, "Undo", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.now(), LocalDate.now().plusDays(1));
        task.setMemo("消しても戻る");
        int id = taskDao.addTask(task);

        List<Task> removed = taskDao.deleteTask(id);
        taskDao.restoreTasks(removed);

        assertEquals("消しても戻る", taskDao.getTaskById(id).getMemo());
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
    void sampleTasksAreInsertedOnlyIntoAnEmptyLedger() {
        SampleDataUtil.insertSampleTasksIfEmpty();
        int sampleCount = taskDao.getAllTasks().size();
        assertTrue(sampleCount > 0);

        SampleDataUtil.insertSampleTasksIfEmpty();
        assertEquals(sampleCount, taskDao.getAllTasks().size(), "2 回目は入れない");

        DatabaseTestConfig.clearTasksTable();
        taskDao.addTask(importRow(0, null, 1, 0, "自分のプロジェクト"));
        SampleDataUtil.insertSampleTasksIfEmpty();
        assertEquals(List.of("自分のプロジェクト"), taskDao.getAllTasks().stream().map(Task::getName).toList());
    }

    private static Task importRow(int id, Integer parentId, int level, int order, String name) {
        return new Task(id, name, parentId, level, order, 0, Task.STATUS_NOT_STARTED,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @Test
    void importAppendsAndRemapsParentsToNewIds() {
        int existingId = taskDao.addTask(importRow(0, null, 1, 0, "既存プロジェクト"));
        Task work = importRow(3, 2, 3, 0, "取り込みタスク");
        work.setMemo("メモも入る");

        taskDao.importTasks(List.of(
                work,
                importRow(1, null, 1, 0, "取り込みプロジェクト"),
                importRow(2, 1, 2, 0, "取り込み工程")), false);

        List<Task> all = taskDao.getAllTasks();
        assertEquals(4, all.size());
        assertNotNull(taskDao.getTaskById(existingId));
        Task project = findByName(all, "取り込みプロジェクト");
        Task stage = findByName(all, "取り込み工程");
        Task imported = findByName(all, "取り込みタスク");
        assertNull(project.getParentId());
        assertEquals(project.getId(), stage.getParentId());
        assertEquals(stage.getId(), imported.getParentId());
        assertEquals("メモも入る", imported.getMemo());
        assertTrue(project.getOrderIndex() > taskDao.getTaskById(existingId).getOrderIndex());
    }

    @Test
    void importReplaceRemovesExistingTasks() {
        taskDao.addTask(importRow(0, null, 1, 0, "消えるプロジェクト"));

        taskDao.importTasks(List.of(importRow(1, null, 1, 0, "新しいプロジェクト")), true);

        assertEquals(List.of("新しいプロジェクト"), taskDao.getAllTasks().stream().map(Task::getName).toList());
    }

    @Test
    void importWithBrokenHierarchyWritesNothing() {
        taskDao.addTask(importRow(0, null, 1, 0, "残るプロジェクト"));

        assertThrows(AppException.class, () -> taskDao.importTasks(List.of(
                importRow(1, null, 1, 0, "取り込みプロジェクト"),
                importRow(2, 99, 2, 0, "親がない工程")), true));
        assertThrows(AppException.class, () -> taskDao.importTasks(List.of(
                importRow(1, null, 1, 0, "A"),
                importRow(1, null, 1, 1, "B")), false));

        assertEquals(List.of("残るプロジェクト"), taskDao.getAllTasks().stream().map(Task::getName).toList());
    }

    private static Task findByName(List<Task> tasks, String name) {
        return tasks.stream().filter(t -> name.equals(t.getName())).findFirst().orElseThrow();
    }
}
