package org.tasknavi.ui.gantt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GanttMetricsTest {

    @Test
    void adjustDayWidthZoomsWithinBounds() {
        assertEquals(GanttMetrics.DEFAULT_DAY_WIDTH + GanttMetrics.DAY_WIDTH_STEP,
                GanttMetrics.adjustDayWidth(GanttMetrics.DEFAULT_DAY_WIDTH, -1));
        assertEquals(GanttMetrics.DEFAULT_DAY_WIDTH - GanttMetrics.DAY_WIDTH_STEP,
                GanttMetrics.adjustDayWidth(GanttMetrics.DEFAULT_DAY_WIDTH, 1));
        assertEquals(GanttMetrics.DAY_WIDTH_MAX,
                GanttMetrics.adjustDayWidth(GanttMetrics.DAY_WIDTH_MAX, -1));
        assertEquals(GanttMetrics.DAY_WIDTH_MIN,
                GanttMetrics.adjustDayWidth(GanttMetrics.DAY_WIDTH_MIN, 1));
    }

    @Test
    void dragSessionClearsAfterBeginNull() {
        GanttDragSession session = new GanttDragSession();
        session.begin(null, 10);
        assertTrue(!session.isActive());
    }
}
