package org.tasknavi.ui.calendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.AppMessages;

import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarPeriodLabelsTest {

    private Locale previous;

    @BeforeEach
    void pinLocale() {
        previous = Locale.getDefault();
        AppMessages.setLocale(Locale.JAPAN);
    }

    @AfterEach
    void restoreLocale() {
        AppMessages.setLocale(previous);
    }

    @Test
    void weekTitleShowsTheYearWithoutAThousandsSeparator() {
        assertEquals("2026年 10月 第5週 (10/25 〜 10/31)", CalendarPeriodLabels.forWeek(LocalDate.of(2026, 10, 29)));
    }
}
