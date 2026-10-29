package org.tasknavi.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.tasknavi.DatabaseTestConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @BeforeEach
    void ensureSchema() {
        Database.initialize();
    }

    @Test
    void databaseShouldHaveRequiredTables() throws Exception {
        try (Connection conn = Database.connect()) {
            assertTrue(hasColumn(conn, "tasks", "priority"));
            assertTrue(hasColumn(conn, "tasks", "memo"));
            assertFalse(hasColumn(conn, "tasks", "owner_id"));
            assertFalse(hasColumn(conn, "users", "id"), "ログインをやめたので users は無い");
        }
    }

    @Test
    void upgradingFromLoginVersionKeepsEveryAccountsTasks(@TempDir Path dir) throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dir.resolve("old.db"));
        flyway(dataSource, "5").migrate();
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO users (id, username, password, email) VALUES (1, 'a', 'x', 'a@example.com')");
            stmt.executeUpdate("INSERT INTO users (id, username, password, email) VALUES (2, 'b', 'x', 'b@example.com')");
            stmt.executeUpdate("INSERT INTO tasks (name, owner_id) VALUES ('Aのタスク', 1), ('Bのタスク', 2), ('持ち主なし', NULL)");
        }

        flyway(dataSource, "latest").migrate();

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM tasks ORDER BY id")) {
            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
            assertEquals(List.of("Aのタスク", "Bのタスク", "持ち主なし"), names);
            assertFalse(hasColumn(conn, "tasks", "owner_id"));
        }
    }

    private static Flyway flyway(SQLiteDataSource dataSource, String target) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target(target).load();
    }

    private static boolean hasColumn(Connection conn, String tableName, String columnName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns.contains(columnName);
    }
}
