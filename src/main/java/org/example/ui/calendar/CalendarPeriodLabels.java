package org.example.ui.calendar;

import org.example.AppMessages;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * カレンダー画面の期間タイトル文言を組み立てる。
 */
public final class CalendarPeriodLabels {
    private CalendarPeriodLabels() {
    }

    public static String forMonth(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(
                AppMessages.get("calendar.date.format.yearmonth", "yyyy年 M月")));
    }

    public static String forWeek(LocalDate date) {
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = date.get(weekFields.weekOfMonth());

        return AppMessages.format(
                "calendar.title.week",
                "{0}年 {1}月 第{2}週 ({3}/{4} 〜 {5}/{6})",
                date.getYear(),
                date.getMonthValue(),
                weekNumber,
                startOfWeek.getMonthValue(), startOfWeek.getDayOfMonth(),
                endOfWeek.getMonthValue(), endOfWeek.getDayOfMonth());
    }
}
