package org.tasknavi.persistence;

import org.tasknavi.model.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * 兄弟タスクの order_index 再割当て。
 */
public final class TaskReorderSupport {

    private TaskReorderSupport() {
    }

    /** 並びが変わった兄弟だけを返す。DB へは呼び出し側がまとめて書く。 */
    public static List<Task> reorderSiblings(Integer parentId, List<Task> allTasks) {
        List<Task> siblings = new ArrayList<>();
        for (Task task : allTasks) {
            if (parentId == null) {
                if (task.getParentId() == null || task.getParentId() == 0) {
                    siblings.add(task);
                }
            } else if (parentId.equals(task.getParentId())) {
                siblings.add(task);
            }
        }

        siblings.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));
        List<Task> changed = new ArrayList<>();
        for (int i = 0; i < siblings.size(); i++) {
            Task task = siblings.get(i);
            if (task.getOrderIndex() != i) {
                task.setOrderIndex(i);
                changed.add(task);
            }
        }
        return changed;
    }
}
