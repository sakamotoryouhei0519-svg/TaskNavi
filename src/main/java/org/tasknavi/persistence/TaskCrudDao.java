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
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * tasks テーブルの基本 CRUD。
 */
public final class TaskCrudDao {

    private static final Logger logger = LoggerFactory.getLogger(TaskCrudDao.class);

    static final String SQL_INSERT =
            "INSERT INTO tasks (parent_id, level, order_index, name, assignee, start_date, end_date, status, progress, priority, memo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_WITH_ID =
            "INSERT INTO tasks (parent_id, level, order_index, name, assignee, start_date, end_date, status, progress, priority, memo, id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_ALL =
            "SELECT * FROM tasks ORDER BY parent_id, order_index, id ASC";
    private static final String SQL_UPDATE =
            "UPDATE tasks SET parent_id = ?, level = ?, order_index = ?, name = ?, assignee = ?, start_date = ?, end_date = ?, "
                    + "status = ?, progress = ?, priority = ?, memo = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM tasks WHERE id = ?";
    private static final String SQL_UPDATE_PROGRESS_STATUS =
            "UPDATE tasks SET progress = ?, status = ? WHERE id = ?";
    private static final String SQL_UPDATE_ORDER_INDEX =
            "UPDATE tasks SET order_index = ? WHERE id = ?";

    private TaskCrudDao() {
    }

    public static int add(Task task) {
        int generatedId = -1;
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            TaskStatementBinder.bindInsert(pstmt, task);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedId = keys.getInt(1);
                        task.setId(generatedId);
                    }
                }
            }
            logger.info("タスク「{}」を追加しました。", task.getName());
        } catch (SQLException e) {
            logger.error("タスク追加エラー", e);
            throw AppException.database("タスクの追加に失敗しました。", e);
        }
        if (generatedId <= 0) {
            throw AppException.database("タスクの追加に失敗しました。");
        }
        return generatedId;
    }

    public static List<Task> getAll() {
        try (Connection conn = Database.connect()) {
            return getAll(conn);
        } catch (SQLException e) {
            logger.error("タスク取得エラー", e);
            return new ArrayList<>();
        }
    }

    static List<Task> getAll(Connection conn) throws SQLException {
        List<Task> taskList = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ALL)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    taskList.add(TaskRowMapper.fromResultSet(rs));
                }
            }
        }
        return taskList;
    }

    public static Task getById(int taskId) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return TaskRowMapper.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("単一タスク取得エラー", e);
            throw AppException.database("タスクの取得に失敗しました。", e);
        }
        return null;
    }

    public static void update(Task task) {
        updateAll(List.of(task));
    }

    /**
     * 複数タスクを1トランザクションで更新し、親の進捗も同じ接続で反映する。
     * 途中で失敗した場合はすべてロールバックする。
     */
    public static void updateAll(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        try {
            Database.withTransaction(connection -> {
                for (Task task : tasks) {
                    update(connection, task);
                }
                List<Task> allTasks = getAll(connection);
                for (Task task : tasks) {
                    for (Task rolledUp : TaskHierarchySupport.rollupAncestors(task.getId(), allTasks)) {
                        updateProgressAndStatus(connection, rolledUp);
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            logger.error("タスク更新エラー", e);
            throw AppException.database("タスクの更新に失敗しました。", e);
        }
    }

    private static void update(Connection conn, Task task) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {
            TaskStatementBinder.bindUpdate(pstmt, task);
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw AppException.notFound("タスクID: " + task.getId());
            }
            logger.info("タスクID: {} を更新しました。", task.getId());
        }
    }

    private static void updateProgressAndStatus(Connection conn, Task task) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_PROGRESS_STATUS)) {
            pstmt.setInt(1, task.getProgress());
            pstmt.setString(2, task.getStatusCode());
            pstmt.setInt(3, task.getId());
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw AppException.notFound("タスクID: " + task.getId());
            }
        }
    }

    public static void deleteIds(Collection<Integer> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }
        try {
            Database.withTransaction(connection -> {
                try (PreparedStatement pstmt = connection.prepareStatement(SQL_DELETE)) {
                    for (Integer id : targetIds) {
                        pstmt.setInt(1, id);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                return null;
            });
        } catch (SQLException e) {
            logger.error("タスク削除エラー", e);
            throw AppException.database("タスクの削除に失敗しました。", e);
        }
    }

    /** 削除したタスクを元の ID のまま1トランザクションで戻す。1件でも失敗したら全部戻さない。 */
    public static void insertWithIds(Collection<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        try {
            Database.withTransaction(connection -> {
                try (PreparedStatement pstmt = connection.prepareStatement(SQL_INSERT_WITH_ID)) {
                    for (Task task : tasks) {
                        TaskStatementBinder.bindInsert(pstmt, task);
                        pstmt.setInt(TaskStatementBinder.INSERT_PARAM_COUNT + 1, task.getId());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                return null;
            });
        } catch (SQLException e) {
            logger.error("タスク復元エラー", e);
            throw AppException.database("タスクを元に戻せませんでした。", e);
        }
    }

    public static void updateProgressAndStatus(Task task) {
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_PROGRESS_STATUS)) {
            pstmt.setInt(1, task.getProgress());
            pstmt.setString(2, task.getStatusCode());
            pstmt.setInt(3, task.getId());
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw AppException.notFound("タスクID: " + task.getId());
            }
        } catch (SQLException e) {
            logger.error("親タスク自動更新エラー", e);
            throw AppException.database("親タスクの進捗を更新できませんでした。", e);
        }
    }

    public static void updateOrderIndex(int taskId, int newOrderIndex) {
        updateOrderIndexes(Map.of(taskId, newOrderIndex));
    }

    /** 兄弟の並びを1トランザクションで書く。途中失敗なら全部戻す。 */
    public static void updateOrderIndexes(Map<Integer, Integer> orderByTaskId) {
        if (orderByTaskId == null || orderByTaskId.isEmpty()) {
            return;
        }
        try {
            Database.withTransaction(connection -> {
                try (PreparedStatement pstmt = connection.prepareStatement(SQL_UPDATE_ORDER_INDEX)) {
                    for (Map.Entry<Integer, Integer> entry : orderByTaskId.entrySet()) {
                        pstmt.setInt(1, entry.getValue());
                        pstmt.setInt(2, entry.getKey());
                        int updated = pstmt.executeUpdate();
                        if (updated == 0) {
                            throw AppException.notFound("タスクID: " + entry.getKey());
                        }
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            logger.error("順序インデックス更新エラー", e);
            throw AppException.database("タスクの並び替えに失敗しました。", e);
        }
    }
}
