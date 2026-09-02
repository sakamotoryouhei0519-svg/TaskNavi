package org.example;

// --- データベース接続および SQL 操作用ライブラリ ---
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// --- 日付処理・コレクション関連ライブラリ ---
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.example.util.DatabaseUtil; // DB接続用の共通クラス

/**
 * 【データアクセスオブジェクト (DAO) クラス】
 * SQLite データベースの `tasks` テーブルに対する CRUD（作成・取得・更新・削除）
 * および親タスクの進捗率自動再計算ロジックを担当します。
 */
public class TaskDao {

    /**
     * 【新規タスク追加】
     * 入力画面で作成した Task オブジェクトを SQLite の tasks テーブルへ保存します。
     *
     * 初心者向けの解説:
     * - 画面から送られてくるデータは Task オブジェクトとして扱われます。
     * - ここで DB に INSERT することで、次回アプリ起動時にもデータを保持できます。
     * - generatedId を返すのは、保存したレコードの自動採番 ID を呼び出し元に伝えるためです。
     *
     * @param task 登録したいタスク情報
     * @return 挿入されたレコードの ID。失敗時は -1
     */
    public static int addTask(Task task) {
       if (!isValidHierarchy(task)) {
           org.example.util.Logger.error("タスク追加エラー: 親子関係が不正です。 level=" + task.getLevel() + ", parentId=" + task.getParentId());
           return -1;
       }

       String sql = "INSERT INTO tasks (parent_id, level, name, assignee, start_date, end_date, status, progress, priority) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

       int generatedId = -1;

       try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

           if (task.getParentId() != null && task.getParentId() != 0) {
               pstmt.setInt(1, task.getParentId());
           } else {
               pstmt.setNull(1, java.sql.Types.INTEGER);
           }

           pstmt.setInt(2, task.getLevel());
           pstmt.setString(3, task.getName());
           pstmt.setString(4, task.getAssignee());

            pstmt.setString(5, task.getStartDate() != null ? task.getStartDate().toString() : null);
            pstmt.setString(6, task.getEndDate() != null ? task.getEndDate().toString() : null);
            pstmt.setString(7, task.getStatus() != null ? task.getStatus() : "未着手");
            pstmt.setInt(8, task.getProgress());
            pstmt.setString(9, task.getPriority() != null ? task.getPriority() : Task.DEFAULT_PRIORITY);
 
           int affected = pstmt.executeUpdate();
           if (affected > 0) {
               try (ResultSet keys = pstmt.getGeneratedKeys()) {
                   if (keys.next()) {
                       generatedId = keys.getInt(1);
                       task.setId(generatedId);
                   }
               }
           }
           org.example.util.Logger.info("タスク「" + task.getName() + "」を追加しました。");

       } catch (SQLException e) {
           org.example.util.Logger.error("タスク追加エラー", e);
       }
       return generatedId;
    }

    /**
     * 【全タスク取得】
     * tasks テーブルのすべてのレコードを読み取り、Task オブジェクトの一覧に変換して返します。
     *
     * 初心者向けの解説:
     * - DB の一行一行が Task として扱われます。
     * - ここで取得した一覧が WBS / カンバン / ガントチャート の表示に使われます。
     * - 1 行ずつループして各列を Task に詰め直しています。
     *
     * @return すべてのタスク情報を保持したリスト
     */
    public static List<Task> getAllTasks() {
       List<Task> taskList = new ArrayList<>();
       String sql = "SELECT * FROM tasks ORDER BY id ASC";

       try (Connection conn = DatabaseUtil.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

           while (rs.next()) {
               int taskId = getColumnInt(rs, "id", "task_id");
               Integer parentId = rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null;
               int level = getColumnIntOrDefault(rs, "level", 1);
               String name = getColumnString(rs, "name", "title");
               String assignee = rs.getString("assignee");

               LocalDate startDate = parseSafeDate(rs, "start_date");
               LocalDate endDate = parseSafeDate(rs, "end_date");

               String status = rs.getString("status");
               int progress = rs.getInt("progress");
               String priority = rs.getString("priority");

               Task task = new Task(taskId, name, parentId, level, progress, status, startDate, endDate);
               task.setAssignee(assignee);
               task.setPriority(priority);
               taskList.add(task);
           }
       } catch (SQLException e) {
           org.example.util.Logger.error("タスク取得エラー", e);
       }

       return taskList;
    }

    /**
     * 【単一タスク取得】
     * 指定した ID を持つ 1 件のタスクだけを取り出します。
     *
     * 初心者向けの解説:
     * - 画面でタスクを編集するとき、現在のデータを DB から拾うときに使います。
     * - すべてのレコードを読むので、見つかったものだけ返します。
     *
     * @param taskId 調べたいタスクの ID
     * @return 該当タスク。見つからなければ null
     */
    public static Task getTaskById(int taskId) {
       String sql = "SELECT * FROM tasks";

       try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {

           while (rs.next()) {
               int currentId = getColumnInt(rs, "id", "task_id");

               if (currentId == taskId) {
                   Integer parentId = rs.getObject("parent_id") != null ? rs.getInt("parent_id") : null;
                   int level = getColumnIntOrDefault(rs, "level", 1);
                   String name = getColumnString(rs, "name", "title");
                   String assignee = rs.getString("assignee");

                   LocalDate startDate = parseSafeDate(rs, "start_date");
                   LocalDate endDate = parseSafeDate(rs, "end_date");

                   String status = rs.getString("status");
                   int progress = rs.getInt("progress");

                   Task task = new Task(taskId, name, parentId, level, progress, status, startDate, endDate);
                   task.setAssignee(assignee);
                   try { task.setPriority(rs.getString("priority")); } catch (SQLException ignored) { }
                   return task;
               }
           }
       } catch (SQLException e) {
           org.example.util.Logger.error("単一タスク取得エラー", e);
       }
       return null;
    }

    /**
     * 【タスク情報更新】
     * 画面で編集した内容を DB に反映します。
     *
     * 初心者向けの解説:
     * - UPDATE 文を使って、既存レコードの値を差し替えます。
     * - 親タスクがある場合は、親の進捗率も自動的に再計算させます。
     *
     * @param task 更新後の Task オブジェクト
     */
    public static void updateTask(Task task) {
       if (!isValidHierarchy(task)) {
           org.example.util.Logger.error("タスク更新エラー: 親子関係が不正です。 level=" + task.getLevel() + ", parentId=" + task.getParentId());
           return;
       }

       String sql = "UPDATE tasks SET parent_id = ?, level = ?, name = ?, assignee = ?, "
               + "start_date = ?, end_date = ?, status = ?, progress = ?, priority = ? WHERE id = ?";

       try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

           if (task.getParentId() != null && task.getParentId() != 0) {
               pstmt.setInt(1, task.getParentId());
           } else {
               pstmt.setNull(1, java.sql.Types.INTEGER);
           }
           pstmt.setInt(2, task.getLevel());
           pstmt.setString(3, task.getName());
           pstmt.setString(4, task.getAssignee());

           pstmt.setString(5, task.getStartDate() != null ? task.getStartDate().toString() : null);
           pstmt.setString(6, task.getEndDate() != null ? task.getEndDate().toString() : null);
           pstmt.setString(7, task.getStatus());
           pstmt.setInt(8, task.getProgress());
           pstmt.setString(9, task.getPriority() != null ? task.getPriority() : Task.DEFAULT_PRIORITY);
           pstmt.setInt(10, task.getId());

           int updatedRows = pstmt.executeUpdate();

           if (updatedRows == 0) {
               String fallbackSql = "UPDATE tasks SET parent_id = ?, level = ?, name = ?, assignee = ?, "
                       + "start_date = ?, end_date = ?, status = ?, progress = ?, priority = ? WHERE task_id = ?";
               try (PreparedStatement fallbackPstmt = conn.prepareStatement(fallbackSql)) {
                   if (task.getParentId() != null && task.getParentId() != 0) {
                       fallbackPstmt.setInt(1, task.getParentId());
                   } else {
                       fallbackPstmt.setNull(1, java.sql.Types.INTEGER);
                   }
                   fallbackPstmt.setInt(2, task.getLevel());
                   fallbackPstmt.setString(3, task.getName());
                   fallbackPstmt.setString(4, task.getAssignee());
                   fallbackPstmt.setString(5, task.getStartDate() != null ? task.getStartDate().toString() : null);
                   fallbackPstmt.setString(6, task.getEndDate() != null ? task.getEndDate().toString() : null);
                   fallbackPstmt.setString(7, task.getStatus());
                   fallbackPstmt.setInt(8, task.getProgress());
                   fallbackPstmt.setString(9, task.getPriority() != null ? task.getPriority() : Task.DEFAULT_PRIORITY);
                   fallbackPstmt.setInt(10, task.getId());
                   fallbackPstmt.executeUpdate();
               }
           }

           org.example.util.Logger.info("タスクID: " + task.getId() + " を更新しました。");

           if (task.getParentId() != null && task.getParentId() != 0) {
               updateParentProgress(task.getParentId(), getAllTasks());
           }

       } catch (SQLException e) {
           org.example.util.Logger.error("タスク更新エラー", e);
       }
    }

    /**
     * 【状態だけを更新】
     * カンバンボードでドラッグ＆ドロップしたあと、状態の変更を反映するときに使います。
     *
     * @param taskId 対象のタスク ID
     * @param newStatus 新しい状態（例: 完了）
     * @return 更新成功なら true
     */
    public static boolean updateTaskStatus(int taskId, String newStatus) {
       String sql = "UPDATE tasks SET status = ? WHERE id = ?";

       try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

           pstmt.setString(1, newStatus);
           pstmt.setInt(2, taskId);

           int updated = pstmt.executeUpdate();

           if (updated == 0) {
               String fallbackSql = "UPDATE tasks SET status = ? WHERE task_id = ?";
               try (PreparedStatement fallbackPstmt = conn.prepareStatement(fallbackSql)) {
                   fallbackPstmt.setString(1, newStatus);
                   fallbackPstmt.setInt(2, taskId);
                   return fallbackPstmt.executeUpdate() > 0;
               }
           }
           return true;

       } catch (SQLException e) {
           org.example.util.Logger.error("状態更新エラー", e);
           return false;
       }
    }

    /**
     * 【タスク削除】
     * 指定した ID のタスクを DB から削除します。
     *
     * @param taskId 削除したいタスクの ID
     */
    public static void deleteTask(int taskId) {
       List<Task> allTasks = getAllTasks();
       java.util.Set<Integer> targetIds = new java.util.HashSet<>();
       collectDescendantIds(taskId, allTasks, targetIds);

       if (targetIds.isEmpty()) {
           targetIds.add(taskId);
       }

       try (Connection conn = DatabaseUtil.getConnection()) {
           for (Integer id : targetIds) {
               String sql = "DELETE FROM tasks WHERE id = ?";
               try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                   pstmt.setInt(1, id);
                   int deleted = pstmt.executeUpdate();
                   if (deleted == 0) {
                       String fallbackSql = "DELETE FROM tasks WHERE task_id = ?";
                       try (PreparedStatement fallbackPstmt = conn.prepareStatement(fallbackSql)) {
                           fallbackPstmt.setInt(1, id);
                           fallbackPstmt.executeUpdate();
                       }
                   }
               }
           }
           org.example.util.Logger.info("タスクID: " + taskId + " を削除しました。対象件数=" + targetIds.size());
       } catch (SQLException e) {
           org.example.util.Logger.error("タスク削除エラー", e);
       }
    }

    private static void collectDescendantIds(int rootTaskId, List<Task> allTasks, java.util.Set<Integer> targetIds) {
       if (allTasks == null || allTasks.isEmpty()) {
           return;
       }

       targetIds.add(rootTaskId);
       for (Task task : allTasks) {
           if (task.getParentId() != null && task.getParentId() == rootTaskId) {
               collectDescendantIds(task.getId(), allTasks, targetIds);
           }
       }
    }

    /**
     * 【インポート用トランザクション】
     * 既存データを保持する／置換する方法に応じて、1回のトランザクション内で
     * 追加または置換を行います。途中で失敗した場合はロールバックして中途半端な状態を残しません。
     */
    public static void importTasks(List<Task> tasks, boolean replaceExisting) {
       if (tasks == null) {
           return;
       }

       try (Connection conn = DatabaseUtil.getConnection()) {
           boolean previousAutoCommit = conn.getAutoCommit();
           conn.setAutoCommit(false);
           try {
               if (replaceExisting) {
                   try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM tasks")) {
                       deleteStmt.executeUpdate();
                   }
               }

               String sql = "INSERT INTO tasks (parent_id, level, name, assignee, start_date, end_date, status, progress, priority) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
               try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                   for (Task task : tasks) {
                       if (task == null) {
                           continue;
                       }
                       if (!isValidHierarchy(task)) {
                           throw new SQLException("不正な親子関係です: " + task.getName() + " (level=" + task.getLevel() + ", parentId=" + task.getParentId() + ")");
                       }
                       task.setId(0);
                       if (task.getParentId() != null && task.getParentId() != 0) {
                           insertStmt.setInt(1, task.getParentId());
                       } else {
                           insertStmt.setNull(1, java.sql.Types.INTEGER);
                       }
                       insertStmt.setInt(2, task.getLevel() > 0 ? task.getLevel() : 1);
                       insertStmt.setString(3, task.getName() != null ? task.getName() : "");
                       insertStmt.setString(4, task.getAssignee());
                       insertStmt.setString(5, task.getStartDate() != null ? task.getStartDate().toString() : null);
                       insertStmt.setString(6, task.getEndDate() != null ? task.getEndDate().toString() : null);
                       insertStmt.setString(7, task.getStatus() != null ? task.getStatus() : "未着手");
                       insertStmt.setInt(8, task.getProgress());
                       insertStmt.setString(9, task.getPriority() != null ? task.getPriority() : Task.DEFAULT_PRIORITY);
                       insertStmt.addBatch();
                   }
                   insertStmt.executeBatch();
               }
               conn.commit();
           } catch (SQLException e) {
               conn.rollback();
               throw e;
           } finally {
               conn.setAutoCommit(previousAutoCommit);
           }
       } catch (SQLException e) {
           org.example.util.Logger.error("インポート処理エラー", e);
           throw new RuntimeException("タスクのインポートに失敗しました。", e);
       }
    }

    /**
     * 【親タスクの進捗率を自動更新】
     * 子タスクの進捗率の平均を計算し、親タスクの進捗率と状態を自動的に整えます。
     *
     * 初心者向けの解説:
     * - WBS では親タスクが「子タスクの進捗の平均」で動きます。
     * - これがあると、子供の進捗を変えるだけで親も自動更新されます。
     *
     * @param parentId 親タスクの ID
     */
    public static void updateParentProgress(int parentId) {
       updateParentProgress(parentId, getAllTasks());
    }

    private static void updateParentProgress(int parentId, List<Task> allTasks) {
       if (allTasks == null || allTasks.isEmpty()) {
           return;
       }

       Task parent = null;
       int totalProgress = 0;
       int childCount = 0;

       for (Task task : allTasks) {
           if (task.getId() == parentId) {
               parent = task;
           }
           if (task.getParentId() != null && task.getParentId() == parentId) {
               totalProgress += task.getProgress();
               childCount++;
           }
       }

       if (parent == null || childCount == 0) {
           return;
       }

       int avgProgress = (int) Math.round((double) totalProgress / childCount);
       parent.setProgress(avgProgress);

       if (avgProgress == 100) {
           parent.setStatus("完了");
       } else if (avgProgress > 0) {
           parent.setStatus("進行中");
       } else {
           parent.setStatus("未着手");
       }

       directUpdateTask(parent);

       if (parent.getParentId() != null && parent.getParentId() != 0) {
           updateParentProgress(parent.getParentId(), allTasks);
       }
    }

    /**
     * 【親タスクの進捗・状態を即座に保存する内部メソッド】
     * updateParentProgress() が使う補助メソッドです。
     *
     * 初心者向けの解説:
     * - このメソッドは「実運用に必要な最小の更新処理」を持つメソッドです。
     * - updateParentProgress の中で、親を DB に反映する部分だけを切り出しています。
     *
     * @param task 更新したい親タスク
     */
    private static void directUpdateTask(Task task) {
       String sql = "UPDATE tasks SET progress = ?, status = ? WHERE id = ?";
       try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
           pstmt.setInt(1, task.getProgress());
           pstmt.setString(2, task.getStatus());
           pstmt.setInt(3, task.getId());

           if (pstmt.executeUpdate() == 0) {
               String fallbackSql = "UPDATE tasks SET progress = ?, status = ? WHERE task_id = ?";
               try (PreparedStatement fallbackPstmt = conn.prepareStatement(fallbackSql)) {
                   fallbackPstmt.setInt(1, task.getProgress());
                   fallbackPstmt.setString(2, task.getStatus());
                   fallbackPstmt.setInt(3, task.getId());
                   fallbackPstmt.executeUpdate();
               }
           }
       } catch (SQLException e) {
           org.example.util.Logger.error("親タスク自動更新エラー", e);
       }
    }

    public static boolean isValidHierarchy(Task task) {
       if (task == null) {
           return false;
       }

       if (task.getLevel() < 1 || task.getLevel() > 3) {
           return false;
       }

       if (task.getParentId() == null || task.getParentId() == 0) {
           return task.getLevel() == 1;
       }

       Task parent = getTaskById(task.getParentId());
       if (parent == null) {
           return false;
       }

       if (task.getLevel() == 2) {
           return parent.getLevel() == 1;
       }
       if (task.getLevel() == 3) {
           return parent.getLevel() == 2;
       }
       return false;
    }

    // --- 日付・カラム解析用プライベートヘルパーメソッド群 ---

    /**
     * 【DBの日付文字列を LocalDate に変換する】
     * SQLite から読む日付は文字列として保存されることが多いため、Java の LocalDate に変換します。
     *
     * 初心者向けの解説:
     * - 文字列をそのまま使うと日付比較がしづらいので、LocalDate に変換しています。
     * - 2026-01-05T09:00:00 のような形式も、T より前の部分だけ取り出して対応します。
     *
     * @param rs 結果セット
     * @param columnName 取得したい日付カラム名
     * @return 変換後の LocalDate。失敗時は null
     */
    private static LocalDate parseSafeDate(ResultSet rs, String columnName) {
        try {
            String dateStr = rs.getString(columnName);
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }
            if (dateStr.contains("T")) {
                dateStr = dateStr.split("T")[0];
            }
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 【安全に int を取得する】
     * 既存 DB では id と task_id のどちらが使われているかが違う場合があるため、順番に試します。
     *
     * @param rs 結果セット
     * @param col1 先に試すカラム名
     * @param col2 失敗時に試す代替カラム名
     * @return 取得した int 値
     */
    private static int getColumnInt(ResultSet rs, String col1, String col2) throws SQLException {
        try {
            return rs.getInt(col1);
        } catch (SQLException e) {
            return rs.getInt(col2);
        }
    }

    /**
     * 【存在しないカラムには既定値を返す】
     * 互換性の高いコードにするため、列がなかったときの安全策です。
     *
     * @param rs 結果セット
     * @param col カラム名
     * @param defaultValue 取得できなかったときの既定値
     * @return 整数値
     */
    private static int getColumnIntOrDefault(ResultSet rs, String col, int defaultValue) {
        try {
            return rs.getInt(col);
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    /**
     * 【文字列カラムも安全に取得する】
     * name か title のどちらが存在するか分からないときに使います。
     *
     * @param rs 結果セット
     * @param col1 先に試すカラム名
     * @param col2 代替カラム名
     * @return 文字列値
     */
    private static String getColumnString(ResultSet rs, String col1, String col2) throws SQLException {
        try {
            String val = rs.getString(col1);
            if (val != null) return val;
        } catch (SQLException ignored) {}
        return rs.getString(col2);
    }

    // ==========================================
    //  【検索・フィルタ機能】
    // ==========================================

    /**
     * 【名前検索（DB 経由）】
     * DB 全体を取得してから、TaskSearchService でキーワード検索を実行します。
     *
     * @param keyword 検索キーワード
     * @return 条件に合うタスク一覧
     */
    public static List<Task> searchByName(String keyword) {
        List<Task> allTasks = getAllTasks();
        return TaskSearchService.searchByName(allTasks, keyword);
    }

    /**
     * 【担当者検索（DB 経由）】
     * 指定した担当者のタスクだけを抽出します。
     *
     * @param assignee 担当者名
     * @return 該当担当者のタスク一覧
     */
    public static List<Task> searchByAssignee(String assignee) {
        List<Task> allTasks = getAllTasks();
        return TaskSearchService.searchByAssignee(allTasks, assignee);
    }

    /**
     * 【状態フィルタ（DB 経由）】
     * 例: 未着手 / 進行中 / 完了 を絞り込みます。
     *
     * @param status 目的の状態
     * @return 条件に合うタスク一覧
     */
    public static List<Task> filterByStatus(String status) {
        List<Task> allTasks = getAllTasks();
        return TaskSearchService.filterByStatus(allTasks, status);
    }

    /**
     * 【期間フィルタ（DB 経由）】
     * 指定した期間に入るタスクを抽出します。
     *
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 期間内のタスク一覧
     */
    public static List<Task> filterByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Task> allTasks = getAllTasks();
        return TaskSearchService.filterByDateRange(allTasks, startDate, endDate);
    }

    /**
     * 【複合検索（DB 経由）】
     * 複数条件をまとめて一度に処理するためのエントリーポイントです。
     *
     * @param keyword タスク名キーワード
     * @param assignee 担当者
     * @param status 状態
     * @param startDate 開始日
     * @param endDate 終了日
     * @param projectId プロジェクト ID
     * @return 条件に合うタスク一覧
     */
    public static List<Task> searchCombined(String keyword, String assignee, String status,
                                           LocalDate startDate, LocalDate endDate, Integer projectId) {
        List<Task> allTasks = getAllTasks();
        return TaskSearchService.searchCombined(allTasks, keyword, assignee, status, startDate, endDate, projectId);
    }
}