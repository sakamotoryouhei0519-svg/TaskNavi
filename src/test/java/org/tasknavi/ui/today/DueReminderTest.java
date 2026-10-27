package org.tasknavi.ui.today;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.persistence.TaskStore;
import org.tasknavi.service.DueTasks;
import org.tasknavi.service.TaskService;
import org.tasknavi.service.TaskSource;
import org.tasknavi.team.TeamNotice;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DueReminderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @BeforeEach
    void useJapanese() {
        AppMessages.setLocale(Locale.JAPAN);
    }

    @AfterEach
    void restoreLocale() {
        AppMessages.setLocale(Locale.getDefault());
    }

    @Test
    void listsOnlyNonZeroCounts() {
        DueTasks due = DueTasks.of(List.of(
                task(1, TODAY.minusDays(1)),
                task(2, TODAY.minusDays(3)),
                task(3, TODAY.plusDays(1))
        ), TODAY);

        assertEquals("期限切れ 2件 / 明日まで 1件", DueReminder.buildMessage(due, TODAY));
    }

    @Test
    void staysSilentWhenOnlyLaterTasksExist() {
        DueTasks due = DueTasks.of(List.of(task(1, TODAY.plusDays(3))), TODAY);

        assertNull(DueReminder.buildMessage(due, TODAY));
    }

    @Test
    void countsTasksOfEveryProject() {
        TaskService personal = new TaskService(new FixedStore(List.of(task(1, TODAY))));
        TaskService shared = new TaskService(new FixedStore(List.of(task(1, TODAY.minusDays(2)), task(2, TODAY))));

        DueTasks due = DueReminder.dueAcross(List.of(
                new TaskSource("自分だけのタスク", true, personal),
                new TaskSource("新システム開発", false, shared)), TODAY, false);

        assertEquals("期限切れ 1件 / 今日まで 2件", DueReminder.buildMessage(due, TODAY));
    }

    @Test
    void onlyMineCountsSharedTasksOfMineOrNoOne() {
        Task mine = task(1, TODAY);
        mine.setAssignee("田中");
        Task others = task(2, TODAY);
        others.setAssignee("鈴木");
        Task nobodys = task(3, TODAY.minusDays(1));
        TaskService shared = new TaskService(new FixedStore(List.of(mine, others, nobodys)));
        List<TaskSource> sources = List.of(new TaskSource("新システム開発", true, shared, "田中"));

        assertEquals("期限切れ 1件 / 今日まで 1件",
                DueReminder.buildMessage(DueReminder.dueAcross(sources, TODAY, true), TODAY));
        assertEquals("期限切れ 1件 / 今日まで 2件",
                DueReminder.buildMessage(DueReminder.dueAcross(sources, TODAY, false), TODAY));
    }

    @Test
    void noticeMessageNamesTheTaskWhenThereIsOnlyOne() {
        Map<String, List<TeamNotice>> one = new LinkedHashMap<>();
        one.put("新システム開発", List.of(new TeamNotice(TeamNotice.Kind.ASSIGNED, "設計", "鈴木")));
        Map<String, List<TeamNotice>> changed = new LinkedHashMap<>();
        changed.put("新システム開発", List.of(new TeamNotice(TeamNotice.Kind.CHANGED, "設計", "鈴木")));

        assertEquals("鈴木さんが「設計」をあなたの担当にしました（新システム開発）", DueReminder.buildNoticeMessage(one));
        assertEquals("鈴木さんがあなたの担当の「設計」を変えました（新システム開発）", DueReminder.buildNoticeMessage(changed));
        assertNull(DueReminder.buildNoticeMessage(Map.of()));
    }

    @Test
    void noticeMessageCountsWhenThereAreSeveral() {
        Map<String, List<TeamNotice>> many = new LinkedHashMap<>();
        many.put("新システム開発", List.of(
                new TeamNotice(TeamNotice.Kind.ASSIGNED, "設計", "鈴木"),
                new TeamNotice(TeamNotice.Kind.CHANGED, "実装", "佐藤")));
        many.put("社内研修", List.of(new TeamNotice(TeamNotice.Kind.CHANGED, "資料", "鈴木")));

        assertEquals("あなたの担当のタスクに 3 件の変更があります（新システム開発、社内研修）",
                DueReminder.buildNoticeMessage(many));
    }

    /** 読むだけの保存先。 */
    private record FixedStore(List<Task> tasks) implements TaskStore {
        @Override
        public List<Task> getAllTasks() {
            return tasks;
        }

        @Override
        public Task getTaskById(int taskId) {
            return tasks.stream().filter(task -> task.getId() == taskId).findFirst().orElse(null);
        }

        @Override
        public int addTask(Task task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateTasks(List<Task> updated) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateTaskOrderIndex(int taskId, int newOrderIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateSiblingOrder(List<Task> orderedTasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Task> deleteTask(int taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void restoreTasks(List<Task> deletedTasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void importTasks(List<Task> imported, boolean replaceExisting) {
            throw new UnsupportedOperationException();
        }
    }

    private static Task task(int id, LocalDate end) {
        return new Task(id, "t" + id, null, 1, 0, 0, Task.STATUS_NOT_STARTED, TODAY.minusDays(5), end);
    }
}
