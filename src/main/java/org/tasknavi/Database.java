package org.tasknavi;

import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:tasknavi.db";
    private static final Object MIGRATION_LOCK = new Object();

    private static volatile String dbUrl = DEFAULT_DB_URL;
    private static volatile boolean migrated = false;

    private Database() {
    }

    /**
     * テスト専用: JDBC URL を切り替え、マイグレーション状態をリセットします。
     */
    public static void setJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("jdbcUrl must not be blank");
        }
        synchronized (MIGRATION_LOCK) {
            dbUrl = jdbcUrl.trim();
            migrated = false;
        }
    }

    public static String getJdbcUrl() {
        return dbUrl;
    }

    public static void resetToDefaultUrl() {
        setJdbcUrl(DEFAULT_DB_URL);
    }

    public static Connection connect() throws SQLException {
        ensureMigrated();

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(dbUrl);
        Connection conn = dataSource.getConnection();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static void ensureMigrated() {
        if (migrated) {
            return;
        }
        synchronized (MIGRATION_LOCK) {
            if (migrated) {
                return;
            }
            runMigration();
            migrated = true;
        }
    }

    private static void runMigration() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(dbUrl);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    public static void initialize() {
        try (Connection conn = connect()) {
            System.out.println("データベースの準備が完了しました。");
        } catch (SQLException e) {
            System.err.println("DB初期化エラー: " + e.getMessage());
        }
    }
}
