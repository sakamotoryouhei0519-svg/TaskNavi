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
 * @param kind     自分が担当になったか、自分の担当のタスクが変わったか
 * @param taskName タスク名（今の名前）
 * @param author   最後に変えた人の表示名
 */
public record TeamNotice(Kind kind, String taskName, String author) {

    public enum Kind { ASSIGNED, CHANGED }

    /**
     * @param arrived ほかの PC から新しく届いた変更（届いた順）
     * @param state   届いた変更を当てはめたあとの状態
     * @param myName  自分の表示名
     */
    static List<TeamNotice> forMe(List<TeamEvent> arrived, TeamState state, String myName) {
        Map<String, TeamNotice> byTask = new LinkedHashMap<>();
        for (TeamEvent event : arrived) {
            Optional<TeamTask> task = state.find(event.taskId());
            if (task.isEmpty()) {
                continue;
            }
            boolean assignedNow = event.values().containsKey(TeamField.ASSIGNEE)
                    && Assignees.same(event.values().get(TeamField.ASSIGNEE), myName);
            boolean mine = Assignees.same(task.get().assignee(), myName);
            TeamNotice previous = byTask.get(event.taskId());
            if (assignedNow && mine) {
                byTask.put(event.taskId(), new TeamNotice(Kind.ASSIGNED, task.get().name(), event.author()));
            } else if (mine && event.op() != TeamEvent.Op.CREATE) {
                Kind kind = previous != null ? previous.kind() : Kind.CHANGED;
                byTask.put(event.taskId(), new TeamNotice(kind, task.get().name(), event.author()));
            }
        }
        return new ArrayList<>(byTask.values());
    }
}
