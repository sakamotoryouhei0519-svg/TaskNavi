package org.tasknavi.team;

import org.tasknavi.model.Priority;
import org.tasknavi.model.TaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 変更記録を古い順に当てはめ直し、1 件ごとに「どの項目が何から何に変わったか」を出す。
 * 並べ方と後勝ちは {@link TeamReplay} と同じなので、最後の値は一覧の表示と一致する。
 */
public final class TeamHistory {

    /** 親は ID ではなく、その時点の親の名前で持つ。 */
    public record Change(TeamField field, String before, String after) {
    }

    public record Entry(String eventId, TeamEvent.Op op, String taskId, String taskName,
                        String author, Instant at, List<Change> changes) {
    }

    private TeamHistory() {
    }

    /** @return 新しい順 */
    public static List<Entry> build(Collection<TeamEvent> events) {
        Map<String, TeamEvent> unique = new LinkedHashMap<>();
        events.forEach(event -> unique.putIfAbsent(event.eventId(), event));
        List<TeamEvent> ordered = new ArrayList<>(unique.values());
        ordered.sort(TeamEvent.ORDER);

        Map<String, Map<TeamField, String>> current = new HashMap<>();
        Map<String, String> commentText = new HashMap<>();
        List<Entry> entries = new ArrayList<>(ordered.size());
        for (TeamEvent event : ordered) {
            Map<TeamField, String> values = current.computeIfAbsent(event.taskId(), id -> new EnumMap<>(TeamField.class));
            List<Change> changes = new ArrayList<>();
            if (event.op() == TeamEvent.Op.CREATE || event.op() == TeamEvent.Op.UPDATE) {
                for (Map.Entry<TeamField, String> value : event.values().entrySet()) {
                    TeamField field = value.getKey();
                    if (field == TeamField.LEVEL || field == TeamField.ORDER) {
                        continue;
                    }
                    String before = effective(field, values.get(field));
                    String after = effective(field, value.getValue());
                    values.put(field, after);
                    if (Objects.equals(before, after)) {
                        continue;
                    }
                    if (field == TeamField.PARENT) {
                        before = nameOf(current, before);
                        after = nameOf(current, after);
                    }
                    changes.add(new Change(field, before, after));
                }
            } else if (event.op() == TeamEvent.Op.COMMENT) {
                String text = event.values().get(TeamField.COMMENT);
                commentText.put(event.eventId(), text);
                changes.add(new Change(TeamField.COMMENT, null, text));
            } else if (event.op() == TeamEvent.Op.COMMENT_EDIT || event.op() == TeamEvent.Op.COMMENT_DELETE) {
                String after = event.op() == TeamEvent.Op.COMMENT_EDIT ? event.values().get(TeamField.COMMENT) : null;
                String before = commentText.put(event.baseEventId(), after);
                changes.add(new Change(TeamField.COMMENT, before, after));
            }
            entries.add(new Entry(event.eventId(), event.op(), event.taskId(),
                    Objects.toString(values.get(TeamField.NAME), ""), event.author(), event.at(), List.copyOf(changes)));
        }
        Collections.reverse(entries);
        return entries;
    }

    private static String nameOf(Map<String, Map<TeamField, String>> current, String taskId) {
        if (taskId == null) {
            return null;
        }
        Map<TeamField, String> values = current.get(taskId);
        return values != null && values.get(TeamField.NAME) != null ? values.get(TeamField.NAME) : taskId;
    }

    /** 一覧と同じく、状態・優先度・進捗は空なら既定値として扱う（「（空）→ 未着手」と出さない）。 */
    private static String effective(TeamField field, String value) {
        String raw = value == null || value.isBlank() ? null : value;
        return switch (field) {
            case STATUS -> TaskStatus.fromString(raw).name();
            case PRIORITY -> Priority.fromString(raw).name();
            case PROGRESS -> raw != null ? raw : "0";
            default -> raw;
        };
    }
}
