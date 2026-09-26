package org.tasknavi.ui.gantt;

import org.tasknavi.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ガントバーのマウスヒット判定。
 */
public final class GanttBarHit {

    private final Task task;
    private final LocalDate displayStart;
    private final LocalDate displayEnd;
    private final int dragMode;

    private GanttBarHit(Task task, LocalDate displayStart, LocalDate displayEnd, int dragMode) {
        this.task = task;
        this.displayStart = displayStart;
        this.displayEnd = displayEnd;
        this.dragMode = dragMode;
    }

    public Task getTask() {
        return task;
    }

    public LocalDate getDisplayStart() {
        return displayStart;
    }

    public LocalDate getDisplayEnd() {
        return displayEnd;
    }

    public int getDragMode() {
        return dragMode;
    }

    public static GanttBarHit find(
            List<Task> tasks,
            Map<Integer, List<Task>> childrenMap,
            LocalDate minDate,
            int mouseX,
            int mouseY,
            int dayWidth,
            int rowHeight,
            int barHeight,
            int resizeHandleWidth
    ) {
        if (tasks == null || tasks.isEmpty() || minDate == null) {
            return null;
        }
        int rowIndex = mouseY / Math.max(1, rowHeight);
        if (rowIndex < 0 || rowIndex >= tasks.size()) {
            return null;
        }

        Task task = tasks.get(rowIndex);
        LocalDate start = GanttDisplayDates.resolveDisplayStartDate(task, childrenMap);
        LocalDate end = GanttDisplayDates.resolveDisplayEndDate(task, childrenMap);
        GanttBarGeometry.Rect bar = GanttBarGeometry.barRect(
                minDate, start, end, rowIndex, dayWidth, rowHeight, barHeight);
        if (bar == null) {
            return null;
        }
        if (mouseY < bar.y() || mouseY > bar.y() + bar.height()) {
            return null;
        }
        int mode = GanttBarGeometry.dragModeForX(mouseX, bar, resizeHandleWidth);
        if (mode == 0) {
            return null;
        }
        return new GanttBarHit(task, start, end, mode);
    }
}
