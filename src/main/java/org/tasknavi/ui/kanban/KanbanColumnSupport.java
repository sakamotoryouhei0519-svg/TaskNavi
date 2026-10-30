package org.tasknavi.ui.kanban;

import org.tasknavi.model.Task;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import java.awt.Component;
import java.awt.Container;

/**
 * カンバン列の見出し文言とステータス列への振り分け。
 */
public final class KanbanColumnSupport {

    public enum Column {
        TODO,
        IN_PROGRESS,
        DONE
    }

    private KanbanColumnSupport() {
    }

    public static String formatHeader(String title, int count) {
        return "\u3010 " + title + " \u3011  (" + count + ")";
    }

    public static Column columnFor(Task task) {
        if (task == null) {
            return Column.TODO;
        }
        if (task.isInProgress()) {
            return Column.IN_PROGRESS;
        }
        if (task.isCompleted()) {
            return Column.DONE;
        }
        return Column.TODO;
    }

    public static String statusOf(Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof JComponent jc) {
                Object status = jc.getClientProperty("kanban.status");
                if (status instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    public static void attachDrop(Component component, TransferHandler handler) {
        if (component instanceof JComponent jc && jc.getTransferHandler() == null) {
            jc.setTransferHandler(handler);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachDrop(child, handler);
            }
        }
    }
}
