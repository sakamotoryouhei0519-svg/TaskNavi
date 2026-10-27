package org.tasknavi.ui.main;

import org.tasknavi.AppMessages;
import org.tasknavi.persistence.TaskStore;
import org.tasknavi.service.TaskService;
import org.tasknavi.service.TaskSource;
import org.tasknavi.team.TeamProject;
import org.tasknavi.ui.team.TeamSession;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「今日」と期限の通知が見る、全部のプロジェクト。今選んでいるもの・自分だけのタスク・最近開いた共有プロジェクトの順。
 * 今選んでいないプロジェクトは、それぞれ専用の TaskService で読み書きする（選んでいるものとは番号が別のため）。
 */
final class ProjectSources {

    private final TaskService taskService;
    private final TeamSession session;
    private final Map<TaskStore, TaskService> others = new IdentityHashMap<>();

    ProjectSources(TaskService taskService, TeamSession session) {
        this.taskService = taskService;
        this.session = session;
    }

    List<TaskSource> all() {
        String personal = AppMessages.get("project.personal", "自分だけのタスク");
        TeamProject current = session.project();
        List<TaskSource> result = new ArrayList<>();
        result.add(new TaskSource(current != null ? current.name() : personal, true, taskService));
        if (current != null) {
            result.add(new TaskSource(personal, false, serviceFor(taskService.personalStore())));
        }
        for (TeamProject other : session.otherProjects()) {
            result.add(new TaskSource(other.name(), false, serviceFor(session.storeOf(other))));
        }
        return result;
    }

    /** 開いていないプロジェクトの中身が変わったので、次に読むときに読み直させる。 */
    void invalidate() {
        others.values().forEach(TaskService::invalidate);
    }

    private TaskService serviceFor(TaskStore store) {
        return others.computeIfAbsent(store, TaskService::new);
    }
}
