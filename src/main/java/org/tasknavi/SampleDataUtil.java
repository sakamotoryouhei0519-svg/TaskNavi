package org.tasknavi;

import java.sql.*;
import java.time.LocalDate;

/**
 * 【初回起動時のサンプルデータ投入クラス】
 * 実務レベルのボリュームと具体性を持ったプロジェクトデータを自動投入します。
 */
public class SampleDataUtil {

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

                // ==========================================
                // プロジェクト 1: 次世代ECサイト構築プロジェクト（大規模）
                // ==========================================
                Integer proj1 = insertSampleTask(insertPstmt, null, 1,
                        "次世代ECサイトリニューアル", "佐藤花子", "2026-04-01", "2026-12-25",
                        Task.STATUS_IN_PROGRESS, 45, Priority.HIGH.name());

                // 階層2: 企画・要件定義
                insertStageWithTasks(insertPstmt, proj1, "1. 企画・要件定義フェーズ", new String[][] {
                        {"現状分析・競合調査", "山田太郎", "2026-04-01", "2026-04-20", Task.STATUS_COMPLETED, "100", Priority.HIGH.name()},
                        {"ビジネス要件定義", "佐藤花子", "2026-04-15", "2026-05-10", Task.STATUS_COMPLETED, "100", Priority.HIGH.name()},
                        {"システム基本構想策定", "鈴木一郎", "2026-05-01", "2026-05-31", Task.STATUS_COMPLETED, "100", Priority.MEDIUM.name()}
                });

                // 階層2: システム設計
                insertStageWithTasks(insertPstmt, proj1, "2. 設計フェーズ", new String[][] {
                        {"画面ワイヤーフレーム作成", "高橋恵子", "2026-06-01", "2026-06-25", Task.STATUS_COMPLETED, "100", Priority.HIGH.name()},
                        {"DB・データモデル設計", "鈴木一郎", "2026-06-15", "2026-07-10", Task.STATUS_COMPLETED, "100", Priority.HIGH.name()},
                        {"API・外部連携設計", "田中次郎", "2026-06-20", "2026-07-20", Task.STATUS_IN_PROGRESS, "80", Priority.MEDIUM.name()}
                });

                // 階層2: 開発・実装
                insertStageWithTasks(insertPstmt, proj1, "3. 開発・実装フェーズ", new String[][] {
                        {"フロントエンド開発", "高橋恵子", "2026-07-15", "2026-09-30", Task.STATUS_IN_PROGRESS, "50", Priority.HIGH.name()},
                        {"バックエンド API 開発", "田中次郎", "2026-07-15", "2026-10-15", Task.STATUS_IN_PROGRESS, "40", Priority.HIGH.name()},
                        {"決済ゲートウェイ連携", "鈴木一郎", "2026-09-01", "2026-10-31", Task.STATUS_NOT_STARTED, "0", Priority.HIGH.name()}
                });

                // 階層2: テスト・移行
                insertStageWithTasks(insertPstmt, proj1, "4. テスト・リリース準備", new String[][] {
                        {"結合テストシナリオ作成", "伊藤誠", "2026-10-01", "2026-10-31", Task.STATUS_NOT_STARTED, "0", Priority.MEDIUM.name()},
                        {"総合・負荷テスト実行", "伊藤誠", "2026-11-01", "2026-11-30", Task.STATUS_NOT_STARTED, "0", Priority.HIGH.name()},
                        {"本番移行・リリース作業", "佐藤花子", "2026-12-01", "2026-12-25", Task.STATUS_NOT_STARTED, "0", Priority.HIGH.name()}
                });


                // ==========================================
                // プロジェクト 2: 社内DX推進・クラウド移行（中規模）
                // ==========================================
                Integer proj2 = insertSampleTask(insertPstmt, null, 1,
                        "社内基幹システム クラウド移行", "田中次郎", "2026-05-15", "2026-11-15",
                        Task.STATUS_IN_PROGRESS, 30, Priority.MEDIUM.name());

                // 階層2: 調査・選定
                insertStageWithTasks(insertPstmt, proj2, "クラウド基盤選定", new String[][] {
                        {"AWS / Azure 費用比較見積", "田中次郎", "2026-05-15", "2026-06-10", Task.STATUS_COMPLETED, "100", Priority.HIGH.name()},
                        {"セキュリティ審査・コンプライアンス確認", "中村健", "2026-06-01", "2026-06-30", Task.STATUS_COMPLETED, "100", Priority.HIGH.name()}
                });

                // 階層2: 移行準備
                insertStageWithTasks(insertPstmt, proj2, "データ移行プロトタイプ", new String[][] {
                        {"DBスキーマ変換スクリプト作成", "田中次郎", "2026-07-01", "2026-08-15", Task.STATUS_IN_PROGRESS, "60", Priority.MEDIUM.name()},
                        {"旧データクレンジング作業", "小林由美", "2026-07-15", "2026-09-10", Task.STATUS_IN_PROGRESS, "20", Priority.LOW.name()}
                });


                // ==========================================
                // プロジェクト 3: 全社セキュリティ監査対応（短期・緊急）
                // ==========================================
                Integer proj3 = insertSampleTask(insertPstmt, null, 1,
                        "2026年度 下期セキュリティ監査", "中村健", "2026-09-01", "2026-10-15",
                        Task.STATUS_NOT_STARTED, 0, Priority.HIGH.name());

                insertStageWithTasks(insertPstmt, proj3, "監査実施", new String[][] {
                        {"アクセス権限ログの抽出・点検", "中村健", "2026-09-01", "2026-09-15", Task.STATUS_NOT_STARTED, "0", Priority.HIGH.name()},
                        {"脆弱性診断レポートの作成", "中村健", "2026-09-16", "2026-09-30", Task.STATUS_NOT_STARTED, "0", Priority.HIGH.name()},
                        {"是正勧告への対策立案", "佐藤花子", "2026-10-01", "2026-10-15", Task.STATUS_NOT_STARTED, "0", Priority.MEDIUM.name()}
                });

                conn.commit();
                System.out.println("実務用サンプルデータの投入が完了しました。");
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
            if (stageStart == null || start.isBefore(stageStart)) stageStart = start;
            if (stageEnd == null || end.isAfter(stageEnd)) stageEnd = end;
        }

        // ステージ（親）のステータス計算（簡易ロジック）
        String stageStatus = Task.STATUS_NOT_STARTED;
        int totalProgress = 0;
        for (String[] task : tasks) {
            totalProgress += Integer.parseInt(task[5]);
        }
        int avgProgress = tasks.length > 0 ? totalProgress / tasks.length : 0;
        if (avgProgress == 100) stageStatus = Task.STATUS_COMPLETED;
        else if (avgProgress > 0) stageStatus = Task.STATUS_IN_PROGRESS;

        Integer stageId = insertSampleTask(insertPstmt, projectId, 2, stageName, "チーム一律",
                stageStart != null ? stageStart.toString() : "2026-04-01",
                stageEnd != null ? stageEnd.toString() : "2026-12-31",
                stageStatus, avgProgress, Priority.MEDIUM.name());

        if (stageId != null) {
            for (String[] task : tasks) {
                insertSampleTask(insertPstmt, stageId, 3, task[0], task[1], task[2], task[3], task[4], Integer.parseInt(task[5]), task[6]);
            }
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
            if (generatedKeys.next()) return generatedKeys.getInt(1);
        }
        return null;
    }
}
