package org.example.ui.calendar;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarViewBuildersTest {

    @Test
    void monthGridStartsOnSunday() {
        LocalDate start = CalendarMonthViewBuilder.gridStart(LocalDate.of(2026, 9, 15));
        assertEquals(DayOfWeek.SUNDAY, start.getDayOfWeek());
        assertTrue(!start.isAfter(LocalDate.of(2026, 9, 1)));
    }

    @Test
    void weekStartIsSunday() {
        assertEquals(LocalDate.of(2026, 9, 20),
                CalendarWeekViewBuilder.weekStart(LocalDate.of(2026, 9, 25)));
        assertEquals(DayOfWeek.SUNDAY,
                CalendarWeekViewBuilder.weekStart(LocalDate.of(2026, 9, 20)).getDayOfWeek());
    }
}
