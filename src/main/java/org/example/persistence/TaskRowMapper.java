package org.example.persistence;

import org.example.Task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * ResultSet から Task へのマッピング（列名互換付き）。
 */
public final class TaskRowMapper {

    private TaskRowMapper() {
    }

    public static Task fromResultSet(ResultSet rs) throws SQLException {
        int taskId = getColumnInt(rs, "id", "task_id");
        Integer parentId = rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null;
        int level = getColumnIntOrDefault(rs, "level", 1);
        int orderIndex = getColumnIntOrDefault(rs, "order_index", 0);
        String name = getColumnString(rs, "name", "title");
        String assignee = rs.getString("assignee");
        LocalDate startDate = parseSafeDate(rs, "start_date");
        LocalDate endDate = parseSafeDate(rs, "end_date");
        String status = rs.getString("status");
        int progress = rs.getInt("progress");

        Task task = new Task(taskId, name, parentId, level, orderIndex, progress, status, startDate, endDate);
        task.setAssignee(assignee);
        try {
            task.setPriority(rs.getString("priority"));
        } catch (SQLException ignored) {
            // 旧スキーマ互換
        }
        return task;
    }

    public static LocalDate parseSafeDate(ResultSet rs, String columnName) {
        try {
            String dateStr = rs.getString(columnName);
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }
            if (dateStr.contains("T")) {
                dateStr = dateStr.split("T")[0];
            }
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static int getColumnInt(ResultSet rs, String col1, String col2) throws SQLException {
        try {
            return rs.getInt(col1);
        } catch (SQLException e) {
            return rs.getInt(col2);
        }
    }

    public static int getColumnIntOrDefault(ResultSet rs, String col, int defaultValue) {
        try {
            return rs.getInt(col);
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    public static String getColumnString(ResultSet rs, String col1, String col2) throws SQLException {
        try {
            String val = rs.getString(col1);
            if (val != null) {
                return val;
            }
        } catch (SQLException ignored) {
        }
        return rs.getString(col2);
    }
}
