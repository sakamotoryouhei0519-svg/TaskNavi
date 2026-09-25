package org.example.persistence;

import org.example.AppException;
import org.example.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskWriteValidatorTest {

    @Test
    void rejectsBlankNameAndBadDates() {
        Task blank = new Task(0, "  ", null, 1, 0, 0, "未着手", LocalDate.now(), LocalDate.now());
        assertThrows(AppException.class, () -> TaskWriteValidator.validate(blank, id -> null));

        Task badDates = new Task(0, "X", null, 1, 0, 0, "未着手",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1));
        AppException ex = assertThrows(AppException.class,
                () -> TaskWriteValidator.validate(badDates, id -> null));
        assertTrue(ex.getUserMessage().contains("開始日") || ex.getMessage().contains("開始日"));
    }

    @Test
    void acceptsValidProject() {
        Task project = new Task(0, "P", null, 1, 0, 0, "未着手", LocalDate.now(), LocalDate.now().plusDays(1));
        assertDoesNotThrow(() -> TaskWriteValidator.validate(project, id -> null));
    }
}
