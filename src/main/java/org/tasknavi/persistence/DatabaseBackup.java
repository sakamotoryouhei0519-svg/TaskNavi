package org.tasknavi.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 1日1回、データベースファイルを丸ごとコピーして残す。
 * DB を開く前（起動直後）に呼ぶこと。開いている最中のコピーは中身が揃わないことがある。
 */
public final class DatabaseBackup {
    public static final int DEFAULT_KEEP_COUNT = 7;
    private static final String PREFIX = "tasknavi-";
    private static final String SUFFIX = ".db";

    private DatabaseBackup() {
    }

    /**
     * 今日の分がまだなければ作り、新しい順に {@code keepCount} 件を残して古いものを消す。
     *
     * @return 作ったバックアップ。DB がない、または今日の分が既にあるときは空
     */
    public static Optional<Path> backupDaily(Path databaseFile, Path backupDir, LocalDate today, int keepCount)
            throws IOException {
        if (!Files.isRegularFile(databaseFile)) {
            return Optional.empty();
        }
        Files.createDirectories(backupDir);
        Path target = backupDir.resolve(PREFIX + today + SUFFIX);
        Optional<Path> created = Optional.empty();
        if (!Files.exists(target)) {
            Path temp = backupDir.resolve(target.getFileName() + ".tmp");
            Files.copy(databaseFile, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            created = Optional.of(target);
        }
        pruneOldBackups(backupDir, keepCount);
        return created;
    }

    /** 起動したフォルダに古い DB だけがあるとき、新しい場所へ1回だけコピーする。 */
    public static boolean copyLegacyDatabaseIfNeeded(Path legacyFile, Path databaseFile) throws IOException {
        if (Files.exists(databaseFile) || !Files.isRegularFile(legacyFile)) {
            return false;
        }
        if (legacyFile.toAbsolutePath().normalize().equals(databaseFile.toAbsolutePath().normalize())) {
            return false;
        }
        Files.createDirectories(databaseFile.toAbsolutePath().getParent());
        Files.copy(legacyFile, databaseFile);
        return true;
    }

    public static List<Path> listBackups(Path backupDir) throws IOException {
        if (!Files.isDirectory(backupDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(backupDir)) {
            List<Path> backups = new ArrayList<>(files
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(PREFIX) && name.endsWith(SUFFIX);
                    })
                    .toList());
            backups.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
            return backups;
        }
    }

    private static void pruneOldBackups(Path backupDir, int keepCount) throws IOException {
        List<Path> backups = listBackups(backupDir);
        for (int i = keepCount; i < backups.size(); i++) {
            Files.deleteIfExists(backups.get(i));
        }
    }
}
