package org.tasknavi.ui.wbs;

import org.tasknavi.AppMessages;
import org.tasknavi.Task;
import org.tasknavi.TaskService;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * WBS ツリーのドラッグ＆ドロップ（親子付け替え・兄弟並び替え）。
 */
public class WbsTreeTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;

    public interface Host {
        Map<DefaultMutableTreeNode, Task> nodeTaskMap();

        TaskService taskService();

        JTree tree();

        Component dialogParent();

        void setHighlightDropPath(TreePath path);

        void clearDropHighlight();
    }

    private final Host host;

    public WbsTreeTransferHandler(Host host) {
        this.host = host;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Task task = host.nodeTaskMap().get(node);
        if (task == null) {
            return null;
        }
        return new StringSelection(String.valueOf(task.getId()));
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) {
            host.clearDropHighlight();
            return false;
        }
        if (!(support.getComponent() instanceof JTree)) {
            host.clearDropHighlight();
            return false;
        }
        if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            host.clearDropHighlight();
            return false;
        }
        if (!(support.getDropLocation() instanceof JTree.DropLocation dropLocation)) {
            host.clearDropHighlight();
            return false;
        }
        TreePath targetPath = dropLocation.getPath();
        if (targetPath == null) {
            host.clearDropHighlight();
            return false;
        }
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
        Task targetTask = host.nodeTaskMap().get(targetNode);
        if (targetTask == null) {
            host.clearDropHighlight();
            return false;
        }

        try {
            String draggedIdText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            int draggedId = Integer.parseInt(draggedIdText);
            if (draggedId == targetTask.getId()) {
                host.clearDropHighlight();
                return false;
            }

            int childIndex = dropLocation.getChildIndex();
            TaskService taskService = host.taskService();

            if (childIndex == -1) {
                int newLevel = targetTask.getLevel() + 1;
                if (newLevel > 3) {
                    host.clearDropHighlight();
                    return false;
                }

                boolean valid = !WbsHierarchyOps.wouldCreateCycle(taskService, draggedId, targetTask.getId());
                host.setHighlightDropPath(valid ? targetPath : null);
                if (host.tree() != null) {
                    host.tree().repaint();
                }
                support.setShowDropLocation(valid);
                return valid;
            }

            Task movedTask = taskService.getTaskById(draggedId);
            if (movedTask == null) {
                host.clearDropHighlight();
                return false;
            }

            if (!Objects.equals(movedTask.getParentId(), targetTask.getParentId())) {
                host.clearDropHighlight();
                return false;
            }

            if (movedTask.getId() == targetTask.getId()) {
                host.clearDropHighlight();
                return false;
            }

            host.setHighlightDropPath(targetPath);
            if (host.tree() != null) {
                host.tree().repaint();
            }
            support.setShowDropLocation(true);
            return true;
        } catch (Exception e) {
            host.clearDropHighlight();
            return false;
        }
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        try {
            String draggedIdText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            int draggedId = Integer.parseInt(draggedIdText);
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            TreePath targetPath = dropLocation.getPath();
            if (targetPath == null) {
                return false;
            }

            DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
            Task targetTask = host.nodeTaskMap().get(targetNode);
            TaskService taskService = host.taskService();
            Task movedTask = taskService.getTaskById(draggedId);
            if (targetTask == null || movedTask == null) {
                return false;
            }

            int childIndex = dropLocation.getChildIndex();

            if (childIndex == -1) {
                if (movedTask.getLevel() == 1 && targetTask.getLevel() == 1) {
                    host.clearDropHighlight();
                    return false;
                }

                movedTask.setParentId(targetTask.getId());
                movedTask.setLevel(targetTask.getLevel() + 1);
                taskService.updateTask(movedTask);
                WbsHierarchyOps.recalculateDescendantLevels(taskService, movedTask.getId(), movedTask.getLevel());
                host.clearDropHighlight();
                return true;
            }

            List<Task> allTasks = taskService.getAllTasks();
            List<Task> siblings = new ArrayList<>();

            for (Task task : allTasks) {
                if (task.getParentId() != null && task.getParentId().equals(movedTask.getParentId())) {
                    siblings.add(task);
                }
            }

            siblings.sort(Comparator.comparingInt(Task::getOrderIndex));

            int movedIndex = -1;
            int targetIndex = -1;
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i).getId() == movedTask.getId()) {
                    movedIndex = i;
                }
                if (siblings.get(i).getId() == targetTask.getId()) {
                    targetIndex = i;
                }
            }

            if (movedIndex == -1 || targetIndex == -1) {
                host.clearDropHighlight();
                return false;
            }

            siblings.remove(movedIndex);

            int newIndex = Math.min(targetIndex, siblings.size());
            siblings.add(newIndex, movedTask);

            for (int i = 0; i < siblings.size(); i++) {
                Task task = siblings.get(i);
                task.setOrderIndex(i);
                taskService.updateTaskOrderIndex(task.getId(), i);
            }

            host.clearDropHighlight();
            return true;
        } catch (Exception e) {
            host.clearDropHighlight();
            JOptionPane.showMessageDialog(
                    host.dialogParent(),
                    AppMessages.get("wbs.error.move", "タスクの移動に失敗しました。"),
                    AppMessages.get("common.dialog.error.title", "エラー"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
