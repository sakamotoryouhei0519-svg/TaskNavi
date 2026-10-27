package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamChangeLogTest {

    @TempDir
    Path dir;

    @Test
    void roundTripKeepsNullsAndJapanese() throws Exception {
        Map<TeamField, String> values = new EnumMap<>(TeamField.class);
        values.put(TeamField.NAME, "設計レビュー, \"第1回\"\n2行目");
        values.put(TeamField.END, null);
        TeamEvent event = new TeamEvent("e1", "t1", TeamEvent.Op.UPDATE, 7, "pc-a", "田中",
                Instant.parse("2026-10-24T01:02:03Z"), values, Map.of(TeamField.NAME, "e0"), "e0");

        TeamChangeLog.append(dir, "pc-a", event);
        TeamEvent read = TeamChangeLog.readAll(dir).events().get(0);

        assertEquals(event, read);
        assertTrue(read.values().containsKey(TeamField.END));
        assertNull(read.values().get(TeamField.END));
    }

    @Test
    void truncatedLineIsSkippedAndNextAppendStartsOnNewLine() throws Exception {
        TeamChangeLog.append(dir, "pc-a", event("e1", 1));
        Path file = TeamChangeLog.logFile(dir, "pc-a");
        Files.writeString(file, "{\"v\":1,\"id\":\"e2\",\"ta", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        TeamChangeLog.ReadResult before = TeamChangeLog.readAll(dir);
        assertEquals(1, before.events().size());
        assertEquals(1, before.unreadableLines());

        TeamChangeLog.append(dir, "pc-a", event("e3", 2));
        TeamChangeLog.ReadResult after = TeamChangeLog.readAll(dir);
        assertEquals(2, after.events().size());
        assertEquals("e3", after.events().get(1).eventId());
        assertEquals(1, after.unreadableLines());
    }

    @Test
    void recordsFromNewerFormatAreCountedApartFromBrokenLines() throws Exception {
        Path file = TeamChangeLog.logFile(dir, "pc-b");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"v\":99,\"id\":\"x\",\"task\":\"t\",\"op\":\"CREATE\",\"clock\":1,\"device\":\"pc-b\"}\n"
                + "{\"v\":1,\"id\":\"y\",\"ta\n");

        TeamChangeLog.ReadResult result = TeamChangeLog.readAll(dir);

        assertEquals(0, result.events().size());
        assertEquals(1, result.newerFormatLines());
        assertEquals(1, result.unreadableLines());
    }

    @Test
    void refusesOtherDevicesRecordsAndUnsafeIds() {
        assertThrows(IllegalArgumentException.class,
                () -> TeamChangeLog.append(dir, "pc-a", event("e1", 1, "pc-b")));
        assertThrows(IllegalArgumentException.class, () -> TeamChangeLog.logFile(dir, "../evil"));
        assertThrows(IllegalArgumentException.class, () -> TeamChangeLog.logFile(dir, "田中"));
    }

    @Test
    void duplicatedFileFromSyncToolIsCountedOnce() throws Exception {
        TeamChangeLog.append(dir, "pc-a", new TeamEvent("e1", "t1", TeamEvent.Op.CREATE, 1, "pc-a", "田中",
                Instant.EPOCH, Map.of(TeamField.NAME, "A"), Map.of(), null));
        Path file = TeamChangeLog.logFile(dir, "pc-a");
        Files.copy(file, file.resolveSibling("pc-a (1).jsonl"));

        TeamChangeLog.ReadResult result = TeamChangeLog.readAll(dir);
        TeamState state = TeamReplay.replay(result.events());

        assertEquals(2, result.events().size());
        assertEquals(1, state.tasks().size());
    }

    private static TeamEvent event(String id, long clock) {
        return event(id, clock, "pc-a");
    }

    private static TeamEvent event(String id, long clock, String device) {
        return new TeamEvent(id, "t1", TeamEvent.Op.UPDATE, clock, device, "田中", Instant.EPOCH,
                Map.of(TeamField.PROGRESS, String.valueOf(clock)), Map.of(), null);
    }
}
