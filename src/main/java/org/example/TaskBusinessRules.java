package org.example;

import java.time.LocalDate;

/**
 * 【業務ルール集約クラス】
 * 画面側のコードで分散していた「状態と進捗の関係」「親プロジェクトとの日付ルール」を
 * ここに集約します。
 *
 * こうすることで、WBS・カンバン・ガントチャートの各画面から同じ判断ロジックを
 * 呼び出せるようになり、同じ条件が複数箇所でズレる事故を減らせます。
 */
public final class TaskBusinessRules {
    private TaskBusinessRules() {
        // Utility class; no instance creation needed.
    }

    public static final class ValidationResult {
       private final boolean valid;
       private final String message;

       private ValidationResult(boolean valid, String message) {
           this.valid = valid;
           this.message = message;
        }

       public static ValidationResult valid() {
           return new ValidationResult(true, null);
       }

       public static ValidationResult invalid(String message) {
           return new ValidationResult(false, message);
       }

       public boolean isValid() {
           return valid;
       }

       public String getMessage() {
           return message;
       }
    }

    /**
     * 【状態に応じた進捗率を計算する】
     * 画面のボタン操作やフォーム保存時に、状態と進捗率の整合性を保つために使います。
     */
    public static int resolveProgress(String status, Integer currentProgress) {
       if (status == null) {
           return currentProgress != null ? currentProgress : 0;
       }

       return switch (TaskStatus.fromString(status)) {
           case NOT_STARTED -> 0;
           case IN_PROGRESS -> (currentProgress == null || currentProgress <= 0 || currentProgress >= 100)
                   ? 50
                   : currentProgress;
           case COMPLETED -> 100;
       };
    }

    /** {@link #resolveProgress(String, Integer)} の Enum オーバーロード。 */
    public static int resolveProgress(TaskStatus status, Integer currentProgress) {
        return resolveProgress(status != null ? status.name() : null, currentProgress);
    }

    public static ValidationResult validateTaskDraft(String name, LocalDate taskStart, LocalDate taskEnd, Task parentProject) {
       if (name == null || name.trim().isEmpty()) {
           return ValidationResult.invalid(
                   AppMessages.get("validation.task.name.required", "タスク名を入力してください。"));
       }
       if (taskStart != null && taskEnd != null && taskEnd.isBefore(taskStart)) {
           return ValidationResult.invalid(
                   AppMessages.get("validation.task.end.before.start", "終了日は開始日以降の日付を指定してください。"));
       }
       if (parentProject != null) {
           return validateProjectDateRangeResult(parentProject, taskStart, taskEnd);
       }
       return ValidationResult.valid();
    }

    /**
     * 【親プロジェクトとの日付整合性を検査する】
     * 子タスクが親プロジェクトの期間外に出ていないかを確認します。
     *
     * @return エラーがあればメッセージ、なければ null
     */
    public static String validateProjectDateRange(Task parentProject, LocalDate taskStart, LocalDate taskEnd) {
       return validateProjectDateRangeResult(parentProject, taskStart, taskEnd).getMessage();
    }

    /**
     * 【日付整合性の検証を結果オブジェクトで返す】
     * 画面側でより明示的に「成功/失敗」とメッセージを扱えるようにします。
     */
    public static ValidationResult validateProjectDateRangeResult(Task parentProject, LocalDate taskStart, LocalDate taskEnd) {
       if (parentProject == null) {
           return ValidationResult.valid();
       }

       LocalDate projectStart = parentProject.getStartDate();
       LocalDate projectEnd = parentProject.getEndDate();

       if (projectStart != null && taskStart != null && taskStart.isBefore(projectStart)) {
           return ValidationResult.invalid(
                   AppMessages.format(
                           "validation.task.start.before.project",
                           "タスクの開始日 ({0}) は親プロジェクトの開始日 ({1}) より前に設定できません。",
                           taskStart, projectStart));
       }

       if (projectEnd != null && taskEnd != null && taskEnd.isAfter(projectEnd)) {
           return ValidationResult.invalid(
                   AppMessages.format(
                           "validation.task.end.after.project",
                           "タスクの終了日 ({0}) は親プロジェクトの終了日 ({1}) より後に設定できません。",
                           taskEnd, projectEnd));
       }

       if (taskStart != null && taskEnd != null && taskEnd.isBefore(taskStart)) {
           return ValidationResult.invalid(
                   AppMessages.get("validation.task.end.before.start", "終了日は開始日以降の日付を指定してください。"));
       }

       return ValidationResult.valid();
    }
}
