package org.example.persistence;

import org.example.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;

/**
 * 兄弟タスクの order_index 再割当て。
 */
public final class TaskReorderSupport {

    private TaskReorderSupport() {
    }

    /**
     * @param updateOrder (taskId, newOrderIndex) を DB へ反映するコールバック
     */
    public static void reorderSiblings(Integer parentId, List<Task> allTasks, ObjIntConsumer<Integer> updateOrder) {
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
        for (int i = 0; i < siblings.size(); i++) {
            Task task = siblings.get(i);
            if (task.getOrderIndex() != i) {
                task.setOrderIndex(i);
                updateOrder.accept(task.getId(), i);
            }
        }
    }
}
