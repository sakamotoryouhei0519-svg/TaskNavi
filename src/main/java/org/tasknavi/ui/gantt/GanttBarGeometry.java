package org.tasknavi.ui.gantt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ガントバーの画面座標計算。
 */
public final class GanttBarGeometry {

    public record Rect(int x, int y, int width, int height) {
    }

    private GanttBarGeometry() {
    }

    public static Rect barRect(
            LocalDate minDate,
            LocalDate start,
            LocalDate end,
            int rowIndex,
            int dayWidth,
            int rowHeight,
            int barHeight
    ) {
        if (minDate == null || start == null || end == null) {
            return null;
        }
        long startOffset = ChronoUnit.DAYS.between(minDate, start);
        long duration = ChronoUnit.DAYS.between(start, end) + 1;
        if (startOffset < 0 || duration <= 0) {
            return null;
        }
        int x = (int) (startOffset * dayWidth);
        int width = (int) (duration * dayWidth);
        int y = rowIndex * rowHeight + (rowHeight - barHeight) / 2;
        return new Rect(x, y, width, barHeight);
    }

    /**
     * ヒット位置からドラッグモードを判定する。
     * 1=移動, 2=左リサイズ, 3=右リサイズ, 0=なし。
     */
    public static int dragModeForX(int mouseX, Rect bar, int resizeHandleWidth) {
        if (bar == null) {
            return 0;
        }
        int left = bar.x();
        int right = bar.x() + bar.width();
        if (mouseX < left || mouseX > right) {
            return 0;
        }
        if (mouseX <= left + resizeHandleWidth) {
            return 2;
        }
        if (mouseX >= right - resizeHandleWidth) {
            return 3;
        }
        return 1;
    }

    public enum ProgressLabelPlace {
        ON_FILL,
        ON_TRACK,
        OUTSIDE
    }

    public record ProgressLabel(int x, ProgressLabelPlace place) {
    }

    /**
     * 進捗の数字を、塗った部分か薄い残りのどちらかに収める。
     * バーの中央に置くと、進みが少ないとき白文字が白い残りに乗って読めなくなる。
     */
    public static ProgressLabel progressLabel(int barX, int barWidth, int progressWidth, int textWidth) {
        int pad = 6;
        int fillRoom = progressWidth - pad * 2;
        int restStart = barX + progressWidth;
        int restRoom = barWidth - progressWidth - pad * 2;
        boolean halfOrMore = progressWidth * 2 >= barWidth;
        if (fillRoom >= textWidth && (halfOrMore || restRoom < textWidth)) {
            return new ProgressLabel(barX + progressWidth - textWidth - pad, ProgressLabelPlace.ON_FILL);
        }
        if (restRoom >= textWidth) {
            return new ProgressLabel(restStart + pad, ProgressLabelPlace.ON_TRACK);
        }
        if (fillRoom >= textWidth) {
            return new ProgressLabel(barX + pad, ProgressLabelPlace.ON_FILL);
        }
        return new ProgressLabel(barX + barWidth + 4, ProgressLabelPlace.OUTSIDE);
    }
}
