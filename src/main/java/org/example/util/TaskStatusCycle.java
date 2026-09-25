package org.example.util;

import org.example.Task;

/**
 * ステータスの前後移動と、進捗からの推定。
 * カンバン／カレンダーなど複数画面で共有する。
 */
public final class TaskStatusCycle {

    private TaskStatusCycle() {
    }

    public static String next(String current) {
        if (Task.STATUS_NOT_STARTED.equals(current)) {
            return Task.STATUS_IN_PROGRESS;
        }
        if (Task.STATUS_IN_PROGRESS.equals(current)) {
            return Task.STATUS_COMPLETED;
        }
        return Task.STATUS_COMPLETED;
    }

    public static String previous(String current) {
        if (Task.STATUS_COMPLETED.equals(current)) {
            return Task.STATUS_IN_PROGRESS;
        }
        if (Task.STATUS_IN_PROGRESS.equals(current)) {
            return Task.STATUS_NOT_STARTED;
        }
        return Task.STATUS_NOT_STARTED;
    }

    public static String fromProgress(int progress) {
        if (progress >= 100) {
            return Task.STATUS_COMPLETED;
        }
        if (progress > 0) {
            return Task.STATUS_IN_PROGRESS;
        }
        return Task.STATUS_NOT_STARTED;
    }
}
