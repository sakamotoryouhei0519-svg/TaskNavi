package org.example.ui.gantt;

import org.example.AppMessages;
import org.example.AppTheme;
import org.example.Task;
import org.example.UiLabels;

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
 */
public final class GanttPainter {
    private GanttPainter() {
    }

    public static void drawHeader(
            Graphics2D g2,
            LocalDate minDate,
            int offsetY,
            int totalDays,
            int dayWidth,
            int headerHeight
    ) {
        LocalDate today = LocalDate.now();

        int currentMonthStartX = 0;
        LocalDate currentMonth = minDate;
        int monthDaysCount = 0;

        for (int i = 0; i < totalDays; i++) {
            LocalDate date = minDate.plusDays(i);
            if (!date.getMonth().equals(currentMonth.getMonth()) || i == totalDays - 1) {
                if (i == totalDays - 1 && date.getMonth().equals(currentMonth.getMonth())) {
                    monthDaysCount++;
                }

                int groupWidth = Math.max(1, monthDaysCount * dayWidth);
                g2.setColor(AppTheme.isDarkMode() ? new Color(30, 41, 59) : new Color(240, 243, 248));
                g2.fillRect(currentMonthStartX, offsetY, groupWidth, 22);
                g2.setColor(AppTheme.BORDER_COLOR);
                g2.drawRect(currentMonthStartX, offsetY, groupWidth, 22);

                g2.setColor(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                String yearMonthStr = currentMonth.format(DateTimeFormatter.ofPattern(
                        AppMessages.get("gantt.date.format.yearmonth", "yyyy年 M月")));
                g2.drawString(yearMonthStr, currentMonthStartX + 8, offsetY + 16);

                currentMonthStartX += groupWidth;
                currentMonth = date;
                monthDaysCount = 1;
            } else {
                monthDaysCount++;
            }
        }

        for (int i = 0; i < totalDays; i++) {
            int x = i * dayWidth;
            LocalDate date = minDate.plusDays(i);

            DayOfWeek dow = date.getDayOfWeek();
            boolean isSaturday = dow == DayOfWeek.SATURDAY;
            boolean isSunday = dow == DayOfWeek.SUNDAY;
            boolean isToday = date.equals(today);

            Color bgHeader;
            if (AppTheme.isDarkMode()) {
                if (isToday) {
                    bgHeader = new Color(71, 85, 105);
                } else if (isSaturday || isSunday) {
                    bgHeader = new Color(30, 41, 59);
                } else {
                    bgHeader = new Color(30, 41, 59);
                }
            } else {
                if (isToday) {
                    bgHeader = new Color(255, 243, 205);
                } else if (isSaturday) {
                    bgHeader = new Color(230, 240, 255);
                } else if (isSunday) {
                    bgHeader = new Color(255, 230, 230);
                } else {
                    bgHeader = new Color(250, 251, 253);
                }
            }

            g2.setColor(bgHeader);
            g2.fillRect(x, offsetY + 22, dayWidth, headerHeight - 22);
            g2.setColor(AppTheme.BORDER_COLOR);
            g2.drawRect(x, offsetY + 22, dayWidth, headerHeight - 22);

            Shape oldClip = g2.getClip();
            g2.clipRect(x, offsetY + 22, dayWidth, headerHeight - 22);

            if (dayWidth >= 18) {
                int dateFontSize = dayWidth >= 26 ? 10 : 9;
                int weekdayFontSize = dayWidth >= 28 ? 9 : 8;
                g2.setFont(new Font("SansSerif", Font.PLAIN, dateFontSize));
                if (AppTheme.isDarkMode()) {
                    g2.setColor(isSunday ? new Color(248, 113, 113) : (isSaturday ? new Color(96, 165, 250) : new Color(226, 232, 240)));
                } else {
                    g2.setColor(isSunday ? new Color(210, 50, 50) : (isSaturday ? new Color(30, 100, 200) : AppTheme.TEXT_PRIMARY));
                }
                String dayText = String.valueOf(date.getDayOfMonth());
                FontMetrics dayMetrics = g2.getFontMetrics();
                int dayTextX = x + Math.max(2, (dayWidth - dayMetrics.stringWidth(dayText)) / 2);
                g2.drawString(dayText, dayTextX, offsetY + 34);

                String dayOfWeekStr = UiLabels.weekdaysMondayFirst()[dow.getValue() - 1];
                g2.setFont(new Font("SansSerif", Font.PLAIN, weekdayFontSize));
                FontMetrics weekMetrics = g2.getFontMetrics();
                int weekTextX = x + Math.max(2, (dayWidth - weekMetrics.stringWidth(dayOfWeekStr)) / 2);
                g2.drawString(dayOfWeekStr, weekTextX, offsetY + 46);
                g2.setClip(oldClip);
            } else {
                g2.setClip(oldClip);
            }

            if (date.getDayOfMonth() == 1 && i > 0) {
                g2.setColor(AppTheme.isDarkMode() ? new Color(71, 85, 105) : new Color(180, 180, 180));
                g2.drawLine(x, offsetY, x, headerHeight);
            } else {
                g2.setColor(AppTheme.isDarkMode() ? new Color(71, 85, 105) : new Color(240, 240, 240));
                g2.drawLine(x, offsetY + headerHeight, x, headerHeight);
            }
        }
    }

    public static void drawTodayLine(
            Graphics2D g2,
            LocalDate minDate,
            int totalY,
            int offsetY,
            int totalDays,
            int dayWidth
    ) {
        LocalDate today = LocalDate.now();
        long daysFromMin = ChronoUnit.DAYS.between(minDate, today);

        if (daysFromMin >= 0 && daysFromMin < totalDays) {
            int todayX = (int) (daysFromMin * dayWidth) + (dayWidth / 2);
            g2.setColor(new Color(230, 50, 50, 180));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 4.0f}, 0.0f));
            g2.drawLine(todayX, offsetY, todayX, totalY);
            g2.setStroke(new BasicStroke(1.0f));
        }
    }

    public static void drawDayGrid(
            Graphics2D g2,
            LocalDate minDate,
            int startY,
            int endY,
            int totalDays,
            int dayWidth
    ) {
        for (int i = 0; i < totalDays; i++) {
            int x = i * dayWidth;
            g2.setColor(new Color(220, 220, 220));
            g2.drawLine(x, startY, x, endY);
        }
    }

    public static void drawTaskNameCell(
            Graphics2D g2,
            Task task,
            int y,
            Integer selectedTaskId,
            int taskNameWidth,
            int rowHeight
    ) {
        Color rowBg;
        if (AppTheme.isDarkMode()) {
            rowBg = new Color(30, 41, 59);
        } else {
            rowBg = (selectedTaskId != null && task.getId() == selectedTaskId)
                    ? new Color(236, 244, 255)
                    : Color.WHITE;
        }
        g2.setColor(rowBg);
        g2.fillRect(0, y, taskNameWidth, rowHeight);
        g2.setColor(AppTheme.isDarkMode() ? new Color(71, 85, 105) : new Color(226, 232, 240));
        g2.fillRect(0, y + rowHeight - 1, taskNameWidth, 1);
        g2.setColor(AppTheme.BORDER_COLOR);
        g2.drawRect(0, y, taskNameWidth, rowHeight);

        int indent = 10 + (Math.max(0, task.getLevel() - 1) * 15);

        g2.setFont(task.getLevel() == 1 ? AppTheme.FONT_HEADER : AppTheme.FONT_MAIN);
        g2.setColor(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
        g2.drawString(task.getName(), indent, y + 24);
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
        Color bgBarColor = AppTheme.getStatusSoftColor(task.getStatusCode());

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
            g2.setColor(new Color(255, 110, 0));
        } else if (isSelected) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(30, 120, 255));
        } else {
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(mainColor.darker());
        }
        g2.drawRoundRect(barX, barY, barWidth, barHeight, 8, 8);
        g2.setStroke(new BasicStroke(1.0f));

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        String progressText = progress + "%";
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(progressText);
        int textX = barX + Math.max(6, (barWidth - textWidth) / 2);
        int textY = barY + (barHeight + fm.getAscent()) / 2 - 2;

        if (progress == 0) {
            g2.setColor(mainColor);
            g2.drawString(progressText, textX, textY);
        } else {
            g2.setColor(Color.WHITE);
            g2.drawString(progressText, textX, textY);
        }

        if (isDragging) {
            g2.setComposite(originalComposite);
        }
    }
}
