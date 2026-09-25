package org.example.ui.wbs;

import org.example.AppMessages;
import org.example.Task;
import org.example.TaskBusinessRules;

import java.time.LocalDate;

/**
 * WBS 右側フォーム向けの入力検証。
 */
public final class WbsFormValidator {

    private WbsFormValidator() {
    }

    public static String validate(String name, String startDateText, String endDateText, Task rootProject) {
        LocalDate startDate = parseOptionalDate(startDateText);
        LocalDate endDate = parseOptionalDate(endDateText);

        if (startDateText != null && !startDateText.trim().isEmpty() && startDate == null) {
            return AppMessages.get("wbs.error.date.format", "日付の形式は YYYY-MM-DD で入力してください。");
        }
        if (endDateText != null && !endDateText.trim().isEmpty() && endDate == null) {
            return AppMessages.get("wbs.error.date.format", "日付の形式は YYYY-MM-DD で入力してください。");
        }

        TaskBusinessRules.ValidationResult result =
                TaskBusinessRules.validateTaskDraft(name, startDate, endDate, rootProject);
        return result.isValid() ? null : result.getMessage();
    }

    public static LocalDate parseOptionalDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateText.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** レベルに応じて検証対象の名称フィールドを選ぶ。 */
    public static String nameForLevel(int level, String projectName, String stageName, String taskName) {
        if (level == 1) {
            return projectName;
        }
        if (level == 2) {
            return stageName;
        }
        return taskName;
    }
}
