package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * テストが本番の tasknavi.db を壊さないよう、一時 DB に切り替える。
 */
public final class DatabaseTestConfig {
    private static Path currentTempDb;

    private DatabaseTestConfig() {
    }

    public static synchronized void useIsolatedDatabase() {
        try {
            Path temp = Files.createTempFile("tasknavi-test-" + UUID.randomUUID(), ".db");
            temp.toFile().deleteOnExit();
            currentTempDb = temp;
            Database.setJdbcUrl("jdbc:sqlite:" + temp.toAbsolutePath());
            Database.initialize();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create isolated test database", e);
        }
    }

    public static void clearTasksTable() {
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM tasks");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear tasks table", e);
        }
    }

    public static Path getCurrentTempDb() {
        return currentTempDb;
    }
}
