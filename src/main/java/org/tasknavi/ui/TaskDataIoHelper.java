package org.tasknavi.ui;

import org.tasknavi.AppException;
import org.tasknavi.AppMessages;
import org.tasknavi.util.CsvUtil;
import org.tasknavi.model.Task;
import org.tasknavi.service.TaskService;
import org.tasknavi.util.TaskViewFilter;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * MainFrame から切り出したタスクのインポート／エクスポート UI ヘルパー。
 */
public final class TaskDataIoHelper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskDataIoHelper.class);

    private TaskDataIoHelper() {
    }

    public static void exportTasks(Component parent, TaskService taskService) {
        exportTasks(parent, taskService, null, null);
    }

    /**
     * @param keyword グローバル検索キーワード（null 可）
     * @param status  グローバル状態フィルタ（null / ALL 可）
     */
    public static void exportTasks(Component parent, TaskService taskService, String keyword, String status) {
        List<Task> all = taskService.getAllTasks();
        List<Task> filtered = TaskViewFilter.apply(all, keyword, status, null, new HashMap<>());
        boolean filterActive = filtered.size() != all.size()
                || (keyword != null && !keyword.isBlank())
                || (status != null && !AppMessages.isFilterAll(status));

        List<Task> toExport = all;
        if (filterActive && !filtered.isEmpty()) {
            int choice = AppDialog.choose(
                    parent,
                    AppMessages.get("mainframe.export.dialog.title"),
                    AppMessages.get("mainframe.export.choose.message", "エクスポート対象を選んでください。"),
                    AppMessages.format("mainframe.export.option.filtered", "絞り込み結果 ({0}件)", filtered.size()),
                    AppMessages.format("mainframe.export.option.all", "全件 ({0}件)", all.size()),
                    AppMessages.get("common.button.cancel", "キャンセル"));
            if (choice != 0 && choice != 1) {
                return;
            }
            toExport = choice == 0 ? filtered : all;
        } else if (filterActive && filtered.isEmpty()) {
            boolean confirmed = AppDialog.confirm(
                    parent,
                    AppMessages.get("mainframe.export.dialog.title"),
                    AppMessages.get("mainframe.export.empty.filtered", "絞り込み結果が0件です。全件をエクスポートしますか？"));
            if (!confirmed) {
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(AppMessages.get("mainframe.export.dialog.title"));
        fileChooser.setSelectedFile(new File("tasks_export.json"));

        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                CsvUtil.exportToJson(selectedFile, toExport);
                NoticeBar.info(
                        parent,
                        AppMessages.format(
                                "mainframe.export.success.count",
                                "エクスポートが正常に完了しました（{0}件）。\n保存先: {1}",
                                toExport.size(),
                                selectedFile.getAbsolutePath()));
            } catch (Exception e) {
                ErrorDialogUtil.showError(
                        parent,
                        AppMessages.format("mainframe.export.failed", "エクスポートに失敗しました: {0}", e.getMessage()));
                logger.error("エクスポートエラー", e);
            }
        }
    }

    public static void importTasks(Component parent, TaskService taskService) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(AppMessages.get("mainframe.import.dialog.title"));

        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            boolean confirmed = AppDialog.confirm(
                    parent,
                    AppMessages.get("mainframe.import.confirm.title"),
                    AppMessages.get("mainframe.import.confirm"));

            if (confirmed) {
                try {
                    List<Task> importedTasks = CsvUtil.importFromJson(selectedFile);
                    taskService.importTasks(importedTasks, false);
                    NoticeBar.info(parent, AppMessages.get("mainframe.import.success"));
                } catch (Exception e) {
                    AppException appException = ErrorDialogUtil.findAppException(e);
                    String message = appException != null
                            ? appException.getUserMessage()
                            : AppMessages.format("mainframe.import.failed", "インポートに失敗しました: {0}", e.getMessage());
                    ErrorDialogUtil.showError(parent, message);
                    logger.error("インポートエラー", e);
                }
            }
        }
    }
}
