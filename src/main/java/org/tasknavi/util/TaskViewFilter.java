package org.tasknavi.util;

import org.tasknavi.AppMessages;
import org.tasknavi.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 画面共通のキーワード／ステータス／プロジェクト絞り込み。
 * カンバン・カレンダー・ガントなどで同じ条件意味を共有する。
 * キーワードはタスク名または担当者に部分一致する。
 */
public final class TaskViewFilter {

    private TaskViewFilter() {
    }

    /**
     * @param source             元タスク一覧（null 可）
     * @param searchKeyword      タスク名または担当者のキーワード（前後空白無視・大小文字無視）
     * @param statusFilter       ステータスコード。null / ALL / 「すべて」は未指定扱い
     * @param projectFilterId    プロジェクト ID。配下（子孫含む）のみ残す
     * @param projectMemberCache プロジェクト ID → メンバー ID 集合のキャッシュ（null 不可）
     */
    public static List<Task> apply(
            List<Task> source,
            String searchKeyword,
            String statusFilter,
            Integer projectFilterId,
            Map<Integer, Set<Integer>> projectMemberCache) {
        if (projectMemberCache == null) {
            throw new IllegalArgumentException("projectMemberCache must not be null");
        }

        List<Task> tasks = source != null ? source : Collections.emptyList();
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        String keyword = SearchFilterUtil.normalizeKeyword(searchKeyword);
        String status = SearchFilterUtil.normalizeStatus(statusFilter);

        Set<Integer> projectMemberIds = null;
        if (projectFilterId != null) {
            // 親子判定には元一覧が必要なため、絞り込み前の source から収集する
            List<Task> membershipSource = tasks;
            projectMemberIds = projectMemberCache.computeIfAbsent(
                    projectFilterId,
                    id -> ProjectMemberFilter.collectMemberIds(membershipSource, id));
        }

        List<Task> filtered = new ArrayList<>();
        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            if (!matchesKeyword(task, keyword)) {
                continue;
            }
            if (!matchesStatus(task, status)) {
                continue;
            }
            if (projectMemberIds != null && !projectMemberIds.contains(task.getId())) {
                continue;
            }
            filtered.add(task);
        }
        return filtered;
    }

    private static boolean matchesKeyword(Task task, String keyword) {
        if (keyword == null) {
            return true;
        }
        String needle = keyword.toLowerCase();
        String name = task.getName();
        if (name != null && name.toLowerCase().contains(needle)) {
            return true;
        }
        String assignee = task.getAssignee();
        return assignee != null && !assignee.isBlank() && assignee.toLowerCase().contains(needle);
    }

    private static boolean matchesStatus(Task task, String status) {
        if (status == null || AppMessages.isFilterAll(status)) {
            return true;
        }
        return status.equals(task.getStatusCode());
    }
}
