package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * 【初回起動時のサンプルデータ投入クラス】
 * このアプリを初めて起動したとき、データベースが空なら見本のタスクを追加して
 * 画面上で何が表示されるかをすぐ確認できるようにします。
 *
 * ここでは「最初の画面を起動したあとに、最低限のタスクがある状態」を作る役割を担っています。
 */
public class SampleDataUtil {

    /**
     * 【データベースが空ならサンプルタスクを作成する】
     * タスクテーブルの件数を確認し、0件なら見本データを登録します。
     * 登録時は priority も明示して、優先度の色分けがすぐに見えるようにしています。
     */
    public static void insertSampleTasksIfEmpty() {
       String countSql = "SELECT COUNT(*) FROM tasks";

       try (Connection conn = Database.connect()) {
           conn.setAutoCommit(false);

           try (PreparedStatement countPstmt = conn.prepareStatement(countSql);
                ResultSet rs = countPstmt.executeQuery()) {
               if (rs.next() && rs.getInt(1) != 0) {
                   conn.commit();
                   return;
               }
           }

           try (PreparedStatement insertPstmt = conn.prepareStatement(
                   "INSERT INTO tasks (parent_id, level, name, assignee, start_date, end_date, status, progress, priority) "
                           + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                   Statement.RETURN_GENERATED_KEYS)) {

               Integer project1 = insertSampleTask(insertPstmt, null, 1, "福祉施設再整備計画", "田中次郎", "2026-04-01", "2026-12-20", "進行中", 62, "高");
               insertStageWithTasks(insertPstmt, project1, "現地調査・要件整理", new String[][] {
                       {"現場ヒアリング", "山田太郎", "2026-04-01", "2026-04-10", "完了", "100", "高"},
                       {"利用者アンケート分析", "鈴木美香", "2026-04-11", "2026-04-25", "完了", "100", "高"},
                       {"実施計画書作成", "田中次郎", "2026-04-26", "2026-05-05", "進行中", "80", "中"}
               });
               insertStageWithTasks(insertPstmt, project1, "設備更新設計", new String[][] {
                       {"設計案レビュー", "佐藤花子", "2026-05-06", "2026-05-20", "進行中", "75", "高"},
                       {"見積もり精査", "伊藤健", "2026-05-21", "2026-06-05", "進行中", "60", "中"},
                       {"施工条件整理", "中村優子", "2026-06-06", "2026-06-18", "未着手", "0", "中"}
               });
               insertStageWithTasks(insertPstmt, project1, "工事・導入支援", new String[][] {
                       {"施工スケジュール調整", "佐藤花子", "2026-06-19", "2026-07-02", "未着手", "0", "高"},
                       {"搬入計画確認", "山田太郎", "2026-07-03", "2026-07-16", "未着手", "0", "中"},
                       {"運用開始チェック", "田中次郎", "2026-07-17", "2026-07-30", "未着手", "0", "高"}
               });

               Integer project2 = insertSampleTask(insertPstmt, null, 1, "業務支援アプリ導入", "佐藤花子", "2026-03-10", "2026-11-30", "進行中", 58, "高");
               insertStageWithTasks(insertPstmt, project2, "基盤整備", new String[][] {
                       {"ユーザー権限設計", "佐藤花子", "2026-03-10", "2026-03-20", "完了", "100", "高"},
                       {"DB移行準備", "高橋翔", "2026-03-21", "2026-04-05", "完了", "100", "高"},
                       {"バックアップ体制検討", "伊藤健", "2026-04-06", "2026-04-15", "進行中", "70", "中"}
               });
               insertStageWithTasks(insertPstmt, project2, "機能開発", new String[][] {
                       {"利用者一覧画面", "山田太郎", "2026-04-16", "2026-05-10", "進行中", "75", "高"},
                       {"業務連携フォーム", "鈴木美香", "2026-05-11", "2026-06-15", "進行中", "50", "高"},
                       {"通知機能実装", "中村優子", "2026-06-16", "2026-07-05", "未着手", "0", "中"}
               });
               insertStageWithTasks(insertPstmt, project2, "運用テスト", new String[][] {
                       {"障害対策テスト", "高橋翔", "2026-07-06", "2026-08-10", "未着手", "0", "高"},
                       {"教育資料作成", "佐藤花子", "2026-08-11", "2026-08-25", "未着手", "0", "中"},
                       {"ユーザートレーニング", "田中次郎", "2026-09-01", "2026-09-15", "未着手", "0", "高"}
               });

               Integer project3 = insertSampleTask(insertPstmt, null, 1, "地域支援サービス改善", "鈴木美香", "2026-05-01", "2026-12-15", "進行中", 47, "中");
               insertStageWithTasks(insertPstmt, project3, "利用者ニーズ調査", new String[][] {
                       {"聞き取り実施", "鈴木美香", "2026-05-01", "2026-05-12", "完了", "100", "高"},
                       {"課題整理", "山田太郎", "2026-05-13", "2026-05-25", "完了", "100", "中"},
                       {"改善候補選定", "中村優子", "2026-05-26", "2026-06-05", "進行中", "65", "中"}
               });
               insertStageWithTasks(insertPstmt, project3, "サービス再設計", new String[][] {
                       {"担当体制見直し", "田中次郎", "2026-06-06", "2026-06-20", "進行中", "60", "高"},
                       {"定例運営ルール作成", "佐藤花子", "2026-06-21", "2026-07-05", "未着手", "0", "中"},
                       {"外部連携調整", "高橋翔", "2026-07-06", "2026-07-20", "未着手", "0", "中"}
               });
               insertStageWithTasks(insertPstmt, project3, "効果測定", new String[][] {
                       {"KPI設定", "伊藤健", "2026-07-21", "2026-08-02", "未着手", "0", "高"},
                       {"実施記録管理", "鈴木美香", "2026-08-03", "2026-08-25", "未着手", "0", "中"},
                       {"改善報告会準備", "山田太郎", "2026-09-01", "2026-09-10", "未着手", "0", "高"}
               });

               conn.commit();
               System.out.println("実務向けサンプルデータの投入が完了しました。");
           } catch (SQLException e) {
               conn.rollback();
               throw e;
           }
       } catch (SQLException e) {
           System.err.println("サンプルデータ投入エラー: " + e.getMessage());
       }
    }

