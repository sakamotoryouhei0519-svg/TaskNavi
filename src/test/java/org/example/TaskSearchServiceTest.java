package org.example;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 【検索ロジックのテスト】
 * このクラスは TaskSearchService の検索機能が正しく動くかを確認するテストコードです。
 *
 * 初心者向けのポイント:
 * - @Test は JUnit で「このメソッドはテストです」と伝える注釈です。
 * - assertEquals は「期待した値と実際の値が一致するか」を確認します。
 * - 1つのテストは 1 つの動作を確認するのが基本です。
 */
public class TaskSearchServiceTest {

    /**
     * 【テスト用タスク生成ヘルパー】
     * テストで何度も使う Task を簡単に作るためのメソッドです。
     *
     * @return 生成した Task オブジェクト
     */
    private Task t(int id, String name, Integer parentId, int level, int progress, String status, LocalDate s, LocalDate e) {
        Task task = new Task(id, name, parentId, level, 0, progress, status, s, e);
        task.setAssignee("user");
        task.setPriority("中");
        return task;
    }

    /**
     * 【名前検索のテスト】
     * タスク名に指定した文字列が含まれるものだけが返るかを確認します。
     */
    @Test
    public void testSearchByName() {
        List<Task> tasks = Arrays.asList(
                t(1, "設計レビュー", null,1,0,"未着手", LocalDate.now(), LocalDate.now().plusDays(1)),
                t(2, "実装", null,1,0,"進行中", LocalDate.now(), LocalDate.now().plusDays(5))
        );
        List<Task> r = TaskSearchService.searchByName(tasks, "設計");
        assertEquals(1, r.size());
        assertEquals(1, r.get(0).getId());
    }

    /**
     * 【状態での絞り込みテスト】
     * 完了済みタスクだけが残るかどうかを確認します。
     */
    @Test
    public void testFilterByStatus() {
        List<Task> tasks = Arrays.asList(
                t(1, "A", null,1,0,"未着手", LocalDate.now(), LocalDate.now()),
                t(2, "B", null,1,0,"完了", LocalDate.now(), LocalDate.now())
        );
        List<Task> r = TaskSearchService.filterByStatus(tasks, Task.STATUS_COMPLETED);
        assertEquals(1, r.size());
        assertEquals(2, r.get(0).getId());
    }

    /**
     * 【日付範囲での検索テスト】
     * 指定した期間に含まれるタスクだけが対象になるかを確認します。
     */
    @Test
    public void testFilterByDateRange() {
        LocalDate start = LocalDate.of(2026,1,1);
        LocalDate end = LocalDate.of(2026,1,31);
        List<Task> tasks = Arrays.asList(
                t(1, "A", null,1,0,"未着手", LocalDate.of(2026,1,5), LocalDate.of(2026,1,6)),
                t(2, "B", null,1,0,"未着手", LocalDate.of(2026,2,1), LocalDate.of(2026,2,2))
        );
        List<Task> r = TaskSearchService.filterByDateRange(tasks, start, end);
        assertEquals(1, r.size());
        assertEquals(1, r.get(0).getId());
    }

    /**
     * 【プロジェクト単位の絞り込みテスト】
     * 親タスクに紐づく子タスクが一緒に取得できるかを確認します。
     */
    @Test
    public void testFilterByProject() {
        Task p = t(10, "Project", null,1,0,"未着手", LocalDate.now(), LocalDate.now());
        Task child = t(11, "Child", 10,2,0,"未着手", LocalDate.now(), LocalDate.now());
        Task other = t(12, "Other", null,1,0,"未着手", LocalDate.now(), LocalDate.now());
        List<Task> tasks = Arrays.asList(p, child, other);
        List<Task> r = TaskSearchService.filterByProject(tasks, 10);
        assertEquals(2, r.size());
    }

    /**
     * 【複数条件をまとめて検索するテスト】
     * 名前・状態・日付・プロジェクトの複数条件を組み合わせた検索を確認します。
     */
    @Test
    public void testSearchCombined() {
        Task p = t(10, "ProjectX", null,1,0,"未着手", LocalDate.of(2026,1,1), LocalDate.of(2026,1,31));
        Task child = t(11, "Implement", 10,2,0,"進行中", LocalDate.of(2026,1,5), LocalDate.of(2026,1,10));
        List<Task> tasks = Arrays.asList(p, child);
        List<Task> r = TaskSearchService.searchCombined(tasks, "Implement", null, Task.STATUS_IN_PROGRESS, LocalDate.of(2026,1,1), LocalDate.of(2026,1,31), 10);
        assertEquals(1, r.size());
        assertEquals(11, r.get(0).getId());
    }
}
