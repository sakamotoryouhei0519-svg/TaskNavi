package org.tasknavi.team;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 記録を組み立てた結果。次に書く記録の clock と base もここから決める。
 *
 * @param tasks     一覧に出すタスク（親 → 子の順）
 * @param conflicts 重なった変更
 * @param maxClock  読んだ記録の clock の最大値
 */
public record TeamState(
        List<TeamTask> tasks,
        List<TeamConflict> conflicts,
        long maxClock,
        Map<String, Version> versions) {

    /**
     * タスクごとの「今の値を書いた記録」。削除済みのタスクも含む。
     *
     * @param lastEventId  このタスクに最後に適用した記録
     * @param fieldEventId 項目ごとに、今の値を書いた記録
     */
    public record Version(String lastEventId, Map<TeamField, String> fieldEventId, boolean deleted,
                          String parentId, String name) {
    }

    public TeamState {
        tasks = List.copyOf(tasks);
        conflicts = List.copyOf(conflicts);
        versions = Map.copyOf(versions);
    }

    public static TeamState empty() {
        return new TeamState(List.of(), List.of(), 0, Map.of());
    }

    public Optional<TeamTask> find(String taskId) {
        return tasks.stream().filter(task -> task.id().equals(taskId)).findFirst();
    }
}
