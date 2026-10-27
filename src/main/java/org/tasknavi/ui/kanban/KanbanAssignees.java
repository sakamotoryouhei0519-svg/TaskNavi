package org.tasknavi.ui.kanban;

import org.tasknavi.model.Assignees;
import org.tasknavi.model.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * カンバンの担当者での絞り込みと、担当者ごとの分け方。
 */
final class KanbanAssignees {

    /** 絞り込みの「担当なし」。担当者の名前とぶつからない値にする。 */
    static final String UNASSIGNED = "\u0000unassigned";

    private KanbanAssignees() {
    }

    /** 担当者の候補。名前の大文字・小文字と前後の空白の違いは 1 つにまとめる（最初に出てきた書き方）。 */
    static List<String> options(List<String> known, List<Task> tasks) {
        List<String> names = new ArrayList<>();
        known.forEach(name -> addOnce(names, name));
        tasks.stream().map(Task::getAssignee).sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                .forEach(name -> addOnce(names, name));
        return names;
    }

    /** @param assignee null なら全員、{@link #UNASSIGNED} なら担当者が空のもの */
    static List<Task> filter(List<Task> tasks, String assignee) {
        if (assignee == null) {
            return tasks;
        }
        return tasks.stream().filter(task -> UNASSIGNED.equals(assignee) ? isBlank(task.getAssignee())
                : Assignees.same(task.getAssignee(), assignee)).toList();
    }

    /** 担当者の名前順。担当なしは最後で、キーは {@link #UNASSIGNED}。担当者の中の並びは元のまま。 */
    static Map<String, List<Task>> group(List<Task> tasks) {
        Map<String, String> shownName = new LinkedHashMap<>();
        Map<String, List<Task>> byKey = new TreeMap<>();
        List<Task> unassigned = new ArrayList<>();
        for (Task task : tasks) {
            String name = task.getAssignee();
            if (isBlank(name)) {
                unassigned.add(task);
                continue;
            }
            String key = name.trim().toLowerCase();
            shownName.putIfAbsent(key, name.trim());
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
        }
        Map<String, List<Task>> result = new LinkedHashMap<>();
        byKey.forEach((key, list) -> result.put(shownName.get(key), list));
        if (!unassigned.isEmpty()) {
            result.put(UNASSIGNED, unassigned);
        }
        return result;
    }

    private static void addOnce(List<String> names, String name) {
        if (!isBlank(name) && names.stream().noneMatch(existing -> Assignees.same(existing, name))) {
            names.add(name.trim());
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
