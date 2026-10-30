package org.tasknavi.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.model.Task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * ResultSet から Task へのマッピング。列は Flyway の V1〜V7 でそろっている前提。
 */
public final class TaskRowMapper {

    private static final Logger logger = LoggerFactory.getLogger(TaskRowMapper.class);

    private TaskRowMapper() {
    }

    public static Task fromResultSet(ResultSet rs) throws SQLException {
        int taskId = rs.getInt("id");
        Integer parentId = rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null;
        Task task = new Task(taskId, rs.getString("name"), parentId, rs.getInt("level"), rs.getInt("order_index"),
                rs.getInt("progress"), rs.getString("status"),
                parseDate(rs.getString("start_date"), taskId), parseDate(rs.getString("end_date"), taskId));
        task.setAssignee(rs.getString("assignee"));
        task.setPriority(rs.getString("priority"));
        task.setMemo(rs.getString("memo"));
        return task;
    }

    /** 「2026-10-24」と、前の版が書いた「2026-10-24T00:00」を読む。読めない日付は空にしてログに残す。 */
    static LocalDate parseDate(String text, int taskId) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String date = text.trim();
        int time = date.indexOf('T');
        try {
            return LocalDate.parse(time >= 0 ? date.substring(0, time) : date);
        } catch (DateTimeParseException e) {
            logger.warn("タスク {} の日付「{}」を読めないため、空として扱います", taskId, text);
            return null;
        }
    }
}
