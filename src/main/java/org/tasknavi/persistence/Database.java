package org.tasknavi.persistence;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * 自分だけのタスクを入れる SQLite の入口。接続・テーブルの準備（Flyway）・トランザクションをまとめる。
 */
public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static final Path DEFAULT_DB_FILE = AppDataLocation.databaseFile();
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:" + DEFAULT_DB_FILE;
    private static final Object MIGRATION_LOCK = new Object();

    private static volatile String dbUrl = DEFAULT_DB_URL;
    private static volatile boolean migrated = false;
    private static volatile boolean createdNow = false;

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

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    /** 1 つのトランザクションで実行する。途中で失敗したら全部取り消す。 */
    public static <T> T withTransaction(SqlWork<T> work) throws SQLException {
        try (Connection connection = connect()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
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

    /**
     * 起動時に1回呼ぶ。DB を開く前に、古い場所からの引き継ぎと1日1回のバックアップを行う。
     * 失敗しても起動は止めない。
     */
    public static void prepareOnStartup() {
        Path legacyFile = Path.of(AppDataLocation.DB_FILE_NAME).toAbsolutePath();
        try {
            if (DatabaseBackup.copyLegacyDatabaseIfNeeded(legacyFile, DEFAULT_DB_FILE)) {
                logger.info("以前の DB を引き継ぎました: {} -> {}", legacyFile, DEFAULT_DB_FILE);
            }
        } catch (IOException e) {
            logger.warn("以前の DB を引き継げませんでした: {}", legacyFile, e);
        }
        try {
            DatabaseBackup.backupDaily(DEFAULT_DB_FILE, AppDataLocation.backupDirectory(),
                            LocalDate.now(), DatabaseBackup.DEFAULT_KEEP_COUNT)
                    .ifPresent(path -> logger.info("DB のバックアップを作成しました: {}", path));
        } catch (IOException e) {
            logger.warn("DB のバックアップに失敗しました", e);
        }
        logger.info("DB の場所: {}", DEFAULT_DB_FILE);
    }

    private static void ensureDefaultDirectoryExists() {
        if (!DEFAULT_DB_URL.equals(dbUrl)) {
            return;
        }
        try {
            Files.createDirectories(DEFAULT_DB_FILE.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("DB のフォルダを作成できません: " + DEFAULT_DB_FILE.getParent(), e);
        }
    }

    private static void runMigration() {
        ensureDefaultDirectoryExists();
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(dbUrl);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        createdNow = flyway.info().applied().length == 0;
        flyway.migrate();
    }

    /**
     * 今回の起動でデータベースを新しく作ったなら、1回だけ true を返す。
     * 見本を入れるのを「初めて使うとき」だけにするため。見本を全部消した人が次に起動しても、また入らない。
     */
    public static boolean takeCreatedNow() {
        ensureMigrated();
        synchronized (MIGRATION_LOCK) {
            boolean result = createdNow;
            createdNow = false;
            return result;
        }
    }

    public static void initialize() {
        try (Connection conn = connect()) {
            logger.info("データベースの準備が完了しました。");
        } catch (SQLException e) {
            logger.error("DB初期化エラー", e);
        }
    }
}
