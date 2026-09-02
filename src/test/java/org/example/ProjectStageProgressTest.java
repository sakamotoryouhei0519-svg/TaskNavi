package org.example;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectStageProgressTest {

    @Test
    void stageShouldAverageChildTaskProgress() {
        Task taskA = new Task(1, "A", null, 3, 50, "進行中", LocalDate.now(), LocalDate.now().plusDays(1));
        Task taskB = new Task(2, "B", null, 3, 100, "完了", LocalDate.now(), LocalDate.now().plusDays(2));

        Stage stage = new Stage(10, "工程1", List.of(taskA, taskB));

        assertEquals(75, stage.calculateChildrenProgressAverage());
        assertEquals("進行中", stage.getStatusSummary());
    }

    @Test
    void projectShouldAverageStageProgress() {
        Task taskA = new Task(1, "A", null, 3, 0, "未着手", LocalDate.now(), LocalDate.now().plusDays(1));
        Task taskB = new Task(2, "B", null, 3, 100, "完了", LocalDate.now(), LocalDate.now().plusDays(2));
        Stage stage1 = new Stage(10, "工程1", List.of(taskA, taskB));

        Task taskC = new Task(3, "C", null, 3, 50, "進行中", LocalDate.now(), LocalDate.now().plusDays(3));
        Stage stage2 = new Stage(11, "工程2", List.of(taskC));

        Project project = new Project(100, "大項目", List.of(stage1, stage2));

        assertEquals(50, project.calculateChildrenProgressAverage());
        assertEquals("進行中", project.getStatusSummary());
    }
}
