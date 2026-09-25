package org.example.event;

/**
 * タスクイベントを購読（リスン）するためのインターフェース
 */
@FunctionalInterface
public interface TaskEventListener {
    /**
     * タスクイベントが発生したときに呼び出されるメソッド
     *
     * @param event 発生したイベント
     */
    void onTaskEvent(TaskEvent event);
}