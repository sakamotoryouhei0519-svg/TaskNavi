package org.tasknavi.io;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Priority;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskCsvTest {

    private static Task task(int id, Integer parentId, int level, int order, String name) {
        Task task = new Task(id, name, parentId, level, order, 0, TaskStatus.NOT_STARTED,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        return task;
    }

    @Test
    void exportThenImportKeepsHierarchyAndEveryField() throws Exception {
        Task project = task(10, null, 1, 0, "新システム");
        Task stage = task(20, 10, 2, 0, "設計, レビュー");
        Task work = task(30, 20, 3, 0, "=画面設計");
        work.setAssignee("田中 太郎");
        work.setStatus(TaskStatus.IN_PROGRESS);
        work.setPriority(Priority.HIGH);
        work.setProgress(40);
        work.setMemo("- 1行目\n\"2行目\"");
        Path file = Files.createTempFile("tasknavi-roundtrip", ".csv");

        TaskCsv.write(file, List.of(work, stage, project));
        TaskCsv.ReadResult read = TaskCsv.read(file);

        assertEquals(List.of(), read.errors());
        assertEquals(List.of("新システム", "設計, レビュー", "=画面設計"),
                read.tasks().stream().map(Task::getName).toList());
        Task importedProject = read.tasks().get(0);
        Task importedStage = read.tasks().get(1);
        Task importedWork = read.tasks().get(2);
        assertEquals(null, importedProject.getParentId());
        assertEquals(importedProject.getId(), importedStage.getParentId());
        assertEquals(importedStage.getId(), importedWork.getParentId());
        assertEquals(3, importedWork.getLevel());
        assertEquals("田中 太郎", importedWork.getAssignee());
        assertEquals(TaskStatus.IN_PROGRESS, importedWork.getStatus());
        assertEquals(Priority.HIGH, importedWork.getPriority());
        assertEquals(40, importedWork.getProgress());
        assertEquals("- 1行目\n\"2行目\"", importedWork.getMemo());
        assertEquals(LocalDate.of(2026, 10, 31), importedWork.getEndDate());
    }

    @Test
    void exportUsesJapaneseLabelsAndProtectsFormulas() {
        Task project = task(1, null, 1, 0, "@集計");
        project.setStatus(TaskStatus.COMPLETED);
        project.setPriority(Priority.LOW);

        String csv = TaskCsv.toCsv(List.of(project));

        assertTrue(csv.startsWith("ID,親ID,階層,タスク名,担当者,開始日,終了日,状態,優先度,進捗(%),メモ\r\n"));
        assertTrue(csv.contains("1,,1,'@集計,,2026-10-01,2026-10-31,完了,低,0,\r\n"), csv);
    }

    @Test
    void readsShiftJisCsvSavedByExcel() throws Exception {
        String excelCsv = "ID,親ID,階層,タスク名,担当者,開始日,終了日,状態,優先度,進捗(%),メモ\r\n"
                + "3,2,3,詳細設計,佐藤,2026/11/1,2026/11/15 0:00,進行中,高,50%,\r\n"
                + "1,,1,社内ポータル,,2026/10/1,2026/12/31,未着手,中,0,\r\n"
                + "2,1,,基本設計,,2026年10月5日,2026.10.30,完了,低,100,\r\n";
        Path file = Files.createTempFile("tasknavi-excel", ".csv");
        Files.write(file, excelCsv.getBytes(CsvCodec.WINDOWS_31J));

        TaskCsv.ReadResult read = TaskCsv.read(file);

        assertEquals(List.of(), read.errors());
        Task detail = read.tasks().get(0);
        Task portal = read.tasks().get(1);
        Task basic = read.tasks().get(2);
        assertEquals("詳細設計", detail.getName());
        assertEquals(basic.getId(), detail.getParentId());
        assertEquals(portal.getId(), basic.getParentId());
        assertEquals(2, basic.getLevel());
        assertEquals(LocalDate.of(2026, 11, 15), detail.getEndDate());
        assertEquals(LocalDate.of(2026, 10, 5), basic.getStartDate());
        assertEquals(50, detail.getProgress());
        assertEquals(TaskStatus.COMPLETED, basic.getStatus());
    }

    @Test
    void readsLegacyEnglishHeaderAndMinimalColumns() {
        String legacy = "taskId,parentId,level,title,assignee,startDate,endDate,status,priority,progress\n"
                + "1,0,1,旧形式,山田,2025-01-01,2025-01-05,IN_PROGRESS,HIGH,70\n";
        String minimal = "タスク名\nだけの行\n";

        TaskCsv.ReadResult legacyRead = TaskCsv.parse(legacy);
        TaskCsv.ReadResult minimalRead = TaskCsv.parse(minimal);

        assertEquals(List.of(), legacyRead.errors());
        assertEquals(TaskStatus.IN_PROGRESS, legacyRead.tasks().get(0).getStatus());
        assertEquals(70, legacyRead.tasks().get(0).getProgress());
        assertEquals(List.of(), minimalRead.errors());
        Task only = minimalRead.tasks().get(0);
        assertEquals(1, only.getLevel());
        assertEquals(null, only.getStartDate());
        assertEquals(TaskStatus.NOT_STARTED, only.getStatus());
        assertEquals(Priority.MEDIUM, only.getPriority());
    }

    @Test
    void badRowsAreReportedWithLineNumbersAndOthersStillImport() {
        String csv = "ID,親ID,階層,タスク名,開始日,終了日,状態,優先度,進捗(%)\n"
                + "1,,1,プロジェクト,,,,,\n"
                + "2,1,2,日付が変,2026/13/01,,,,\n"
                + "3,1,2,状態が変,,,保留,,\n"
                + "4,1,2,進捗が変,,,,,120\n"
                + "5,99,2,親がない,,,,,\n"
                + "6,2,3,親がエラー,,,,,\n"
                + "7,1,3,階層が合わない,,,,,\n"
                + "1,,1,ID重複,,,,,\n"
                + "8,,1,,,,,,\n"
                + "9,1,2,工程,,,,,\n"
                + "10,9,3,タスク,,,,,\n"
                + "11,10,,深すぎる,,,,,\n"
                + "12,,1,日付が逆,2026/10/10,2026/10/01,,,\n";

        TaskCsv.ReadResult read = TaskCsv.parse(csv);

        assertEquals(List.of("プロジェクト", "工程", "タスク"), read.tasks().stream().map(Task::getName).toList());
        List<Integer> errorLines = read.errors().stream().map(TaskCsv.RowError::lineNumber).toList();
        assertEquals(List.of(3, 4, 5, 6, 7, 8, 9, 10, 13, 14), errorLines);
        assertTrue(message(read, 3).contains("開始日「2026/13/01」"));
        assertTrue(message(read, 4).contains("状態「保留」"));
        assertTrue(message(read, 5).contains("進捗「120」"));
        assertTrue(message(read, 6).contains("親ID「99」"));
        assertTrue(message(read, 7).contains("3 行目"));
        assertTrue(message(read, 8).contains("階層は 2"));
        assertTrue(message(read, 9).contains("重複"));
        assertTrue(message(read, 10).contains("タスク名が空"));
        assertTrue(message(read, 13).contains("下には追加できません"));
        assertTrue(message(read, 14).contains("開始日が終了日より後"));
    }

    @Test
    void cyclicParentsAreRejected() {
        String csv = "ID,親ID,タスク名\n1,2,A\n2,1,B\n";

        TaskCsv.ReadResult read = TaskCsv.parse(csv);

        assertEquals(List.of(), read.tasks());
        assertEquals(2, read.errors().size());
    }

    @Test
    void missingNameColumnIsOneClearError() {
        TaskCsv.ReadResult read = TaskCsv.parse("件名,担当\nA,B\n");

        assertEquals(List.of(), read.tasks());
        assertEquals(1, read.errors().size());
        assertTrue(read.errors().get(0).message().contains("タスク名"));
    }

    private static String message(TaskCsv.ReadResult read, int line) {
        return read.errors().stream()
                .filter(e -> e.lineNumber() == line)
                .findFirst()
                .orElseThrow()
                .message();
    }
}
