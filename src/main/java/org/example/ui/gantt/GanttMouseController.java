package org.example.ui.gantt;

import org.example.Task;
import org.example.TaskService;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

/**
 * ガントバーの選択・ドラッグ・ダブルクリック編集。
 */
final class GanttMouseController extends MouseAdapter {

    interface Host {
        GanttBarHit hitTest(MouseEvent e);

        GanttDragSession dragSession();

        void setSelectedTaskId(Integer taskId);

        void setHoveredTask(Task task);

        void setManualVisibleWindow(boolean manual);

        LocalDate visibleStartDate();

        LocalDate visibleEndDate();

        void setVisibleWindow(LocalDate start, LocalDate end);

        int dayWidth();

        int panelWidth();

        TaskService taskService();

        Task resolveRootProjectForDrag(Task task);

        void repaintPanel();

        void setCursor(Cursor cursor);

        void setToolTipText(String text);

        Component component();
    }

    private final Host host;

    GanttMouseController(Host host) {
        this.host = host;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        GanttBarHit hit = host.hitTest(e);
        if (hit == null) {
            host.setSelectedTaskId(null);
            host.dragSession().clear();
            host.repaintPanel();
            return;
        }
        host.setSelectedTaskId(hit.getTask().getId());
        host.dragSession().begin(hit, e.getX());
        host.repaintPanel();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        GanttBarHit hit = host.hitTest(e);
        if (hit == null) {
            host.setSelectedTaskId(null);
            host.repaintPanel();
            return;
        }
        host.setSelectedTaskId(hit.getTask().getId());
        if (e.getClickCount() == 2) {
            GanttTaskEditor.openEdit(host.component(), host.taskService(), hit.getTask());
        }
        host.repaintPanel();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!host.dragSession().isActive()) {
            return;
        }
        host.dragSession().updateMouseX(e.getX());
        host.setManualVisibleWindow(true);
        LocalDate[] window = new LocalDate[]{host.visibleStartDate(), host.visibleEndDate()};
        if (host.dragSession().expandVisibleWindow(e.getX(), host.panelWidth(), host.dayWidth(), window)) {
            host.setVisibleWindow(window[0], window[1]);
        }
        host.repaintPanel();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!host.dragSession().isActive()) {
            return;
        }
        GanttBarDragMath.Result dates = host.dragSession().commitDates(
                e.getX(), host.dayWidth(), host::resolveRootProjectForDrag);
        Task dragging = host.dragSession().draggingTask();
        if (dates != null && dragging != null) {
            dragging.setStartDate(dates.getStart());
            dragging.setEndDate(dates.getEnd());
            host.taskService().updateTask(dragging);
        }
        host.setManualVisibleWindow(false);
        host.setVisibleWindow(null, null);
        host.dragSession().clear();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        GanttBarHit hit = host.hitTest(e);
        if (hit == null) {
            host.setHoveredTask(null);
            host.setCursor(Cursor.getDefaultCursor());
            host.setToolTipText(null);
            return;
        }
        host.setHoveredTask(hit.getTask());
        if (hit.getDragMode() == 2 || hit.getDragMode() == 3) {
            host.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else {
            host.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        host.setToolTipText(GanttTooltips.forTask(hit.getTask()));
    }
}
