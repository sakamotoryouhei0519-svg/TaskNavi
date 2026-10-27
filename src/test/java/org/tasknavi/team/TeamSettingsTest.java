package org.tasknavi.team;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamSettingsTest {

    private Preferences prefs;
    private TeamSettings settings;

    @BeforeEach
    void setUp() {
        prefs = Preferences.userRoot().node("tasknavi-test/" + UUID.randomUUID());
        settings = new TeamSettings(prefs);
    }

    @AfterEach
    void tearDown() throws Exception {
        prefs.removeNode();
    }

    @Test
    void deviceIdIsSafeForFileNamesAndStaysTheSame() {
        String id = settings.deviceId();

        assertTrue(TeamChangeLog.isValidDeviceId(id), id);
        assertEquals(id, settings.deviceId());
        assertEquals(id, new TeamSettings(prefs).deviceId());
    }

    @Test
    void deviceIdDropsCharactersThatCannotBeFileNames() {
        assertTrue(TeamSettings.newDeviceId("Taro.Yamada", new Random(1)).matches("taroyamada-[0-9a-f]{6}"));
        assertTrue(TeamSettings.newDeviceId("山田", new Random(1)).matches("pc-[0-9a-f]{6}"));
        assertTrue(TeamSettings.newDeviceId(null, new Random(1)).matches("pc-[0-9a-f]{6}"));
    }

    @Test
    void recentProjectsKeepNewestFirstWithoutDuplicates() {
        Path a = Path.of("C:\\share\\A").toAbsolutePath();
        Path b = Path.of("C:\\share\\B").toAbsolutePath();
        settings.remember(a);
        settings.remember(b);
        settings.remember(a);

        assertEquals(java.util.List.of(a, b), settings.recentProjects());
        assertEquals(a, settings.lastProject().orElseThrow());

        settings.forget(a);
        assertEquals(java.util.List.of(b), settings.recentProjects());
        assertTrue(settings.lastProject().isEmpty());
    }

    @Test
    void recentProjectsAreCapped() {
        for (int i = 0; i < TeamSettings.MAX_RECENT + 3; i++) {
            settings.remember(Path.of("C:\\share\\P" + i));
        }
        assertEquals(TeamSettings.MAX_RECENT, settings.recentProjects().size());
        assertEquals(Path.of("C:\\share\\P" + (TeamSettings.MAX_RECENT + 2)).toAbsolutePath(),
                settings.recentProjects().get(0));
    }

    @Test
    void acknowledgedConflictsAreKeptPerProject() {
        Path a = Path.of("C:\\share\\A");
        Path b = Path.of("C:\\share\\B");
        settings.acknowledge(a, java.util.List.of("e1:1a", "e2:2b"));

        assertEquals(java.util.Set.of("e1:1a", "e2:2b"), new TeamSettings(prefs).acknowledgedConflicts(a));
        assertTrue(settings.acknowledgedConflicts(b).isEmpty());
    }

    @Test
    void launchArgumentSelectsASeparateProfile() {
        try {
            TeamSettings.applyLaunchArguments(new String[]{"--other", "--team-profile=test2"});
            assertEquals("test2", TeamSettings.profile());
            System.setProperty("tasknavi.team.profile", "../悪い");
            assertEquals("", TeamSettings.profile());
        } finally {
            System.clearProperty("tasknavi.team.profile");
        }
        assertEquals("", TeamSettings.profile());
    }

    @Test
    void displayNameIsTrimmed() {
        assertEquals("", settings.displayName());
        settings.setDisplayName("  田中 ");
        assertEquals("田中", settings.displayName());
    }
}
