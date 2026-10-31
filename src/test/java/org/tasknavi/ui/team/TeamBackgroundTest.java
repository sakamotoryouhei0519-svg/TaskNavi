package org.tasknavi.ui.team;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamBackgroundTest {

    private final List<String> calls = new CopyOnWriteArrayList<>();
    private final CountDownLatch finished = new CountDownLatch(1);
    private final TeamBackground background = new TeamBackground(new TeamBackground.Hooks() {
        @Override
        public void started() {
            calls.add("started");
        }

        @Override
        public void succeeded() {
            calls.add("succeeded");
        }

        @Override
        public void finished() {
            calls.add("finished");
            finished.countDown();
        }
    });

    @AfterEach
    void tearDown() {
        background.shutdown();
    }

    @Test
    void successIsHandedOverOnTheScreenThreadBetweenTheHooks() throws Exception {
        CountDownLatch release = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> background.run(() -> {
            release.await();
            return "読めた";
        }, result -> calls.add("success:" + result + ":" + SwingUtilities.isEventDispatchThread()),
                e -> calls.add("failure")));
        assertTrue(background.busy(), "結果を待つ間は読み込み中");
        release.countDown();

        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertEquals(List.of("started", "succeeded", "success:読めた:true", "finished"), calls);
        assertFalse(background.busy());
    }

    @Test
    void failureSkipsSucceededAndStillFinishes() throws Exception {
        SwingUtilities.invokeAndWait(() -> background.run(() -> {
            throw new IOException("ドライブが外れた");
        }, result -> calls.add("success"), e -> calls.add("failure:" + e.getMessage())));

        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertEquals(List.of("started", "failure:ドライブが外れた", "finished"), calls);
        assertFalse(background.busy());
    }
}
