package org.tasknavi.ui;

import org.tasknavi.AppMessages;
import org.tasknavi.util.CsvUtil;
import org.tasknavi.ErrorDialogUtil;
import org.tasknavi.Task;
import org.tasknavi.TaskService;
import org.tasknavi.util.TaskViewFilter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
            Object[] options = {
                    AppMessages.format("mainframe.export.option.filtered", "絞り込み結果 ({0}件)", filtered.size()),
                    AppMessages.format("mainframe.export.option.all", "全件 ({0}件)", all.size()),
                    AppMessages.get("common.cancel", "キャンセル")
            };
            int choice = JOptionPane.showOptionDialog(
                    parent,
                    AppMessages.get("mainframe.export.choose.message", "エクスポート対象を選んでください。"),
                    AppMessages.get("mainframe.export.dialog.title"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
            toExport = choice == 0 ? filtered : all;
        } else if (filterActive && filtered.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(
                    parent,
                    AppMessages.get("mainframe.export.empty.filtered", "絞り込み結果が0件です。全件をエクスポートしますか？"),
                    AppMessages.get("mainframe.export.dialog.title"),
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
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
                JOptionPane.showMessageDialog(
                        parent,
                        AppMessages.format(
                                "mainframe.export.success.count",
                                "エクスポートが正常に完了しました（{0}件）。\n保存先: {1}",
                                toExport.size(),
                                selectedFile.getAbsolutePath()),
                        AppMessages.get("common.dialog.success.title"),
                        JOptionPane.INFORMATION_MESSAGE);
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
            int confirm = JOptionPane.showConfirmDialog(
                    parent,
                    AppMessages.get("mainframe.import.confirm"),
                    AppMessages.get("mainframe.import.confirm.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    List<Task> importedTasks = CsvUtil.importFromJson(selectedFile);
                    taskService.importTasks(importedTasks, false);
                    JOptionPane.showMessageDialog(
                            parent,
                            AppMessages.get("mainframe.import.success"),
                            AppMessages.get("common.dialog.success.title"),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    ErrorDialogUtil.showError(
                            parent,
                            AppMessages.format("mainframe.import.failed", "インポートに失敗しました: {0}", e.getMessage()));
                    logger.error("インポートエラー", e);
                }
            }
        }
    }
}
