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

    private static int wheelDelta(MouseWheelEvent e) {
        double rotation = e.getPreciseWheelRotation();
        if (rotation == 0) {
            rotation = e.getWheelRotation();
        }
        return (int) Math.round(rotation * 100);
    }
}
