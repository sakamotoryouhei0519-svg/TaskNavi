package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中項目（Stage）
 * Project に属する中間階層で、Task を複数持つことができる。
 */
public class Stage {
    private final int id;
    private final String name;
    private final List<Task> tasks;

    public Stage(int id, String name) {
        this(id, name, new ArrayList<>());
    }

    public Stage(int id, String name, List<Task> tasks) {
        this.id = id;
        this.name = name;
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addTask(Task task) {
        if (task == null) {
            return;
        }
        tasks.add(task);
    }

    public int calculateChildrenProgressAverage() {
        if (tasks.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (Task task : tasks) {
            total += task.getProgress();
        }
        return Math.round((float) total / tasks.size());
    }

    public String getStatusSummary() {
        if (tasks.isEmpty()) {
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
