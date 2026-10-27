package org.tasknavi.model;

/**
 * 状態と進捗率の関係。どの画面から状態を変えても、同じ進捗率になるようにここで決める。
 */
public final class TaskBusinessRules {
    private TaskBusinessRules() {
    }

    /** 状態に合わせた進捗率。未着手は 0、完了は 100、進行中は今の値（0 か 100 なら 50）。 */
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

    public static int resolveProgress(TaskStatus status, Integer currentProgress) {
        return resolveProgress(status != null ? status.name() : null, currentProgress);
    }
}
