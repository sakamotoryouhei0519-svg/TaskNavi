package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.EntryType;
import org.tasknavi.ErrorDialogUtil;
import org.tasknavi.Task;
import org.tasknavi.TaskService;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Component;

/**
 * MainFrame から切り出したタスク削除確認 UI ヘルパー。
 */
public final class TaskDeleteHelper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskDeleteHelper.class);

    private TaskDeleteHelper() {
    }

    public static void confirmAndDelete(Component parent, TaskService taskService, Task selected) {
        if (selected == null) {
            JOptionPane.showMessageDialog(
                    parent,
                    AppMessages.get("mainframe.delete.none"),
                    AppMessages.get("common.dialog.info.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String typeDomain = (selected.getLevel() == 1) ? EntryType.PROJECT.name() : EntryType.TASK.name();
        String typeDisplay = AppMessages.entryTypeDisplay(typeDomain);
        int choice = JOptionPane.showConfirmDialog(
                parent,
                AppMessages.format(
                        "mainframe.delete.confirm",
                        "{0}「{1}」を削除しますか？\n（子タスクが存在する場合は子タスクも同時に削除されます）",
                        typeDisplay,
                        selected.getName()),
                AppMessages.get("mainframe.delete.confirm.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    taskService.deleteTask(selected.getId());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                    } catch (Exception e) {
                        ErrorDialogUtil.showError(parent, AppMessages.get("mainframe.error.delete"));
                        logger.error("タスク削除エラー", e);
                    }
                }
            };
            worker.execute();
        }
    }
}
