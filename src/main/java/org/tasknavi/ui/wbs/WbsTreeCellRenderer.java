package org.tasknavi.ui.wbs;

import org.tasknavi.model.Task;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.CommentCount;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;

/**
 * WBS ツリーのセル描画（ステータス色・ドロップハイライト）。
 */
public class WbsTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1L;

    public interface Context {
        Task taskFor(DefaultMutableTreeNode node);

        TreePath highlightDropPath();

        int hoverRow();
    }

    private final Context context;
    private final Icon projectIcon;
    private final Icon taskIcon;

    public WbsTreeCellRenderer(Context context) {
        this.context = context;
        projectIcon = UIManager.getIcon("Tree.openIcon");
        taskIcon = UIManager.getIcon("Tree.leafIcon");
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        setOpaque(false);
        setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));
        setBackgroundSelectionColor(new Color(0, 0, 0, 0));

        setForeground(AppTheme.textStrong());

        if (value instanceof DefaultMutableTreeNode node) {
            Task task = context.taskFor(node);
            if (task != null) {
                String commentText = CommentCount.label(task);
                if (commentText != null) {
                    // 絵文字は代わりのフォントで描かれ、測った幅より広くなるので、末尾に余白を足す
                    setText(getText() + "   " + commentText + "  ");
                }
                if (task.getLevel() == 1) {
                    setIcon(projectIcon != null ? projectIcon : getDefaultOpenIcon());
                } else {
                    setIcon(taskIcon != null ? taskIcon : getDefaultLeafIcon());
                    setForeground(AppTheme.getStatusColor(task.getStatusCode()));
                }
            }
        }

        TreePath highlightDropPath = context.highlightDropPath();
        if (highlightDropPath != null && highlightDropPath.getLastPathComponent() == value) {
            setOpaque(true);
            Task task = context.taskFor((DefaultMutableTreeNode) value);
            String status = task != null ? task.getStatusCode() : Task.STATUS_NOT_STARTED;
            setBackground(AppTheme.getStatusSoftColor(status));
            setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.PRIMARY));
            setForeground(AppTheme.textStrong());
        } else if (sel) {
            setOpaque(true);
            setBackground(AppTheme.selectedFill());
            setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        } else if (row == context.hoverRow()) {
            setOpaque(true);
            setBackground(AppTheme.hoverFill());
            setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        } else {
            setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
            setOpaque(false);
        }
        return this;
    }
}
