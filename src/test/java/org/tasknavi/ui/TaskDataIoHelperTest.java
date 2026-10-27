package org.tasknavi.ui;

import org.junit.jupiter.api.Test;
import org.tasknavi.io.TaskCsv;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskDataIoHelperTest {

    private static Task task(int id, Integer parentId, int level, String name) {
        return new Task(id, name, parentId, level, 0, 0, TaskStatus.NOT_STARTED, null, null);
    }

    @Test
    void filteredExportIncludesParentsSoTheHierarchyCanBeRebuilt() {
        Task project = task(1, null, 1, "P");
        Task stage = task(2, 1, 2, "S");
        Task hit = task(3, 2, 3, "T");
        Task other = task(4, null, 1, "Other");

        List<Task> exported = TaskDataIoHelper.withAncestors(List.of(hit), List.of(project, stage, hit, other));

        assertEquals(List.of("P", "S", "T"), exported.stream().map(Task::getName).toList());
    }

    @Test
    void summaryCountsLevelsAndLimitsListedErrors() {
        List<TaskCsv.RowError> errors = new ArrayList<>();
        for (int line = 2; line <= 11; line++) {
            errors.add(new TaskCsv.RowError(line, "理由" + line));
        }
        TaskCsv.ReadResult read = new TaskCsv.ReadResult(
                List.of(task(1, null, 1, "P"), task(2, 1, 2, "S"), task(3, 2, 3, "T"), task(4, 2, 3, "U")), errors);

        String summary = TaskDataIoHelper.summarize("tasks.csv", read);

        assertTrue(summary.contains("4 件"), summary);
        assertTrue(summary.contains("プロジェクト 1・工程 1・タスク 2"), summary);
        assertTrue(summary.contains("10 件あります"), summary);
        assertTrue(summary.contains("9 行目: 理由9"), summary);
        assertTrue(!summary.contains("10 行目"), summary);
        assertTrue(summary.contains("ほか 2 件"), summary);
    }
}
