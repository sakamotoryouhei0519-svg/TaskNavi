package org.tasknavi.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 【データベース接続ユーティリティ】
 * 既存コードとの互換性を保つための薄いラッパーです。
 * 実際の初期化/マイグレーション処理は org.tasknavi.Database に集約します。
 */
public class DatabaseUtil {

    @FunctionalInterface
    public interface SqlCallback<T> {
        T execute(Connection connection) throws SQLException;
    }

    /**
     * データベース接続を取得し、必要に応じてスキーマを準備します。
     */
    public static Connection getConnection() throws SQLException {
        return org.tasknavi.Database.connect();
    }

    public static <T> T withTransaction(SqlCallback<T> callback) throws SQLException {
        try (Connection connection = getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = callback.execute(connection);
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

    /**
     * 既存コード互換のため残す。
     */
    public static void initializeDatabase() {
        org.tasknavi.Database.initialize();
    }
}