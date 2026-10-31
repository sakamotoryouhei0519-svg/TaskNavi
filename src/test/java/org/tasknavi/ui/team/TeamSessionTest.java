package org.tasknavi.ui.team;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamChangeLog;
import org.tasknavi.team.TeamConflictGroup;
import org.tasknavi.team.TeamField;
import org.tasknavi.team.TeamNotice;
import org.tasknavi.team.TeamProject;
import org.tasknavi.team.TeamSettings;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** ダイアログを出さない道（起動時に開く・閉じる・開き直す・読み直す・重なりの数・ヘッダーの表示）を、本物のフォルダで確かめる。 */
class TeamSessionTest {

    @TempDir
    Path root;

    private Preferences prefs;
    private TeamSettings settings;
    private TeamSession session;
    private final List<String> events = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("tasknavi-test/" + UUID.randomUUID());
        settings = new TeamSettings(prefs);
        settings.setDisplayName("田中");
        session = new TeamSession(settings, null);
        session.setListener(new TeamSession.Listener() {
            @Override
            public void projectChanged(TeamProject opened) {
                events.add("project:" + (opened != null ? opened.name() : null));
            }

            @Override
            public void dataChanged() {
                events.add("data");
            }

            @Override
            public void statusChanged() {
            }

            @Override
            public void otherProjectsChanged() {
                events.add("others");
            }

            @Override
            public void noticesArrived(Map<String, List<TeamNotice>> byProject) {
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        session.stop();
        prefs.removeNode();
    }

    @Test
    void startOpensTheLastProjectAndReadsTheOtherRecentOnes() throws Exception {
        Path a = TeamProject.create(root.resolve("a"), "A", "pc-tanaka", "田中").directory();
        Path b = TeamProject.create(root.resolve("b"), "B", "pc-tanaka", "田中").directory();
        settings.remember(b);
        settings.remember(a);

        startAndWait();

        assertEquals("A", session.project().name());
        assertEquals(List.of("B"), names(session.otherProjects()));
        assertTrue(events.contains("project:A"), events.toString());
        assertTrue(events.contains("others"), events.toString());
        assertSame(session.storeOf(session.project()), session.storeOf(session.project()));
    }

    @Test
    void closeKeepsTheProjectForTodayAndOpeningAgainReusesIt() throws Exception {
        Path a = TeamProject.create(root.resolve("a"), "A", "pc-tanaka", "田中").directory();
        settings.remember(a);
        startAndWait();
        TeamProject opened = session.project();

        onEdt(session::close);

        assertNull(session.project());
        assertTrue(settings.lastProject().isEmpty());
        assertEquals(List.of(opened), session.otherProjects());

        onEdt(() -> session.open(a));
        waitFor(() -> session.project() != null);

        assertSame(opened, session.project());
        assertTrue(session.otherProjects().isEmpty());
        assertEquals(TeamOtherProjects.key(a), settings.lastProject().orElseThrow());
    }

    @Test
    void aMissingLastProjectIsSkippedQuietlyOnStart() throws Exception {
        settings.remember(root.resolve("消えたフォルダ"));

        startAndWait();

        assertNull(session.project());
        assertTrue(session.otherProjects().isEmpty());
    }

    @Test
    void reloadShowsWhatOtherMembersWrote() throws Exception {
        Path a = TeamProject.create(root.resolve("a"), "A", "pc-tanaka", "田中").directory();
        settings.remember(a);
        startAndWait();
        TeamProject suzuki = TeamProject.open(a, "pc-suzuki", "鈴木");

        String id = suzuki.createTask(Map.of(TeamField.NAME, "設計"));
        onEdt(session::reload);
        waitFor(() -> events.contains("data"));

        assertTrue(session.project().state().find(id).isPresent());
    }

    @Test
    void conflictCountLeavesOutTheOnesAlreadySeen() throws Exception {
        Path a = root.resolve("a");
        TeamProject tanaka = TeamProject.create(a, "A", "pc-tanaka", "田中");
        TeamProject suzuki = TeamProject.open(a, "pc-suzuki", "鈴木");
        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計"));
        suzuki.reload();
        tanaka.updateTask(id, Map.of(TeamField.NAME, "基本設計"));
        suzuki.updateTask(id, Map.of(TeamField.NAME, "詳細設計"));
        settings.remember(a);

        startAndWait();

        assertEquals(1, session.unacknowledgedConflictCount());
        List<TeamConflictGroup> groups = TeamConflictGroup.unacknowledged(
                session.project().state().conflicts(), Set.of());
        settings.acknowledge(session.project().directory(), groups.get(0).keys());
        assertEquals(0, session.unacknowledgedConflictCount());
    }

    @Test
    void headerShowsPartlySyncedAndTheConflictCount() throws Exception {
        Path a = root.resolve("a");
        TeamProject tanaka = TeamProject.create(a, "A", "pc-tanaka", "田中");
        TeamProject suzuki = TeamProject.open(a, "pc-suzuki", "鈴木");
        String id = tanaka.createTask(Map.of(TeamField.NAME, "設計"));
        suzuki.reload();
        tanaka.updateTask(id, Map.of(TeamField.NAME, "基本設計"));
        suzuki.updateTask(id, Map.of(TeamField.NAME, "詳細設計"));
        Files.writeString(TeamChangeLog.logFile(a, "pc-suzuki"), "{\"v\":1,\"id\":\"x\",\"ta",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        settings.remember(a);
        startAndWait();

        AtomicReference<Component[]> parts = new AtomicReference<>();
        onEdt(() -> parts.set(((JPanel) new ProjectSwitcher(session).component()).getComponents()));
        JButton project = (JButton) parts.get()[0];
        JButton sync = (JButton) parts.get()[2];
        JButton conflicts = (JButton) parts.get()[3];

        assertTrue(project.getText().contains("A"), project.getText());
        assertEquals(AppMessages.get("project.sync.partial", "一部を同期中"), sync.getText());
        assertTrue(conflicts.isVisible());
        assertEquals(AppMessages.format("project.conflicts", "重なった変更 {0}件", 1), conflicts.getText());
    }

    @Test
    void headerShowsPersonalTasksWhenNothingIsOpen() throws Exception {
        startAndWait();

        AtomicReference<Component[]> parts = new AtomicReference<>();
        onEdt(() -> parts.set(((JPanel) new ProjectSwitcher(session).component()).getComponents()));

        assertTrue(((JButton) parts.get()[0]).getText().startsWith(AppMessages.get("project.personal", "自分だけのタスク")));
        assertFalse(parts.get()[1].isVisible());
        assertFalse(parts.get()[2].isVisible());
        assertFalse(parts.get()[3].isVisible());
    }

    private void startAndWait() throws Exception {
        AtomicBoolean ready = new AtomicBoolean();
        onEdt(() -> session.start(() -> ready.set(true)));
        waitFor(ready::get);
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    /** 裏側の列と画面のスレッドの両方が片付くまで待つ。 */
    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            AtomicBoolean met = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> met.set(condition.getAsBoolean()));
            if (met.get()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("10 秒待っても終わりませんでした");
    }

    private static List<String> names(List<TeamProject> projects) {
        return projects.stream().map(TeamProject::name).toList();
    }
}
