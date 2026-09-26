package org.tasknavi;

/**
 * 検索可能なパネルが実装すべきインターフェース。
 * WBS、カンバン、ガントチャートの各パネルで共通の検索インターフェースを提供します。
 */
public interface SearchablePanel {
    /**
     * 検索フィルタを適用します。
     *
     * @param keyword 検索キーワード（nullまたは空文字で検索なし）
     * @param status  ステータスフィルタ（nullまたは FILTER_ALL / ALL でフィルタなし）
     */
    void applySearchFilter(String keyword, String status);

    /**
     * フィルタをクリアします。
     */
    void handleClearFilter();

    /**
     * 現在の検索キーワードを取得します。
     *
     * @return 現在の検索キーワード（検索なしの場合はnull）
     */
    String getCurrentSearchKeyword();

    /**
     * 現在のステータスフィルタを取得します。
     *
     * @return 現在のステータスフィルタ（フィルタなしの場合はnull）
     */
    String getCurrentStatusFilter();

    /**
     * 既存プロジェクトでのフィルタダイアログを表示します。
     */
    void handleExistingProject();

    /**
     * テーマ（ダークモード・ライトモード）の更新を行います。
     */
    default void updateTheme() {
        // デフォルト実装（必要に応じて各パネルクラスでオーバーライド）
    }

    /**
     * 現在パネル上で選択されているタスクを取得します。
     *
     * @return 選択中のタスク（選択されていない場合はnull）
     */
    default Task getSelectedTask() {
        return null;
    }
}