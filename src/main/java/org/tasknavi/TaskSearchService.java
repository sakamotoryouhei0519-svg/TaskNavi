package org.tasknavi;

import java.time.LocalDate;
import java.util.*;

/**
 * 【タスク検索・フィルタサービスクラス】
 * 画面で入力された条件をもとに、Task の一覧から必要なものだけを抽出する役割を持ちます。
 *
 * 初心者向けの解説:
 * - これは「検索ロジック」をまとめたクラスです。
 * - 画面側は複雑な条件分岐をここに書かず、メソッドを呼ぶだけで済みます。
 * - ユーザーが見たくない条件はここでフィルタされ、表示用のリストだけが返ります。
 */
public class TaskSearchService {

    /**
     * 【名前検索】
     * 指定したキーワードがタスク名に含まれているものだけを返すメソッドです。
     *
     * 初心者向けの解説:
     * - keyword が空なら「検索条件がない」とみなして全件を返します。
     * - toLowerCase() を使って大文字小文字の違いを吸収しています。
     * - contains() で部分一致検索を行います。
     *
     * @param tasks 検索対象のタスク一覧
     * @param keyword 検索したい文字列（例: 「設計」）
     * @return 条件に合うタスクの新しいリスト
     */
    public static List<Task> searchByName(List<Task> tasks, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(tasks);
        }

        List<Task> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (Task task : tasks) {
            if (task.getName() != null && task.getName().toLowerCase().contains(lowerKeyword)) {
                result.add(task);
            }
        }

