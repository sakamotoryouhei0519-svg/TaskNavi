package org.example.ui.calendar;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.Task;
import org.example.UiLabels;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.function.Consumer;

/**
 * 週表示（7カラム）の組み立て。
 */
public final class CalendarWeekViewBuilder {

    private CalendarWeekViewBuilder() {
    }

    public static LocalDate weekStart(LocalDate currentDate) {
        return currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    public static JPanel build(
            LocalDate currentDate,
            LocalDate selectedDate,
            List<Task> tasks,
            CalendarTaskCardFactory.Actions cardActions,
            Consumer<LocalDate> onDateSelected) {
        JPanel panel = new JPanel(new MigLayout(
                "fill, insets 0, gap 6",
                "[min:130,grow,fill][min:130,grow,fill][min:130,grow,fill]"
                        + "[min:130,grow,fill][min:130,grow,fill][min:130,grow,fill][min:130,grow,fill]",
                "[grow,fill]"));
        panel.setBackground(AppTheme.BACKGROUND);
        panel.setPreferredSize(new Dimension(960, 600));

        LocalDate startOfWeek = weekStart(currentDate);
        String[] dayNames = UiLabels.weekdaysSundayFirst();

        for (int i = 0; i < 7; i++) {
            LocalDate colDate = startOfWeek.plusDays(i);
            boolean isToday = colDate.equals(LocalDate.now());
            boolean isSelected = colDate.equals(selectedDate);
            List<Task> tasksForDay = CalendarTaskFilter.tasksForDate(tasks, colDate);
            JPanel weekColumn = createWeekDayColumn(
                    colDate, dayNames[i], isToday, isSelected, tasksForDay, cardActions, onDateSelected);
            panel.add(weekColumn, (i == 6) ? "grow, wrap" : "grow");
        }
        return panel;
    }

    private static JPanel createWeekDayColumn(
            LocalDate date,
            String dayName,
            boolean isToday,
            boolean isSelected,
            List<Task> tasksForDay,
            CalendarTaskCardFactory.Actions cardActions,
            Consumer<LocalDate> onDateSelected) {
        JPanel colWrapper = new JPanel(new BorderLayout(0, 6));
        colWrapper.setBackground(AppTheme.BACKGROUND);
        colWrapper.setMinimumSize(new Dimension(130, 200));

        JPanel colHeader = new JPanel(new BorderLayout(4, 2));
        colHeader.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);

        if (isSelected) {
            colHeader.setBorder(BorderFactory.createCompoundBorder(
                    AppTheme.createRoundedBorder(AppTheme.PRIMARY, 8, 2, 2, 2, 2),
                    new EmptyBorder(6, 8, 6, 8)
            ));
        } else if (isToday) {
            colHeader.setBorder(BorderFactory.createCompoundBorder(
                    AppTheme.createRoundedBorder(new Color(59, 130, 246), 8, 2, 2, 2, 2),
                    new EmptyBorder(6, 8, 6, 8)
            ));
        } else {
            colHeader.setBorder(BorderFactory.createCompoundBorder(
                    AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                    new EmptyBorder(7, 9, 7, 9)
            ));
        }

        JLabel titleLbl = new JLabel(String.format("%d/%d (%s)", date.getMonthValue(), date.getDayOfMonth(), dayName));
        titleLbl.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 12f));
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            titleLbl.setForeground(new Color(239, 68, 68));
        } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            titleLbl.setForeground(new Color(59, 130, 246));
        } else {
            titleLbl.setForeground(AppTheme.TEXT_PRIMARY);
        }

        JLabel countBadge = new JLabel(AppMessages.format("calendar.count.format", "{0}件", tasksForDay.size()));
        countBadge.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 10f));
        countBadge.setForeground(AppTheme.PRIMARY);

        colHeader.add(titleLbl, BorderLayout.WEST);
        colHeader.add(countBadge, BorderLayout.EAST);
        colHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
        colHeader.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                onDateSelected.accept(date);
            }
        });

        colWrapper.add(colHeader, BorderLayout.NORTH);

        JPanel cardList = new JPanel(new MigLayout("fillx, insets 2 2 6 2, wrap", "[grow,fill]", "[]6"));
        cardList.setBackground(AppTheme.BACKGROUND);
        for (Task task : tasksForDay) {
            cardList.add(CalendarTaskCardFactory.create(task, true, cardActions));
        }

        JScrollPane colScroll = new JScrollPane(cardList);
        colScroll.setBorder(null);
        colScroll.setBackground(AppTheme.BACKGROUND);
        colScroll.getViewport().setBackground(AppTheme.BACKGROUND);
        colScroll.getVerticalScrollBar().setUnitIncrement(16);
        colScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        colWrapper.add(colScroll, BorderLayout.CENTER);
        return colWrapper;
    }
}
