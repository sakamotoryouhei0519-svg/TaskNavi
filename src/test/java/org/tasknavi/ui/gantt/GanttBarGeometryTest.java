package org.tasknavi.ui.gantt;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GanttBarGeometryTest {

    @Test
    void barRectComputesOffsetAndWidth() {
        GanttBarGeometry.Rect rect = GanttBarGeometry.barRect(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 5),
                2,
                20,
                40,
                20);
        assertNotNull(rect);
        assertEquals(40, rect.x());
        assertEquals(60, rect.width());
        assertEquals(2 * 40 + 10, rect.y());
    }

    @Test
    void dragModeDetectsHandles() {
        GanttBarGeometry.Rect bar = new GanttBarGeometry.Rect(100, 0, 80, 20);
        assertEquals(2, GanttBarGeometry.dragModeForX(102, bar, 8));
        assertEquals(3, GanttBarGeometry.dragModeForX(175, bar, 8));
        assertEquals(1, GanttBarGeometry.dragModeForX(140, bar, 8));
        assertEquals(0, GanttBarGeometry.dragModeForX(50, bar, 8));
        assertNull(GanttBarGeometry.barRect(null, null, null, 0, 10, 10, 10));
    }
}
