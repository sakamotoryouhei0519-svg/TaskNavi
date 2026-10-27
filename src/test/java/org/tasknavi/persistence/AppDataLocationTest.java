package org.tasknavi.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppDataLocationTest {

    @Test
    void propertyWinsOverEverything() {
        Path dir = AppDataLocation.resolveDataDirectory("C:/custom", "C:/env", "C:/appdata", "C:/home");
        assertEquals(Path.of("C:/custom").toAbsolutePath(), dir);
    }

    @Test
    void envIsUsedWhenPropertyIsBlank() {
        Path dir = AppDataLocation.resolveDataDirectory(" ", "C:/env", "C:/appdata", "C:/home");
        assertEquals(Path.of("C:/env").toAbsolutePath(), dir);
    }

    @Test
    void appDataIsUsedOnWindows() {
        Path dir = AppDataLocation.resolveDataDirectory(null, null, "C:/appdata", "C:/home");
        assertEquals(Path.of("C:/appdata", "TaskNavi").toAbsolutePath(), dir);
    }

    @Test
    void homeIsUsedWhenNothingElseIsSet() {
        Path dir = AppDataLocation.resolveDataDirectory(null, "", null, "/home/me");
        assertEquals(Path.of("/home/me", ".tasknavi").toAbsolutePath(), dir);
    }

    @Test
    void logsLiveNextToTheDatabase() {
        assertEquals(AppDataLocation.dataDirectory().resolve("logs"), AppDataLocation.logDirectory());
    }

    @Test
    void logbackPathUsesForwardSlashes() {
        assertEquals("C:/Users/me/AppData/Roaming/TaskNavi/logs",
                LogDirectoryDefiner.toLogbackPath("C:\\Users\\me\\AppData\\Roaming\\TaskNavi\\logs"));
    }
}
