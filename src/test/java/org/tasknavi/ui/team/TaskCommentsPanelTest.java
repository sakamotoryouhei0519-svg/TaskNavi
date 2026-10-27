package org.tasknavi.ui.team;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamComment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCommentsPanelTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 27, 9, 5);

    @BeforeEach
    void useJapanese() {
        AppMessages.setLocale(Locale.JAPAN);
    }

    @AfterEach
    void restoreLocale() {
        AppMessages.setLocale(Locale.getDefault());
    }

    @Test
    void showsAuthorAndTimeAboveTheText() {
        TeamComment comment = comment("ありがとう\n明日直します", false, false);

        assertEquals("鈴木  10/27 09:05", TaskCommentsPanel.headerText(comment));
        assertEquals("ありがとう\n明日直します", TaskCommentsPanel.bodyText(comment));
    }

    @Test
    void marksEditedComments() {
        assertEquals("鈴木  10/27 09:05  （編集済み）", TaskCommentsPanel.headerText(comment("直した文", true, false)));
    }

    @Test
    void deletedCommentsSaySoInsteadOfTheText() {
        TeamComment comment = comment("", true, true);

        assertEquals("鈴木  10/27 09:05", TaskCommentsPanel.headerText(comment));
        assertEquals("（削除されました）", TaskCommentsPanel.bodyText(comment));
    }

    private static TeamComment comment(String text, boolean edited, boolean deleted) {
        return new TeamComment("e1", "鈴木", "pc-suzuki", AT.atZone(ZoneId.systemDefault()).toInstant(), text, edited, deleted);
    }
}
