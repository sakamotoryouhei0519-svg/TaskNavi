package org.tasknavi.ui.gantt;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseWheelEvent;

/**
 * ガントの親ビューポートへのホイールスクロール委譲。
 */
public final class GanttViewportSupport {

    private GanttViewportSupport() {
    }

    public static void scrollParentViewport(Component source, MouseWheelEvent e) {
        Container parent = source.getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        if (!(parent instanceof JScrollPane scrollPane)) {
            return;
        }

        if (e.isShiftDown()) {
            JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
            if (horizontal != null) {
                horizontal.setValue(horizontal.getValue() + wheelDelta(e));
            }
            return;
        }

        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        if (vertical != null) {
            vertical.setValue(vertical.getValue() + wheelDelta(e));
        }
    }

    /** その日の列が見えている幅の真ん中に来る横位置。端では行き過ぎないよう止める。 */
    static int centeredScrollX(long dayOffset, int dayWidth, int extentWidth, int viewWidth) {
        long center = dayOffset * dayWidth + dayWidth / 2;
        long x = center - extentWidth / 2;
        long max = Math.max(0, viewWidth - extentWidth);
        return (int) Math.max(0, Math.min(x, max));
    }

    private static int wheelDelta(MouseWheelEvent e) {
        double rotation = e.getPreciseWheelRotation();
        if (rotation == 0) {
            rotation = e.getWheelRotation();
        }
        return (int) Math.round(rotation * 100);
    }
}
