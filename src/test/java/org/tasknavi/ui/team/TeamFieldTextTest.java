package org.tasknavi.ui.team;

import org.junit.jupiter.api.Test;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TeamFieldTextTest {

    @Test
    void emptyValuesAreShownAsEmpty() {
        String empty = AppMessages.get("team.conflict.empty", "（空）");

        assertEquals(empty, TeamFieldText.value(TeamField.NAME, null));
        assertEquals(empty, TeamFieldText.value(TeamField.MEMO, "  "));
    }

    @Test
    void longMemosBecomeOneShortLine() {
        String memo = "1行目\n" + "あ".repeat(50);

        String shown = TeamFieldText.value(TeamField.MEMO, memo);

        assertEquals(41, shown.length());
        assertEquals("1行目 " + "あ".repeat(36) + "…", shown);
        assertEquals("短いメモ", TeamFieldText.value(TeamField.COMMENT, " 短いメモ "));
    }

    @Test
    void progressGetsAPercentSignAndOtherValuesStayAsTheyAre() {
        assertEquals("40%", TeamFieldText.value(TeamField.PROGRESS, "40"));
        assertEquals("2026-11-30", TeamFieldText.value(TeamField.END, "2026-11-30"));
    }

    @Test
    void everyFieldHasALabel() {
        for (TeamField field : TeamField.values()) {
            assertFalse(TeamFieldText.label(field).isBlank(), field.name());
        }
    }
}
