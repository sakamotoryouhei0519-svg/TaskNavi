package org.example.ui;

import org.example.AppMessages;
import org.example.CsvUtil;
import org.example.ErrorDialogUtil;
import org.example.Task;
import org.example.TaskService;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.util.List;

/**
 * MainFrame から切り出したタスクのインポート／エクスポート UI ヘルパー。
 */
public final class TaskDataIoHelper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskDataIoHelper.class);

    private TaskDataIoHelper() {
    }

    public static void exportTasks(Component parent, TaskService taskService) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(AppMessages.get("mainframe.export.dialog.title"));
        fileChooser.setSelectedFile(new File("tasks_export.json"));

        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                List<Task> tasks = taskService.getAllTasks();
                CsvUtil.exportToJson(selectedFile, tasks);
                JOptionPane.showMessageDialog(
                        parent,
                        AppMessages.format(
                                "mainframe.export.success",
                                "エクスポートが正常に完了しました。\n保存先: {0}",
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
