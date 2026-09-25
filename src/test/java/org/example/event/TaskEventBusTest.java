package org.example.event;

import org.example.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TaskEventBusTest {

    private final TaskEventBus bus = TaskEventBus.getInstance();
    private TaskEventListener listener;

    @AfterEach
    void tearDown() {
        if (listener != null) {
            bus.unregister(listener);
        }
    }

    @Test
    void refreshAllWithNullTaskDoesNotThrow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TaskEvent> received = new AtomicReference<>();
        listener = event -> {
            received.set(event);
            latch.countDown();
        };
        bus.register(listener);

        assertDoesNotThrow(() -> bus.post(new TaskEvent(TaskEvent.Type.REFRESH_ALL, null)));
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(TaskEvent.Type.REFRESH_ALL, received.get().getType());
        assertNull(received.get().getTask());
    }

    @Test
    void unregisterStopsDelivery() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        listener = event -> latch.countDown();
        bus.register(listener);
        bus.unregister(listener);

        bus.post(new TaskEvent(TaskEvent.Type.TASK_UPDATED, new Task(1, "t", null, 1, 0, 0, "未着手", null, null)));
        assertFalse(latch.await(300, TimeUnit.MILLISECONDS));
    }
}
