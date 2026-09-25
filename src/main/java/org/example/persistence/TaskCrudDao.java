package org.example.persistence;

import org.example.AppException;
import org.example.Task;
import org.example.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * tasks テーブルの基本 CRUD。
 */
public final class TaskCrudDao {

    private static final Logger logger = LoggerFactory.getLogger(TaskCrudDao.class);

    static final String SQL_INSERT =
            "INSERT INTO tasks (parent_id, level, order_index, name, assignee, start_date, end_date, status, progress, priority) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_ALL =
            "SELECT * FROM tasks ORDER BY parent_id, order_index, id ASC";
    private static final String SQL_SELECT_ALL_UNORDERED = "SELECT * FROM tasks";
    private static final String SQL_UPDATE =
            "UPDATE tasks SET parent_id = ?, level = ?, order_index = ?, name = ?, assignee = ?, start_date = ?, end_date = ?, "
                    + "status = ?, progress = ?, priority = ? WHERE id = ?";
    private static final String SQL_UPDATE_STATUS = "UPDATE tasks SET status = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM tasks WHERE id = ?";
    private static final String SQL_UPDATE_PROGRESS_STATUS =
            "UPDATE tasks SET progress = ?, status = ? WHERE id = ?";
    private static final String SQL_UPDATE_ORDER_INDEX = "UPDATE tasks SET order_index = ? WHERE id = ?";

    private TaskCrudDao() {
    }

    public static int add(Task task) {
        int generatedId = -1;
        try (Connection conn = DatabaseUtil.getConnection();
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
            return -1;
        }
        return generatedId;
    }

    public static List<Task> getAll() {
        List<Task> taskList = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_SELECT_ALL)) {
            while (rs.next()) {
                taskList.add(TaskRowMapper.fromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("タスク取得エラー", e);
        }
        return taskList;
    }

    public static Task getById(int taskId) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ALL_UNORDERED);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Task task = TaskRowMapper.fromResultSet(rs);
                if (task.getId() == taskId) {
                    return task;
                }
            }
        } catch (SQLException e) {
            logger.error("単一タスク取得エラー", e);
        }
        return null;
    }

    public static void update(Task task) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {
            TaskStatementBinder.bindUpdate(pstmt, task);
            pstmt.executeUpdate();
            logger.info("タスクID: {} を更新しました。", task.getId());
        } catch (SQLException e) {
            logger.error("タスク更新エラー", e);
            throw AppException.database("タスクの更新に失敗しました。", e);
        }
    }

    public static boolean updateStatus(int taskId, String newStatus) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("状態更新エラー", e);
            return false;
        }
    }

    public static void deleteIds(Collection<Integer> targetIds) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            for (Integer id : targetIds) {
                try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("タスク削除エラー", e);
            throw AppException.database("タスクの削除に失敗しました。", e);
        }
    }

    public static void updateProgressAndStatus(Task task) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_PROGRESS_STATUS)) {
            pstmt.setInt(1, task.getProgress());
            pstmt.setString(2, task.getStatusCode());
            pstmt.setInt(3, task.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("親タスク自動更新エラー", e);
        }
    }

    public static void updateOrderIndex(int taskId, int newOrderIndex) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_ORDER_INDEX)) {
            pstmt.setInt(1, newOrderIndex);
            pstmt.setInt(2, taskId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("順序インデックス更新エラー", e);
        }
    }
}
