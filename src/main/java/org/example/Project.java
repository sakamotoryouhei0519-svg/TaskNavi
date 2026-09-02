package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大項目（Project）
 * Stage を複数持ち、その進捗率の平均を計算する責務を持つ。
 */
public class Project {
    private final int id;
    private final String name;
    private final List<Stage> stages;

    public Project(int id, String name) {
        this(id, name, new ArrayList<>());
    }

    public Project(int id, String name, List<Stage> stages) {
        this.id = id;
        this.name = name;
        this.stages = stages != null ? new ArrayList<>(stages) : new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Stage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    public void addStage(Stage stage) {
        if (stage == null) {
            return;
        }
        stages.add(stage);
    }

    public int calculateChildrenProgressAverage() {
        if (stages.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (Stage stage : stages) {
            total += stage.calculateChildrenProgressAverage();
        }
        return Math.round((float) total / stages.size());
    }

    public String getStatusSummary() {
        if (stages.isEmpty()) {
            return TaskStatus.NOT_STARTED.getLabel();
        }

        int average = calculateChildrenProgressAverage();
        if (average >= 100) {
            return TaskStatus.COMPLETED.getLabel();
        }
        if (average > 0) {
            return TaskStatus.IN_PROGRESS.getLabel();
        }
        return TaskStatus.NOT_STARTED.getLabel();
    }
}
