package org.tasknavi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

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
            assertTrue(hasColumn(conn, "users", "role"));
            assertTrue(hasColumn(conn, "tasks", "priority"));
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns.contains(columnName);
    }
}
