package org.example.ui.gantt;

import java.time.LocalDate;

/**
 * 手動で指定した表示期間の扱い。
 */
public final class GanttDateWindow {

    private GanttDateWindow() {
    }

    /**
     * 手動表示期間が有効なら [start, end]、そうでなければ null。
     */
    public static LocalDate[] manualWindowOrNull(
            boolean manualVisibleWindow,
            LocalDate visibleStart,
            LocalDate visibleEnd
    ) {
        if (!manualVisibleWindow || visibleStart == null || visibleEnd == null) {
            return null;
        }
        if (visibleStart.isAfter(visibleEnd)) {
            return new LocalDate[]{visibleEnd, visibleStart};
        }
        return new LocalDate[]{visibleStart, visibleEnd};
    }
}
