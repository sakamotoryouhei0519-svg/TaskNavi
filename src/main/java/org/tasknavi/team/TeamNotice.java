package org.tasknavi.team;

import org.tasknavi.model.Assignees;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ほかの人の変更のうち、自分に知らせるもの。1 つのタスクにつき 1 件にまとめる。
 *
 * @param kind     自分が担当になったか、自分の担当のタスクにコメントが付いたか、
 *                 自分もコメントしたタスクにコメントが付いたか、自分の担当のタスクが変わったか
 * @param taskName タスク名（今の名前）
 * @param author   そうした人の表示名
 */
public record TeamNotice(Kind kind, String taskName, String author) {

    /** 知らせる強さの順。1 つのタスクに何件届いても、一番強いもの 1 件にまとめる。 */
    public enum Kind { ASSIGNED, COMMENTED, REPLIED, CHANGED }

    /**
     * @param arrived    ほかの PC から新しく届いた変更（届いた順）
     * @param state      届いた変更を当てはめたあとの状態
     * @param myName     自分の表示名
     * @param myDeviceId 自分の PC。自分もコメントしたタスクかどうかは、表示名ではなくこれで決める（名前は変えられるため）
     */
    static List<TeamNotice> forMe(List<TeamEvent> arrived, TeamState state, String myName, String myDeviceId) {
        Map<String, TeamNotice> byTask = new LinkedHashMap<>();
        for (TeamEvent event : arrived) {
            Optional<TeamTask> task = state.find(event.taskId());
            if (task.isEmpty() || event.op() == TeamEvent.Op.COMMENT_EDIT || event.op() == TeamEvent.Op.COMMENT_DELETE) {
                continue;
            }
            Kind kind = kindOf(event, task.get(), state, myName, myDeviceId);
            if (kind == null) {
                continue;
            }
            TeamNotice previous = byTask.get(event.taskId());
            if (previous == null || kind.compareTo(previous.kind()) <= 0) {
                byTask.put(event.taskId(), new TeamNotice(kind, task.get().name(), event.author()));
            }
        }
        return new ArrayList<>(byTask.values());
    }

    /** 知らせないものは null。 */
    private static Kind kindOf(TeamEvent event, TeamTask task, TeamState state, String myName, String myDeviceId) {
        if (!Assignees.same(task.assignee(), myName)) {
            return event.op() == TeamEvent.Op.COMMENT && commentedBy(state, event.taskId(), myDeviceId)
                    ? Kind.REPLIED : null;
        }
        if (event.values().containsKey(TeamField.ASSIGNEE)
                && Assignees.same(event.values().get(TeamField.ASSIGNEE), myName)) {
            return Kind.ASSIGNED;
        }
        if (event.op() == TeamEvent.Op.COMMENT) {
            return Kind.COMMENTED;
        }
        return event.op() == TeamEvent.Op.CREATE ? null : Kind.CHANGED;
    }

    private static boolean commentedBy(TeamState state, String taskId, String deviceId) {
        return state.comments(taskId).stream().anyMatch(comment -> comment.deviceId().equals(deviceId));
    }
}
