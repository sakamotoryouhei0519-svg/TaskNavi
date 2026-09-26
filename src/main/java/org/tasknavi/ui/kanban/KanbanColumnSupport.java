package org.tasknavi.ui.kanban;

import org.tasknavi.Task;

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
}
