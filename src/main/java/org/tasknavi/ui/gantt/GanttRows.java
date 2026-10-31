package org.tasknavi.ui.gantt;

import org.tasknavi.model.Task;
import org.tasknavi.model.TaskHierarchyUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ガントに出す行（階層順で絞り込んだもの）と親子の表。
 * 描くたび・マウスが動くたびに何千件を並べ直さないよう、元の一覧か絞り込みが変わったときだけ作り直す。
 * 元の一覧は読み直すたびに新しい List になるので、同じ List（{@code ==}）なら中身も同じとみなす。
 */
final class GanttRows {

    private final Map<Integer, Set<Integer>> projectMembers = new HashMap<>();
    private List<Task> source;
    private String keyword;
    private String status;
    private Integer projectId;
    private List<Task> rows = List.of();
    private Map<Integer, List<Task>> children = Map.of();

    void update(List<Task> source, String keyword, String status, Integer projectId) {
        if (source == this.source && Objects.equals(keyword, this.keyword)
                && Objects.equals(status, this.status) && Objects.equals(projectId, this.projectId)) {
            return;
        }
        if (source != this.source) {
            projectMembers.clear();
        }
        this.source = source;
        this.keyword = keyword;
        this.status = status;
        this.projectId = projectId;
        rows = GanttTaskFilter.visibleTasks(source, keyword, status, projectId, projectMembers);
        children = TaskHierarchyUtil.buildChildrenMap(TaskHierarchyUtil.sortDepthFirst(source));
    }

    List<Task> rows() {
        return rows;
    }

    Map<Integer, List<Task>> children() {
        return children;
    }
}
