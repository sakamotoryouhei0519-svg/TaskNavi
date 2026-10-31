package org.tasknavi.ui.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tasknavi.team.TeamField;
import org.tasknavi.team.TeamNotice;
import org.tasknavi.team.TeamProject;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamPollTest {

    @TempDir
    Path root;

    @Test
    void nothingOpenMeansNothingToDo() {
        TeamPoll result = TeamPoll.check(null, List.of());

        assertNull(result.failure());
        assertFalse(result.othersChanged());
        assertTrue(result.notices().isEmpty());
    }

    @Test
    void readsOnlyChangedProjectsAndCollectsNoticesOpenedFirst() throws Exception {
        TeamProject opened = TeamProject.create(root.resolve("a"), "A", "pc-tanaka", "田中");
        TeamProject other = TeamProject.create(root.resolve("b"), "B", "pc-tanaka", "田中");
        TeamProject suzukiA = TeamProject.open(opened.directory(), "pc-suzuki", "鈴木");
        TeamProject suzukiB = TeamProject.open(other.directory(), "pc-suzuki", "鈴木");
        opened.reload();
        other.reload();

        TeamPoll quiet = TeamPoll.check(opened, List.of(other));
        assertFalse(quiet.othersChanged());
        assertTrue(quiet.notices().isEmpty());

        suzukiB.createTask(Map.of(TeamField.NAME, "見積もり", TeamField.ASSIGNEE, "田中"));
        suzukiA.createTask(Map.of(TeamField.NAME, "設計", TeamField.ASSIGNEE, "田中"));
        TeamPoll changed = TeamPoll.check(opened, List.of(other));

        assertNull(changed.failure());
        assertTrue(changed.othersChanged());
        assertEquals(List.of("A", "B"), List.copyOf(changed.notices().keySet()));
        assertEquals(List.of(new TeamNotice(TeamNotice.Kind.ASSIGNED, "設計", "鈴木")), changed.notices().get("A"));
        assertTrue(TeamPoll.check(opened, List.of(other)).notices().isEmpty(), "一度渡した知らせは出さない");
    }
}
