package org.example.ui.calendar;

import org.example.AppTheme;
import org.example.Task;
import org.example.UiLabels;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * 月表示グリッド（曜日ヘッダー＋42セル）の組み立て。
 */
public final class CalendarMonthViewBuilder {

    private CalendarMonthViewBuilder() {
    }

    /** 日曜始まりの月グリッド先頭日。 */
    public static LocalDate gridStart(LocalDate currentDate) {
        LocalDate firstOfMonth = currentDate.withDayOfMonth(1);
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // 日曜=0
        return firstOfMonth.minusDays(startDayOfWeek);
    }

    public static JPanel build(
            LocalDate currentDate,
            LocalDate selectedDate,
            List<Task> tasks,
            CalendarMonthCellFactory.Actions monthActions) {
        JPanel container = new JPanel(new BorderLayout(0, 6));
        container.setBackground(AppTheme.BACKGROUND);
        container.setPreferredSize(new Dimension(650, 640));
        container.setMinimumSize(new Dimension(450, 560));

        JPanel headerPanel = new JPanel(new GridLayout(1, 7, 4, 0));
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(0, 32));

        String[] dayNames = UiLabels.weekdaysSundayFirst();
        Color[] dayColors = {
                new Color(239, 68, 68),
                AppTheme.TEXT_PRIMARY,
                AppTheme.TEXT_PRIMARY,
                AppTheme.TEXT_PRIMARY,
                AppTheme.TEXT_PRIMARY,
                AppTheme.TEXT_PRIMARY,
                new Color(59, 130, 246)
        };

        for (int i = 0; i < 7; i++) {
            JLabel lblDay = new JLabel(dayNames[i], SwingConstants.CENTER);
            lblDay.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 12f));
            lblDay.setForeground(dayColors[i]);
            lblDay.setOpaque(true);
            lblDay.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
            lblDay.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1));
            headerPanel.add(lblDay);
        }
        container.add(headerPanel, BorderLayout.NORTH);

        LocalDate calDate = gridStart(currentDate);
        JPanel gridPanel = new JPanel(new GridLayout(6, 7, 4, 4));
        gridPanel.setBackground(AppTheme.BACKGROUND);

        for (int i = 0; i < 42; i++) {
            LocalDate thisDate = calDate;
            boolean isCurrentMonth = thisDate.getMonthValue() == currentDate.getMonthValue();
            boolean isToday = thisDate.equals(LocalDate.now());
            boolean isSelected = thisDate.equals(selectedDate);
            List<Task> tasksForDay = CalendarTaskFilter.tasksForDate(tasks, thisDate);
            gridPanel.add(CalendarMonthCellFactory.createDayCell(
                    thisDate, isCurrentMonth, isToday, isSelected, tasksForDay, monthActions));
            calDate = calDate.plusDays(1);
        }

        container.add(gridPanel, BorderLayout.CENTER);
        return container;
    }
}