        return result;
    }

    /**
     * 【担当者検索】
     * 特定の担当者が assigned されているタスクだけを抽出します。
     *
     * 初心者向けの解説:
     * - 同じ担当者が複数タスクを持っていても、1 件ずつ見て条件を満たすものだけを集めます。
     * - ここでは「完全一致」で比較しています。
     *
     * @param tasks 一覧
     * @param assignee 担当者名
     * @return 該当担当者のリスト
     */
    public static List<Task> searchByAssignee(List<Task> tasks, String assignee) {
        if (assignee == null || assignee.trim().isEmpty()) {
            return new ArrayList<>(tasks);
        }

        List<Task> result = new ArrayList<>();

        for (Task task : tasks) {
            if (assignee.equals(task.getAssignee())) {
                result.add(task);
            }
        }

        return result;
    }

    /**
     * 【状態フィルタ】
     * 例: 「未着手」「進行中」「完了」などの状態に一致するタスクだけを返します。
     *
     * 初心者向けの解説:
     * - status が null または FILTER_ALL（ALL）ならフィルタしません。
     * - これにより、検索画面で「すべて」を選択したときは全件表示になります。
     *
     * @param tasks 一覧
     * @param status フィルタしたい状態
     * @return 条件に一致するタスク一覧
     */
    public static List<Task> filterByStatus(List<Task> tasks, String status) {
        if (status == null || status.trim().isEmpty() || AppMessages.isFilterAll(status)) {
            return new ArrayList<>(tasks);
        }

        List<Task> result = new ArrayList<>();

        for (Task task : tasks) {
            if (status.equals(task.getStatusCode())) {
                result.add(task);
            }
        }

        return result;
    }

    /**
     * 【日付範囲でのフィルタ】
     * 開始日と終了日が指定した期間と重なるタスクだけを抽出します。
     *
     * 初心者向けの解説:
     * - 期間は「開始日 >= startDate かつ 終了日 <= endDate」のようなイメージですが、
     *   実際にはタスクの開始日・終了日が期間に跨っているかをチェックしています。
     * - 範囲が重なるケースを扱えるように、2つの条件を分けて確認しています。
     *
     * @param tasks 一覧
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 指定期間に含まれるタスク
     */
    public static List<Task> filterByDateRange(List<Task> tasks, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return new ArrayList<>(tasks);
        }

        List<Task> result = new ArrayList<>();

        for (Task task : tasks) {
            LocalDate taskStart = task.getStartDate();
            LocalDate taskEnd = task.getEndDate();

            boolean inRange = false;
            if (taskStart != null &&
                (taskStart.isEqual(startDate) || taskStart.isAfter(startDate)) &&
                (taskStart.isEqual(endDate) || taskStart.isBefore(endDate))) {
                inRange = true;
            }
            if (!inRange && taskEnd != null &&
                (taskEnd.isEqual(startDate) || taskEnd.isAfter(startDate)) &&
                (taskEnd.isEqual(endDate) || taskEnd.isBefore(endDate))) {
                inRange = true;
            }

            if (inRange) {
                result.add(task);
            }
        }

        return result;
    }

    /**
     * 【プロジェクトに属するタスクを取得】
     * 指定した projectId を親にもつタスク、その子孫すべてを取得します。
     *
     * 初心者向けの解説:
     * - WBS のような親子関係があるとき、1 つの大きなプロジェクト配下の全タスクを見たいときに便利です。
     * - projectId が null なら何もしないで全件返す設計です。
     * - isDescendantOrSelf() を使って、自分自身か祖先が対象 ID かを判定します。
     *
     * @param tasks 一覧
     * @param projectId ルートにしたいタスクの ID
     * @return そのプロジェクト配下の全タスク
     */
    public static List<Task> filterByProject(List<Task> tasks, Integer projectId) {
        if (projectId == null) {
            return new ArrayList<>(tasks);
        }

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : tasks) {
            taskMap.put(t.getId(), t);
        }

        Set<Integer> allowedTaskIds = new HashSet<>();
        for (Task task : tasks) {
            if (isDescendantOrSelf(task, projectId, taskMap)) {
                allowedTaskIds.add(task.getId());
            }
        }

        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (allowedTaskIds.contains(task.getId())) {
                result.add(task);
            }
        }

        return result;
    }

    /**
     * 【複合条件検索】
     * 検索画面で入力された複数条件を順番にかけて最終的な結果を返します。
     *
     * 初心者向けの解説:
     * - これは「検索条件を丸ごとまとめて処理する総合メソッド」です。
     * - 条件があるたびに result を絞り込み、最後に最終的なリストを返します。
     * - 条件は順番に適用されるため、どの条件が先にかかっても最終結果は同じように見えます。
     *
     * @param tasks 元データ一覧
     * @param keyword タスク名キーワード
     * @param assignee 担当者
     * @param status 状態
     * @param startDate 開始日
     * @param endDate 終了日
     * @param projectId プロジェクトID
     * @return 条件に一致したタスク一覧
     */
    public static List<Task> searchCombined(List<Task> tasks, String keyword, String assignee,
                                           String status, LocalDate startDate, LocalDate endDate, Integer projectId) {
        List<Task> result = new ArrayList<>(tasks);

        if (projectId != null) {
            result = filterByProject(result, projectId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            result = searchByName(result, keyword);
        }

        if (assignee != null && !assignee.trim().isEmpty()) {
            result = searchByAssignee(result, assignee);
        }

        if (status != null && !status.trim().isEmpty() && !AppMessages.isFilterAll(status)) {
            result = filterByStatus(result, status);
        }

        if (startDate != null && endDate != null) {
            result = filterByDateRange(result, startDate, endDate);
        }

        return result;
    }

    /**
     * 【親子関係をたどって対象のプロジェクト配下か確認する】
     * タスクが指定した projectId を自分または祖先に持っているかを判定します。
     *
     * 初心者向けの解説:
     * - 親子関係は parentId をたどることで確認できます。
     * - ルートまで見て、途中に projectId があれば true を返します。
     *
     * @param task 判定対象のタスク
     * @param projectId ルートとして見たいプロジェクトID
     * @param taskMap ID をキーにしたタスク辞書
     * @return projectId の配下に属していれば true
     */
    private static boolean isDescendantOrSelf(Task task, int projectId, Map<Integer, Task> taskMap) {
        Task current = task;
        while (current != null) {
            if (current.getId() == projectId) {
                return true;
            }
            if (current.getParentId() == null) {
                break;
            }
            current = taskMap.get(current.getParentId());
        }
        return false;
    }
}
