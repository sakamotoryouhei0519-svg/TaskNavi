# TaskNavi

ローカルで動く **プロジェクト／タスク管理デスクトップアプリ**（Java 17 + Swing）。  
WBS・カンバン・ガント・カレンダーを一つのウィンドウで切り替え、SQLite に保存します。

## 解決したいこと

- 進捗を「一覧・板・日程」で切り替えて見たい
- 担当・期限・優先度を同じデータで共有したい
- ネット不要のローカル環境で、すぐに試せる形にしたい

## 主な機能

- 認証（登録／ログイン／パスワード再設定、試行制限付き）
- 「今日」タブ（期限切れ・今日まで・7日以内）と、Windows の通知での期限のお知らせ。自分だけのタスクと最近開いた共有プロジェクトをまとめて出す
- WBS ツリー、カンバン、ガント、カレンダー
- タスクごとのメモ（複数行）
- グローバル検索（タスク名・担当者・メモ）とステータス絞り込み
- 期限超過の強調表示、適用中フィルタの表示
- 削除の「元に戻す」（10秒間）、毎日の DB 自動バックアップ（7日分）
- CSV（Excel で編集して取り込み直せる）／JSON のエクスポート・インポート。取り込み前に件数とエラー行を確認でき、「追加」か「置き換え」を選べる
- ライト／ダークテーマ、最後に開いたタブの記憶
- キーボード操作（Ctrl+N 追加、Enter 編集、Delete 削除、Ctrl+F 検索）
- 共有プロジェクト（作成中）: 画面左上の「プロジェクト ▼」で、自分だけのタスクと、共有フォルダ（Google ドライブ）に置いたチームのプロジェクトを切り替える。選んだプロジェクトのタスクが全タブに出て、そこで追加・編集・削除・並べ替えができる。5秒ごとに他の人の変更を読み直す。ヘッダーにメンバー数・同期の様子・重なった変更の件数が出て、重なった変更は見比べて消えた方の値に戻せる。「変更履歴」で誰がいつ何を変えたかを見られる。担当者はメンバーから選べる。CSV／JSON から大量に取り込むときは、全部の行をチェックしてから1回で書く

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
- チーム共有（作成中・`team` パッケージ）: サーバーなしで共有フォルダを使う。各 PC は自分の変更記録ファイルに追記するだけで、全員の記録を決まった順に組み立てて一覧にする。互いを知らずに同じ項目を変えた場合は検出して残す
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

## 配布（Windows）

```bash
mvn -Pdist -DskipTests verify
```

- `target/jpackage/dist/TaskNavi/TaskNavi.exe` … ダブルクリックで起動。Java ごと入っているので、使うパソコンに Java は要りません
- `target/TaskNavi-1.0-SNAPSHOT-windows.zip` … 上のフォルダをまとめたもの。展開して `TaskNavi.exe` を起動します
- `TaskNavi.exe --team-profile=test2` … 共有プロジェクトを別の人として使う（1台で2人分の共有を試すとき。手順は教科書16章）
- データ・バックアップ・ログは `%APPDATA%\TaskNavi` に置きます（`tasknavi.db` / `backups` / `logs`）。アプリを入れ替えても消えません
- 署名していないため、初回は Windows が「発行元不明」の警告を出すことがあります（「詳細情報」→「実行」）

## デモ手順（約2分）

1. 起動 → ログイン（または skipLogin）
2. 「追加」でタスクを作成
3. カンバン／カレンダーで同じタスクが見えることを確認（EventBus）
4. 検索欄で名前・担当者・メモを絞る → フィルタ表示が変わる
5. CSV でエクスポート → Excel で編集・保存 → インポートで件数とエラー行を確認して取り込む

## このリポジトリで自分が説明できる変更例

- UI／persistence のパッケージ分割と CI 導入
- 担当者キーワード検索、タブ記憶、期限超過表示、絞り込みエクスポート
- 初学者向け handbook / study-checklist

AI エージェントを活用して構築・リファクタを加速しましたが、層分け・テスト・上記の変更内容は自分で追い説明できます。

## 開発メモ

- 調査メモ: `AI_HISTORY.md`
- パッケージ／Maven `groupId` はともに `org.tasknavi`

## ライセンス

個人ポートフォリオ用途。再利用時は作者に確認してください。
