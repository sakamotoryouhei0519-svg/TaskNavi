package org.tasknavi.team;

import org.tasknavi.AppMessages;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 取り込む記録をまとめて作る。全件を確かめてから返すので、途中の 1 件が駄目なら何も書かれない。 */
final class TeamImport {

    /** @param taskIds 作るタスクの ID（取り込む順） */
    record Batch(List<TeamEvent> events, List<String> taskIds) {
    }

    private TeamImport() {
    }

    /** @param replaceExisting 今あるタスクを全部消してから取り込む */
    static Batch build(TeamState state, List<TeamProject.ImportDraft> drafts, boolean replaceExisting,
                       String deviceId, String author, Instant now) {
        long clockValue = state.maxClock();
        List<TeamEvent> events = new ArrayList<>();
        if (replaceExisting) {
            List<TeamTask> live = state.tasks();
            for (int i = live.size() - 1; i >= 0; i--) {
                String id = live.get(i).id();
                TeamState.Version version = state.versions().get(id);
                events.add(new TeamEvent(UUID.randomUUID().toString(), id, TeamEvent.Op.DELETE, ++clockValue,
                        deviceId, author, now, Map.of(), Map.of(), version != null ? version.lastEventId() : null));
            }
        }
        List<String> ids = new ArrayList<>(drafts.size());
        List<Integer> levels = new ArrayList<>(drafts.size());
        Map<String, Integer> siblings = new HashMap<>();
        for (int i = 0; i < drafts.size(); i++) {
            TeamProject.ImportDraft draft = drafts.get(i);
            Map<TeamField, String> fields = new EnumMap<>(TeamField.class);
            fields.putAll(draft.values());
            String row = String.valueOf(i + 1);
            int parentIndex = draft.parentIndex();
            int level;
            try {
                TeamTaskRules.requireName(fields.get(TeamField.NAME));
                if (parentIndex >= i) {
                    throw new IllegalArgumentException(AppMessages.get("team.error.import.parent.order", "親タスクは先に並べてください"));
                }
                level = TeamTaskRules.requireLevel(parentIndex >= 0 ? levels.get(parentIndex) + 1 : 1);
                TeamTaskRules.requireDateOrder(fields.get(TeamField.START), fields.get(TeamField.END));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(AppMessages.format("team.error.import.row", "{0} 件目: {1}", row, e.getMessage()), e);
            }
            String parentId = parentIndex >= 0 ? ids.get(parentIndex) : null;
            fields.put(TeamField.PARENT, parentId);
            fields.put(TeamField.LEVEL, String.valueOf(level));
            int nextOrder = siblings.merge(String.valueOf(parentId), 1, Integer::sum) - 1;
            if (!fields.containsKey(TeamField.ORDER)) {
                long existing = parentId == null && !replaceExisting
                        ? state.tasks().stream().filter(task -> task.parentId() == null).count() : 0;
                fields.put(TeamField.ORDER, String.valueOf(existing + nextOrder));
            }
            String taskId = UUID.randomUUID().toString();
            ids.add(taskId);
            levels.add(level);
            events.add(new TeamEvent(UUID.randomUUID().toString(), taskId, TeamEvent.Op.CREATE, ++clockValue,
                    deviceId, author, now, fields, Map.of(), null));
        }
        return new Batch(events, ids);
    }
}
