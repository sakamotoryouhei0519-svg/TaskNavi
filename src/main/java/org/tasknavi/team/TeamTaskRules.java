package org.tasknavi.team;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 書く前の確かめ（名前・日付・階層・コメント）と、親子のたどり方。状態は持たず、渡された {@link TeamState} だけを見る。 */
final class TeamTaskRules {
    static final int MAX_LEVEL = 3;
    static final int MAX_COMMENT_LENGTH = 2000;
    /** 壊れた記録で親子が輪になっていても止まるように。 */
    private static final int MAX_DEPTH = 16;

    private TeamTaskRules() {
    }

    static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("タスク名を入力してください");
        }
    }

    /** parentId の下に作るタスクの階層。親が無ければ 1。 */
    static int levelUnder(TeamState state, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return 1;
        }
        TeamTask parent = state.find(parentId)
                .orElseThrow(() -> new IllegalArgumentException("親タスクが見つかりません"));
        return requireLevel(parent.level() + 1);
    }

    static int requireLevel(int level) {
        if (level > MAX_LEVEL) {
            throw new IllegalArgumentException("階層は " + MAX_LEVEL + " までです");
        }
        return level;
    }

    static void requireDateOrder(String start, String end) {
        if (start == null || end == null || start.isBlank() || end.isBlank()) {
            return;
        }
        try {
            if (LocalDate.parse(end.trim()).isBefore(LocalDate.parse(start.trim()))) {
                throw new IllegalArgumentException("終了日は開始日以降の日付にしてください");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日付は YYYY-MM-DD 形式で入力してください");
        }
    }

    /** @return 前後の空白を除いた本文 */
    static String requireCommentText(String text) {
        String body = text == null ? "" : text.strip();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("コメントを入力してください");
        }
        if (body.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("コメントは " + MAX_COMMENT_LENGTH + " 文字までです");
        }
        return body;
    }

    /** 自分の下や子孫の下には動かせない。動かした先で階層が {@link #MAX_LEVEL} を超えてもいけない。 */
    static void requireMovable(TeamState state, String taskId, String newParentId) {
        TeamTask task = state.find(taskId).orElseThrow();
        if (Objects.equals(task.parentId(), newParentId)) {
            return;
        }
        List<String> subtree = subtree(state, taskId);
        int parentLevel = 0;
        if (newParentId != null) {
            if (subtree.contains(newParentId)) {
                throw new IllegalArgumentException("自分の下には移動できません");
            }
            parentLevel = state.find(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("移動先の親タスクが見つかりません"))
                    .level();
        }
        int deepest = subtree.stream()
                .mapToInt(id -> state.find(id).map(TeamTask::level).orElse(task.level()))
                .max().orElse(task.level());
        requireLevel(parentLevel + 1 + (deepest - task.level()));
    }

    /** 自分と、まだある子孫。親が先に並ぶ。 */
    static List<String> subtree(TeamState state, String taskId) {
        List<String> out = new ArrayList<>();
        collectSubtree(state, taskId, out);
        return out;
    }

    private static void collectSubtree(TeamState state, String taskId, List<String> out) {
        out.add(taskId);
        for (TeamTask task : state.tasks()) {
            if (taskId.equals(task.parentId())) {
                collectSubtree(state, task.id(), out);
            }
        }
    }

    /** 削除したタスクを戻すときに一緒に戻すもの。消えている先祖（表示に必要）、自分、消えている子孫の順。 */
    static List<String> restoreTargets(Map<String, TeamState.Version> versions, String taskId) {
        List<String> targets = new ArrayList<>();
        String parentId = versions.get(taskId).parentId();
        for (int depth = 0; parentId != null && depth < MAX_DEPTH; depth++) {
            TeamState.Version parent = versions.get(parentId);
            if (parent == null) {
                break;
            }
            if (parent.deleted()) {
                targets.add(0, parentId);
            }
            parentId = parent.parentId();
        }
        collectDeletedSubtree(taskId, versions, targets, 0);
        return targets;
    }

    private static void collectDeletedSubtree(String taskId, Map<String, TeamState.Version> versions,
                                              List<String> out, int depth) {
        out.add(taskId);
        if (depth > MAX_DEPTH) {
            return;
        }
        versions.forEach((id, version) -> {
            if (taskId.equals(version.parentId()) && version.deleted()) {
                collectDeletedSubtree(id, versions, out, depth + 1);
            }
        });
    }

    static String currentValue(TeamTask task, TeamField field) {
        return switch (field) {
            case PARENT -> task.parentId();
            case LEVEL -> String.valueOf(task.level());
            case ORDER -> String.valueOf(task.order());
            case NAME -> task.name();
            case ASSIGNEE -> task.assignee();
            case START -> task.startDate() != null ? task.startDate().toString() : null;
            case END -> task.endDate() != null ? task.endDate().toString() : null;
            case STATUS -> task.status().name();
            case PRIORITY -> task.priority().name();
            case PROGRESS -> String.valueOf(task.progress());
            case MEMO -> task.memo();
            case COMMENT -> null;
        };
    }
}
