package org.tasknavi.ui.taskdialog;

import org.tasknavi.AppMessages;
import org.tasknavi.EntryType;

import java.time.LocalDate;

/**
 * TaskDialog 保存前の入力検証。
 */
public final class TaskDialogSaveValidator {

    public record Result(boolean ok, String message) {
        public static Result success() {
            return new Result(true, null);
        }

        public static Result error(String message) {
            return new Result(false, message);
        }
    }

    private TaskDialogSaveValidator() {
    }

    public static Result validate(String name, String entryType, Integer parentProjectId, String startDate, String endDate) {
        if (name == null || name.trim().isEmpty()) {
            return Result.error(AppMessages.get("dialog.error.name.required", "名前を入力してください。"));
        }

        if (!EntryType.PROJECT.name().equals(entryType) && parentProjectId == null) {
            return Result.error(AppMessages.get(
                    "dialog.error.parent.required",
                    "工程・タスクは親プロジェクトまたは親工程を選択してから登録してください。"));
        }

        try {
            LocalDate.parse(startDate == null ? "" : startDate.trim());
            LocalDate.parse(endDate == null ? "" : endDate.trim());
        } catch (Exception ignored) {
            return Result.error(AppMessages.get(
                    "dialog.error.date.format",
                    "日付は YYYY-MM-DD 形式で入力してください。"));
        }

        return Result.success();
    }
}
