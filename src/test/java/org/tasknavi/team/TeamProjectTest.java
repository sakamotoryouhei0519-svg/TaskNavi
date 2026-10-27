package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tasknavi.model.TaskStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 同期フォルダの代わりに普通のフォルダを使い、ファイルのコピーで「同期」を再現する。
 */
class TeamProjectTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-24T03:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    @Test
    void reopenRebuildsTheSameTree() throws Exception {
        Path dir = root.resolve("新システム開発");
        TeamProject tanaka = TeamProject.create(dir, "新システム開発", "pc-tanaka", "田中", NOW);
        String design = tanaka.createTask(Map.of(TeamField.NAME, "設計", TeamField.END, "2026-11-30"));
        String review = tanaka.createTask(Map.of(TeamField.NAME, "レビュー", TeamField.PARENT, design));
        tanaka.createTask(Map.of(TeamField.NAME, "実装"));

        TeamProject reopened = TeamProject.open(dir, "pc-tanaka", "田中", NOW);

        assertEquals("新システム開発", reopened.name());
        assertEquals(List.of("設計", "レビュー", "実装"), names(reopened.state()));
        TeamTask child = reopened.state().find(review).orElseThrow();
        assertEquals(design, child.parentId());
        assertEquals(2, child.level());
        assertEquals(LocalDate.of(2026, 11, 30), reopened.state().find(design).orElseThrow().endDate());
        assertEquals("田中", child.createdBy());
    }

    @Test
    void membersOnTheSameFolderSeeEachOthersChanges() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木", NOW);

        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計"));
        suzuki.reload();
        suzuki.updateTask(id, Map.of(TeamField.STATUS, "IN_PROGRESS", TeamField.PROGRESS, "40"));
        tanaka.reload();

        TeamTask task = tanaka.state().find(id).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, task.status());
        assertEquals(40, task.progress());
        assertEquals("鈴木", task.updatedBy());
        assertTrue(tanaka.state().conflicts().isEmpty());
        assertTrue(Files.exists(TeamChangeLog.logFile(dir, "pc-tanaka")));
        assertTrue(Files.exists(TeamChangeLog.logFile(dir, "pc-suzuki")));
    }

    @Test
    void sameFieldEditedOfflineByBothKeepsOneAndRecordsTheOther() throws Exception {
        Pair pair = sharedTask("設計");
        pair.tanaka.updateTask(pair.taskId, Map.of(TeamField.NAME, "基本設計"));
        pair.suzuki.updateTask(pair.taskId, Map.of(TeamField.NAME, "詳細設計"));
        pair.syncBoth();

        TeamState a = pair.tanaka.state();
        TeamState b = pair.suzuki.state();
        assertEquals(names(a), names(b), "どちらの PC でも同じ結果になる");
        assertEquals(1, a.conflicts().size());
        assertEquals(a.conflicts(), b.conflicts());

        TeamConflict conflict = a.conflicts().get(0);
        assertEquals(TeamConflict.Kind.SAME_FIELD, conflict.kind());
        assertEquals(TeamField.NAME, conflict.field());
        assertEquals(a.find(pair.taskId).orElseThrow().name(), conflict.keptValue());
        assertEquals(List.of("基本設計", "詳細設計"),
                List.of(conflict.keptValue(), conflict.lostValue()).stream().sorted().toList());
    }

    @Test
    void differentFieldsEditedOfflineAreBothKept() throws Exception {
        Pair pair = sharedTask("設計");
        pair.tanaka.updateTask(pair.taskId, Map.of(TeamField.NAME, "基本設計"));
        pair.suzuki.updateTask(pair.taskId, Map.of(TeamField.PROGRESS, "60"));
        pair.syncBoth();

        TeamTask task = pair.tanaka.state().find(pair.taskId).orElseThrow();
        assertEquals("基本設計", task.name());
        assertEquals(60, task.progress());
        assertTrue(pair.tanaka.state().conflicts().isEmpty());
        assertTrue(pair.suzuki.state().conflicts().isEmpty());
    }

    @Test
    void changeMadeAfterSeeingTheOtherWinsEvenIfThePcClockIsBehind() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        Clock slow = Clock.offset(NOW, java.time.Duration.ofDays(-3));
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木", slow);

        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計"));
        tanaka.updateTask(id, Map.of(TeamField.ASSIGNEE, "田中"));
        suzuki.reload();
        suzuki.updateTask(id, Map.of(TeamField.ASSIGNEE, "鈴木"));
        tanaka.reload();

        assertEquals("鈴木", tanaka.state().find(id).orElseThrow().assignee());
        assertTrue(tanaka.state().conflicts().isEmpty());
    }

    @Test
    void deleteAndOfflineEditAreReportedOnBothPcs() throws Exception {
        Pair pair = sharedTask("設計");
        pair.tanaka.deleteTask(pair.taskId);
        pair.suzuki.updateTask(pair.taskId, Map.of(TeamField.PROGRESS, "80"));
        pair.syncBoth();

        for (TeamProject project : List.of(pair.tanaka, pair.suzuki)) {
            assertTrue(project.state().find(pair.taskId).isEmpty());
            assertEquals(1, project.state().conflicts().size());
            TeamConflict.Kind kind = project.state().conflicts().get(0).kind();
            assertTrue(kind == TeamConflict.Kind.EDIT_AFTER_DELETE || kind == TeamConflict.Kind.DELETE_OVER_EDIT);
        }
    }

    @Test
    void childAddedOfflineUnderDeletedParentIsHiddenAndReported() throws Exception {
        Pair pair = sharedTask("設計");
        pair.tanaka.deleteTask(pair.taskId);
        String child = pair.suzuki.createTask(Map.of(TeamField.NAME, "レビュー", TeamField.PARENT, pair.taskId));
        pair.syncBoth();

        TeamState state = pair.tanaka.state();
        assertTrue(state.tasks().isEmpty());
        assertTrue(state.conflicts().stream().anyMatch(c ->
                c.kind() == TeamConflict.Kind.PARENT_DELETED && c.taskId().equals(child)));
    }

    @Test
    void updateArrivingBeforeItsCreateIsAppliedOnceTheCreateArrives() throws Exception {
        Path a = root.resolve("a");
        Path b = root.resolve("b");
        Path c = root.resolve("c");
        TeamProject tanaka = TeamProject.create(a, "P", "pc-tanaka", "田中", NOW);
        copyProjectFile(a, b);
        copyProjectFile(a, c);
        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計"));
        sync(a, b, "pc-tanaka");
        TeamProject suzuki = TeamProject.open(b, "pc-suzuki", "鈴木", NOW);
        suzuki.updateTask(id, Map.of(TeamField.PROGRESS, "30"));

        sync(b, c, "pc-suzuki");
        TeamProject sato = TeamProject.open(c, "pc-sato", "佐藤", NOW);
        assertTrue(sato.state().tasks().isEmpty());

        sync(a, c, "pc-tanaka");
        sato.reload();
        assertEquals(30, sato.state().find(id).orElseThrow().progress());
        assertTrue(sato.state().conflicts().isEmpty());
    }

    @Test
    void deleteRemovesTheWholeSubtree() throws Exception {
        TeamProject project = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        String parent = project.createTask(Map.of(TeamField.NAME, "設計"));
        String child = project.createTask(Map.of(TeamField.NAME, "レビュー", TeamField.PARENT, parent));
        project.createTask(Map.of(TeamField.NAME, "資料", TeamField.PARENT, child));
        project.createTask(Map.of(TeamField.NAME, "実装"));

        project.deleteTask(parent);
        project.reload();

        assertEquals(List.of("実装"), names(project.state()));
        assertTrue(project.state().conflicts().isEmpty());
    }

    @Test
    void unchangedUpdateWritesNothing() throws Exception {
        Path dir = root.resolve("p");
        TeamProject project = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        String id = project.createTask(Map.of(TeamField.NAME, "設計", TeamField.PROGRESS, "10"));
        long before = Files.size(TeamChangeLog.logFile(dir, "pc-tanaka"));

        project.updateTask(id, Map.of(TeamField.NAME, "設計", TeamField.PROGRESS, "10"));

        assertEquals(before, Files.size(TeamChangeLog.logFile(dir, "pc-tanaka")));
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        Path dir = root.resolve("p");
        TeamProject project = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        String l1 = project.createTask(Map.of(TeamField.NAME, "1"));
        String l2 = project.createTask(Map.of(TeamField.NAME, "2", TeamField.PARENT, l1));
        String l3 = project.createTask(Map.of(TeamField.NAME, "3", TeamField.PARENT, l2));

        assertThrows(IllegalArgumentException.class,
                () -> project.createTask(Map.of(TeamField.NAME, "4", TeamField.PARENT, l3)));
        assertThrows(IllegalArgumentException.class, () -> project.createTask(Map.of(TeamField.NAME, " ")));
        assertThrows(IllegalArgumentException.class,
                () -> project.createTask(Map.of(TeamField.NAME, "x", TeamField.PARENT, "unknown")));
        assertThrows(IOException.class, () -> TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW));
        assertThrows(IOException.class, () -> TeamProject.open(root.resolve("none"), "pc-tanaka", "田中", NOW));

        project.deleteTask(l1);
        assertThrows(IllegalArgumentException.class,
                () -> project.updateTask(l1, Map.of(TeamField.NAME, "x")));
        assertFalse(project.state().find(l3).isPresent());
    }

    @Test
    void openingRegistersTheMemberAndRenameUpdatesOnlyOwnFile() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木", NOW);
        tanaka.reload();

        assertEquals(List.of("田中", "鈴木"), tanaka.members().stream().map(TeamMember::name).sorted().toList());

        suzuki.rename("鈴木 花子");
        String id = suzuki.createTask(Map.of(TeamField.NAME, "設計"));
        tanaka.reload();

        assertTrue(tanaka.members().stream().anyMatch(m -> m.deviceId().equals("pc-suzuki") && m.name().equals("鈴木 花子")));
        assertEquals(2, tanaka.members().size());
        assertEquals("鈴木 花子", tanaka.state().find(id).orElseThrow().createdBy());
        assertThrows(IllegalArgumentException.class, () -> suzuki.rename(" "));
    }

    @Test
    void halfSyncedMemberFileIsSkipped() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        Files.writeString(dir.resolve(TeamProject.MEMBERS_DIR).resolve("pc-sato.json"), "{\"name\":");

        tanaka.reload();

        assertEquals(List.of("田中"), tanaka.members().stream().map(TeamMember::name).toList());
    }

    @Test
    void parentProgressIsTheAverageOfChildren() throws Exception {
        TeamProject project = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        String parent = project.createTask(Map.of(TeamField.NAME, "設計", TeamField.STATUS, "NOT_STARTED"));
        String a = project.createTask(Map.of(TeamField.NAME, "A", TeamField.PARENT, parent,
                TeamField.STATUS, "COMPLETED", TeamField.PROGRESS, "100"));
        String b = project.createTask(Map.of(TeamField.NAME, "B", TeamField.PARENT, parent,
                TeamField.STATUS, "IN_PROGRESS", TeamField.PROGRESS, "50"));

        TeamTask rolled = project.state().find(parent).orElseThrow();
        assertEquals(75, rolled.progress());
        assertEquals(TaskStatus.IN_PROGRESS, rolled.status());

        project.updateTask(b, Map.of(TeamField.STATUS, "COMPLETED", TeamField.PROGRESS, "100"));
        rolled = project.state().find(parent).orElseThrow();
        assertEquals(100, rolled.progress());
        assertEquals(TaskStatus.COMPLETED, rolled.status());
        assertEquals(TaskStatus.COMPLETED, project.state().find(a).orElseThrow().status());
    }

    @Test
    void rejectsEndBeforeStartAndBlankName() throws Exception {
        TeamProject project = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        assertThrows(IllegalArgumentException.class, () -> project.createTask(Map.of(TeamField.NAME, "x",
                TeamField.START, "2026-11-10", TeamField.END, "2026-11-01")));

        String id = project.createTask(Map.of(TeamField.NAME, "x", TeamField.START, "2026-11-01"));
        assertThrows(IllegalArgumentException.class,
                () -> project.updateTask(id, Map.of(TeamField.END, "2026-10-31")));
        assertThrows(IllegalArgumentException.class,
                () -> project.updateTask(id, Map.of(TeamField.NAME, " ")));
        project.updateTask(id, Map.of(TeamField.END, "2026-11-01"));
        assertEquals(LocalDate.of(2026, 11, 1), project.state().find(id).orElseThrow().endDate());
    }

    @Test
    void reloadIfChangedReadsOnlyWhenSomeoneWrote() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木", NOW);
        suzuki.reloadIfChanged();
        assertFalse(suzuki.reloadIfChanged());

        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計"));

        assertTrue(suzuki.reloadIfChanged());
        assertTrue(suzuki.state().find(id).isPresent());
        assertFalse(suzuki.reloadIfChanged());
    }

    @Test
    void savingAgainstWhatTheEditorSawDetectsChangesThatArrivedMeanwhile() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木", NOW);
        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計", TeamField.PROGRESS, "10"));
        suzuki.reload();
        TeamState seen = suzuki.state();

        tanaka.updateTask(id, Map.of(TeamField.NAME, "基本設計"));
        suzuki.reload();
        suzuki.updateTask(id, Map.of(TeamField.NAME, "詳細設計", TeamField.PROGRESS, "10"), seen);
        tanaka.reload();

        TeamConflict conflict = tanaka.state().conflicts().get(0);
        assertEquals(1, tanaka.state().conflicts().size());
        assertEquals(TeamConflict.Kind.SAME_FIELD, conflict.kind());
        assertEquals("詳細設計", conflict.keptValue());
        assertEquals("鈴木", conflict.keptBy());
        assertEquals("基本設計", conflict.lostValue());
        assertEquals("田中", conflict.lostBy());

        tanaka.updateTask(id, Map.of(TeamField.NAME, conflict.lostValue()));
        suzuki.reload();
        assertEquals("基本設計", suzuki.state().find(id).orElseThrow().name());
        assertEquals(1, suzuki.state().conflicts().size(), "戻しても重なりの記録は残り、確認済みかどうかは各 PC で覚える");
    }

    @Test
    void restoreBringsBackTheSubtreeAndDeletedAncestors() throws Exception {
        Path dir = root.resolve("p");
        TeamProject project = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        String parent = project.createTask(Map.of(TeamField.NAME, "設計"));
        String child = project.createTask(Map.of(TeamField.NAME, "レビュー", TeamField.PARENT, parent));
        project.createTask(Map.of(TeamField.NAME, "資料", TeamField.PARENT, child));
        project.createTask(Map.of(TeamField.NAME, "実装"));
        project.deleteTask(parent);

        assertEquals(3, project.restoreTask(child));

        assertEquals(List.of("設計", "レビュー", "資料", "実装"), names(project.state()));
        TeamProject reopened = TeamProject.open(dir, "pc-tanaka", "田中", NOW);
        assertEquals(names(project.state()), names(reopened.state()));
        assertEquals(0, project.restoreTask(child));
    }

    @Test
    void restoringATaskDeletedOverAnOfflineEditKeepsTheEdit() throws Exception {
        Pair pair = sharedTask("設計");
        pair.tanaka.deleteTask(pair.taskId);
        pair.suzuki.updateTask(pair.taskId, Map.of(TeamField.PROGRESS, "80"));
        pair.syncBoth();
        List<String> keys = pair.tanaka.state().conflicts().stream().map(TeamConflict::key).toList();

        pair.suzuki.restoreTask(pair.taskId);
        pair.syncBoth();

        for (TeamProject project : List.of(pair.tanaka, pair.suzuki)) {
            assertEquals(80, project.state().find(pair.taskId).orElseThrow().progress());
            assertEquals(keys, project.state().conflicts().stream().map(TeamConflict::key).toList(),
                    "確認済みの印が外れないよう、読み直しても重なりの key は変わらない");
        }
    }

    @Test
    void conflictsFromOneSaveAreGroupedAndAcknowledgedOnesAreHidden() throws Exception {
        Pair pair = sharedTask("設計");
        pair.tanaka.updateTask(pair.taskId, Map.of(TeamField.NAME, "基本設計", TeamField.ASSIGNEE, "田中"));
        pair.suzuki.updateTask(pair.taskId, Map.of(TeamField.NAME, "詳細設計", TeamField.ASSIGNEE, "鈴木"));
        pair.syncBoth();

        List<TeamConflict> conflicts = pair.tanaka.state().conflicts();
        List<TeamConflictGroup> groups = TeamConflictGroup.unacknowledged(conflicts, java.util.Set.of());
        assertEquals(2, conflicts.size());
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).items().size());

        assertTrue(TeamConflictGroup.unacknowledged(conflicts, java.util.Set.copyOf(groups.get(0).keys())).isEmpty());
    }

    @Test
    void folderNameDropsCharactersWindowsRejects() {
        assertEquals("新システム_開発_v2", TeamProject.folderNameFor(" 新システム/開発:v2 "));
        assertEquals("案件", TeamProject.folderNameFor("案件..."));
        assertEquals("TaskNavi-project", TeamProject.folderNameFor("  "));
    }

    private Pair sharedTask(String name) throws IOException {
        Path a = root.resolve("tanaka");
        Path b = root.resolve("suzuki");
        TeamProject tanaka = TeamProject.create(a, "P", "pc-tanaka", "田中", NOW);
        String id = tanaka.createTask(Map.of(TeamField.NAME, name));
        copyProjectFile(a, b);
        sync(a, b, "pc-tanaka");
        TeamProject suzuki = TeamProject.open(b, "pc-suzuki", "鈴木", NOW);
        return new Pair(a, b, tanaka, suzuki, id);
    }

    @Test
    void importWritesAllTasksInOneGoAndKeepsTheTree() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        tanaka.createTask(Map.of(TeamField.NAME, "既存"));
        long before = Files.readAllLines(TeamChangeLog.logFile(dir, "pc-tanaka")).size();

        List<String> ids = tanaka.importTasks(List.of(
                new TeamProject.ImportDraft(Map.of(TeamField.NAME, "設計"), -1),
                new TeamProject.ImportDraft(Map.of(TeamField.NAME, "レビュー", TeamField.ASSIGNEE, "鈴木"), 0),
                new TeamProject.ImportDraft(Map.of(TeamField.NAME, "実装"), -1)), false);

        assertEquals(before + 3, Files.readAllLines(TeamChangeLog.logFile(dir, "pc-tanaka")).size());
        TeamProject reopened = TeamProject.open(dir, "pc-tanaka", "田中", NOW);
        assertEquals(List.of("既存", "設計", "レビュー", "実装"), names(reopened.state()));
        TeamTask child = reopened.state().find(ids.get(1)).orElseThrow();
        assertEquals(ids.get(0), child.parentId());
        assertEquals(2, child.level());
        assertEquals("鈴木", child.assignee());
    }

    @Test
    void importWritesNothingWhenOneRowIsInvalid() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中", NOW);
        tanaka.createTask(Map.of(TeamField.NAME, "既存"));
        long before = Files.readAllLines(TeamChangeLog.logFile(dir, "pc-tanaka")).size();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> tanaka.importTasks(List.of(
                new TeamProject.ImportDraft(Map.of(TeamField.NAME, "設計"), -1),
                new TeamProject.ImportDraft(Map.of(TeamField.NAME, "逆の日付",
                        TeamField.START, "2026-11-05", TeamField.END, "2026-11-01"), -1)), true));

        assertTrue(error.getMessage().startsWith("2 件目"));
        assertEquals(before, Files.readAllLines(TeamChangeLog.logFile(dir, "pc-tanaka")).size());
        assertEquals(List.of("既存"), names(tanaka.state()));
    }

    @Test
    void importCanReplaceEverything() throws Exception {
        TeamProject tanaka = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        String old = tanaka.createTask(Map.of(TeamField.NAME, "古い親"));
        tanaka.createTask(Map.of(TeamField.NAME, "古い子", TeamField.PARENT, old));

        tanaka.importTasks(List.of(new TeamProject.ImportDraft(Map.of(TeamField.NAME, "新しい"), -1)), true);

        assertEquals(List.of("新しい"), names(tanaka.state()));
        assertTrue(tanaka.state().conflicts().isEmpty());
        assertEquals(2, tanaka.restoreTask(old), "置き換えで消したタスクも、親と子をまとめて戻せる");
    }

    private record Pair(Path a, Path b, TeamProject tanaka, TeamProject suzuki, String taskId) {
        void syncBoth() throws IOException {
            sync(a, b, "pc-tanaka");
            sync(b, a, "pc-suzuki");
            tanaka.reload();
            suzuki.reload();
        }
    }

    private static void copyProjectFile(Path from, Path to) throws IOException {
        Files.createDirectories(to.resolve(TeamChangeLog.CHANGES_DIR));
        Files.copy(from.resolve(TeamProject.PROJECT_FILE), to.resolve(TeamProject.PROJECT_FILE),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static void sync(Path from, Path to, String deviceId) throws IOException {
        Path target = TeamChangeLog.logFile(to, deviceId);
        Files.createDirectories(target.getParent());
        Files.copy(TeamChangeLog.logFile(from, deviceId), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static List<String> names(TeamState state) {
        return state.tasks().stream().map(TeamTask::name).toList();
    }
}
