package org.example.ui.wbs;

import org.example.AppException;
import org.example.AppMessages;
import org.example.EntryType;
import org.example.ErrorDialogUtil;
import org.example.Task;
import org.example.ui.taskdialog.TaskDialog;
import org.example.TaskEntryFactory;
import org.example.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Frame;
import java.util.List;
import java.util.function.Function;

/**
 * WBS からのタスク新規登録（共有 TaskDialog → TaskEntryFactory）。
 */
public final class WbsEntryCreator {

    private static final Logger logger = LoggerFactory.getLogger(WbsEntryCreator.class);

    private WbsEntryCreator() {
    }

    /**
     * @param selectedTask        選択中タスク（親推定用。null 可）
     * @param findRootProject     ルートプロジェクト解決
     * @param useSelectedParent   選択を親候補に使うか
     */
    public static void openFromSelection(
            Component parent,
            TaskService taskService,
            Task selectedTask,
            Function<Task, Task> findRootProject,
            boolean useSelectedParent) {
        Integer defaultParentProjectId = null;
        if (useSelectedParent && selectedTask != null) {
            Task root = findRootProject.apply(selectedTask);
            if (root != null) {
                defaultParentProjectId = root.getId();
            }
        }
        openAndRegister(parent, taskService, EntryType.TASK.name(), defaultParentProjectId);
    }

    public static void openAndRegister(
            Component parent,
            TaskService taskService,
            String defaultType,
            Integer defaultParentProjectId) {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(parent);
        TaskDialog dialog = new TaskDialog(frame, taskService, defaultType, defaultParentProjectId);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        if (TaskEntryFactory.requiresParent(dialog.getEntryType()) && dialog.getParentProjectId() == null) {
            JOptionPane.showMessageDialog(parent,
                    AppMessages.get("dialog.error.parent.required",
                            "工程・タスクは親プロジェクトまたは親工程を選択してから登録してください。"),
                    AppMessages.get("dialog.error.input", "入力エラー"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            List<Task> allTasks = taskService.getAllTasks();
            Task newTask = TaskEntryFactory.fromDialog(dialog, allTasks);
            if (newTask == null) {
                return;
            }

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    taskService.addTask(newTask);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(parent,
                                AppMessages.get("wbs.message.registered", "タスクを登録しました。"));
                    } catch (AppException e) {
                        ErrorDialogUtil.showError(parent, e.getUserMessage());
                    } catch (Exception ex) {
                        ErrorDialogUtil.showError(parent,
                                AppMessages.get("wbs.error.register", "タスクの登録に失敗しました。"));
                        logger.error("タスク登録時の予期せぬエラー", ex);
                    }
                }
            };
            worker.execute();
        } catch (AppException e) {
            ErrorDialogUtil.showError(parent, e.getUserMessage());
        } catch (Exception ex) {
            ErrorDialogUtil.showError(parent,
                    AppMessages.get("wbs.error.register", "タスクの登録に失敗しました。"));
            logger.error("タスク登録時の予期せぬエラー", ex);
        }
    }
}
