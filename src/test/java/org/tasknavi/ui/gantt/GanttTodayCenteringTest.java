package org.tasknavi.ui.gantt;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GanttTodayCenteringTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @Test
    void windowOfPastTasksIsStretchedToIncludeToday() {
        LocalDate[] tasks = {LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)};

        LocalDate[] window = GanttDisplayDates.includeToday(tasks, TODAY);

        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 4, 1), TODAY.plusDays(GanttDisplayDates.TODAY_MARGIN_DAYS)}, window);
    }

    @Test
    void windowAlreadyAroundTodayIsKept() {
        LocalDate[] tasks = {LocalDate.of(2026, 4, 1), LocalDate.of(2026, 12, 25)};

        assertArrayEquals(tasks, GanttDisplayDates.includeToday(tasks, TODAY));
    }

    @Test
    void todayColumnLandsInTheMiddleOfTheVisibleWidth() {
        int x = GanttViewportSupport.centeredScrollX(100, 35, 1000, 7000);

        assertEquals(100 * 35 + 17 - 500, x);
    }

    @Test
    void monthLabelFollowsTheVisibleLeftEdgeAndStopsBeforeTheMonthEnds() {
        assertEquals(108, GanttBarGeometry.stickyLabelX(100, 1000, 0, 60, 8));
        assertEquals(508, GanttBarGeometry.stickyLabelX(100, 1000, 500, 60, 8));
        assertEquals(932, GanttBarGeometry.stickyLabelX(100, 1000, 990, 60, 8));
    }

    @Test
    void scrollStopsAtBothEnds() {
        assertEquals(0, GanttViewportSupport.centeredScrollX(3, 35, 1000, 7000));
        assertEquals(6000, GanttViewportSupport.centeredScrollX(199, 35, 1000, 7000));
    }
}
