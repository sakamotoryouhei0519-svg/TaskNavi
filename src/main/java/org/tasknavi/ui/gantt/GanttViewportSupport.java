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
                horizontal.setValue(horizontal.getValue() + e.getWheelRotation() * 24);
            }
            return;
        }

        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        if (vertical != null) {
            vertical.setValue(vertical.getValue() + e.getWheelRotation() * 24);
        }
    }
}
