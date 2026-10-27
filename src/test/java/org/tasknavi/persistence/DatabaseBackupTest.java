package org.tasknavi.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBackupTest {

    @TempDir
    Path tempDir;

    @Test
    void createsOneBackupPerDay() throws Exception {
        Path db = Files.writeString(tempDir.resolve("tasknavi.db"), "v1");
        Path backups = tempDir.resolve("backups");
        LocalDate today = LocalDate.of(2026, 10, 24);

        Optional<Path> first = DatabaseBackup.backupDaily(db, backups, today, 7);
        Files.writeString(db, "v2");
        Optional<Path> second = DatabaseBackup.backupDaily(db, backups, today, 7);

        assertTrue(first.isPresent());
        assertEquals("tasknavi-2026-10-24.db", first.get().getFileName().toString());
        assertEquals("v1", Files.readString(first.get()));
        assertFalse(second.isPresent());
    }

    @Test
    void keepsOnlyNewestBackups() throws Exception {
        Path db = Files.writeString(tempDir.resolve("tasknavi.db"), "data");
        Path backups = tempDir.resolve("backups");

        for (int day = 1; day <= 5; day++) {
            DatabaseBackup.backupDaily(db, backups, LocalDate.of(2026, 10, day), 3);
        }

        List<Path> remaining = DatabaseBackup.listBackups(backups);
        assertEquals(3, remaining.size());
        assertEquals("tasknavi-2026-10-05.db", remaining.get(0).getFileName().toString());
        assertEquals("tasknavi-2026-10-03.db", remaining.get(2).getFileName().toString());
    }

    @Test
    void skipsWhenDatabaseDoesNotExist() throws Exception {
        Optional<Path> result = DatabaseBackup.backupDaily(
                tempDir.resolve("missing.db"), tempDir.resolve("backups"), LocalDate.now(), 7);
        assertFalse(result.isPresent());
    }

    @Test
    void copiesLegacyDatabaseOnlyOnce() throws Exception {
        Path legacy = Files.writeString(tempDir.resolve("old.db"), "old");
        Path target = tempDir.resolve("data").resolve("tasknavi.db");

        assertTrue(DatabaseBackup.copyLegacyDatabaseIfNeeded(legacy, target));
        assertEquals("old", Files.readString(target));

        Files.writeString(target, "new");
        assertFalse(DatabaseBackup.copyLegacyDatabaseIfNeeded(legacy, target));
        assertEquals("new", Files.readString(target));
    }
}
