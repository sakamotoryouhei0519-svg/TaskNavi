package org.tasknavi.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppException;
import org.tasknavi.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * タスクの一括取り込み（1 トランザクション）。
 * 取り込むタスクの ID・親 ID はファイル内の番号なので、親から順に入れて、振られた ID で子の親 ID を付け替える。
 */
public final class TaskImportDao {

    private static final Logger logger = LoggerFactory.getLogger(TaskImportDao.class);
    private static final String SQL_DELETE_ALL = "DELETE FROM tasks";
    private static final String SQL_NEXT_ROOT_ORDER =
            "SELECT COALESCE(MAX(order_index) + 1, 0) FROM tasks WHERE parent_id IS NULL";

    private TaskImportDao() {
    }

    /** 呼ぶ前に {@code tasks} の中だけで親子関係が成り立っていることを確かめておくこと。 */
    public static void importTasks(List<Task> tasks, boolean replaceExisting) {
        List<Task> parentsFirst = new ArrayList<>(tasks);
        parentsFirst.sort(Comparator.comparingInt(Task::getLevel));

        try (Connection conn = Database.connect()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (replaceExisting) {
                    try (PreparedStatement deleteStmt = conn.prepareStatement(SQL_DELETE_ALL)) {
                        deleteStmt.executeUpdate();
                    }
                }
                int rootOrderOffset = nextRootOrder(conn);
                insertRemappingParents(conn, parentsFirst, rootOrderOffset);
                conn.commit();
                logger.info("タスクを取り込みました。件数={}, 置き換え={}", tasks.size(), replaceExisting);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            logger.error("インポート処理エラー", e);
            throw AppException.database("タスクのインポートに失敗しました。", e);
        }
    }

    private static int nextRootOrder(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_NEXT_ROOT_ORDER)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void insertRemappingParents(Connection conn, List<Task> parentsFirst, int rootOrderOffset) throws SQLException {
        Map<Integer, Integer> newIdByImportId = new HashMap<>();
        try (PreparedStatement insertStmt =
                     conn.prepareStatement(TaskCrudDao.SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            for (Task source : parentsFirst) {
                Task row = source.copy();
                Integer importParentId = source.getParentId();
                if (importParentId == null || importParentId == 0) {
                    row.setParentId(null);
                    row.setOrderIndex(rootOrderOffset + source.getOrderIndex());
                } else {
                    Integer newParentId = newIdByImportId.get(importParentId);
                    if (newParentId == null) {
                        throw AppException.businessRule(
                                "取り込み対象に親がありません: parentId=" + importParentId,
                                "「" + source.getName() + "」の親タスクが取り込むデータの中にありません。");
                    }
                    row.setParentId(newParentId);
                }
                TaskStatementBinder.bindInsert(insertStmt, row);
                insertStmt.executeUpdate();
                try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("取り込んだタスクの ID を取得できませんでした。");
                    }
                    newIdByImportId.put(source.getId(), keys.getInt(1));
                }
            }
        }
    }
}
