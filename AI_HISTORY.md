# AI History

## 2026-09-25

### TaskDao 追加整理

- `persistence` に追加
  - `TaskCrudDao` … INSERT/SELECT/UPDATE/DELETE／進捗・順序更新
  - `TaskStatementBinder` … INSERT/UPDATE 共通バインド
  - `TaskImportDao` … インポート用トランザクション
  - `TaskWriteValidator` … 保存前検証
  - `TaskReorderSupport` … 兄弟 order_index 再割当て
- `TaskDao` は公開 API を維持し委譲（約 480 行 → 約 120 行）。`TaskWriteValidatorTest` を追加

### パッケージ最終整理

- ビュー本体を `ui.*` へ移動
  - `ui.wbs.WbsPanel` / `ui.kanban.KanbanPanel` / `ui.gantt.GanttPanel` / `ui.calendar.CalendarPanel`
  - `ui.taskdialog.TaskDialog`
- 未使用の互換ファサード `ui.calendar.CalendarStatusCycle` を削除（実装は `util.TaskStatusCycle`）
- `MainFrame` / `TaskEntryFactory` / 各 Editor・テストの import を更新

### 認証 UI 整理

- `ui.auth` に追加
  - `AuthFormWidgets` … タイトル／ラベル／リンク／入力欄／パスワードトグル／主ボタン／エラー枠
  - `AuthPasswordPolicy` … メール形式・パスワード強度・登録検証
  - `AuthNavigation` … ログイン画面への遷移
- `LoginFrame` / `RegisterFrame` / `ResetPasswordFrame` を共通部品へ差し替え
- `AuthPasswordPolicyTest` を追加

### TaskDialog 分割

- `ui.taskdialog` に追加
  - `TaskDialogTitles` … 新規／編集タイトル判定
  - `TaskDialogParentSupport` … 親選択肢の構築・既定選択・表示切替
  - `TaskDialogFormFactory` … フォーム／ボタンパネル組み立て
  - `TaskDialogSaveValidator` … 保存前入力検証
  - `TaskDialogUiStyle` … コンボ見た目・種別レンダラ
- `TaskDialog` 約 450 行 → 約 200 行。`TaskDialogSupportTest` を追加

### GanttPanel 追加分割

- `ui.gantt` に追加
  - `GanttMetrics` … 行高・日幅・ズーム範囲などの定数
  - `GanttSideViews` … タスク名列／日付ヘッダー
  - `GanttDragSession` … ドラッグ／リサイズ状態
  - `GanttTooltips` / `GanttTaskEditor` / `GanttViewportSupport`
  - `GanttPainter.drawBar` … バー描画をパネルから移動
- `GanttPanel` 約 640 行 → 約 440 行。`GanttMetricsTest` を追加

### CalendarPanel 追加分割

- `ui.calendar` に追加
  - `CalendarNavToolbar` … 前／今日／次・期間タイトル・月／週切替
  - `CalendarMonthViewBuilder` … 月グリッド（日曜始まり計算含む）
  - `CalendarWeekViewBuilder` … 週7カラム
  - `CalendarDailySideSupport` … 右側ヘッダーと日別一覧
  - `CalendarTaskEditor` … 追加／編集／ステータス更新
- `CalendarPanel` 約 660 行 → 約 340 行。`CalendarViewBuildersTest` を追加

### MainFrame 分割

- `ui` に追加
  - `MainHeaderBar` … ロゴ・ユーザー・I/O・テーマ・ログアウト
  - `MainGlobalSearchBar` … グローバル検索欄とステータスフィルタ
  - `MainThemeSupport` … テーマ再帰適用／ビュー更新
  - `MainToolbarButtons` … ツールバーボタン生成
  - `MainTaskAddHelper` … タスク追加ダイアログ＋登録
- `AppTheme.RoundedFillButton` を public 化（`ui` パッケージから利用）
- `MainFrame` 約 710 行 → 約 260 行（タブ配線・ログアウト／I/O に寄せた）

### WbsPanel 追加分割

- `ui.wbs` に追加
  - `WbsDetailForm` … 右側詳細フォーム（日付は共通 `DatePickerDialog`）
  - `WbsFormValidator` … 入力検証・日付パース
  - `WbsEntryCreator` … TaskDialog 経由の新規登録