    private static void insertStageWithTasks(PreparedStatement insertPstmt, Integer projectId,
                                           String stageName, String[][] tasks) throws SQLException {
       LocalDate stageStart = null;
       LocalDate stageEnd = null;

       for (String[] task : tasks) {
           LocalDate start = LocalDate.parse(task[2]);
           LocalDate end = LocalDate.parse(task[3]);
           if (stageStart == null || start.isBefore(stageStart)) {
               stageStart = start;
           }
           if (stageEnd == null || end.isAfter(stageEnd)) {
               stageEnd = end;
           }
       }

       Integer stageId = insertSampleTask(insertPstmt, projectId, 2, stageName, "担当者確認",
               stageStart != null ? stageStart.toString() : "2026-01-01",
               stageEnd != null ? stageEnd.toString() : "2026-01-31",
               "進行中", 50, "中");
       if (stageId == null) {
           return;
       }

       for (String[] task : tasks) {
           insertSampleTask(insertPstmt, stageId, 3, task[0], task[1], task[2], task[3], task[4], Integer.parseInt(task[5]), task[6]);
       }
    }

    private static Integer insertSampleTask(PreparedStatement insertPstmt, Integer parentId, int level,
                                           String name, String assignee, String startDate,
                                           String endDate, String status, int progress, String priority) throws SQLException {
       insertPstmt.setObject(1, parentId);
       insertPstmt.setInt(2, level);
       insertPstmt.setString(3, name);
       insertPstmt.setString(4, assignee);
       insertPstmt.setString(5, startDate);
       insertPstmt.setString(6, endDate);
       insertPstmt.setString(7, status);
       insertPstmt.setInt(8, progress);
       insertPstmt.setString(9, priority);
       insertPstmt.executeUpdate();

       try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
           if (generatedKeys.next()) {
               return generatedKeys.getInt(1);
           }
       }

       return null;
    }
}