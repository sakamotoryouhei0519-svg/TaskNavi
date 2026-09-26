package org.tasknavi.ui.wbs;

import org.tasknavi.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WbsFormValidatorTest {

    @Test
    void parseOptionalDateAcceptsBlankAndIso() {
        assertNull(WbsFormValidator.parseOptionalDate(""));
        assertNull(WbsFormValidator.parseOptionalDate("  "));
        assertEquals(LocalDate.of(2026, 9, 25), WbsFormValidator.parseOptionalDate("2026-09-25"));
        assertNull(WbsFormValidator.parseOptionalDate("not-a-date"));
    }

    @Test
    void nameForLevelPicksCorrectField() {
        assertEquals("P", WbsFormValidator.nameForLevel(1, "P", "S", "T"));
        assertEquals("S", WbsFormValidator.nameForLevel(2, "P", "S", "T"));
        assertEquals("T", WbsFormValidator.nameForLevel(3, "P", "S", "T"));
    }

    @Test
    void validateRejectsBadDateFormat() {
        String error = WbsFormValidator.validate("Name", "bad", "", null);
        assertNotNull(error);
    }

    @Test
    void validateAcceptsValidDraft() {
        Task root = new Task(1, "Root", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        String error = WbsFormValidator.validate(
                "Child", "2026-02-01", "2026-02-10", root);
        assertNull(error);
    }
}
