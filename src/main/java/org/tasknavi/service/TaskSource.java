package org.tasknavi.service;

import org.tasknavi.model.Assignees;
import org.tasknavi.model.Task;

/**
 * 「今日」と期限の通知が見る、プロジェクト 1 つ分のタスク。
 *
 * @param name    画面に出す名前（「自分だけのタスク」や共有プロジェクト名）
 * @param current 今選んでいるプロジェクトなら true
 * @param service そのプロジェクトのタスクを読み書きする入口
 * @param myName  共有プロジェクトでの自分の表示名。自分だけのタスクは null で、全部を自分の担当とみなす
 */
public record TaskSource(String name, boolean current, TaskService service, String myName) {

    /** 自分だけのタスク用。 */
    public TaskSource(String name, boolean current, TaskService service) {
        this(name, current, service, null);
    }

    /** 自分の担当か。担当者が空のタスクも含める（誰も担当していない期限に、誰も気づかなくならないように）。 */
    public boolean isMine(Task task) {
        if (myName == null || myName.isBlank()) {
            return true;
        }
        String assignee = task.getAssignee();
        return assignee == null || assignee.isBlank() || Assignees.same(assignee, myName);
    }
}
