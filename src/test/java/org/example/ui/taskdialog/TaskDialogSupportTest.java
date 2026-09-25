package org.example.ui.taskdialog;

import org.example.EntryType;
import org.example.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskDialogSupportTest {

    @Test
    void saveValidatorRejectsEmptyNameAndBadDates() {
        assertFalse(TaskDialogSaveValidator.validate("", EntryType.PROJECT.name(), null, "2026-01-01", "2026-01-02").ok());
        assertFalse(TaskDialogSaveValidator.validate("x", EntryType.PHASE.name(), null, "2026-01-01", "2026-01-02").ok());
        assertFalse(TaskDialogSaveValidator.validate("x", EntryType.PROJECT.name(), null, "bad", "2026-01-02").ok());
        assertTrue(TaskDialogSaveValidator.validate("x", EntryType.PROJECT.name(), null, "2026-01-01", "2026-01-02").ok());
    }

    @Test
    void parentOptionsForPhaseListOnlyProjects() {
        Task project = new Task(1, "P", null, 1, 0, 0, "未着手", LocalDate.now(), LocalDate.now());
        Task phase = new Task(2, "Ph", 1, 2, 0, 0, "未着手", LocalDate.now(), LocalDate.now());
        Map<String, Task> options = TaskDialogParentSupport.buildOptions(
                EntryType.PHASE.name(), List.of(project, phase));
        assertEquals(1, options.size());
        assertEquals(project, options.values().iterator().next());
    }

    @Test
    void titlesDistinguishNewAndProjectEdit() {
        assertTrue(TaskDialogTitles.forTask(null).length() > 0);
        Task project = new Task(1, "P", null, 1, 0, 0, "未着手", LocalDate.now(), LocalDate.now());
        assertTrue(TaskDialogTitles.isProjectTask(project));
        assertNull(TaskDialogParentSupport.selectedParent(null, Map.of()));
    }
}