- インライン日付カレンダー／未使用の作成・複製 private メソッドを除去
- `WbsPanel` 約 780 行 → 約 370 行。`WbsFormValidatorTest` を追加

### 横断フィルタ共通化 ＋ searchCombined 整理

- `util.TaskViewFilter` を追加（キーワードは大小文字無視、ステータス／プロジェクト配下を共通適用）
- `KanbanTaskFilter` / `CalendarTaskFilter.applyFilters` / `GanttTaskFilter` は委譲に縮小（ガントのみ階層ソート後に適用）
- `TaskDao.searchCombined` の projectId を修正: DB 結果とプロジェクト配下 ID を交差（従来はフィルタ結果を捨てていた）
- `TaskViewFilterTest` と `TaskDaoIntegrationTest.shouldSearchCombinedRespectProjectHierarchy` を追加

## 2026-09-24

### Phase B（続き）: パッケージ整理

- `util.TaskStatusCycle` を追加（カンバン／カレンダーで共有）。`CalendarStatusCycle` は互換ファサードに縮小
- 未使用の `ui.CalendarPeriodLabels` ファサードを削除
- `ui.MainTabChrome` を追加し、`MainFrame` のタブアイコン／タブ装飾を抽出（約 820 行 → 約 710 行）
- `org.example.gantt` を `org.example.ui.gantt` に移し、他ビュー抽出物と配置を揃えた
- `IconManager.paintCalendarOutline` を public 化（`MainTabChrome` から利用）

### Phase B（続き）: KanbanPanel 分割

- `org.example.ui.kanban` を追加
  - `KanbanTaskFilter` … キーワード／ステータス／プロジェクト絞り込み
  - `KanbanColumnSupport` … 列見出し文言とカラム振り分け
  - `KanbanCardFactory` … カード UI・進捗バー・前後ステータスボタン（`CalendarStatusCycle` 再利用）
- `KanbanTaskFilterTest` を追加
- `KanbanPanel` 約 650 行 → 約 460 行（配線・DnD・削除／編集に寄せた）

### Phase B（続き）: TaskDao 分割

- `org.example.persistence` を追加し、巨大だった `TaskDao` から以下を抽出
  - `TaskRowMapper` … ResultSet → Task（列名互換・日付パース）
  - `TaskHierarchySupport` … 子孫収集・親進捗ロールアップ・階層検証
  - `TaskSearchDao` … 名前／担当／ステータス／日付／複合検索（DB 側）
- `TaskDao` は CRUD・トランザクション・公開 API を維持し、上記へ委譲（約 900 行 → 約 530 行）
- `TaskHierarchySupportTest` を追加。ビルド・統合テスト（階層・日付フィルタ）通過

### Phase B（一部）: WbsPanel 分割

- `org.example.ui.wbs` パッケージを追加し、巨大だった `WbsPanel` から以下を抽出
  - `WbsTreeFilter` … プロジェクト／キーワード／ステータス絞り込み（祖先保持）
  - `WbsTreeModelBuilder` … ツリーノード組み立て
  - `WbsTreeCellRenderer` … セル描画・ドロップハイライト
  - `WbsTreeTransferHandler` / `WbsHierarchyOps` … DnD と階層レベル再計算
  - `WbsTaskDuplicator` … 再帰複製
- `WbsTreeFilterTest` を追加
- `WbsPanel` は画面配線・フォーム・作成ダイアログに責務を寄せた（約 1320 行 → 約 920 行）

### Phase B（続き）: CalendarPanel 分割

- `org.example.ui.calendar` を追加
  - `CalendarTaskFilter` … キーワード／ステータス／プロジェクト絞り込みと日付マッチ
  - `CalendarStatusCycle` … ステータス前後移動・進捗からの推定
  - `CalendarTaskCardFactory` … 日別／週表示カード
  - `CalendarMonthCellFactory` … 月表示セルとバッジ
  - `CalendarPeriodLabels` … 期間タイトル（旧 `ui.CalendarPeriodLabels` は互換ファサード）
- `CalendarTaskFilterTest` を追加
- `CalendarPanel` 約 1150 行 → 約 790 行

### Phase B（続き）: GanttPanel 分割

