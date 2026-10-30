package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.model.EntryType;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.List;

/**
 * MainFrame から切り出したタスク削除確認 UI ヘルパー。
 */
public final class TaskDeleteHelper {

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
        SwingWorker<List<Task>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Task> doInBackground() throws Exception {
                return taskService.deleteTask(selected.getId());
            }

            @Override
            protected void done() {
                try {
                    List<Task> removed = get();
                    String message = AppMessages.format(
                            "message.deleted.named", "「{0}」を削除しました。", name);
                    boolean shown = NoticeBar.infoWithAction(parent, message,
                            AppMessages.get("notice.undo", "元に戻す"),
                            () -> restore(parent, taskService, removed, name));
                    if (!shown) {
                        NoticeBar.info(parent, message);
                    }
                } catch (Exception e) {
                    ErrorDialogUtil.showBackgroundError(parent, e, AppMessages.get("mainframe.error.delete"));
                }
            }
        };
        worker.execute();
    }

    private static void restore(Component parent, TaskService taskService, List<Task> removed, String name) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                taskService.restoreDeleted(removed);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    NoticeBar.info(parent, AppMessages.format(
                            "message.restored.named", "「{0}」を元に戻しました。", name));
                } catch (Exception e) {
                    ErrorDialogUtil.showBackgroundError(parent, e, AppMessages.get("mainframe.error.restore"));
                }
            }
        };
        worker.execute();
    }
}
