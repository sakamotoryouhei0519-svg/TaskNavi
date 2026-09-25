# TaskNavi 初学者向け教科書

## 1. このプロジェクトで最初に押さえること
- 画面は Swing で作られている
- レイアウトは MigLayout が中心
- 画面ごとの役割がはっきり分かれている
- UI は `TaskService` 経由でデータを扱い、DB 直接操作は `Dao` に閉じる
- 画面間の同期は `TaskEventBus` で行う

## 2. 全体構成の考え方
- `Frame` 系は画面の入口（ログイン・メインウィンドウ）
- `Panel` 系はタブ内の業務画面（WBS / カンバン / ガント / カレンダー）
- `Dialog` 系は補助入力や確認用
- `TaskService` は CRUD とイベント発行のユースケース層
- `Dao` 系はデータ取得や保存を担当
- `Task` / `TaskStatus` / `Priority` / `EntryType` はドメインモデル

## 3. 絶対に覚える単語
- `JFrame` / `JDialog` / `JPanel`
- `MigLayout`
- `ActionListener`
- `LocalDate`
- `TaskService` / `TaskDao`
- `persistence`（`TaskRowMapper` / `TaskHierarchySupport` / `TaskSearchDao` / `TaskCrudDao` / `TaskImportDao`）
- `TaskEventBus`
- `AppTheme` / `AppMessages`
- `IconManager`
- `SearchablePanel`

## 4. MigLayout の見方
- `fill` は領域いっぱいに広げる
- `insets` は外側余白
- `gap` は部品間の間隔
- `wrap` は改行
- `grow` は余白に合わせて伸びる
- `align right/left/center` は寄せ方

## 5. 画面ごとの重要ポイント
### LoginFrame / RegisterFrame / ResetPasswordFrame
- 認証画面のため、入力欄とボタン配置が重要
- 認証ロジックは `AuthService`（試行制限付き）
- 見た目の統一は `AppTheme` と `ui.auth.AuthFormWidgets` に寄せる
- 登録入力ポリシーは `ui.auth.AuthPasswordPolicy`

### MainFrame
- アプリ全体の土台
- 上部ヘッダー（ユーザー・インポート/エクスポート・テーマ・ログアウト）とツールバー（検索・フィルタ）
- メイン領域はタブ: WBS / カンバン / ガント / カレンダー
- グローバル検索は各タブの `SearchablePanel` に同期する
- ヘッダー／検索バー／テーマ適用／ツールバーボタン／追加フローは `ui.MainHeaderBar`・`MainGlobalSearchBar`・`MainThemeSupport`・`MainToolbarButtons`・`MainTaskAddHelper` に分割
- タブ装飾は `ui.MainTabChrome`

### WbsPanel / KanbanPanel / GanttPanel / CalendarPanel
- 主要業務画面（いずれも `TaskService` を受け取る）
- 本体はそれぞれ `org.example.ui.wbs` / `ui.kanban` / `ui.gantt` / `ui.calendar` に配置
- タスク一覧、状態、操作ボタンのつながりを見る
- 表示更新は `refresh` 系と `TaskEventListener` に注目する
- WBS のツリー構築・フィルタ・DnD・詳細フォーム／登録は同パッケージ内に分割
- カンバンのフィルタ・カード／列振り分けは同パッケージ内に分割
- カレンダーのナビ／月・週ビュー／日別サイド／カードは同パッケージ内に分割
- ガントの描画・ヒット・ドラッグ・サイド列は同パッケージ内に分割
- ステータス前後移動は `util.TaskStatusCycle`

### TaskDialog
- タスク作成・編集の中心（`org.example.ui.taskdialog.TaskDialog`）
- 種別は `EntryType`（PROJECT / PHASE / TASK）
- 日付入力は `DatePickerDialog` と連動する
- 新規作成の組み立ては `TaskEntryFactory` も参照
- 親選択肢／フォーム／保存検証は同パッケージ内のヘルパーに分割

### DatePickerDialog
- 日付選択の再利用部品
- `LocalDate` の加減算と日付ボタン生成が重要

### TaskSearchDialog
- 検索条件の入力と確定を担当
- 確定後は `SearchablePanel.applySearchFilter` を呼ぶ

## 6. よく見るロジック
- `selectedItem` でコンボボックスの値を取る
- `setText` / `getText` で入力欄を扱う
- `addActionListener` でクリック処理を書く
- `setVisible(true)` でダイアログ表示
- `dispose()` で閉じる
- `LocalDate.parse()` で日付検証
- ステータス等の表示文言は `AppMessages`（永続化キーは英語コード）

## 7. 事故りやすい点
- `null` チェックを省くと落ちやすい
- `JComboBox` の選択値（コード）と表示文字列がズレると不具合になる
- レイアウトの `wrap` を忘れると部品が横に並び続ける
- ダークモード対応では背景色と文字色の両方を見る
- Panel から `TaskDao` を直接呼ばず、必ず `TaskService` 経由にする

## 8. 読む順番
1. `MainFrame`
2. `TaskService` / `event/TaskEventBus`
3. `ui.wbs.WbsPanel`
4. `ui.taskdialog.TaskDialog` / `TaskEntryFactory`
5. `DatePickerDialog`
6. `TaskSearchDialog`
7. `ui.kanban.KanbanPanel` / `ui.gantt.GanttPanel` / `ui.calendar.CalendarPanel`
8. `TaskDao`（検索・階層は `persistence` へ委譲）/ `Database`（Flyway）

## 9. まず覚えるべき設計感覚
- 画面は「表示するだけ」に寄せる
- ユースケースは `TaskService`、永続化は `Dao` に寄せる
- 共通処理は使い回す（`TaskBusinessRules` / `SearchFilterUtil` / `TaskViewFilter` など）
- UI は `AppTheme`、文言は `AppMessages` に統一する
- 複雑な画面は小さな部品に分ける（`ui/` / `ui.gantt/` / `persistence/` / `util/`）

## 10. 新しく入った人の最短ルート
- まず `MainFrame` でタブとヘッダーの流れをつかむ
- 次に `TaskService` と `TaskEventBus` でデータ更新の経路を見る
- そのあと `TaskDialog` と `WbsPanel` を読む
- 最後にカンバン・ガント・カレンダーを確認する
