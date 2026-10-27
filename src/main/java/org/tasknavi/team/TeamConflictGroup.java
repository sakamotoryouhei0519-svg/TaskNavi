package org.tasknavi.team;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 画面に 1 件として出す重なり。同じ記録が同じ人の値を複数の項目で上書きしたら、まとめて 1 件にする。
 *
 * @param items 項目ごとの重なり（SAME_FIELD 以外は 1 件）
 */
public record TeamConflictGroup(
        TeamConflict.Kind kind,
        String taskId,
        String taskName,
        String keptBy,
        String lostBy,
        Instant at,
        List<TeamConflict> items) {

    public List<String> keys() {
        return items.stream().map(TeamConflict::key).toList();
    }

    /** 確認済みの重なりを除き、新しい順に並べる。 */
    public static List<TeamConflictGroup> unacknowledged(List<TeamConflict> conflicts, Collection<String> acknowledged) {
        Map<String, List<TeamConflict>> grouped = new LinkedHashMap<>();
        for (TeamConflict conflict : conflicts) {
            if (acknowledged.contains(conflict.key())) {
                continue;
            }
            String groupKey = conflict.eventId() + "|" + conflict.taskId() + "|" + conflict.kind() + "|"
                    + Objects.toString(conflict.lostBy(), "");
            grouped.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(conflict);
        }
        List<TeamConflictGroup> result = new ArrayList<>();
        for (List<TeamConflict> items : grouped.values()) {
            TeamConflict first = items.get(0);
            result.add(new TeamConflictGroup(first.kind(), first.taskId(), first.taskName(),
                    first.keptBy(), first.lostBy(), first.at(), List.copyOf(items)));
        }
        result.sort((a, b) -> b.at().compareTo(a.at()));
        return result;
    }
}
