package org.tasknavi.persistence;

import org.tasknavi.AppException;
import org.tasknavi.Task;
import org.tasknavi.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * タスク CSV インポート用トランザクション。
 */
public final class TaskImportDao {

    private static final Logger logger = LoggerFactory.getLogger(TaskImportDao.class);
    private static final String SQL_DELETE_ALL = "DELETE FROM tasks";

    private TaskImportDao() {
    }

    public static void importTasks(List<Task> tasks, boolean replaceExisting, Consumer<Task> validate) {
        if (tasks == null) {
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (replaceExisting) {
                    try (PreparedStatement deleteStmt = conn.prepareStatement(SQL_DELETE_ALL)) {
                        deleteStmt.executeUpdate();
                    }
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(TaskCrudDao.SQL_INSERT)) {
                    for (Task task : tasks) {
                        if (task == null) {
                            continue;
                        }
                        validate.accept(task);
                        task.setId(0);
                        TaskStatementBinder.bindInsert(insertStmt, task);
                        insertStmt.addBatch();
                    }
                    insertStmt.executeBatch();
                }
                conn.commit();
            } catch (AppException e) {
                conn.rollback();
                logger.error("インポート入力エラー", e);
                throw AppException.businessRule("インポートデータに不正な値があります。", e.getUserMessage());
            } catch (SQLException e) {
                conn.rollback();
                throw AppException.database("タスクのインポートに失敗しました。", e);
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (AppException e) {
            logger.error("インポート処理エラー", e);
            throw e;
        } catch (SQLException e) {
            logger.error("インポート処理エラー", e);
            throw AppException.database("タスクのインポートに失敗しました。", e);
        }
    }
}
