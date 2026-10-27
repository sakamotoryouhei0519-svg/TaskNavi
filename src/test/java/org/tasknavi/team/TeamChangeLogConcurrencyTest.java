package org.tasknavi.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 同じ PC で TaskNavi を 2 つ起動したときを、別々の JVM で再現する。 */
class TeamChangeLogConcurrencyTest {

    private static final int LINES = 150;

    @TempDir
    Path dir;

    @Test
    void twoProcessesOnOnePcDoNotOverwriteEachOther() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<Process> processes = new ArrayList<>();
        for (String name : List.of("a", "b")) {
            processes.add(new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                    Writer.class.getName(), dir.toString(), name)
                    .redirectErrorStream(true)
                    .redirectOutput(new File(dir.toFile(), name + ".out"))
                    .start());
        }
        for (Process process : processes) {
            assertTrue(process.waitFor(60, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue());
        }

        TeamChangeLog.ReadResult result = TeamChangeLog.readAll(dir);
        assertEquals(0, result.unreadableLines());
        assertEquals(LINES * 2, result.events().size());
    }

    /** 子プロセスの入口。 */
    public static final class Writer {
        public static void main(String[] args) throws Exception {
            Path projectDir = Path.of(args[0]);
            for (int i = 0; i < LINES; i++) {
                TeamChangeLog.append(projectDir, "pc-same", new TeamEvent(args[1] + "-" + i, "t", TeamEvent.Op.UPDATE,
                        i, "pc-same", "田中", Instant.now(), Map.of(TeamField.NAME, "x".repeat(200)), Map.of(), null));
            }
        }
    }
}
