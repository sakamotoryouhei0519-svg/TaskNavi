package org.tasknavi.event;

import org.tasknavi.Task;

/**
 * 発生したタスクイベントの情報を保持するクラス
 */
public class TaskEvent {

    /**
     * タスクイベントの種類を定義するEnum
     */
    public enum Type {
        TASK_CREATED,  // タスク新規作成
        TASK_UPDATED,  // タスク更新（ステータス、進捗、日付など）
        TASK_DELETED,  // タスク削除
        REFRESH_ALL    // 画面全体の再描画通知
    }

    private final Type type;
    private final Task task;

    public TaskEvent(Type type, Task task) {
        this.type = type;
        this.task = task;
    }

    public Type getType() {
        return type;
    }

    public Task getTask() {
        return task;
    }
}