package org.example;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {

    @Test
    void exportAndImportShouldRoundTripQuotedFields() throws Exception {
        File file = File.createTempFile("tasknavi-csv-roundtrip", ".csv");
        Task task = new Task(1, "要件定義,見積もり", 0, 1, 80, "進行中",
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20));
        task.setAssignee("田中 太郎");
        task.setPriority("高");

        CsvUtil.exportToCsv(file, List.of(task));
        List<Task> imported = CsvUtil.importFromCsv(file);

        assertEquals(1, imported.size());
        assertEquals("要件定義,見積もり", imported.get(0).getTitle());
        assertEquals("田中 太郎", imported.get(0).getAssignee());
        assertEquals("高", imported.get(0).getPriority());
        assertEquals(80, imported.get(0).getProgress());
    }

    @Test
    void exportToCsvWithBomShouldWriteUtf8Bom() throws Exception {
        File file = File.createTempFile("tasknavi-csv-bom", ".csv");
        Task task = new Task(1, "基本設計", 0, 1, 40, "未着手",
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 5));
        task.setAssignee("佐藤");
        task.setPriority("中");

        CsvUtil.exportToCsvWithBom(file, List.of(task));
        byte[] bytes = Files.readAllBytes(file.toPath());

        assertEquals(3, bytes.length >= 3 ? 3 : bytes.length);
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, java.util.Arrays.copyOf(bytes, 3));
    }

    @Test
    void importFromCsvShouldSkipMalformedRows() throws Exception {
        File file = File.createTempFile("tasknavi-csv-invalid", ".csv");
        String csv = String.join(System.lineSeparator(),
                "taskId,parentId,level,title,assignee,startDate,endDate,status,priority,progress",
                "1,0,1,\"要件定義,確認\",山田,2025-01-01,2025-01-05,進行中,高,70",
                "2,0,1,不正な日付,山田,2025-99-99,2025-01-05,進行中,中,50"
        ) + System.lineSeparator();

        Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
        List<Task> tasks = CsvUtil.importFromCsv(file);

        assertEquals(1, tasks.size());
        assertEquals("要件定義,確認", tasks.get(0).getTitle());
    }
}
