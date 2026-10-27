package org.tasknavi.team;

import org.tasknavi.model.TaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 全員の記録を決まった順に並べ、頭から適用して今のタスク一覧を作る。
 * 同じ記録の集まりからは、どの PC でも同じ結果になる。
 */
public final class TeamReplay {

    private TeamReplay() {
    }

    public static TeamState replay(Collection<TeamEvent> events) {
        List<TeamEvent> ordered = dedupe(events);
        ordered.sort(TeamEvent.ORDER);

        Map<String, Entry> entries = new LinkedHashMap<>();
        List<TeamConflict> conflicts = new ArrayList<>();
        long maxClock = 0;
        for (TeamEvent event : ordered) {
            maxClock = Math.max(maxClock, event.clock());
            switch (event.op()) {
                case CREATE -> applyCreate(entries, event);
                case UPDATE -> applyUpdate(entries, event, conflicts);
                case DELETE -> applyDelete(entries, event, conflicts);
                case RESTORE -> applyRestore(entries, event);
                case COMMENT -> applyComment(entries, event);
                case COMMENT_EDIT, COMMENT_DELETE -> applyCommentChange(entries, event);
            }
        }

        List<TeamTask> tasks = visibleTasks(entries, conflicts);
        Map<String, TeamState.Version> versions = new HashMap<>();
        Map<String, List<TeamComment>> comments = new HashMap<>();
        entries.forEach((id, entry) -> {
            versions.put(id, new TeamState.Version(
                    entry.lastEventId, Map.copyOf(entry.fieldEventId), entry.deleted, entry.parentId(), entry.name()));
            if (!entry.comments.isEmpty()) {
                comments.put(id, List.copyOf(entry.comments.values()));
            }
        });
        return new TeamState(tasks, conflicts, maxClock, versions, comments);
    }

    /** 同じ記録が二重に届いても（ファイルの複製など）1 回だけ数える。 */
    private static List<TeamEvent> dedupe(Collection<TeamEvent> events) {
        Map<String, TeamEvent> byId = new LinkedHashMap<>();
        for (TeamEvent event : events) {
            byId.putIfAbsent(event.eventId(), event);
        }
        return new ArrayList<>(byId.values());
    }

    private static void applyCreate(Map<String, Entry> entries, TeamEvent event) {
        if (entries.containsKey(event.taskId())) {
            return;
        }
        Entry entry = new Entry(event.taskId(), event.clock(), event.author());
        for (Map.Entry<TeamField, String> value : event.values().entrySet()) {
            entry.set(value.getKey(), value.getValue(), event);
        }
        entry.touch(event);
        entries.put(event.taskId(), entry);
    }

    private static void applyUpdate(Map<String, Entry> entries, TeamEvent event, List<TeamConflict> conflicts) {
        Entry entry = entries.get(event.taskId());
        if (entry == null) {
            // 作成の記録がまだ同期されていない。届けば最初から組み立て直すので今は飛ばす。
            return;
        }
        if (entry.deleted) {
            // 一覧には出ないが値は残す。タスクを戻したとき、この変更も戻ってくる。
            conflicts.add(new TeamConflict(event.eventId(), TeamConflict.Kind.EDIT_AFTER_DELETE, entry.id, entry.name(),
                    null, null, null, entry.deletedBy, event.author(), event.at()));
            event.values().forEach((field, value) -> entry.set(field, value, event));
            entry.touch(event);
            return;
        }
        for (Map.Entry<TeamField, String> change : event.values().entrySet()) {
            TeamField field = change.getKey();
            String newValue = change.getValue();
            String currentValue = entry.values.get(field);
            String seenEventId = event.base().get(field);
            boolean overwroteUnseen = !Objects.equals(seenEventId, entry.fieldEventId.get(field));
            if (overwroteUnseen && !Objects.equals(newValue, currentValue)) {
                conflicts.add(new TeamConflict(event.eventId(), TeamConflict.Kind.SAME_FIELD, entry.id, entry.name(),
                        field, newValue, currentValue, event.author(), entry.fieldAuthor.get(field), event.at()));
            }
            entry.set(field, newValue, event);
        }
        entry.touch(event);
    }

    private static void applyRestore(Map<String, Entry> entries, TeamEvent event) {
        Entry entry = entries.get(event.taskId());
        if (entry == null || !entry.deleted) {
            return;
        }
        entry.deleted = false;
        entry.deletedBy = null;
        entry.touch(event);
    }

