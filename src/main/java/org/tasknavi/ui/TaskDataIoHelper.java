package org.tasknavi.ui;

import org.tasknavi.AppException;
import org.tasknavi.AppMessages;
import org.tasknavi.io.TaskCsv;
import org.tasknavi.io.TaskJson;
import org.tasknavi.model.Task;
import org.tasknavi.persistence.AppDataLocation;
import org.tasknavi.service.TaskService;
import org.tasknavi.util.TaskViewFilter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * タスクの書き出し・取り込みの画面操作。
 * CSV は Excel で編集して戻す用、JSON は TaskNavi 同士でそのまま移す用。
 */
public final class TaskDataIoHelper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskDataIoHelper.class);
    private static final int MAX_ERRORS_SHOWN = 8;
    private static final int MAX_ERROR_LENGTH = 70;

    private TaskDataIoHelper() {
    }

    // ---------------------------------------------------------------- 書き出し

    /**
     * @param keyword グローバル検索キーワード（null 可）
     * @param status  グローバル状態フィルタ（null / ALL 可）
     */
    public static void exportTasks(Component parent, TaskService taskService, String keyword, String status) {
        List<Task> all = taskService.getAllTasks();
        List<Task> toExport = chooseExportTarget(parent, all, keyword, status);
        if (toExport == null) {
            return;
        }

        FileNameExtensionFilter csvFilter = new FileNameExtensionFilter(
                AppMessages.get("mainframe.export.filter.csv", "CSV（Excel で開けます）"), "csv");
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter(
                AppMessages.get("mainframe.export.filter.json", "JSON（TaskNavi 同士の移行用）"), "json");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(AppMessages.get("mainframe.export.dialog.title"));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(csvFilter);
        chooser.addChoosableFileFilter(jsonFilter);
        chooser.setFileFilter(csvFilter);
        chooser.setSelectedFile(new File("tasknavi-tasks-" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        boolean json = isJson(chooser.getSelectedFile()) || chooser.getFileFilter() == jsonFilter;
        File file = withExtension(chooser.getSelectedFile(), json ? ".json" : ".csv");
        if (file.exists() && !AppDialog.confirm(parent,
                AppMessages.get("mainframe.export.dialog.title"),
                AppMessages.format("mainframe.export.overwrite", "「{0}」は既にあります。上書きしますか？", file.getName()))) {
            return;
        }
        try {
            if (json) {
                TaskJson.write(file.toPath(), toExport);
            } else {
                TaskCsv.write(file.toPath(), toExport);
            }
            NoticeBar.info(parent, AppMessages.format(
                    "mainframe.export.success.count",
                    "エクスポートが正常に完了しました（{0}件）。\n保存先: {1}",
                    toExport.size(),
                    file.getAbsolutePath()));
        } catch (Exception e) {
            ErrorDialogUtil.showError(parent,
                    AppMessages.format("mainframe.export.failed", "エクスポートに失敗しました: {0}", e.getMessage()));
            logger.error("エクスポートエラー", e);
        }
    }

    /** @return 書き出すタスク。やめたときは null */
    private static List<Task> chooseExportTarget(Component parent, List<Task> all, String keyword, String status) {
        List<Task> filtered = TaskViewFilter.apply(all, keyword, status, null, new HashMap<>());
        boolean filterActive = filtered.size() != all.size()
                || (keyword != null && !keyword.isBlank())
                || (status != null && !AppMessages.isFilterAll(status));
        if (!filterActive) {
            return all;
        }
        if (filtered.isEmpty()) {
            boolean confirmed = AppDialog.confirm(
                    parent,
                    AppMessages.get("mainframe.export.dialog.title"),
                    AppMessages.get("mainframe.export.empty.filtered", "絞り込み結果が0件です。全件をエクスポートしますか？"));
            return confirmed ? all : null;
        }
        int choice = AppDialog.choose(
                parent,
                AppMessages.get("mainframe.export.dialog.title"),
                AppMessages.get("mainframe.export.choose.message", "エクスポート対象を選んでください。"),
                AppMessages.format("mainframe.export.option.filtered", "絞り込み結果 ({0}件)", filtered.size()),
                AppMessages.format("mainframe.export.option.all", "全件 ({0}件)", all.size()),
                AppMessages.get("common.button.cancel", "キャンセル"));
        if (choice == 0) {
            return withAncestors(filtered, all);
        }
        return choice == 1 ? all : null;
    }

    /** 絞り込み結果に親（プロジェクト・工程）を足す。親がないと取り込み直したときに階層が組めない。 */
    static List<Task> withAncestors(List<Task> selected, List<Task> all) {
        Map<Integer, Task> byId = new HashMap<>();
        for (Task task : all) {
            byId.put(task.getId(), task);
        }
        Set<Integer> include = new HashSet<>();
        for (Task task : selected) {
            Task current = task;
            while (current != null && include.add(current.getId())) {
                Integer parentId = current.getParentId();
                current = parentId == null || parentId == 0 ? null : byId.get(parentId);
            }
        }
        List<Task> result = new ArrayList<>();
        for (Task task : all) {
            if (include.contains(task.getId())) {
                result.add(task);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- 取り込み

    public static void importTasks(Component parent, TaskService taskService) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(AppMessages.get("mainframe.import.dialog.title"));
        chooser.setFileFilter(new FileNameExtensionFilter(
                AppMessages.get("mainframe.import.filter", "CSV / JSON"), "csv", "txt", "tsv", "json"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();

        TaskCsv.ReadResult read;
        try {
            read = isJson(file)
                    ? new TaskCsv.ReadResult(TaskJson.read(file.toPath()), List.of())
                    : TaskCsv.read(file.toPath());
        } catch (Exception e) {
            ErrorDialogUtil.showError(parent,
                    AppMessages.format("mainframe.import.failed", "インポートに失敗しました: {0}", e.getMessage()));
            logger.error("インポートファイルの読み込みエラー", e);
            return;
        }

        String title = AppMessages.get("mainframe.import.dialog.title");
        if (read.tasks().isEmpty()) {
            ErrorDialogUtil.showError(parent, summarize(file.getName(), read));
            return;
        }
        int choice = AppDialog.choose(parent, title,
                summarize(file.getName(), read) + "\n\n"
                        + AppMessages.get("mainframe.import.choose", "取り込み方を選んでください。"),
                AppMessages.get("mainframe.import.option.append", "今のタスクに追加"),
                AppMessages.get("mainframe.import.option.replace", "すべて置き換え"),
                AppMessages.get("common.button.cancel", "キャンセル"));
        if (choice != 0 && choice != 1) {
            return;
        }
        boolean replace = choice == 1;

        Path backup = null;
        if (replace) {
            List<Task> current = taskService.getAllTasks();
            if (!AppDialog.confirm(parent, title, AppMessages.format(
                    "mainframe.import.replace.confirm",
                    "今のタスク {0} 件をすべて消して、取り込む内容に置き換えます。\n消す前のタスクはバックアップフォルダに CSV で残します。\nよろしいですか？",
                    current.size()))) {
                return;
            }
            try {
                backup = backupBeforeReplace(current);
            } catch (Exception e) {
                ErrorDialogUtil.showError(parent, AppMessages.format(
                        "mainframe.import.backup.failed",
                        "置き換え前のバックアップを作れなかったため、中止しました: {0}", e.getMessage()));
                logger.error("取り込み前バックアップの作成エラー", e);
                return;
            }
        }

        try {
            taskService.importTasks(read.tasks(), replace);
        } catch (Exception e) {
            AppException appException = ErrorDialogUtil.findAppException(e);
            ErrorDialogUtil.showError(parent, appException != null
                    ? appException.getUserMessage()
                    : AppMessages.format("mainframe.import.failed", "インポートに失敗しました: {0}", e.getMessage()));
            logger.error("インポートエラー", e);
            return;
        }

        StringBuilder done = new StringBuilder(AppMessages.format(
                "mainframe.import.success.count", "{0} 件を取り込みました。", read.tasks().size()));
        if (!read.errors().isEmpty()) {
            done.append(AppMessages.format("mainframe.import.success.skipped", "（{0} 行は飛ばしました）", read.errors().size()));
        }
        if (backup != null) {
            done.append('\n').append(AppMessages.format(
                    "mainframe.import.success.backup", "置き換え前のタスク: {0}", backup.toAbsolutePath()));
        }
        NoticeBar.info(parent, done.toString());
    }

    /** 取り込み前の確認文。件数の内訳と、取り込めない行の理由（多いときは先頭だけ）。 */
    static String summarize(String fileName, TaskCsv.ReadResult read) {
        int[] byLevel = new int[TaskCsv.MAX_LEVEL + 1];
        for (Task task : read.tasks()) {
            if (task.getLevel() >= 1 && task.getLevel() <= TaskCsv.MAX_LEVEL) {
                byLevel[task.getLevel()]++;
            }
        }
        StringBuilder text = new StringBuilder();
        if (read.tasks().isEmpty()) {
            text.append(AppMessages.format(
                    "mainframe.import.summary.none", "「{0}」から取り込めるタスクがありません。", fileName));
        } else {
            text.append(AppMessages.format(
                    "mainframe.import.summary.count",
                    "「{0}」から {1} 件を取り込めます（プロジェクト {2}・工程 {3}・タスク {4}）。",
                    fileName, read.tasks().size(), byLevel[1], byLevel[2], byLevel[3]));
        }
        List<TaskCsv.RowError> errors = read.errors();
        if (!errors.isEmpty()) {
            text.append("\n\n").append(AppMessages.format(
                    "mainframe.import.summary.errors", "取り込めない行が {0} 件あります（この行は飛ばします）:", errors.size()));
            for (int i = 0; i < Math.min(MAX_ERRORS_SHOWN, errors.size()); i++) {
                TaskCsv.RowError error = errors.get(i);
                text.append("\n  ").append(AppMessages.format(
                        "mainframe.import.summary.error.line", "{0} 行目: {1}",
                        String.valueOf(error.lineNumber()), shorten(error.message())));
            }
            if (errors.size() > MAX_ERRORS_SHOWN) {
                text.append("\n  ").append(AppMessages.format(
                        "mainframe.import.summary.more", "ほか {0} 件", errors.size() - MAX_ERRORS_SHOWN));
            }
        }
        return text.toString();
    }

    private static Path backupBeforeReplace(List<Task> current) throws Exception {
        Path dir = AppDataLocation.backupDirectory();
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = dir.resolve("before-import-" + stamp + ".csv");
        TaskCsv.write(file, current);
        return file;
    }

    private static String shorten(String message) {
        String oneLine = message.replace('\n', ' ');
        return oneLine.length() <= MAX_ERROR_LENGTH ? oneLine : oneLine.substring(0, MAX_ERROR_LENGTH) + "…";
    }

    private static boolean isJson(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static File withExtension(File file, String extension) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0 && name.substring(dot).equalsIgnoreCase(extension)) {
            return file;
        }
        String base = dot > 0 && (name.substring(dot).equalsIgnoreCase(".csv")
                || name.substring(dot).equalsIgnoreCase(".json")) ? name.substring(0, dot) : name;
        return new File(file.getParentFile(), base + extension);
    }
}
