package org.example.persistence;

import org.example.Task;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Task → PreparedStatement への共通バインド。
 */
public final class TaskStatementBinder {

    private TaskStatementBinder() {
    }

    /** INSERT 用（1..10）。 */
    public static void bindInsert(PreparedStatement pstmt, Task task) throws SQLException {
        if (task.getParentId() != null && task.getParentId() != 0) {
            pstmt.setInt(1, task.getParentId());
        } else {
            pstmt.setNull(1, Types.INTEGER);
        }
        pstmt.setInt(2, task.getLevel() > 0 ? task.getLevel() : 1);
        pstmt.setInt(3, task.getOrderIndex());
        pstmt.setString(4, task.getName() != null ? task.getName() : "");
        pstmt.setString(5, task.getAssignee());
        pstmt.setString(6, task.getStartDate() != null ? task.getStartDate().toString() : null);
        pstmt.setString(7, task.getEndDate() != null ? task.getEndDate().toString() : null);
        pstmt.setString(8, task.getStatusCode() != null ? task.getStatusCode() : Task.STATUS_NOT_STARTED);
        pstmt.setInt(9, task.getProgress());
        pstmt.setString(10, task.getPriorityCode());
    }

    /** UPDATE 用（1..10 は INSERT と同じ、11 に id）。 */
    public static void bindUpdate(PreparedStatement pstmt, Task task) throws SQLException {
        bindInsert(pstmt, task);
        pstmt.setInt(11, task.getId());
    }
}
