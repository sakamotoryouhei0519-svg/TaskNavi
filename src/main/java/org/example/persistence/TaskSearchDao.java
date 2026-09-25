package org.example.persistence;

import org.example.AppMessages;
import org.example.Task;
import org.example.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * tasks テーブルの検索・フィルタ（DB 側）。
 */
public final class TaskSearchDao {

    private static final Logger logger = LoggerFactory.getLogger(TaskSearchDao.class);

    private static final String SQL_SEARCH_BY_NAME =
            "SELECT * FROM tasks WHERE name LIKE ? ORDER BY parent_id, order_index, id ASC";
    private static final String SQL_SEARCH_BY_ASSIGNEE =
            "SELECT * FROM tasks WHERE assignee = ? ORDER BY parent_id, order_index, id ASC";
    private static final String SQL_FILTER_BY_STATUS =
            "SELECT * FROM tasks WHERE status = ? ORDER BY parent_id, order_index, id ASC";
    private static final String SQL_FILTER_BY_DATE_RANGE =
            "SELECT * FROM tasks WHERE (start_date >= ? AND start_date <= ?) OR (end_date >= ? AND end_date <= ?) "
                    + "ORDER BY parent_id, order_index, id ASC";

    private final Supplier<List<Task>> allTasksSupplier;

    public TaskSearchDao(Supplier<List<Task>> allTasksSupplier) {
        this.allTasksSupplier = allTasksSupplier;
    }

    public List<Task> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return allTasksSupplier.get();
        }
        return query(SQL_SEARCH_BY_NAME, "名前検索エラー", ps -> ps.setString(1, "%" + keyword.toLowerCase() + "%"));
    }

    public List<Task> searchByAssignee(String assignee) {
        if (assignee == null || assignee.trim().isEmpty()) {
            return allTasksSupplier.get();
        }
        return query(SQL_SEARCH_BY_ASSIGNEE, "担当者検索エラー", ps -> ps.setString(1, assignee));
    }

    public List<Task> filterByStatus(String status) {
        if (status == null || status.trim().isEmpty() || AppMessages.isFilterAll(status)) {
            return allTasksSupplier.get();
        }
        return query(SQL_FILTER_BY_STATUS, "ステータスフィルタエラー", ps -> ps.setString(1, status));
    }

    public List<Task> filterByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return allTasksSupplier.get();
        }
        return query(SQL_FILTER_BY_DATE_RANGE, "日付範囲フィルタエラー", ps -> {
            ps.setString(1, startDate.toString());
            ps.setString(2, endDate.toString());
            ps.setString(3, startDate.toString());
            ps.setString(4, endDate.toString());
        });
    }

    public List<Task> searchCombined(
            String keyword, String assignee, String status, LocalDate startDate, LocalDate endDate) {
        if ((keyword == null || keyword.isBlank())
                && (assignee == null || assignee.isBlank())
                && (status == null || status.isBlank() || AppMessages.isFilterAll(status))
                && (startDate == null || endDate == null)) {
            return allTasksSupplier.get();
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + keyword.toLowerCase() + "%");
        }
        if (assignee != null && !assignee.trim().isEmpty()) {
            sql.append(" AND assignee = ?");
            params.add(assignee);
        }
        if (status != null && !status.trim().isEmpty() && !AppMessages.isFilterAll(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (startDate != null && endDate != null) {
            sql.append(" AND ((start_date >= ? AND start_date <= ?) OR (end_date >= ? AND end_date <= ?))");
            params.add(startDate.toString());
            params.add(endDate.toString());
            params.add(startDate.toString());
            params.add(endDate.toString());
        }
        sql.append(" ORDER BY parent_id, order_index, id ASC");

        List<Task> taskList = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    taskList.add(TaskRowMapper.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("複合検索エラー", e);
        }
        return taskList;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Task> query(String sql, String errorLabel, StatementBinder binder) {
        List<Task> taskList = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            binder.bind(pstmt);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    taskList.add(TaskRowMapper.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error(errorLabel, e);
        }
        return taskList;
    }
}
