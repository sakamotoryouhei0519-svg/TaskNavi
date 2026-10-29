package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;

/**
 * 一覧（カンバンのカード・WBS の行）に出すコメント件数。開かなくてもコメントがあると分かるようにする。
 */
public final class CommentCount {

    private CommentCount() {
    }

    /** コメントが無ければ null。 */
    public static String label(Task task) {
        int count = task.getComments().size();
        return count > 0 ? "\uD83D\uDCAC " + count : null;
    }

    public static String tooltip(Task task) {
        return AppMessages.format("comment.count.tooltip", "コメント {0} 件", task.getComments().size());
    }
}
