package org.tasknavi.persistence;

import org.tasknavi.AppException;
import org.tasknavi.Task;

import java.util.function.Function;

/**
 * タスク保存前の入力・階層検証。
 */
public final class TaskWriteValidator {

    private TaskWriteValidator() {
    }

    public static void validate(Task task, Function<Integer, Task> taskById) {
        if (task == null) {
            throw AppException.validation("タスクデータが存在しません。", "保存するデータがありません。");
        }
        if (task.getName() == null || task.getName().isBlank()) {
            throw AppException.requiredField("名前");
        }
        if (task.getLevel() < 1 || task.getLevel() > 3) {
            throw AppException.businessRule(
                    "タスクのレベルが不正です: " + task.getLevel(),
                    "タスクの階層が不正です。");
        }
        if (task.getParentId() != null && task.getParentId() <= 0) {
            throw AppException.businessRule(
                    "親要素のIDが不正です: " + task.getParentId(),
                    "親要素の情報が不正です。");
        }
        if (task.getStartDate() != null && task.getEndDate() != null
                && task.getStartDate().isAfter(task.getEndDate())) {
            throw AppException.validation(
                    "開始日が終了日より後になっています。",
                    "開始日を終了日より前に設定してください。");
        }
        if (!TaskHierarchySupport.isValidHierarchy(task, taskById)) {
            throw AppException.businessRule(
                    "親子関係が不正です。 level=" + task.getLevel() + ", parentId=" + task.getParentId(),
                    "親子関係が正しく設定されていません。");
        }
    }
}