    /**
     * 削除済みのタスクにも付ける（戻したときに一緒に戻る）。タスクの版（lastEventId）は進めない。
     * 進めると、コメントを見ずに削除した人の削除が「変更と重なった」ことになるため。
     */
    private static void applyComment(Map<String, Entry> entries, TeamEvent event) {
        Entry entry = entries.get(event.taskId());
        String text = event.values().get(TeamField.COMMENT);
        if (entry == null || text == null || text.isBlank()) {
            return;
        }
        entry.comments.putIfAbsent(event.eventId(),
                new TeamComment(event.eventId(), event.author(), event.deviceId(), event.at(), text, false, false));
    }

    /**
     * 書いた PC からの編集・削除だけを当てはめる。ほかの PC からの記録（ファイルを手で書き換えたなど）は無視するので、
     * どの PC でも同じ結果になる。削除したあとの編集も無視する。
     */
    private static void applyCommentChange(Map<String, Entry> entries, TeamEvent event) {
        Entry entry = entries.get(event.taskId());
        TeamComment comment = entry != null && event.baseEventId() != null
                ? entry.comments.get(event.baseEventId()) : null;
        if (comment == null || comment.deleted() || !comment.deviceId().equals(event.deviceId())) {
            return;
        }
        if (event.op() == TeamEvent.Op.COMMENT_DELETE) {
            entry.comments.put(comment.eventId(), comment.asDeleted());
            return;
        }
        String text = event.values().get(TeamField.COMMENT);
        if (text != null && !text.isBlank()) {
            entry.comments.put(comment.eventId(), comment.withText(text));
        }
    }

    private static void applyDelete(Map<String, Entry> entries, TeamEvent event, List<TeamConflict> conflicts) {
        Entry entry = entries.get(event.taskId());
        if (entry == null || entry.deleted) {
            return;
        }
        if (!Objects.equals(event.baseEventId(), entry.lastEventId)) {
            conflicts.add(new TeamConflict(event.eventId(), TeamConflict.Kind.DELETE_OVER_EDIT, entry.id, entry.name(),
                    null, null, null, event.author(), entry.updatedBy, event.at()));
        }
        entry.deleted = true;
        entry.deletedBy = event.author();
        entry.touch(event);
    }

    /**
     * 削除済みと、親が見えないタスクを除き、親 → 子の順に並べる。階層は記録の値ではなく親子の深さから決める
     * （親を付け替えると子孫の階層も変わるため）。
     */
    private static List<TeamTask> visibleTasks(Map<String, Entry> entries, List<TeamConflict> conflicts) {
        Map<String, List<Entry>> childrenByParent = new HashMap<>();
        for (Entry entry : entries.values()) {
            if (!entry.deleted) {
                childrenByParent.computeIfAbsent(entry.parentId(), key -> new ArrayList<>()).add(entry);
            }
        }
        Comparator<Entry> siblingOrder = Comparator
                .comparingInt(Entry::order)
                .thenComparingLong(entry -> entry.createdClock)
                .thenComparing(entry -> entry.id);

        List<TeamTask> result = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        appendChildren(null, childrenByParent, siblingOrder, result, 0, placed);
        // 2 人が互いを相手の下へ同時に動かすと、親をたどっても一番上に着かない輪ができる。
        // 消すわけにはいかないので、輪の中で最初に作られたタスクを一番上に出す（どの PC でも同じ結果）。
        List<String> creationOrder = new ArrayList<>(entries.keySet());
        for (Entry entry : entries.values()) {
            if (entry.deleted || placed.contains(entry.id)) {
                continue;
            }
            Entry top = cycleTop(entry, entries, creationOrder);
            if (top != null && placed.add(top.id)) {
                result.add(top.toTask(null, 1));
                appendChildren(top.id, childrenByParent, siblingOrder, result, 1, placed);
            }
        }
        result = rollUpProgress(result);

        for (Entry entry : entries.values()) {
            if (entry.deleted || entry.parentId() == null) {
                continue;
            }
            Entry parent = entries.get(entry.parentId());
            if (parent != null && parent.deleted) {
                conflicts.add(new TeamConflict(parent.lastEventId, TeamConflict.Kind.PARENT_DELETED, entry.id, entry.name(),
                        null, null, null, parent.deletedBy, entry.updatedBy, entry.updatedAt));
            }
        }
        return result;
    }

