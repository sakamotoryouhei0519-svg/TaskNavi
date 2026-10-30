package org.tasknavi.persistence;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TaskRowMapperTest {

    @Test
    void readsDatesWithAndWithoutTime() {
        assertEquals(LocalDate.of(2026, 10, 24), TaskRowMapper.parseDate("2026-10-24", 1));
        assertEquals(LocalDate.of(2026, 10, 24), TaskRowMapper.parseDate(" 2026-10-24T00:00 ", 1));
    }

    @Test
    void emptyOrBrokenDatesBecomeEmpty() {
        assertNull(TaskRowMapper.parseDate(null, 1));
        assertNull(TaskRowMapper.parseDate("  ", 1));
        assertNull(TaskRowMapper.parseDate("2026/13/40", 1));
    }
}
