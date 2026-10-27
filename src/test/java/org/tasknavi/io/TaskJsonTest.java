package org.tasknavi.io;

import org.junit.jupiter.api.Test;
import org.tasknavi.model.Priority;
import org.tasknavi.model.Task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskJsonTest {

    @Test
    void writeThenReadKeepsFields() throws Exception {
        Path file = Files.createTempFile("tasknavi-json-roundtrip", ".json");
        Task task = new Task(3, "JSONタスク", 0, 1, 0, 25, "未着手",
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 10));
        task.setAssignee("鈴木");
        task.setPriority(Priority.LOW);
        task.setMemo("メモ");

        TaskJson.write(file, List.of(task));
        List<Task> imported = TaskJson.read(file);

        assertEquals(1, imported.size());
        assertEquals("JSONタスク", imported.get(0).getTitle());
        assertEquals("鈴木", imported.get(0).getAssignee());
        assertEquals(Priority.LOW.name(), imported.get(0).getPriorityCode());
        assertEquals(25, imported.get(0).getProgress());
        assertEquals("メモ", imported.get(0).getMemo());
        assertEquals(LocalDate.of(2025, 3, 10), imported.get(0).getEndDate());
    }
}
