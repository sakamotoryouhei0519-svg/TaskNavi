package org.example.event;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * タスクイベントの登録と通知を管理するイベントバス（シングルトン）
 */
public class TaskEventBus {
    private static final Logger logger = LoggerFactory.getLogger(TaskEventBus.class);
    private static final TaskEventBus INSTANCE = new TaskEventBus();

    private final List<TaskEventListener> listeners = new CopyOnWriteArrayList<>();

    private TaskEventBus() {
    }

    public static TaskEventBus getInstance() {
        return INSTANCE;
    }

    public void register(TaskEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.debug("リスナーが登録されました。現在の総数: {}", listeners.size());
        }
    }

    public void unregister(TaskEventListener listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
        logger.debug("リスナーが解除されました。現在の総数: {}", listeners.size());
    }

    /**
     * イベントを発行し、すべてのリスナーに通知する（SwingのEDTスレッドで実行）
     */
    public void post(TaskEvent event) {
        if (event == null) {
            return;
        }

        Integer taskId = event.getTask() != null ? event.getTask().getId() : null;
        logger.debug("イベント発行: {} (対象タスクID: {})", event.getType(), taskId);

        Runnable notify = () -> {
            for (TaskEventListener listener : listeners) {
                try {
                    listener.onTaskEvent(event);
                } catch (Exception e) {
                    logger.error("イベント通知中にエラーが発生しました", e);
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            notify.run();
        } else {
            SwingUtilities.invokeLater(notify);
        }
    }
}
