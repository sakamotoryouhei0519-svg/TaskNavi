package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamFilesTest {

    @TempDir
    Path dir;

    @Test
    void writeAtomicallyReplacesWholeFileAndLeavesNoTemporaryFile() throws Exception {
        Path file = dir.resolve("a.json");
        Files.writeString(file, "{\"name\":\"とても長い古い名前の中身\"}", StandardCharsets.UTF_8);

        TeamFiles.writeAtomically(file, "{\"name\":\"新\"}");

        assertEquals("{\"name\":\"新\"}", Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(Files.exists(dir.resolve("a.json.tmp")));
    }

    @Test
    void readTextRefusesFilesOverTheLimit() throws Exception {
        Path file = dir.resolve("big.txt");
        Files.writeString(file, "12345", StandardCharsets.UTF_8);

        assertEquals("12345", TeamFiles.readText(file, 5));
        assertThrows(TeamFiles.TooLargeException.class, () -> TeamFiles.readText(file, 4));
    }

    @Test
    void tooLargeLogFileIsSkippedAndNamedWhileOthersAreRead() throws Exception {
        TeamChangeLog.append(dir, "pc-a", event("e1", "pc-a"));
        Path big = TeamChangeLog.logFile(dir, "pc-b");
        Files.writeString(big, "x".repeat(5000), StandardCharsets.UTF_8);

        TeamChangeLog.ReadResult result = TeamChangeLog.readAll(dir, 1000);

        assertEquals(1, result.events().size());
        assertEquals(List.of("pc-b.jsonl"), result.tooLargeFiles());
        assertEquals(0, result.unreadableLines());
    }

    @Test
    void tooLargeMemberFileIsSkipped() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneOffset.UTC);
        TeamMembers.register(dir, List.of(), "pc-a", "田中", "1.2.0", clock);
        Path big = dir.resolve(TeamProject.MEMBERS_DIR).resolve("pc-b.json");
        Files.writeString(big, "{\"name\":\"" + "x".repeat((int) TeamFiles.MAX_JSON_BYTES) + "\"}",
                StandardCharsets.UTF_8);

        List<TeamMember> members = TeamMembers.read(dir);

        assertEquals(1, members.size());
        assertEquals("田中", members.get(0).name());
        assertFalse(Files.exists(dir.resolve(TeamProject.MEMBERS_DIR).resolve("pc-a.json.tmp")));
    }

    private static TeamEvent event(String id, String device) {
        return new TeamEvent(id, "t1", TeamEvent.Op.CREATE, 1, device, "田中", Instant.EPOCH,
                Map.of(TeamField.NAME, "A"), Map.of(), null);
    }
}
