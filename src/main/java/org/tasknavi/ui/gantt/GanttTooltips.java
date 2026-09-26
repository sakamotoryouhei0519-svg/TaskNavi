package org.tasknavi.ui.gantt;

import org.tasknavi.AppMessages;
import org.tasknavi.Task;

/**
 * ガントバーのツールチップ HTML。
 */
public final class GanttTooltips {

    private GanttTooltips() {
    }

    public static String forTask(Task task) {
        if (task == null) {
            return null;
        }
        String unset = AppMessages.get("label.unset", "未設定");
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><b>").append(task.getName()).append("</b><br>");
        tooltip.append(AppMessages.get("label.assignee", "担当者")).append(": ")
                .append(task.getAssignee() != null && !task.getAssignee().isBlank()
                        ? task.getAssignee() : unset).append("<br>");
        tooltip.append(AppMessages.get("label.period", "期間")).append(": ")
                .append(task.getStartDate()).append(" ～ ").append(task.getEndDate()).append("<br>");
        tooltip.append(AppMessages.get("label.progress", "進捗")).append(": ")
                .append(task.getProgress()).append("%<br>");
        tooltip.append(AppMessages.get("label.status", "状態")).append(": ")
                .append(task.getStatus() != null ? AppMessages.statusDisplay(task.getStatus()) : unset).append("<br>");
        tooltip.append(AppMessages.get("label.priority", "優先度")).append(": ")
                .append(AppMessages.priorityDisplay(task.getPriorityCode()));
        tooltip.append("</html>");
        return tooltip.toString();
    }
}
