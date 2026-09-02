package org.example;

// --- データベース接続および SQL 操作用ライブラリ ---
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 【データベース接続・初期化管理クラス】
 * このクラスは SQLite データベースに接続し、アプリ起動時に必要なテーブルが
 * あるかどうかを確認して自動生成する役割を持ちます。
 *
 * Java ではデータベースとの接続処理はたいてい「Connection」「Statement」などの
 * オブジェクトを使います。ここではその処理を一箇所にまとめて、他のクラスが
 * 簡単に使えるようにしています。
 */
public class Database {

    // SQLite のデータベースファイル名
    // jdbc:sqlite:xxx のような形式で、ローカルファイルの DB へ接続します。
    private static final String DB_URL = "jdbc:sqlite:tasknavi.db";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /**
     * 【データベース接続取得】
     * ここで接続を取得してから、テーブルがなければ自動生成します。
     *
     * Java では DB 接続を使ったら必ず close する必要がありますが、
     * try-with-resources などで自動クローズする設計が一般的です。
     *
     * @return Connection オブジェクト
     */
    public static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // 接続した直後に初期化を行うことで、起動時に DB を確実に用意できます。
        initialize(conn);
        return conn;
    }

    /**
     * 【表を作る処理】
     * users テーブルと tasks テーブルが存在しない場合に SQL で作成します。
     *
     * SQL の CREATE TABLE IF NOT EXISTS は「すでにある場合は作らない」という
     * 安全策です。初回起動時にだけ必要な処理です。
     *
     * @param conn データベース接続オブジェクト
     */
    public static void initialize(Connection conn) {
        if (INITIALIZED.get() && isPrimaryDatabase(conn)) {
            return;
        }

        if (!INITIALIZED.compareAndSet(false, true) && isPrimaryDatabase(conn)) {
            return;
        }

        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            // 1. users テーブルの作成 SQL
            // username は重複禁止、password はハッシュ化済み文字列、role は権限情報
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL UNIQUE, "
                    + "password TEXT NOT NULL, "
                    + "email TEXT NOT NULL UNIQUE, "
                    + "role TEXT DEFAULT 'USER'"
                    + ");";

            // 2. tasks テーブルの作成 SQL
            // タスクの親子関係や開始日・終了日・進捗率・優先度を持ちます。
            String createTasksTable = "CREATE TABLE IF NOT EXISTS tasks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "parent_id INTEGER REFERENCES tasks(id) ON DELETE CASCADE, "
                    + "level INTEGER NOT NULL DEFAULT 1 CHECK (level >= 1), "
                    + "name TEXT NOT NULL, "
                    + "assignee TEXT, "
                    + "start_date TEXT, "
                    + "end_date TEXT, "
                    + "progress INTEGER NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100), "
                    + "status TEXT NOT NULL DEFAULT '未着手' CHECK (status IN ('未着手', '進行中', '完了')), "
                    + "priority TEXT NOT NULL DEFAULT '中', "
                    + "CHECK ((parent_id IS NULL AND level = 1) OR (parent_id IS NOT NULL AND level BETWEEN 2 AND 3))"
                    + ");";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsersTable);
                stmt.execute(createTasksTable);
            }

            ensureColumnExists(conn, "users", "role", "TEXT DEFAULT 'USER'");
            ensureColumnExists(conn, "tasks", "priority", "TEXT DEFAULT '中'");
            ensureTaskTableIntegrity(conn);
        } catch (SQLException e) {
            INITIALIZED.set(false);
            System.err.println("DB初期化エラー: " + e.getMessage());
        }
    }

    private static void ensureTaskTableIntegrity(Connection conn) throws SQLException {
        String tableSql = "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'tasks'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(tableSql)) {
            if (rs.next()) {
                String ddl = rs.getString("sql");
                if (ddl == null || (!ddl.contains("REFERENCES tasks(id)") && !ddl.contains("CHECK (parent_id IS NULL"))) {
                    migrateTasksTable(conn);
                }
            }
        }
    }

    private static void migrateTasksTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE tasks RENAME TO tasks_old");
        }

        String createTasksTable = "CREATE TABLE tasks ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "parent_id INTEGER REFERENCES tasks(id) ON DELETE CASCADE, "
                + "level INTEGER NOT NULL DEFAULT 1 CHECK (level >= 1), "
                + "name TEXT NOT NULL, "
                + "assignee TEXT, "
                + "start_date TEXT, "
                + "end_date TEXT, "
                + "progress INTEGER NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100), "
                + "status TEXT NOT NULL DEFAULT '未着手' CHECK (status IN ('未着手', '進行中', '完了')), "
                + "priority TEXT NOT NULL DEFAULT '中', "
                + "CHECK ((parent_id IS NULL AND level = 1) OR (parent_id IS NOT NULL AND level BETWEEN 2 AND 3))"
                + ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTasksTable);
            stmt.execute("INSERT INTO tasks (id, parent_id, level, name, assignee, start_date, end_date, progress, status, priority) "
                    + "SELECT id, parent_id, level, name, assignee, start_date, end_date, progress, status, priority FROM tasks_old");
            stmt.execute("DROP TABLE tasks_old");
        }
    }

    /**
     * 既存テーブルに指定のカラムが存在しない場合のみ追加します。
     * これにより、旧バージョンのユーザー向けデータを壊さずに新しいカラムを安全に追加できます。
     */
    private static void ensureColumnExists(Connection conn, String tableName, String columnName, String columnDefinition)
            throws SQLException {
        boolean exists = false;
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String currentColumn = rs.getString("COLUMN_NAME");
                if (columnName.equalsIgnoreCase(currentColumn)) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    private static boolean isPrimaryDatabase(Connection conn) {
        try {
            String url = conn.getMetaData().getURL();
            return url == null || url.equals(DB_URL) || url.endsWith("tasknavi.db");
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 【引数なしの初期化メソッド】
     * 互換性のために残されたメソッドです。
     * 実際には connect() の中で initialize(conn) が呼ばれます。
     */
    public static void initialize() {
        try (Connection conn = connect()) {
            // connect() 内で initialize(conn) が呼ばれているので、
            // このメソッドでは何もせずに終わっても問題ありません。
            System.out.println("データベースとテーブルの初期化が完了しました。");
        } catch (SQLException e) {
            System.err.println("DB初期化エラー: " + e.getMessage());
        }
    }
}