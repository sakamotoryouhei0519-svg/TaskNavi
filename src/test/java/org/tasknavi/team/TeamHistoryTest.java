package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamHistoryTest {

    @TempDir
    Path root;

    @Test
    void listsWhoChangedWhatNewestFirst() throws Exception {
        Path dir = root.resolve("p");
        TeamProject tanaka = TeamProject.create(dir, "P", "pc-tanaka", "田中");
        String parent = tanaka.createTask(Map.of(TeamField.NAME, "設計"));
        String child = tanaka.createTask(Map.of(TeamField.NAME, "レビュー", TeamField.PARENT, parent));
        TeamProject suzuki = TeamProject.open(dir, "pc-suzuki", "鈴木");
        suzuki.updateTask(child, Map.of(TeamField.NAME, "設計レビュー", TeamField.ASSIGNEE, "鈴木"));
        suzuki.deleteTask(child);
        suzuki.restoreTask(child);

        List<TeamHistory.Entry> history = suzuki.history();

        assertEquals(List.of(TeamEvent.Op.RESTORE, TeamEvent.Op.DELETE, TeamEvent.Op.UPDATE,
                TeamEvent.Op.CREATE, TeamEvent.Op.CREATE), history.stream().map(TeamHistory.Entry::op).toList());
        TeamHistory.Entry update = history.get(2);
        assertEquals("鈴木", update.author());
        assertEquals("設計レビュー", update.taskName());
        assertEquals(List.of(new TeamHistory.Change(TeamField.NAME, "レビュー", "設計レビュー"),
                new TeamHistory.Change(TeamField.ASSIGNEE, null, "鈴木")), update.changes());
        assertTrue(history.get(3).changes().contains(new TeamHistory.Change(TeamField.PARENT, null, "設計")),
                "親は ID ではなく名前で出す");
        assertTrue(history.get(0).changes().isEmpty());
        assertEquals("設計レビュー", history.get(0).taskName());
    }

    @Test
    void lastValueMatchesTheListAfterOverlappingEdits() throws Exception {
        Path a = root.resolve("a");
        TeamProject tanaka = TeamProject.create(a, "P", "pc-tanaka", "田中");
        TeamProject suzuki = TeamProject.open(a, "pc-suzuki", "鈴木");
        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計"));
        suzuki.reload();
        TeamState seen = suzuki.state();
        tanaka.updateTask(id, Map.of(TeamField.NAME, "基本設計"));
        suzuki.reload();
        suzuki.updateTask(id, Map.of(TeamField.NAME, "詳細設計"), seen);
        tanaka.reload();

        TeamHistory.Entry latest = tanaka.history().get(0);
        assertEquals(new TeamHistory.Change(TeamField.NAME, "基本設計", "詳細設計"), latest.changes().get(0));
        assertEquals(tanaka.state().find(id).orElseThrow().name(), latest.taskName());
    }
}
