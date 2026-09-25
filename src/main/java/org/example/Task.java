package org.example;

import java.time.LocalDate;

/**
 * 【タスクデータモデルクラス】
 * 状態・優先度を Enum で型安全に保持・管理します。
 */
public class Task {
    public static final String DEFAULT_PRIORITY = Priority.MEDIUM.name();
    public static final String[] PRIORITY_OPTIONS = {
            Priority.HIGH.name(),
            Priority.MEDIUM.name(),
            Priority.LOW.name()
    };
    public static final String[] STATUS_OPTIONS = {
            TaskStatus.NOT_STARTED.name(),
            TaskStatus.IN_PROGRESS.name(),
            TaskStatus.COMPLETED.name()
    };
    public static final String STATUS_NOT_STARTED = TaskStatus.NOT_STARTED.name();
    public static final String STATUS_IN_PROGRESS = TaskStatus.IN_PROGRESS.name();
    public static final String STATUS_COMPLETED = TaskStatus.COMPLETED.name();

    private int id;
    private Integer parentId;
    private int level;
    private int orderIndex;
    private String name;
    private String assignee;
    private int progress;
    private TaskStatus status;
    private Priority priority;
    private LocalDate startDate;
    private LocalDate endDate;

    public Task(int id, String name, Integer parentId, int level, int orderIndex, int progress, String status, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.level = level;
        this.orderIndex = orderIndex;
        this.progress = progress;
        this.status = TaskStatus.fromString(status);
        this.priority = Priority.MEDIUM;
        this.startDate = startDate;
        this.endDate = endDate;
        this.assignee = "";
    }

    public Task(int id, String name, Integer parentId, int level, int orderIndex, int progress, TaskStatus status, LocalDate startDate, LocalDate endDate) {
        this(id, name, parentId, level, orderIndex, progress, status != null ? status.name() : null, startDate, endDate);
    }

    /** 表示言語に依存しない永続化コード（NOT_STARTED 等）へ正規化する。 */
    public static String normalizeStatus(String status) {
        return TaskStatus.fromString(status).name();
    }

    public int getId() {
        return id;
    }

    public int getTaskId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssignee() {
        return assignee != null ? assignee : "";
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        if (progress < 0) {
            this.progress = 0;
        } else if (progress > 100) {
            this.progress = 100;
        } else {
            this.progress = progress;
        }
    }

    /** 型安全な状態。 */
    public TaskStatus getStatus() {
        return status != null ? status : TaskStatus.NOT_STARTED;
    }

    /** 永続化・フィルタ比較用コード（NOT_STARTED 等）。 */
    public String getStatusCode() {
        return getStatus().name();
    }

    public void setStatus(TaskStatus status) {
        this.status = status != null ? status : TaskStatus.NOT_STARTED;
    }

    public void setStatus(String status) {
        this.status = TaskStatus.fromString(status);
    }

    public TaskStatus getStatusEnum() {
        return getStatus();
    }

    public boolean isNotStarted() {
        return getStatus() == TaskStatus.NOT_STARTED;
    }

    public boolean isInProgress() {
        return getStatus() == TaskStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return getStatus() == TaskStatus.COMPLETED;
    }

    public String getPriorityCode() {
        return getPriority().name();
    }

    public Priority getPriority() {
        return priority != null ? priority : Priority.MEDIUM;
    }

    public String getPriorityLabel() {
        return getPriority().getLabel();
    }

    public void setPriority(Priority priority) {
        this.priority = (priority != null) ? priority : Priority.MEDIUM;
    }

    public void setPriority(String priorityLabel) {
        this.priority = Priority.fromString(priorityLabel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Task)) return false;
        Task other = (Task) obj;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return name;
    }
}
