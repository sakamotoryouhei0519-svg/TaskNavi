package org.tasknavi.ui.gantt;

import org.tasknavi.Task;

import java.time.LocalDate;

/**
 * ガントバーの移動／リサイズ時の日付計算（親プロジェクト期間でクランプ）。
 */
public final class GanttBarDragMath {

    public static final class Result {
        private final LocalDate start;
        private final LocalDate end;

        public Result(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }
    }

    private GanttBarDragMath() {
    }

    /**
     * @param dragMode 1=移動, 2=左リサイズ, 3=右リサイズ
     * @param project  親プロジェクト（期間クランプ用。null ならクランプなし）
     */
    public static Result apply(
            int dragMode,
            LocalDate originalStart,
            LocalDate originalEnd,
            long diffDays,
            Task project
    ) {
        if (originalStart == null || originalEnd == null) {
            return new Result(originalStart, originalEnd);
        }

        LocalDate start = originalStart;
        LocalDate end = originalEnd;
        long duration = java.time.temporal.ChronoUnit.DAYS.between(originalStart, originalEnd);

        switch (dragMode) {
            case 1 -> {
                start = originalStart.plusDays(diffDays);
                end = start.plusDays(duration);
            }
            case 2 -> {
                start = originalStart.plusDays(diffDays);
                if (start.isAfter(end)) {
                    start = end;
                }
            }
            case 3 -> {
                end = originalEnd.plusDays(diffDays);
                if (end.isBefore(start)) {
                    end = start;
                }
            }
            default -> {
            }
        }

        return clampToProject(start, end, duration, dragMode, project);
    }

    private static Result clampToProject(
            LocalDate start,
            LocalDate end,
            long duration,
            int dragMode,
            Task project
    ) {
        if (project == null || project.getStartDate() == null || project.getEndDate() == null) {
            return new Result(start, end);
        }
        LocalDate pStart = project.getStartDate();
        LocalDate pEnd = project.getEndDate();

        if (dragMode == 1) {
            if (start.isBefore(pStart)) {
                start = pStart;
                end = start.plusDays(duration);
            }
            if (end.isAfter(pEnd)) {
                end = pEnd;
                start = end.minusDays(duration);
                if (start.isBefore(pStart)) {
                    start = pStart;
                }
            }
        } else {
            if (start.isBefore(pStart)) {
                start = pStart;
            }
            if (end.isAfter(pEnd)) {
                end = pEnd;
            }
            if (start.isAfter(end)) {
                start = end;
            }
        }
        return new Result(start, end);
    }
}
