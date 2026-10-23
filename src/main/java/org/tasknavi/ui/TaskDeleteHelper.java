package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.model.EntryType;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;

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
            NoticeBar.info(parent, AppMessages.get("mainframe.delete.none"));
            return;
        }

        String typeDomain = (selected.getLevel() == 1) ? EntryType.PROJECT.name() : EntryType.TASK.name();
        String typeDisplay = AppMessages.entryTypeDisplay(typeDomain);
        String message = AppMessages.format(
                "mainframe.delete.confirm",
                "{0}「{1}」を削除しますか？\n（子タスクが存在する場合は子タスクも同時に削除されます）",
                typeDisplay,
                selected.getName());
        boolean shown = NoticeBar.confirm(parent, message, () -> delete(parent, taskService, selected));
        if (shown) {
            return;
        }
        if (AppDialog.confirm(parent, AppMessages.get("mainframe.delete.confirm.title"), message)) {
            delete(parent, taskService, selected);
        }
    }

    private static void delete(Component parent, TaskService taskService, Task selected) {
        String name = selected.getName();
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
                    NoticeBar.info(parent, AppMessages.format(
                            "message.deleted.named", "「{0}」を削除しました。", name));
                } catch (Exception e) {
                    ErrorDialogUtil.showBackgroundError(parent, e, AppMessages.get("mainframe.error.delete"));
                    logger.error("タスク削除エラー", e);
                }
            }
        };
        worker.execute();
    }
}
