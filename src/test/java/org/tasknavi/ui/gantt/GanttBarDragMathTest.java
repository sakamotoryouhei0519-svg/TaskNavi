package org.tasknavi.ui.gantt;

import org.tasknavi.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttBarDragMathTest {

    @Test
    void leftResizeCannotGoAfterEnd() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 10);
        GanttBarDragMath.Result r = GanttBarDragMath.apply(2, start, end, 20, null);
        assertEquals(end, r.getStart());
        assertEquals(end, r.getEnd());
    }

    @Test
    void rightResizeCannotGoBeforeStart() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 10);
        GanttBarDragMath.Result r = GanttBarDragMath.apply(3, start, end, -20, null);
        assertEquals(start, r.getStart());
        assertEquals(start, r.getEnd());
    }

    @Test
    void moveKeepsDurationAndClampsToProject() {
        LocalDate start = LocalDate.of(2026, 9, 5);
        LocalDate end = LocalDate.of(2026, 9, 7);
        Task project = new Task(1, "P", null, 1, 0, 0, Task.STATUS_NOT_STARTED,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));

        GanttBarDragMath.Result r = GanttBarDragMath.apply(1, start, end, -10, project);
        assertEquals(LocalDate.of(2026, 9, 1), r.getStart());
        assertEquals(LocalDate.of(2026, 9, 3), r.getEnd());
        assertEquals(2, java.time.temporal.ChronoUnit.DAYS.between(r.getStart(), r.getEnd()));
        assertTrue(!r.getStart().isBefore(project.getStartDate()));
        assertTrue(!r.getEnd().isAfter(project.getEndDate()));
    }
}
