package org.tasknavi.tools;

import org.junit.jupiter.api.Test;
import org.tasknavi.io.CsvCodec;
import org.tasknavi.io.TaskCsv;
import org.tasknavi.model.Task;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDataToolTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Test
    void everyTestRowCanBeImported() {
        TaskCsv.ReadResult result = TaskCsv.parse(CsvCodec.format(TestDataTool.goodRows(TODAY)));

        assertEquals(List.of(), result.errors());
        assertEquals(TestDataTool.goodRows(TODAY).size() - 1, result.tasks().size());
        assertEquals(TODAY, find(result, "今日が期限").getEndDate());
        assertEquals("=1+1", find(result, "=1+1").getName());
        assertEquals(3, find(result, "月をまたぐ作業 1").getLevel());
    }

    @Test
    void badRowsAreRejectedOneByOne() {
        TaskCsv.ReadResult result = TaskCsv.parse(CsvCodec.format(TestDataTool.badRows(TODAY)));

        assertEquals(3, result.tasks().size());
        assertEquals(TestDataTool.BAD_ROW_COUNT, result.errors().size());
    }

    @Test
    void manyRowsMakeExactlyTheAskedNumber() {
        TaskCsv.ReadResult result = TaskCsv.parse(CsvCodec.format(TestDataTool.manyRows(1000, TODAY)));

        assertTrue(result.errors().isEmpty());
        assertEquals(1000, result.tasks().size());
    }

    private static Task find(TaskCsv.ReadResult result, String name) {
        return result.tasks().stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow();
    }
}
