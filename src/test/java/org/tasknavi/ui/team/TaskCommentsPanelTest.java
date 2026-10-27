package org.tasknavi.ui.team;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamComment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCommentsPanelTest {

    @BeforeEach
    void useJapanese() {
        AppMessages.setLocale(Locale.JAPAN);
    }

    @AfterEach
    void restoreLocale() {
        AppMessages.setLocale(Locale.getDefault());
    }

    @Test
    void showsAuthorAndTimeAboveEachCommentOldestFirst() {
        List<TeamComment> comments = List.of(
                comment("鈴木", LocalDateTime.of(2026, 10, 27, 9, 5), "見ておきます"),
                comment("田中", LocalDateTime.of(2026, 10, 27, 14, 30), "ありがとう\n明日直します"));

        assertEquals("鈴木  10/27 09:05\n見ておきます\n\n田中  10/27 14:30\nありがとう\n明日直します",
                TaskCommentsPanel.format(comments));
    }

    @Test
    void saysSoWhenThereAreNoComments() {
        assertEquals("まだコメントはありません", TaskCommentsPanel.format(List.of()));
    }

    private static TeamComment comment(String author, LocalDateTime at, String text) {
        return new TeamComment("e-" + author, author, at.atZone(ZoneId.systemDefault()).toInstant(), text);
    }
}
