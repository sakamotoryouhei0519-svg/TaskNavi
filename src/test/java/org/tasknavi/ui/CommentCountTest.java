package org.tasknavi.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommentCountTest {

    @BeforeEach
    void useJapanese() {
        AppMessages.setLocale(Locale.JAPAN);
    }

    @AfterEach
    void restoreLocale() {
        AppMessages.setLocale(Locale.getDefault());
    }

    @Test
    void showsNothingWithoutComments() {
        assertNull(CommentCount.label(task(List.of())));
    }

    @Test
    void showsHowManyComments() {
        Task task = task(List.of("見ておきます", "ありがとう"));

        assertEquals("\uD83D\uDCAC 2", CommentCount.label(task));
        assertEquals("コメント 2 件", CommentCount.tooltip(task));
    }

    private static Task task(List<String> comments) {
        Task task = new Task(1, "設計", null, 3, 0, 0, Task.STATUS_NOT_STARTED, null, null);
        task.setComments(comments);
        return task;
    }
}
