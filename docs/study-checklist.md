# TaskNavi 学習チェックリスト（操作 → 読むファイル）

プログラミング開始から間もない人が、**一つの操作ごとに必要なクラスだけ**を追えるようにした地図です。  
併用ドキュメント: [beginner-handbook.md](./beginner-handbook.md)

## 使い方（毎回これだけ）

1. アプリを起動する
2. 下の操作を **1つだけ** 選んで実際に触る
3. 「必読」を上から順に開く（全部通読しない）
4. ノートに矢印を1本書く: `画面 → Service → Dao → （必要なら）イベント`
5. 最後の「できたチェック」を自分の言葉で埋める

詰まったとき AI に聞く例:

> 「〇〇操作で、MainFrame から TaskService までの呼び出し順をクラス名だけで教えて」

---

## レベル0: 起動と画面の骨格（最初の2〜3日）

### 操作A: アプリを起動する

| 区分 | ファイル |
|------|----------|
| 必読 | `Main.java` |
| 必読 | `Database.java`（Flyway で表を用意） |
| 関連 | `SampleDataUtil.java`（空DBならサンプル投入） |
| 関連 | `LoginFrame`（`ui/auth/LoginFrame.java`）※ skipLogin なしの場合 |

できたチェック:

- [ ] `main` が何をしているか、3行で言える
- [ ] 「DB準備 → スプラッシュ → ログイン or メイン」の順が言える

### 操作B: ログインしてメイン画面を開く

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/auth/LoginFrame.java` |
| 必読 | `AuthService.java` |
| 必読 | `TaskService.java` の `createDefault()` |
| 必読 | `MainFrame.java`（コンストラクタ前半: ヘッダー・タブ追加） |
| 関連 | `UserDao.java` / `UserSession.java` |
| 関連 | `ui/MainHeaderBar.java` / `ui/MainToolbarButtons.java` |

できたチェック:

- [ ] ログイン成功後、誰が `MainFrame` を `new` しているか言える
- [ ] UI が `new TaskDao()` せず `TaskService.createDefault()` を使う理由を言える

### 操作C: タブを切り替える（WBS / カンバン / ガント / カレンダー）

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame.java`（`JTabbedPane` と `addChangeListener`） |
| 必読 | `SearchablePanel.java`（各タブが実装する約束） |
| 関連 | `ui/MainTabChrome.java`（見た目） |
| 関連 | `ui/MainGlobalSearchBar.java` の `syncActive()` |

できたチェック:

- [ ] タブ本体のクラス名を4つ言える
- [ ] 「最後のタブを Preferences で覚える」場所を `MainFrame` 内で指せる

---

## レベル1: データの一本道（最重要・1週間）

### 操作D: ツールバーの「追加」でタスクを作る

これがこのプロジェクトの**幹**です。

```
MainFrame
  → MainTaskAddHelper.showAndAdd
    → TaskDialog（入力）
    → TaskEntryFactory（Task 組み立て）
    → TaskService.addTask
      → TaskDao / persistence.TaskCrudDao
      → TaskEventBus.post(TASK_CREATED)
        → 各 Panel が聞いて再描画
```

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame.java`（追加ボタンの `ActionListener`） |
| 必読 | `ui/MainTaskAddHelper.java` |
| 必読 | `ui/taskdialog/TaskDialog.java` |
| 必読 | `TaskEntryFactory.java` |
| 必読 | `TaskService.java`（`addTask`） |
| 必読 | `event/TaskEventBus.java` / `event/TaskEvent.java` |
| 関連 | `ui/taskdialog/TaskDialogFormFactory.java` |
| 関連 | `ui/DatePickerDialog.java`（日付ボタン） |
| 関連 | `TaskDao.java` + `persistence/TaskCrudDao.java` |
| テスト | `TaskDialogUiTest.java` / `MainFrameSmokeTest.java` |

できたチェック:

- [ ] 上の矢印を見ずに、紙に書き直せる
- [ ] 「画面は保存せず、Service が保存とイベントを担当」と言える

### 操作E: どれかの画面でタスクを編集して保存する

| 区分 | ファイル |
|------|----------|
| 必読 | 使った Panel（例: `ui/wbs/WbsPanel.java`） |
| 必読 | `TaskService.updateTask` |
| 必読 | その Panel の `TaskEventListener`（イベント受信 → refresh） |
| 関連 | `ui/wbs/WbsDetailForm.java`（WBS の場合） |
| 関連 | `persistence/TaskWriteValidator.java` |

できたチェック:

- [ ] 保存後、他タブも更新される理由を `TaskEventBus` で説明できる

### 操作F: ツールバーの「削除」

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame.handleDeleteSelectedItem` |
| 必読 | `SearchablePanel.getSelectedTask` |
| 必読 | `ui/TaskDeleteHelper.java` |
| 必読 | `TaskService.deleteTask` |
| 関連 | `persistence/TaskHierarchySupport.java`（子孫も消える） |

できたチェック:

- [ ] 「今どのタブが選ばれているか」から削除対象を取る流れが言える

