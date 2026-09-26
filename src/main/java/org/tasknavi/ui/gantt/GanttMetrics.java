package org.tasknavi.ui.gantt;

/**
 * ガントチャートのレイアウト定数。
 */
public final class GanttMetrics {

    public static final int TASK_NAME_WIDTH = 240;
    public static final int ROW_HEIGHT = 38;
    public static final int BAR_HEIGHT = 22;
    public static final int HEADER_HEIGHT = 60;
    public static final int RESIZE_HANDLE_WIDTH = 6;
    public static final int DAY_WIDTH_MIN = 24;
    public static final int DAY_WIDTH_MAX = 80;
    public static final int DAY_WIDTH_STEP = 3;
    public static final int DEFAULT_DAY_WIDTH = 35;
    public static final int MIN_VISIBLE_DAYS = 31;

    private GanttMetrics() {
    }

    public static int adjustDayWidth(int current, int wheelRotation) {
        if (wheelRotation < 0) {
            return Math.min(DAY_WIDTH_MAX, current + DAY_WIDTH_STEP);
        }
        return Math.max(DAY_WIDTH_MIN, current - DAY_WIDTH_STEP);
    }
}
