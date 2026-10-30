package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamTaskRulesTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-24T03:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    @Test
    void dateOrderAllowsBlankAndSameDay() {
        assertDoesNotThrow(() -> TeamTaskRules.requireDateOrder(null, "2026-11-01"));
        assertDoesNotThrow(() -> TeamTaskRules.requireDateOrder("2026-11-01", " "));
        assertDoesNotThrow(() -> TeamTaskRules.requireDateOrder("2026-11-01", "2026-11-01"));
        assertThrows(IllegalArgumentException.class, () -> TeamTaskRules.requireDateOrder("2026-11-02", "2026-11-01"));
        assertThrows(IllegalArgumentException.class, () -> TeamTaskRules.requireDateOrder("2026/11/01", "2026-11-02"));
    }

    @Test
    void commentIsTrimmedAndLimited() {
        assertEquals("確認しました", TeamTaskRules.requireCommentText("  確認しました\n"));
        assertThrows(IllegalArgumentException.class, () -> TeamTaskRules.requireCommentText("   "));
        assertThrows(IllegalArgumentException.class,
                () -> TeamTaskRules.requireCommentText("あ".repeat(TeamTaskRules.MAX_COMMENT_LENGTH + 1)));
    }

    @Test
    void levelStopsAtMax() throws Exception {
        TeamProject project = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        String first = project.createTask(Map.of(TeamField.NAME, "1"));
        String second = project.createTask(Map.of(TeamField.NAME, "2", TeamField.PARENT, first));
        String third = project.createTask(Map.of(TeamField.NAME, "3", TeamField.PARENT, second));

        assertEquals(1, TeamTaskRules.levelUnder(project.state(), null));
        assertEquals(3, TeamTaskRules.levelUnder(project.state(), second));
        assertThrows(IllegalArgumentException.class, () -> TeamTaskRules.levelUnder(project.state(), third));
        assertThrows(IllegalArgumentException.class, () -> TeamTaskRules.levelUnder(project.state(), "none"));
    }

    @Test
    void movingUnderOwnChildIsRejected() throws Exception {
        TeamProject project = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        String parent = project.createTask(Map.of(TeamField.NAME, "親"));
        String child = project.createTask(Map.of(TeamField.NAME, "子", TeamField.PARENT, parent));

        assertEquals(List.of(parent, child), TeamTaskRules.subtree(project.state(), parent));
        assertThrows(IllegalArgumentException.class,
                () -> TeamTaskRules.requireMovable(project.state(), parent, child));
        assertDoesNotThrow(() -> TeamTaskRules.requireMovable(project.state(), child, null));
    }

    @Test
    void restoreTargetsBringBackDeletedParentFirst() throws Exception {
        TeamProject project = TeamProject.create(root.resolve("p"), "P", "pc-tanaka", "田中", NOW);
        String parent = project.createTask(Map.of(TeamField.NAME, "親"));
        String child = project.createTask(Map.of(TeamField.NAME, "子", TeamField.PARENT, parent));
        String grandchild = project.createTask(Map.of(TeamField.NAME, "孫", TeamField.PARENT, child));
        project.deleteTask(parent);

        assertEquals(List.of(parent, child, grandchild), TeamTaskRules.restoreTargets(project.state().versions(), child));
    }
}
