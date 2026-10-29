package org.tasknavi.ui.gantt;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.UiLabels;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * ガントチャートの描画ヘルパー（状態非依存）。
 * 地は1行おきの色・土日の列・今日の列で見分け、罫線は細く薄くする（月の区切りだけ濃く）。
 * 描く順は、1行おきの地 → 選択・触れている行 → 土日と今日の列と区切り → バー → 今日の線。
 */
public final class GanttPainter {
    private static final int MONTH_ROW_HEIGHT = 22;

    private GanttPainter() {
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    public static void drawHeader(
            Graphics2D g2,
            LocalDate minDate,
            int offsetY,
            int totalDays,
            int dayWidth,
            int headerHeight,
            int visibleX
    ) {
        LocalDate today = LocalDate.now();
        int width = totalDays * dayWidth;
        int dayRowY = offsetY + MONTH_ROW_HEIGHT;
        int dayRowHeight = headerHeight - MONTH_ROW_HEIGHT;

        g2.setColor(AppTheme.PANEL_BG);
        g2.fillRect(0, offsetY, width, headerHeight);

        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern(
                AppMessages.get("gantt.date.format.yearmonth", "yyyy年 M月"));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics monthMetrics = g2.getFontMetrics();
        int monthStart = 0;
        while (monthStart < totalDays) {
            LocalDate first = minDate.plusDays(monthStart);
            int monthEnd = monthStart + 1;
            while (monthEnd < totalDays && minDate.plusDays(monthEnd).getDayOfMonth() != 1) {
                monthEnd++;
            }
            int startX = monthStart * dayWidth;
            int endX = monthEnd * dayWidth;
            if (monthStart > 0) {
                g2.setColor(AppTheme.ganttMonthLine());
                g2.fillRect(startX, offsetY, 2, headerHeight);
            }
            String label = first.format(monthFormat);
            int labelX = GanttBarGeometry.stickyLabelX(startX, endX, visibleX, monthMetrics.stringWidth(label), 8);
            g2.setColor(AppTheme.textStrong());
            g2.drawString(label, labelX, offsetY + 16);
            monthStart = monthEnd;
        }

        for (int i = 0; i < totalDays; i++) {
            int x = i * dayWidth;
            LocalDate date = minDate.plusDays(i);
            if (isWeekend(date)) {
                g2.setColor(AppTheme.ganttWeekend());
                g2.fillRect(x, dayRowY, dayWidth, dayRowHeight);
            }
            if (date.getDayOfMonth() != 1) {
                g2.setColor(AppTheme.ganttDayLine());
                g2.fillRect(x, dayRowY, 1, dayRowHeight);
            }
            if (dayWidth < 18) {
                continue;
            }

            DayOfWeek dow = date.getDayOfWeek();
            boolean isToday = date.equals(today);
            Shape oldClip = g2.getClip();
            g2.clipRect(x, dayRowY, dayWidth, dayRowHeight);

            String dayText = String.valueOf(date.getDayOfMonth());
            g2.setFont(new Font("SansSerif", Font.BOLD, dayWidth >= 26 ? 12 : 10));
            FontMetrics dayMetrics = g2.getFontMetrics();
            int dayTextX = x + (dayWidth - dayMetrics.stringWidth(dayText)) / 2;
            int dayBaseline = dayRowY + 16;
            if (isToday) {
                int chip = Math.min(dayWidth - 4, 24);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PRIMARY);
                g2.fillOval(x + (dayWidth - chip) / 2, dayBaseline - 14, chip, chip - 4);
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(dow == DayOfWeek.SUNDAY ? AppTheme.sundayText()
                        : dow == DayOfWeek.SATURDAY ? AppTheme.saturdayText() : AppTheme.textStrong());
            }
            g2.drawString(dayText, dayTextX, dayBaseline);

            String dayOfWeekStr = UiLabels.weekdaysMondayFirst()[dow.getValue() - 1];
            g2.setFont(new Font("SansSerif", Font.PLAIN, dayWidth >= 28 ? 10 : 9));
            FontMetrics weekMetrics = g2.getFontMetrics();
            g2.setColor(isToday ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED);
            g2.drawString(dayOfWeekStr, x + (dayWidth - weekMetrics.stringWidth(dayOfWeekStr)) / 2, dayRowY + 32);
            g2.setClip(oldClip);
        }

        g2.setColor(AppTheme.BORDER_COLOR);
        g2.fillRect(0, offsetY + headerHeight - 1, width, 1);
    }

    /** 今日の列に、上から下まで線を引く。 */
    public static void drawTodayLine(
            Graphics2D g2,
            LocalDate minDate,
            int totalY,
            int offsetY,
            int totalDays,
            int dayWidth
    ) {
        long daysFromMin = ChronoUnit.DAYS.between(minDate, LocalDate.now());
        if (daysFromMin >= 0 && daysFromMin < totalDays) {
            int todayX = (int) (daysFromMin * dayWidth) + (dayWidth / 2);
            g2.setColor(AppTheme.PRIMARY);
            g2.fillRect(todayX - 1, offsetY, 2, totalY - offsetY);
        }
    }

    public static void drawRowStripes(Graphics2D g2, int rowCount, int rowHeight, int width) {
        g2.setColor(AppTheme.ganttStripe());
        for (int row = 1; row < rowCount; row += 2) {
            g2.fillRect(0, row * rowHeight, width, rowHeight);
        }
    }