    /**
     * 子のある親の進捗と状態を、子の平均から出す（個人用の TaskHierarchySupport と同じ決まり）。
     * 記録には書かない。書くと、子を変えた人全員が親も書き換え、重なりが増えるため。
     *
     * @param tree 親 → 子の順。逆から辿れば子が先に決まる
     */
    static List<TeamTask> rollUpProgress(List<TeamTask> tree) {
        Map<String, TeamTask> byId = new LinkedHashMap<>();
        Map<String, List<String>> childIds = new HashMap<>();
        for (TeamTask task : tree) {
            byId.put(task.id(), task);
            if (task.parentId() != null) {
                childIds.computeIfAbsent(task.parentId(), key -> new ArrayList<>()).add(task.id());
            }
        }
        for (int i = tree.size() - 1; i >= 0; i--) {
            String id = tree.get(i).id();
            List<String> children = childIds.get(id);
            if (children == null) {
                continue;
            }
            int total = 0;
            boolean allCompleted = true;
            for (String childId : children) {
                TeamTask child = byId.get(childId);
                total += child.progress();
                allCompleted &= child.status() == TaskStatus.COMPLETED;
            }
            int average = (int) Math.round((double) total / children.size());
            TaskStatus status = allCompleted ? TaskStatus.COMPLETED
                    : average > 0 ? TaskStatus.IN_PROGRESS : TaskStatus.NOT_STARTED;
            byId.put(id, byId.get(id).withProgress(allCompleted ? 100 : average, status));
        }
        return new ArrayList<>(byId.values());
    }

    private static void appendChildren(String parentId, Map<String, List<Entry>> childrenByParent,
                                       Comparator<Entry> siblingOrder, List<TeamTask> result, int depth,
                                       Set<String> placed) {
        List<Entry> children = childrenByParent.get(parentId);
        if (children == null) {
            return;
        }
        children.sort(siblingOrder);
        for (Entry child : children) {
            if (!placed.add(child.id)) {
                continue;
            }
            result.add(child.toTask(parentId, depth + 1));
            appendChildren(child.id, childrenByParent, siblingOrder, result, depth + 1, placed);
        }
    }

    /**
     * 親をたどって輪に入るなら、輪の中で最初に作られたタスク。途中に削除済みやまだ届いていない親があれば null。
     */
    private static Entry cycleTop(Entry entry, Map<String, Entry> entries, List<String> creationOrder) {
        List<String> path = new ArrayList<>();
        Entry current = entry;
        while (!path.contains(current.id)) {
            path.add(current.id);
            String parentId = current.parentId();
            current = parentId != null ? entries.get(parentId) : null;
            if (current == null || current.deleted) {
                return null;
            }
        }
        List<String> cycle = path.subList(path.indexOf(current.id), path.size());
        return entries.get(cycle.stream().min(Comparator.comparingInt(creationOrder::indexOf)).orElseThrow());
    }

    private static final class Entry {
        final String id;
        final long createdClock;
        final String createdBy;
        final Map<TeamField, String> values = new EnumMap<>(TeamField.class);
        final Map<TeamField, String> fieldEventId = new EnumMap<>(TeamField.class);
        final Map<TeamField, String> fieldAuthor = new EnumMap<>(TeamField.class);
        /** コメントの記録 ID → コメント（書いた順）。 */
        final Map<String, TeamComment> comments = new LinkedHashMap<>();
        String lastEventId;
        String updatedBy;
        Instant updatedAt;
        boolean deleted;
        String deletedBy;

        Entry(String id, long createdClock, String createdBy) {
            this.id = id;
            this.createdClock = createdClock;
            this.createdBy = createdBy;
        }

        void set(TeamField field, String value, TeamEvent event) {
            values.put(field, value);
            fieldEventId.put(field, event.eventId());
            fieldAuthor.put(field, event.author());
        }

        void touch(TeamEvent event) {
            lastEventId = event.eventId();
            updatedBy = event.author();
            updatedAt = event.at();
        }

        String name() {
            String name = values.get(TeamField.NAME);
            return name != null ? name : "";
        }

        String parentId() {
            String parent = values.get(TeamField.PARENT);
            return parent == null || parent.isBlank() ? null : parent;
        }

        int order() {
            try {
                return Integer.parseInt(values.getOrDefault(TeamField.ORDER, "0"));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        TeamTask toTask(String shownParentId, int level) {
            return TeamTask.from(id, values, createdBy, updatedBy, updatedAt).withPlacement(shownParentId, level);
        }
    }
}
