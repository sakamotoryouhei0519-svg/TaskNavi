package org.tasknavi.ui.gantt;

import org.tasknavi.Task;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * ガントバーのドラッグ／リサイズ操作状態。
 */
public final class GanttDragSession {

    private Task draggingTask;
    private int dragMode;
    private int dragStartX;
    private int currentMouseX;
    private LocalDate originalStartDate;
    private LocalDate originalEndDate;

    public void begin(GanttBarHit hit, int mouseX) {
        if (hit == null) {
            clear();
            return;
        }
        draggingTask = hit.getTask();
        dragStartX = mouseX;
        currentMouseX = mouseX;
        originalStartDate = hit.getDisplayStart();
        originalEndDate = hit.getDisplayEnd();
        dragMode = hit.getDragMode();
    }

    public void updateMouseX(int mouseX) {
        currentMouseX = mouseX;
    }

    public boolean isActive() {
        return draggingTask != null && dragMode != 0;
    }

    public boolean isDragging(Task task) {
        return draggingTask != null && task != null && draggingTask.getId() == task.getId();
    }

    public Task draggingTask() {
        return draggingTask;
    }

    public int dragMode() {
        return dragMode;
    }

    public GanttBarDragMath.Result previewDates(int dayWidth, Function<Task, Task> rootResolver) {
        if (!isActive() || originalStartDate == null || originalEndDate == null) {
            return null;
        }
        long diffDays = Math.round((double) (currentMouseX - dragStartX) / dayWidth);
        return GanttBarDragMath.apply(
                dragMode,
                originalStartDate,
                originalEndDate,
                diffDays,
                rootResolver.apply(draggingTask));
    }

    public GanttBarDragMath.Result commitDates(int mouseX, int dayWidth, Function<Task, Task> rootResolver) {
        if (!isActive() || originalStartDate == null || originalEndDate == null) {
            return null;
        }
        long diffDays = Math.round((double) (mouseX - dragStartX) / dayWidth);
        return GanttBarDragMath.apply(
                dragMode,
                originalStartDate,
                originalEndDate,
                diffDays,
                rootResolver.apply(draggingTask));
    }

    /**
     * ドラッグ中に端へ寄せたときの表示ウィンドウ拡張。
     * @return true なら manualVisibleWindow を有効化すべき
     */
    public boolean expandVisibleWindow(
            int mouseX,
            int panelWidth,
            int dayWidth,
            LocalDate[] visibleWindow) {
        if (visibleWindow == null || visibleWindow.length < 2) {
            return false;
        }
        int leftEdgeThreshold = 60;
        int rightEdgeThreshold = panelWidth - 60;
        boolean changed = false;
        if (mouseX < leftEdgeThreshold && visibleWindow[0] != null) {
            visibleWindow[0] = visibleWindow[0].minusDays(
                    Math.max(1, (leftEdgeThreshold - mouseX) / Math.max(1, dayWidth)));
            changed = true;
        } else if (mouseX > rightEdgeThreshold && visibleWindow[1] != null) {
            visibleWindow[1] = visibleWindow[1].plusDays(
                    Math.max(1, (mouseX - rightEdgeThreshold) / Math.max(1, dayWidth)));
            changed = true;
        }
        return changed;
    }

    public void clear() {
        draggingTask = null;
        dragMode = 0;
        originalStartDate = null;
        originalEndDate = null;
    }
}