    public static void overlayDayColumns(
            Graphics2D g2,
            LocalDate minDate,
            int height,
            int totalDays,
            int dayWidth
    ) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < totalDays; i++) {
            int x = i * dayWidth;
            LocalDate date = minDate.plusDays(i);
            if (date.equals(today)) {
                g2.setColor(AppTheme.ganttToday());
                g2.fillRect(x, 0, dayWidth, height);
            } else if (isWeekend(date)) {
                g2.setColor(AppTheme.ganttWeekend());
                g2.fillRect(x, 0, dayWidth, height);
            }
            if (date.getDayOfMonth() == 1 && i > 0) {
                g2.setColor(AppTheme.ganttMonthLine());
                g2.fillRect(x, 0, 2, height);
            } else {
                g2.setColor(AppTheme.ganttDayLine());
                g2.fillRect(x, 0, 1, height);
            }
        }
    }

    public static void drawTaskNameCell(
            Graphics2D g2,
            Task task,
            int y,
            int rowIndex,
            Integer selectedTaskId,
            Integer hoveredTaskId,
            int taskNameWidth,
            int rowHeight
    ) {
        boolean selected = selectedTaskId != null && task.getId() == selectedTaskId;
        boolean hovered = hoveredTaskId != null && task.getId() == hoveredTaskId;
        Color rowBg;
        if (selected) {
            rowBg = AppTheme.selectedFill();
        } else if (hovered) {
            rowBg = AppTheme.hoverFill();
        } else {
            rowBg = rowIndex % 2 == 1 ? AppTheme.ganttStripe() : AppTheme.PANEL_BG;
        }
        g2.setColor(rowBg);
        g2.fillRect(0, y, taskNameWidth, rowHeight);
        if (selected) {
            g2.setColor(AppTheme.PRIMARY);
            g2.fillRect(0, y, 3, rowHeight);
        }

        int indent = 12 + (Math.max(0, task.getLevel() - 1) * 16);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AppTheme.getStatusColor(task.getStatusCode()));
        g2.fillOval(indent, y + rowHeight / 2 - 4, 8, 8);

        g2.setFont(task.getLevel() == 1 ? AppTheme.FONT_HEADER : AppTheme.FONT_MAIN);
        g2.setColor(task.getLevel() == 1 ? AppTheme.textStrong() : AppTheme.TEXT_PRIMARY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(task.getName(), indent + 16, y + (rowHeight + fm.getAscent() - fm.getDescent()) / 2);

        g2.setColor(AppTheme.BORDER_COLOR);
        g2.fillRect(taskNameWidth - 1, y, 1, rowHeight);
    }

    public static void drawBar(
            Graphics2D g2,
            Task task,
            LocalDate minDate,
            int y,
            Map<Integer, List<Task>> childrenMap,
            int dayWidth,
            int rowHeight,
            int barHeight,
            boolean isDragging,
            boolean isSelected,
            LocalDate dragStart,
            LocalDate dragEnd
    ) {
        LocalDate startToUse = isDragging && dragStart != null
                ? dragStart
                : GanttDisplayDates.resolveDisplayStartDate(task, childrenMap);
        LocalDate endToUse = isDragging && dragEnd != null
                ? dragEnd
                : GanttDisplayDates.resolveDisplayEndDate(task, childrenMap);
        if (startToUse == null || endToUse == null) {
            return;
        }

        long startOffset = ChronoUnit.DAYS.between(minDate, startToUse);
        long duration = ChronoUnit.DAYS.between(startToUse, endToUse) + 1;
        if (startOffset < 0 || duration <= 0) {
            return;
        }

        int barX = (int) (startOffset * dayWidth);
        int barWidth = (int) (duration * dayWidth);
        int barY = y + (rowHeight - barHeight) / 2;

        Composite originalComposite = g2.getComposite();
        if (isDragging) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        }

        int progress = Math.max(0, Math.min(100, task.getProgress()));
        Color mainColor = AppTheme.getStatusColor(task.getStatusCode());
        Color bgBarColor = AppTheme.ganttBarTrack(task.getStatusCode());

        java.awt.geom.RoundRectangle2D barShape =
                new java.awt.geom.RoundRectangle2D.Double(barX, barY, barWidth, barHeight, 8, 8);
        g2.setColor(bgBarColor);
        g2.fill(barShape);

        int progressWidth = (int) Math.round(barWidth * (progress / 100.0));
        if (progress > 0) {
            Shape oldClip = g2.getClip();
            g2.clip(barShape);
            g2.setColor(mainColor);
            g2.fillRoundRect(barX, barY, Math.max(progressWidth, 1), barHeight, 8, 8);
            g2.setClip(oldClip);
        }

        if (isDragging) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(AppTheme.FILL_WARNING);
        } else if (isSelected) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(AppTheme.PRIMARY);
        } else {
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(mainColor.darker());
        }
        g2.drawRoundRect(barX, barY, barWidth, barHeight, 8, 8);
        g2.setStroke(new BasicStroke(1.0f));

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String progressText = progress + "%";
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(progressText);
        GanttBarGeometry.ProgressLabel label = GanttBarGeometry.progressLabel(
                barX, barWidth, progressWidth, textWidth);
        int textY = barY + (barHeight + fm.getAscent()) / 2 - 2;
        if (label.place() == GanttBarGeometry.ProgressLabelPlace.ON_FILL) {
            g2.setColor(Color.WHITE);
        } else if (label.place() == GanttBarGeometry.ProgressLabelPlace.ON_TRACK) {
            g2.setColor(AppTheme.isDarkMode() ? AppTheme.textStrong() : new Color(15, 23, 42));
        } else {
            g2.setColor(AppTheme.TEXT_PRIMARY);
        }
        g2.drawString(progressText, label.x(), textY);

        if (isDragging) {
            g2.setComposite(originalComposite);
        }
    }
}