- `org.example.gantt` に追加
  - `GanttTaskFilter` … 表示対象タスクの絞り込み
  - `GanttBarHit` … マウスヒット判定
  - `GanttBarDragMath` … 移動／リサイズ時の日付計算（親期間クランプ）
- `GanttBarDragMathTest` / `GanttTaskFilterTest` を追加
- `GanttPanel` 約 880 行 → 約 740 行（ヒット・ドラッグ・フィルタを委譲）

### Phase A クリーンアップ（評価フォローアップ）

- `docs/beginner-handbook.md` を現状に合わせて更新（タブ構成、`TaskService` / `TaskEventBus`、削除済み `WbsSearchDialog` の除去）
- 検証なしパスワード更新 `UserDao.updatePasswordByEmail` を削除（呼び出し箇所なし。リセットは `resetPasswordWithVerification` / `AuthService.resetPassword` のみ）
- `UiDebugUtil` をカスタム `util.Logger` から SLF4J に移行し、`util/Logger.java` を削除

### これまでの P0〜P2 消化メモ（再評価時点）

- **P0 認証**: `AuthAttemptGuard` によるログイン/コード送信/検証の試行制限、UI からの検証付きリセット経路、非 BCrypt 平文拒否
- **P1 ドメイン**: `FILTER_ALL="ALL"`、`Task.status` を `TaskStatus` enum 化、`TaskDialog` → `TaskService`、`SearchFilterUtil` でフィルタ正規化共通化
- **P2 整理**: `TaskBusinessRules` / `EntryType` の i18n・コード化、`TaskEntryFactory` / `ProjectMemberFilter` 抽出、README アーキ記述の更新

## 2026-09-02

### カレンダー絵文字表示の調査

- 症状: 日付選択ボタンのカレンダー絵文字が期待通りに表示されず、`...` のような代替表示になるケースがあった
- 原因候補:
  - `WbsPanel.java` / `TaskDialog.java` 側でボタン生成後にフォントを上書きしていた
  - Windows 環境でソースに絵文字を直接書くと、`javac` の既定文字コード影響を受ける可能性があった
  - Swing ボタン描画では、文字列・フォント・Look & Feel の組み合わせで見え方がぶれる
- 対応:
  - `IconManager.java` のカレンダー絵文字はコードポイント生成に変更した
  - `WbsPanel.java` / `TaskDialog.java` での `Segoe UI Symbol` 上書きを外した
  - 画像確認に頼らず切り分けるための `UiDebugUtil.java` と `EmojiTest.java` を追加した

### 今後の運用

- UI 崩れや絵文字表示の問題は `tasknavi.uiDebug=true` で起動して、コンポーネントツリーとフォント情報をログで確認する
- フォント・絵文字の最小再現は `EmojiTest` を使って本体から切り離して確認する
- 変更内容の要約はこのファイルへ追記する

## 2026-09-25

### マージ後フォロー（1〜5）

1. **スモーク**: `MainFrameSmokeTest` 追加（4タブ開閉・検索クリア・テーマ切替）。`CalendarPanelTest` / `TaskDialogUiTest` を隔離 DB 化。`mvn test` 79件すべて成功。
2. **CI**: `.github/workflows/ci.yml`（JDK 17 + Maven `test`）を追加。
3. **認証 UI**: `LoginFrame` / `RegisterFrame` / `ResetPasswordFrame` を `org.example.ui.auth` へ移動。
4. **薄切り**: `GanttMouseController`、`IconGlyphs`、`AppThemeControls` を抽出。
5. **小機能**: `MainFrame` が最後に開いたタブを Preferences で記憶・復元。
6. **評価フォロー**: UI からの `new TaskDao()` を排除（`TaskService.createDefault()`）。`TaskDialog` は `taskService` 必須。handbook / README を現状パッケージに合わせて更新。

### 2026-09-25（続き）: push 後フォロー

- CI を `xvfb-run` 対応にし、ロケール依存アサーションを修正（Linux headless での失敗解消）
- キーワード検索が担当者名にもマッチ（`TaskViewFilter`）
- `DatePickerDialog` / `TaskSearchDialog` を `org.example.ui` へ移動
- `AuthServiceTest`・JSON ラウンドトリップ・担当者キーワードテストを追加
