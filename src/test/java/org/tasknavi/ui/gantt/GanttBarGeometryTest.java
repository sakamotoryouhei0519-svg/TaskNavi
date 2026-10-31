package org.tasknavi.ui.gantt;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void progressLabelStaysOffThePaleRemainderWhenFillIsShort() {
        GanttBarGeometry.ProgressLabel label = GanttBarGeometry.progressLabel(0, 1000, 340, 28);
        assertEquals(GanttBarGeometry.ProgressLabelPlace.ON_TRACK, label.place());
        assertEquals(346, label.x());
    }

    @Test
    void progressLabelSitsOnTheFillWhenMostOfTheBarIsDone() {
        GanttBarGeometry.ProgressLabel label = GanttBarGeometry.progressLabel(0, 1000, 600, 28);
        assertEquals(GanttBarGeometry.ProgressLabelPlace.ON_FILL, label.place());
        assertEquals(566, label.x());
    }

    @Test
    void progressLabelOfALongBarStaysInsideTheVisiblePart() {
        GanttBarGeometry.ProgressLabel pastFill = GanttBarGeometry.progressLabel(0, 5000, 2250, 28, 3000, 4000);
        assertEquals(GanttBarGeometry.ProgressLabelPlace.ON_TRACK, pastFill.place());
        assertEquals(3006, pastFill.x());

        GanttBarGeometry.ProgressLabel nearFillEnd = GanttBarGeometry.progressLabel(0, 5000, 2250, 28, 2000, 3000);
        assertEquals(2256, nearFillEnd.x());
    }

    @Test
    void cellRangeCoversOnlyTheRowsInView() {
        assertArrayEquals(new int[]{3, 6}, GanttBarGeometry.cellRange(100, 160, 30, 1000));
        assertArrayEquals(new int[]{0, 2}, GanttBarGeometry.cellRange(-50, 60, 30, 1000));
        assertArrayEquals(new int[]{8, 10}, GanttBarGeometry.cellRange(250, 900, 30, 10));
        assertArrayEquals(new int[]{10, 10}, GanttBarGeometry.cellRange(400, 500, 30, 10));
        assertArrayEquals(new int[]{0, 0}, GanttBarGeometry.cellRange(0, 500, 30, 0));
    }

    @Test
    void barsAndTheirOutsideLabelOffScreenAreSkipped() {
        assertTrue(GanttBarGeometry.outsideView(2000, 100, 0, 1000));
        assertTrue(GanttBarGeometry.outsideView(0, 100, 1000, 2000));
        assertFalse(GanttBarGeometry.outsideView(950, 100, 1000, 2000));
        assertFalse(GanttBarGeometry.outsideView(500, 3000, 1000, 2000));
    }
}
