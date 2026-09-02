package org.example;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @Test
    void initializeShouldCreateRequiredTablesAndColumns() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            Database.initialize(conn);

            assertTrue(hasColumn(conn, "users", "role"));
            assertTrue(hasColumn(conn, "tasks", "priority"));
        }
    }

    @Test
    void initializeShouldAddMissingColumnsToLegacyTables() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, email TEXT NOT NULL UNIQUE);");
                stmt.execute("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, parent_id INTEGER, level INTEGER DEFAULT 1, name TEXT, assignee TEXT, start_date TEXT, end_date TEXT, progress INTEGER DEFAULT 0, status TEXT DEFAULT '未着手');");
            }

            Database.initialize(conn);

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
