package org.tasknavi.ui.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tasknavi.team.TeamProject;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamOtherProjectsTest {

    @TempDir
    Path root;

    @Test
    void keepsOnlyRecentProjects() throws Exception {
        TeamProject recent = TeamProject.create(root.resolve("a"), "A", "pc-tanaka", "田中");
        TeamProject old = TeamProject.create(root.resolve("b"), "B", "pc-tanaka", "田中");
        TeamOtherProjects others = new TeamOtherProjects();

        others.keep(recent, List.of(TeamOtherProjects.key(recent.directory())));
        others.keep(old, List.of(TeamOtherProjects.key(recent.directory())));
        others.keep(null, List.of());

        assertEquals(List.of(recent), others.list());
        assertSame(recent, others.get(root.resolve("x").resolve("..").resolve("a")));
        assertTrue(others.remove(root.resolve("a")));
        assertFalse(others.remove(root.resolve("a")));
        assertNull(others.get(root.resolve("a")));
    }

    @Test
    void addLoadedSkipsCurrentAndDuplicates() throws Exception {
        TeamProject current = TeamProject.create(root.resolve("a"), "A", "pc-tanaka", "田中");
        TeamProject other = TeamProject.create(root.resolve("b"), "B", "pc-tanaka", "田中");
        TeamOtherProjects others = new TeamOtherProjects();

        assertTrue(others.addLoaded(List.of(current, other), current));
        assertFalse(others.addLoaded(List.of(other), current));
        assertEquals(List.of(other), others.list());
    }

    @Test
    void openAllSkipsFoldersThatAreNotProjects() throws Exception {
        TeamProject.create(root.resolve("a"), "A", "pc-tanaka", "田中");

        List<TeamProject> opened = TeamOtherProjects.openAll(
                List.of(root.resolve("a"), root.resolve("missing")), "pc-tanaka", "田中");

        assertEquals(1, opened.size());
        assertEquals("A", opened.get(0).name());
    }
}
