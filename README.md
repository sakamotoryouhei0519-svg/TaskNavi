# TaskNavi

ローカルで動く **プロジェクト／タスク管理デスクトップアプリ**（Java 17 + Swing）。  
WBS・カンバン・ガント・カレンダーを一つのウィンドウで切り替え、SQLite に保存します。

## 解決したいこと

- 進捗を「一覧・板・日程」で切り替えて見たい
- 担当・期限・優先度を同じデータで共有したい
- ネット不要のローカル環境で、すぐに試せる形にしたい

## 主な機能

- 認証（登録／ログイン／パスワード再設定、試行制限付き）
- WBS ツリー、カンバン、ガント、カレンダー
- グローバル検索（タスク名 **または担当者**）とステータス絞り込み
- 期限超過の強調表示、適用中フィルタの表示
- 絞り込み結果または全件の JSON エクスポート／インポート
- ライト／ダークテーマ、最後に開いたタブの記憶

## 技術スタック

| 領域 | 技術 |
|------|------|
| 言語 | Java 17 |
| UI | Swing + MigLayout |
| DB | SQLite + Flyway |
| ビルド／テスト | Maven, JUnit 5 |
| CI | GitHub Actions（Xvfb 上で `mvn test`） |

## 設計の要点（面接用）

```
UI (Panel / Dialog)
  → TaskService（ユースケース + TaskEventBus で画面同期）
    → TaskDao（公開 API）
      → persistence.*（CRUD / 検索 / 階層）
```

- UI は `TaskDao` を直接 `new` しない（`TaskService.createDefault()`）
- 画面間の再描画は `TaskEventBus`
- 学習用の読み方: [`docs/beginner-handbook.md`](docs/beginner-handbook.md) / [`docs/study-checklist.md`](docs/study-checklist.md)

## 起動方法

```bash
mvn -B test
mvn -B -DskipTests package
java -jar target/TaskNavi-1.0-SNAPSHOT-jar-with-dependencies.jar
```

ログイン省略（デモ／スモーク）:

```bash
java -Dtasknavi.skipLogin=true -jar target/TaskNavi-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## デモ手順（約2分）

1. 起動 → ログイン（または skipLogin）
2. 「追加」でタスクを作成
3. カンバン／カレンダーで同じタスクが見えることを確認（EventBus）
4. 検索欄で名前または担当者を絞る → フィルタ表示が変わる
5. エクスポートで「絞り込み結果／全件」を選べることを確認

## このリポジトリで自分が説明できる変更例

- UI／persistence のパッケージ分割と CI 導入
- 担当者キーワード検索、タブ記憶、期限超過表示、絞り込みエクスポート
- 初学者向け handbook / study-checklist

AI エージェントを活用して構築・リファクタを加速しましたが、層分け・テスト・上記の変更内容は自分で追い説明できます。

## 開発メモ

- UI デバッグ: `-Dtasknavi.uiDebug=true`
- 調査メモ: `AI_HISTORY.md`
- パッケージ／Maven `groupId` はともに `org.tasknavi`

## ライセンス

個人ポートフォリオ用途。再利用時は作者に確認してください。
