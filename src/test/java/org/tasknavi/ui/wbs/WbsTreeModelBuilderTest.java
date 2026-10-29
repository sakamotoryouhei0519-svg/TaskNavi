package org.tasknavi.ui.wbs;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Task;

import javax.swing.tree.DefaultMutableTreeNode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WbsTreeModelBuilderTest {

    @Test
    void codesAreJoinedWithSingleDots() {
        List<Task> tasks = List.of(
                task(1, "プロジェクト", null, 1),
                task(2, "設計", 1, 2),
                task(3, "画面設計", 2, 3),
                task(4, "実装", 1, 2),
                task(5, "別のプロジェクト", null, 1));

        DefaultMutableTreeNode root = WbsTreeModelBuilder.build(tasks, null, null, null).getRoot();

        DefaultMutableTreeNode project = child(root, 0);
        assertEquals("1 プロジェクト", project.getUserObject());
        assertEquals("1.1 設計", child(project, 0).getUserObject());
        assertEquals("1.1.1 画面設計", child(child(project, 0), 0).getUserObject());
        assertEquals("1.2 実装", child(project, 1).getUserObject());
        assertEquals("2 別のプロジェクト", child(root, 1).getUserObject());
    }

    private static DefaultMutableTreeNode child(DefaultMutableTreeNode node, int index) {
        return (DefaultMutableTreeNode) node.getChildAt(index);
    }

    private static Task task(int id, String name, Integer parentId, int level) {
        return new Task(id, name, parentId, level, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }
}
