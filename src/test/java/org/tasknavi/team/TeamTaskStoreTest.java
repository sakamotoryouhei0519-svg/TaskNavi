package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tasknavi.AppException;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskStatus;
import org.tasknavi.persistence.TaskStore;
import org.tasknavi.service.TaskService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 共有プロジェクトを、個人用の画面が使う保存先として扱えるか。 */
class TeamTaskStoreTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-26T03:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    @Test
    void screenIdsStayTheSameAfterOthersAddTasks() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        int design = store.addTask(task("設計", null));
        int review = store.addTask(task("レビュー", design));

        suzuki.reload();
        suzuki.createTask(Map.of(TeamField.NAME, "先頭に追加", TeamField.ORDER, "-1"));
        tanaka.reload();

        assertEquals("設計", store.getTaskById(design).getName());
        Task child = store.getTaskById(review);
        assertEquals(design, child.getParentId());
        assertEquals(2, child.getLevel());
        assertEquals(3, store.getAllTasks().size());
    }

    @Test
    void tasksCarryTheirCommentsWithoutDeletedOnes() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        int id = store.addTask(task("設計", null));
        String uuid = store.uuidOf(id);
        tanaka.addComment(uuid, "見ておきます");
        tanaka.addComment(uuid, "まちがい");
        tanaka.deleteComment(uuid, tanaka.state().comments(uuid).get(1).eventId());

        assertEquals(List.of("見ておきます"), store.getTaskById(id).getComments());
        assertEquals(List.of("見ておきます"), store.getAllTasks().get(0).getComments());
        assertEquals(List.of("見ておきます"), store.getTaskById(id).copy().getComments());
    }

    @Test
    void savingAnUnchangedTaskWritesNothing() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        int id = store.addTask(task("設計", null));
        int before = tanaka.history().size();

        store.updateTasks(List.of(store.getTaskById(id)));

        assertEquals(before, tanaka.history().size(), "担当者やメモが空でも、変わっていなければ記録しない");
    }

    @Test
    void editOpenedBeforeAnotherPersonsChangeIsKeptAsOverlap() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        int id = store.addTask(task("設計", null));
        Task opened = store.getTaskById(id);

        suzuki.reload();
        suzuki.updateTask(tanaka.state().tasks().get(0).id(), Map.of(TeamField.NAME, "基本設計"));
        tanaka.reload();
        store.getAllTasks();

        opened.setName("詳細設計");
        store.updateTasks(List.of(opened));

        assertEquals(1, tanaka.state().conflicts().size());
        assertEquals(TeamConflict.Kind.SAME_FIELD, tanaka.state().conflicts().get(0).kind());
    }

    @Test
    void movingUnderAnotherTaskDerivesTheLevel() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        int design = store.addTask(task("設計", null));
        int build = store.addTask(task("実装", null));
        int test = store.addTask(task("テスト", build));

        Task moved = store.getTaskById(build);
        moved.setParentId(design);
        store.updateTasks(List.of(moved));

        assertEquals(2, store.getTaskById(build).getLevel());
        assertEquals(3, store.getTaskById(test).getLevel());

        Task loop = store.getTaskById(design);
        loop.setParentId(test);
        assertThrows(AppException.class, () -> store.updateTasks(List.of(loop)));
    }

    @Test
    void deleteReturnsTheSubtreeAndRestoreBringsItBack() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        int design = store.addTask(task("設計", null));
        int review = store.addTask(task("レビュー", design));

        List<Task> removed = store.deleteTask(design);

        assertEquals(2, removed.size());
        assertNull(store.getTaskById(review));
        store.restoreTasks(removed);
        assertNotNull(store.getTaskById(design));
        assertEquals(design, store.getTaskById(review).getParentId());
    }

    @Test
    void importKeepsTheTreeAndCanReplace() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        store.addTask(task("古いタスク", null));

        Task parent = new Task(10, "設計", null, 1, 0, 0, TaskStatus.NOT_STARTED, null, null);
        Task child = new Task(11, "レビュー", 10, 2, 0, 50, TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5));
        store.importTasks(new ArrayList<>(List.of(child, parent)), true);

        List<Task> all = store.getAllTasks();
        assertEquals(List.of("設計", "レビュー"), all.stream().map(Task::getName).toList());
        assertEquals(all.get(0).getId(), all.get(1).getParentId());
        assertEquals(50, all.get(1).getProgress());
    }

    @Test
    void parentProgressIsTheChildrensRollup() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        TeamTaskStore store = new TeamTaskStore(tanaka);
        int design = store.addTask(task("設計", null));
        int review = store.addTask(task("レビュー", design));

        Task done = store.getTaskById(review);
        done.setStatus(TaskStatus.COMPLETED);
        done.setProgress(100);
        store.updateTasks(List.of(done));

        assertEquals(100, store.getTaskById(design).getProgress());
        Task parent = store.getTaskById(design);
        parent.setProgress(0);
        store.updateTasks(List.of(parent));
        assertEquals(100, store.getTaskById(design).getProgress(), "親の進捗は子から決まるので保存しない");
    }

    @Test
    void assigneeOptionsListMembersFirstThenOtherAssignees() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        TeamProject.open(dir, "pc-suzuki", "鈴木", NOW);
        tanaka.reload();
        TeamTaskStore store = new TeamTaskStore(tanaka);
        Task outsourced = task("外注の見積もり", null);
        outsourced.setAssignee("協力会社");
        store.addTask(outsourced);

        List<String> options = new TaskService(store).assigneeOptions();

        assertEquals(List.of("協力会社"), options.subList(2, options.size()));
        assertTrue(options.subList(0, 2).containsAll(List.of("田中", "鈴木")));
    }

    @Test
    void serviceSwitchesBetweenPersonalAndSharedTasks() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        TeamTaskStore shared = new TeamTaskStore(tanaka);
        shared.addTask(task("共有の設計", null));
        TaskStore personal = new TeamTaskStore(TeamProject.create(root.resolve("me"), "自分", "pc-me", "私", NOW));
        personal.addTask(task("自分のメモ", null));
        TaskService service = new TaskService(personal);

        assertTrue(service.isPersonal());
        assertEquals("自分のメモ", service.getAllTasks().get(0).getName());

        service.useStore(shared);
        assertFalse(service.isPersonal());
        assertSame(shared, service.currentStore());
        assertEquals("共有の設計", service.getAllTasks().get(0).getName());

        service.useStore(null);
        assertTrue(service.isPersonal());
        assertEquals("自分のメモ", service.getAllTasks().get(0).getName());
    }

    private static Task task(String name, Integer parentId) {
        return new Task(0, name, parentId, parentId == null ? 1 : 2, 0, 0, TaskStatus.NOT_STARTED, null, null);
    }
}