### 操作G: ツールバーの「更新」（REFRESH）

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame` の refresh ボタン |
| 必読 | `TaskEvent.Type.REFRESH_ALL` |
| 必読 | どれか1 Panel のイベントハンドラ |

できたチェック:

- [ ] DBを触らず「再読み込みせよ」と广播しているだけ、と説明できる

---

## レベル2: 検索・フィルタ（共通化の練習）

### 操作H: グローバル検索欄に文字を入れて絞る

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/MainGlobalSearchBar.java` |
| 必読 | `SearchablePanel.applySearchFilter` |
| 必読 | `util/TaskViewFilter.java`（名前 **または担当者**） |
| 必読 | 今アクティブな Panel の `applySearchFilter` |
| 関連 | `util/SearchFilterUtil.java` |
| テスト | `util/TaskViewFilterTest.java` |

できたチェック:

- [ ] なぜ Panel ごとに別ロジックを書かず `TaskViewFilter` に寄せるのか言える

### 操作I: フィルタダイアログ／プロジェクト絞り込み

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/TaskSearchDialog.java` |
| 必読 | `ui/ProjectFilterDialog.java` |
| 必読 | Panel の `handleExistingProject` / `handleClearFilter` |
| 関連 | `util/ProjectMemberFilter.java` |

---

## レベル3: 画面ごとの深掘り（1画面ずつ）

一度に全部やらない。**週に1画面**で十分。

### 操作J: WBS でツリーを触る

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/wbs/WbsPanel.java` |
| 関連 | `WbsTreeModelBuilder` / `WbsTreeFilter` / `WbsDetailForm` |
| 後回し可 | `WbsTreeTransferHandler`（DnD） |
| テスト | `WbsTreeFilterTest` / `WbsFormValidatorTest` |

### 操作K: カンバンでカードを別列へ移す

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/kanban/KanbanPanel.java` |
| 関連 | `KanbanCardFactory` / `KanbanColumnSupport` / `KanbanTaskFilter` |
| 関連 | `util/TaskStatusCycle.java` |
| テスト | `KanbanTaskFilterTest` |

### 操作L: カレンダーで月／週を切り替える

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/calendar/CalendarPanel.java` |
| 関連 | `CalendarNavToolbar` / `CalendarMonthViewBuilder` / `CalendarWeekViewBuilder` |
| 関連 | `CalendarTaskFilter` / `CalendarPeriodLabels` |
| テスト | `CalendarPanelTest` / `CalendarTaskFilterTest` |

### 操作M: ガントのバーを見る（ドラッグは後で）

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/gantt/GanttPanel.java`（全体構造だけ） |
| 関連 | `GanttPainter` / `GanttTaskFilter` / `GanttSideViews` |
| 後回し | `GanttMouseController` / `GanttBarDragMath`（複雑） |
| テスト | `GanttTaskFilterTest` / `GanttBarDragMathTest` |

---

## レベル4: 認証・入出力・テーマ

### 操作N: 新規登録／パスワード再設定画面を開く

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/auth/RegisterFrame.java` または `ResetPasswordFrame.java` |
| 必読 | `ui/auth/AuthFormWidgets.java` / `AuthPasswordPolicy.java` |
| 必読 | `AuthService.java` |
| 関連 | `util/AuthAttemptGuard.java` |
| テスト | `AuthServiceTest` / `AuthPasswordPolicyTest` / `AuthAttemptGuardTest` |

### 操作O: インポート／エクスポート

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/TaskDataIoHelper.java` |
| 必読 | `util/CsvUtil.java` |
| 必読 | `TaskService.importTasks` |
| テスト | `util/CsvUtilTest` |

### 操作P: ライト／ダーク切替

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame` のテーマトグル |
| 必読 | `AppTheme.java` / `AppThemeControls.java` |
| 関連 | `ui/MainThemeSupport.java` |

---

## 自分で練習する課題（チェックしてから実装）

AI に丸投げせず、**先に直すファイルを自分で書いてから**着手する。

| 課題 | まず開くべき場所 |
|------|------------------|
| 検索プレースホルダの文言を変える | `messages.properties` / `messages_ja.properties` |
| デフォルトタブを変える | `MainFrame` のタブ記憶ロジック |
| 「完了を隠す」フィルタ | `TaskViewFilter` + 既存テストに1ケース |
| 期限超過を目立たせる | `CalendarTaskCardFactory` またはカンバンカード |
| 絞り込み結果だけエクスポート | `TaskDataIoHelper` + アクティブ Panel の一覧取得 |

---

## 就活・説明用の最短台本（5分）

次を、チェックリストを見ずに言えるようにする。

1. TaskNavi は WBS／カンバン／ガント／カレンダーで進捗を見る Swing アプリ
2. 画面は薄く、更新は `TaskService`、保存は `TaskDao`、同期は `TaskEventBus`
3. 自分が追った操作は「追加」（操作D）で、経路は MainFrame → Helper → Dialog → Service → Bus
4. テストは `mvn test`、CI でも同じことを動かしている
5. AI で加速したが、層の分け方と自分で追った変更は説明できる

---

## 進捗メモ（自由記入）

| 日付 | やった操作 | わかったこと（1行） | まだ曖昧な点 |
|------|------------|-------------------|--------------|
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
