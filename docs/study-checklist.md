# TaskNavi 学習チェックリスト（操作 → 読むファイル）

このファイルは辞書です。毎日の手順は [beginner-handbook.md](./beginner-handbook.md) の「12. 最初の2週間」です。  
10日が終わるまで、下の操作を自分で選んで進まない。

| 日 | やること | このファイルの場所 |
|---|---|---|
| 1日目 | 起動 | 操作A |
| 2日目 | メイン画面と保存の窓口 | 操作B |
| 3日目 | 追加（ダイアログから `addTask` まで） | 操作Dの画面側 |
| 4日目 | 他の画面への知らせ | 操作Dの `TaskEventBus` と、WBS の `taskEventListener` |
| 5日目 | 編集（`updateTask`。道は追加と同じ） | 操作E |
| 6日目 | 削除 | 操作F |
| 7日目 | 打ちながら検索 | 操作H |
| 8日目 | WBS は表示、編集はダイアログ | 操作J |
| 9日目 | カンバンで状態を変える | 操作K |
| 10日目 | 話す | handbook の13章 |

10日目のあとで、操作L（カレンダー）か操作M（ガント）を1つだけ選ぶ。触ってから、その操作の「必読」だけ開く。

---

## レベル0: 起動と画面の骨格（最初の2〜3日）

### 操作A: アプリを起動する

| 区分 | ファイル |
|------|----------|
| 必読 | `Main.java` |
| 必読 | `persistence/Database.java`（Flyway で表を用意） |
| 関連 | `persistence/SampleDataUtil.java`（タスクが 1 件もなければサンプル投入） |
| 関連 | `db/migration/V6__drop_login.sql`（前の版のログイン用の表を外す） |
| 関連 | `db/migration/V7__unnumber_sample_stages.sql`（サンプルの段階名の「1. 」などを外す） |

**コードの流れ（Main.java）:**

```
main()
  → 未処理例外ハンドラ設定
  → Database.prepareOnStartup() (古い DB の引き継ぎと1日1回のバックアップ)
  → SwingUtilities.invokeLater (EDT上で実行)
    → UIManager.setLookAndFeel (Look & Feel設定)
    → applyGlobalUiDefaults() (テーマ適用)
    → createSplashWindow() (スプラッシュ作成)
    → splash.setVisible(true) (表示)
    → Timer(1000ms) で1秒待機
      → splash.dispose() (スプラッシュ消去)
      → SampleDataUtil.insertSampleTasksIfEmpty() (空のときだけサンプル)
      → openMainFrame()
        → TaskService.createDefault() (サービス作成)
        → new MainFrame() (メイン画面作成)
        → mainFrame.setVisible(true)
```

できたチェック:

- [ ] `main` が何をしているか、3行で言える
- [ ] 「DB準備 → スプラッシュ → メイン」の順が言える
- [ ] ログインがない理由（データは PC の中にあり、守りは Windows のログインに任せる）を言える。詳しくは handbook の「ログイン画面を消した理由」の5つ

### 操作B: メイン画面を開く

| 区分 | ファイル |
|------|----------|
| 必読 | `service/TaskService.java` の `createDefault()` |
| 必読 | `ui/main/MainFrame.java`（コンストラクタ前半: ヘッダー・タブ追加） |
| 関連 | `ui/main/MainHeaderBar.java` / `ui/main/MainToolbarButtons.java` |

**コードの流れ（Main → MainFrame）:**

```
Main.openMainFrame()
  → TaskService.createDefault()
    → new TaskDao() (この PC のタスクの保存先)
    → new TaskService(taskDao)
  → new MainFrame(taskService) (メイン画面作成)
    → ウィンドウ設定
    → MainHeaderBar.create() (ヘッダー作成。共有で使う表示名を出す)
    → createToolBarPanel() (ツールバー作成)
    → new WbsPanel(taskService)
    → new KanbanPanel(taskService)
    → new GanttPanel(taskService)
    → new CalendarPanel(taskService)
    → addMainTab() で5つのタブ追加（今日 / WBS / カンバン / ガント / カレンダー）
    → setSelectedIndex() でいつも「今日」のタブを選ぶ
    → addChangeListener() でタブ切り替えリスナー設定
  → mainFrame.setVisible(true)
```

できたチェック:

- [ ] 誰が `MainFrame` を `new` しているか言える
- [ ] UI が `new TaskDao()` せず `TaskService.createDefault()` を使う理由を言える

### 操作C: タブを切り替える（WBS / カンバン / ガント / カレンダー）

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/main/MainFrame.java`（`JTabbedPane` と `addChangeListener`） |
| 必読 | `ui/SearchablePanel.java`（各タブが実装する約束） |
| 関連 | `ui/main/MainTabChrome.java`（見た目） |
| 関連 | `ui/main/MainGlobalSearchBar.java` の `syncActive()` |

**コードの流れ（MainFrame タブ切り替え）:**

```
MainFrame コンストラクタ
  → new JTabbedPane() (タブペイン作成)
  → new WbsPanel(taskService)
  → new KanbanPanel(taskService)
  → new GanttPanel(taskService)
  → new CalendarPanel(taskService)
  → addMainTab() で「今日」と上の4つのパネルをタブに追加
  → 起動時はいつも「今日」のタブを選ぶ（前回のタブは覚えない）
  → mainTabbedPane.addChangeListener(e -> {
      → MainTabChrome.updateTabTextColors(mainTabbedPane)
        → 選択タブを PRIMARY 色、他を TEXT_MUTED 色に変更
        → TabIcon.setColor() でアイコン色も更新
      → globalSearchBar.syncActive()
        → getActiveComponent() で現在のタブのパネル取得
        → SearchablePanel.applySearchFilter(keyword, status) でフィルタ再適用
        → refreshFilterSummary() でフィルタ要約ラベル更新
      → MainThemeSupport.updateViewTheme(getActiveComponent())
        → アクティブなパネルのテーマを更新
    })
  → MainTabChrome.updateTabTextColors(mainTabbedPane) (初期表示時の色設定)

ユーザーがタブをクリック
  → ChangeListener が発火
  → 上記の3つの処理が実行される
```

できたチェック:

- [ ] タブ本体のクラス名を4つ言える
- [ ] 起動したとき必ず「今日」のタブが選ばれる行を `MainFrame` 内で指せる（前回のタブは覚えない。毎朝まず今日のタスクを見るため）
- [ ] タブ切り替え時に `syncActive()` が呼ばれてフィルタが再適用されると言える
- [ ] `SearchablePanel` インターフェースが各タブで共通の検索メソッドを提供していると言える

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
| 必読 | `ui/main/MainFrame.java`（追加ボタンの `ActionListener`） |
| 必読 | `ui/main/MainTaskAddHelper.java` |
| 必読 | `ui/taskdialog/TaskDialog.java` |
| 必読 | `ui/TaskEntryFactory.java` |
| 必読 | `service/TaskService.java`（`addTask`） |
| 必読 | `event/TaskEventBus.java` / `event/TaskEvent.java` |
| 関連 | `ui/taskdialog/TaskDialogFormFactory.java` |
| 関連 | `ui/DatePickerDialog.java`（日付ボタン） |
| 関連 | `persistence/TaskDao.java` + `persistence/TaskCrudDao.java` |
| テスト | `TaskDialogUiTest.java` / `MainFrameSmokeTest.java` |

**コードの流れ（追加ボタン → DB保存 → 画面更新）:**

```
MainFrame.createToolBarPanel()
  → addTaskButton.addActionListener(e -> MainTaskAddHelper.showAndAdd(this, taskService))

ユーザーが「追加」ボタンをクリック
  → MainTaskAddHelper.showAndAdd(parent, taskService)
    → new TaskDialog(parent, taskService) (ダイアログ作成)
    → dialog.setVisible(true) (モーダルダイアログ表示)
    → ユーザーが入力して「保存」ボタンをクリック
      → TaskDialog.handleSave()
        → TaskDialogSaveValidator.validate() (入力バリデーション)
        → confirmed = true
        → dispose() (ダイアログを閉じる)
    → dialog.isConfirmed() で確認
    → TaskEntryFactory.requiresParent(dialog.getEntryType()) で親プロジェクト必須チェック
    → TaskEntryFactory.fromDialog(dialog, taskService.getAllTasks())
      → dialog.getEntryType(), getTaskName(), getAssignee() などから値取得
      → LocalDate.parse() で日付変換
      → EntryType に応じて level, parentId を決定
      → new Task() でタスクオブジェクト作成
    → SwingWorker.doInBackground() (バックグラウンドで保存)
      → taskService.addTask(newTask)
        → TaskDao.addTask(newTask)
          → validateTask(newTask) (バリデーション)
          → TaskCrudDao.add(newTask)
            → Database.connect() でDB接続取得
            → PreparedStatement で INSERT SQL 実行
            → getGeneratedKeys() で生成されたID取得
            → task.setId(generatedId)
          → updateParentProgress(parentId) (親の進捗を再計算)
            → TaskHierarchySupport.rollupAncestors() で祖先タスクの進捗計算
            → TaskCrudDao.updateProgressAndStatus() で親タスク更新
        → TaskEventBus.getInstance().post(new TaskEvent(TASK_CREATED, created))
          → SwingUtilities.invokeLater() でEDT上で通知
          → 登録された全 TaskEventListener.onTaskEvent() を呼び出し
            → 各 Panel (WbsPanel, KanbanPanel, etc.) が refresh() を実行
    → SwingWorker.done() (EDTで結果処理)
      → NoticeBar.info() で「保存しました」を表示
      → 例外なら ErrorDialogUtil.showBackgroundError() でエラー表示
```

できたチェック:

- [ ] 上の矢印を見ずに、紙に書き直せる
- [ ] 「画面は保存せず、Service が保存とイベントを担当」と言える
- [ ] `TaskEntryFactory.fromDialog()` がダイアログの入力から Task オブジェクトを組み立てていると言える
- [ ] `TaskEventBus.post()` が各パネルの `onTaskEvent()` を呼び出して再描画させていると言える

### 操作E: どれかの画面でタスクを編集して保存する

| 区分 | ファイル |
|------|----------|
| 必読 | 使った Panel（例: `ui/wbs/WbsPanel.java` の `openEditDialog`） |
| 必読 | `TaskService.updateTask` と `updateTasks` |
| 必読 | その Panel の `TaskEventListener`（イベント受信 → refresh） |
| 関連 | `ui/wbs/WbsDetailForm.java`（WBS の場合） |
| 関連 | `persistence/TaskWriteValidator.java` |
| 関連 | `ErrorDialogUtil.findAppException`（失敗を画面に出す） |
| テスト | `TaskDaoIntegrationTest` の `updateTasks` |

**コードの流れ（WBS編集の例）:**

```
WbsPanel コンストラクタ
  → TaskEventBus.getInstance().register(taskEventListener)
    → taskEventListener = event -> {
        → invalidateTaskCache()
        → refreshWbs()
      }

ユーザーがWBSでタスクを選択して、右側の名前の行をダブルクリック
  → WbsDetailForm の editOnDoubleClick が反応する（何も出していないときは showing が false で無視）
  → openEditDialog() コールバックが呼ばれる
  → WbsPanel.openEditDialog()
    → selectedTaskId で選択中タスクIDを確認
    → taskService.getTaskById(selectedTaskId) でタスク取得
    → new TaskDialog(frame, taskService, task) (既存タスクでダイアログ作成)
    → dialog.setVisible(true) (モーダルダイアログ表示)
    → ユーザーが値を変更して「保存」をクリック
      → TaskDialog.handleSave()
        → TaskDialogSaveValidator.validate() (バリデーション)
        → confirmed = true
        → dispose()
    → dialog.isConfirmed() で確認
    → LocalDate.parse() で日付変換
    → task.setName(), setAssignee(), setProgress(), setPriority(), setStartDate(), setEndDate() で値更新
    → task.setStatus(TaskStatusCycle.fromProgress(task.getProgress())) でステータス更新
    → SwingWorker.doInBackground() (バックグラウンドで保存)
      → taskService.updateTask(task)
        → TaskService.updateTasks(List.of(task))
          → TaskDao.updateTasks(tasks)
            → 各タスクを validateTask() でバリデーション
            → TaskCrudDao.updateAll(tasks)
              → Database.withTransaction() でトランザクション開始
              → 各タスクを UPDATE SQL で更新
              → getAll(connection) で全タスク再取得
              → TaskHierarchySupport.rollupAncestors() で祖先の進捗計算
              → updateProgressAndStatus() で親タスク更新
          → 各タスクに対して TaskEventBus.post(TASK_UPDATED, task)
            → SwingUtilities.invokeLater() でEDT上で通知
            → 登録された全 TaskEventListener.onTaskEvent() を呼び出し
              → WbsPanel の taskEventListener が発火
                → invalidateTaskCache() (キャッシュ無効化)
                → refreshWbs() (ツリー再描画)
              → 他の Panel (KanbanPanel, etc.) も同様に refresh()
    → SwingWorker.done() (EDTで結果処理)
      → NoticeBar.info() で「保存しました」を表示
      → 例外なら ErrorDialogUtil.showBackgroundError() でエラー表示
```

できたチェック:

- [ ] 保存後、他タブも更新される理由を `TaskEventBus` で説明できる
- [ ] 入力が不正なとき、1件も保存されないと言える
- [ ] WBS の編集は `openEditDialog` から `updateTask` を呼び、成功したあとだけ他の画面に知らせると言える
- [ ] `TaskCrudDao.updateAll()` がトランザクションで複数タスクを更新していると言える

### 操作F: ツールバーの「削除」

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame.handleDeleteSelectedItem` |
| 必読 | `SearchablePanel.getSelectedTask` |
| 必読 | `ui/TaskDeleteHelper.java` |
| 必読 | `TaskService.deleteTask` |
| 関連 | `persistence/TaskHierarchySupport.java`（子孫も消える） |
| 関連 | `persistence/TaskCrudDao.deleteIds`（1回で消す。失敗したら全部戻す） |

**コードの流れ（削除ボタン → DB削除 → 画面更新）:**

```
MainFrame.createToolBarPanel()
  → deleteButton.addActionListener(e -> handleDeleteSelectedItem())

ユーザーが「削除」ボタンをクリック
  → MainFrame.handleDeleteSelectedItem()
    → getActiveComponent() で現在アクティブなパネル取得
    → SearchablePanel.getSelectedTask() で選択中タスク取得
    → TaskDeleteHelper.confirmAndDelete(parent, taskService, selectedTask)
      → selected == null なら NoticeBar.info() で警告
      → EntryType に応じて削除確認メッセージ作成
      → NoticeBar.confirm() または JOptionPane.showConfirmDialog() で確認ダイアログ表示
      → ユーザーが「はい」を選択
        → delete(parent, taskService, selected)
          → SwingWorker.doInBackground() (バックグラウンドで削除)
            → taskService.deleteTask(selected.getId())
              → TaskDao.deleteTask(taskId)
                → getAllTasks() で全タスク取得
                → タスク存在確認
                → parentId を取得
                → TaskHierarchySupport.collectDescendantIds(taskId, allTasks, targetIds)
                  → 再帰的に子孫タスクのIDを収集
                → TaskCrudDao.deleteIds(targetIds)
                  → Database.withTransaction() でトランザクション開始
                  → PreparedStatement で DELETE SQL をバッチ実行
                  → 全IDを1回のトランザクションで削除
                  → 失敗ならロールバック
                → parentId が存在すれば updateParentProgress(parentId)
                  → TaskHierarchySupport.rollupAncestors() で祖先の進捗計算
                  → TaskCrudDao.updateProgressAndStatus() で親タスク更新
              → TaskEventBus.post(TASK_DELETED, deleted)
                → SwingUtilities.invokeLater() でEDT上で通知
                → 登録された全 TaskEventListener.onTaskEvent() を呼び出し
                  → 各 Panel が refresh() を実行
          → SwingWorker.done() (EDTで結果処理)
            → NoticeBar.info() で「削除しました」を表示
            → 例外なら ErrorDialogUtil.showBackgroundError() でエラー表示
```

できたチェック:

- [ ] 「今どのタブが選ばれているか」から削除対象を取る流れが言える
- [ ] 子が3件あっても、途中だけ消えない理由を「1回の削除」で言える
- [ ] `TaskHierarchySupport.collectDescendantIds()` が子孫タスクを再帰的に収集していると言える
- [ ] `TaskCrudDao.deleteIds()` がトランザクションでバッチ削除していると言える

### 操作G: ツールバーの「更新」（REFRESH）

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame` の refresh ボタン |
| 必読 | `TaskEvent.Type.REFRESH_ALL` |
| 必読 | どれか1 Panel のイベントハンドラ |

**コードの流れ（更新ボタン → 画面再描画）:**

```
MainFrame.createToolBarPanel()
  → refreshButton.addActionListener(e ->
      TaskEventBus.getInstance().post(new TaskEvent(TaskEvent.Type.REFRESH_ALL, null)))

ユーザーが「更新」ボタンをクリック
  → TaskEventBus.post(REFRESH_ALL, null)
    → SwingUtilities.invokeLater() でEDT上で通知
    → 登録された全 TaskEventListener.onTaskEvent() を呼び出し
      → WbsPanel の taskEventListener が発火
        → invalidateTaskCache()
        → refreshWbs()
      → KanbanPanel の taskEventListener が発火
        → refreshKanban()
      → GanttPanel の taskEventListener が発火
        → refreshGantt()
      → CalendarPanel の taskEventListener が発火
        → refreshCalendar()
```

できたチェック:

- [ ] DBを触らず「再読み込みせよ」と知らせているだけ、と説明できる
- [ ] `REFRESH_ALL` イベントが全パネルの `refresh()` メソッドを呼び出していると言える

---

## レベル2: 検索・フィルタ（共通化の練習）

### 操作H: グローバル検索欄に文字を入れて絞る

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/main/MainGlobalSearchBar.java` |
| 必読 | `SearchablePanel.applySearchFilter` |
| 必読 | `util/TaskViewFilter.java`（名前・担当者・メモ・コメント。`matchesKeyword`） |
| 必読 | 今アクティブな Panel の `applySearchFilter` |
| 関連 | `util/SearchFilterUtil.java` |
| 関連 | `MainGlobalSearchBar.describeFilters`（適用中フィルタの表示文言） |
| テスト | `util/TaskViewFilterTest.java` / `MainGlobalSearchBarDescribeTest` |

**コードの流れ（検索入力 → フィルタ適用）:**

```
MainGlobalSearchBar.createSearchBox()
  → txtGlobalSearch.getDocument().addDocumentListener()
    → insertUpdate/removeUpdate/changedUpdate で onSearchTextChange() を呼ぶ
      → btnClearSearch.setVisible(!txtGlobalSearch.getText().isEmpty())
      → applyToActive()

ユーザーが検索欄に文字を入力
  → onSearchTextChange()
    → applyToActive()
      → currentGlobalKeyword = txtGlobalSearch.getText().trim() (空なら null)
      → activeViewSupplier.get() で現在アクティブなパネル取得
      → SearchablePanel.applySearchFilter(currentGlobalKeyword, currentGlobalStatus)
        → 例: WbsPanel.applySearchFilter(keyword, status)
          → currentSearchKeyword = keyword
          → currentStatusFilter = status
          → invalidateTaskCache()
          → refreshWbs()
            → taskService.getAllTasks() で全タスク取得
            → TaskViewFilter.matches(task, keyword, status) でフィルタリング
              → keyword が null または task.getName().contains(keyword)
              → または task.getAssignee().contains(keyword)
              → または task.getMemo().contains(keyword)
              → status が null または task.getStatusCode().equals(status)
            → フィルタ済みタスクでツリー再構築
      → refreshFilterSummary()
        → describeActiveFilters() でフィルタ要約文字列作成
        → filterSummaryLabel に表示
```

できたチェック:

- [ ] なぜ Panel ごとに別ロジックを書かず `TaskViewFilter` に寄せるのか言える
- [ ] `DocumentListener` が入力のたびに `applyToActive()` を呼び出していると言える
- [ ] `TaskViewFilter.matches()` が名前・担当者・メモ・コメントでのマッチングを行っていると言える

### 操作I: フィルタダイアログ／プロジェクト絞り込み

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/main/MainGlobalSearchBar.java` |
| 必読 | `ui/ProjectFilterDialog.java` |
| 必読 | Panel の `handleExistingProject` / `handleClearFilter` |
| 関連 | `util/ProjectMemberFilter.java` |

**コードの流れ（プロジェクト絞り込み）:**

```
MainGlobalSearchBar.showFilterMenu()
  → JMenuItem "プロジェクトで絞り込む" を追加
  → projectItem.addActionListener(e -> {
      → activeViewSupplier.get() で現在アクティブなパネル取得
      → SearchablePanel.handleExistingProject()
    })

ユーザーがフィルターメニューから「プロジェクトで絞り込む」を選択
  → SearchablePanel.handleExistingProject()
    → 例: WbsPanel.handleExistingProject()
      → ProjectFilterDialog.show(parent, taskService)
        → taskService.getAllTasks() で全タスク取得
        → level == 1 または parentId == null のタスクをプロジェクトとして収集
        → プロジェクトが空なら NoticeBar.info() で警告
        → JComboBox でプロジェクト選択ダイアログ表示
        → ユーザーがプロジェクトを選択して「OK」
          → Result(confirmed=true, projectId) を返す
      → Result.confirmed なら:
        → currentFilterProjectId = projectId
        → invalidateTaskCache()
        → refreshWbs()
          → taskService.getAllTasks() で全タスク取得
          → ProjectMemberFilter.collectMemberIds(allTasks, projectId)
            → 各タスクの祖先をたどって projectId に属するか判定
            → 属するタスクのIDを Set に収集
          → TaskViewFilter.apply() で projectMemberIds でフィルタリング
            → projectMemberIds.contains(task.getId()) で判定
          → フィルタ済みタスクでツリー再構築
```

できたチェック:

- [ ] `ProjectFilterDialog.show()` がプロジェクト一覧を表示して選択を取得していると言える
- [ ] `ProjectMemberFilter.collectMemberIds()` がプロジェクト配下の全タスクIDを収集していると言える

---

## レベル3: 画面ごとの深掘り（1画面ずつ）

一度に全部やらない。**週に1画面**で十分。

### 操作J: WBS でツリーを触る

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/wbs/WbsPanel.java` |
| 関連 | `WbsTreeModelBuilder` / `WbsTreeFilter` / `WbsDetailForm` |
| 後回し可 | `WbsTreeTransferHandler`（DnD） |
| テスト | `WbsTreeFilterTest` |

**コードの流れ（WBSツリー表示）:**

```
WbsPanel コンストラクタ
  → new JTree() (ツリー作成)
  → setCellRenderer(new WbsTreeCellRenderer()) (カスタムレンダラー設定)
  → addMouseListener() (選択イベント)
  → addTreeSelectionListener() (ツリー選択変更イベント)
  → TaskEventBus.getInstance().register(taskEventListener) (イベントリスナー登録)
  → refreshWbs() (初期表示)
    → taskService.getAllTasks() で全タスク取得
    → TaskViewFilter.apply() でフィルタリング
    → WbsTreeModelBuilder.build() でツリーモデル構築
      → 親子関係に基づいて DefaultMutableTreeNode の階層構造を作成
      → nodeTaskMap にノードとタスクのマッピングを保存
    → wbsTree.setModel(newModel) でツリーモデル設定
    → 展開状態を復元

ユーザーがツリーノードをクリック
  → TreeSelectionListener が発火
  → selectedTaskId を更新
  → detailForm.updateForm(task) で詳細フォーム更新
```

できたチェック:

- [ ] `WbsTreeModelBuilder.build()` がタスクリストからツリー構造を構築していると言える
- [ ] ノード選択時に `selectedTaskId` が更新され、詳細フォームが更新されると言える

### 操作K: カンバンでカードを別列へ移す

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/kanban/KanbanPanel.java` |
| 関連 | `KanbanCardFactory` / `KanbanColumnSupport` / `KanbanTaskFilter` / `KanbanAssignees`（担当者での絞り込みと分け方） |
| 関連 | `util/TaskStatusCycle.java` / `ui/TaskStatusChanger.java` / `service/TaskService.java` の `changeStatus` |
| テスト | `KanbanTaskFilterTest` / `KanbanAssigneesTest` / `TaskServiceStatusChangeTest` |

**コードの流れ（カンバンカード移動）:**

```
KanbanPanel コンストラクタ
  → createColumnPanel() で3つのカラム（todo/inProgress/done）作成
  → KanbanCardFactory.Actions でカードのアクション定義
    → onDrop(taskId, source) でドロップ処理
  → TaskEventBus.getInstance().register(taskEventListener) (イベントリスナー登録)
  → refreshKanban() (初期表示)
    → taskService.getAllTasks() で全タスク取得
    → KanbanTaskFilter でフィルタリング
    → KanbanAssignees.filter で担当者を絞る（上の「担当者」の選択）
    → 「担当者ごとに分ける」がオンなら KanbanAssignees.group で担当者ごとにまとめ、列の中に見出しを入れる
    → ステータスごとにタスクを分類
    → KanbanCardFactory.createCard() でカード作成
    → 各カラムにカードを追加

ユーザーがカードの矢印ボタン（← →）を押す
  → onStatusChange(task, newStatus)
    → changeTaskStatus(taskId, newStatus)

ユーザーがカードを別のカラムにドラッグ＆ドロップ
  → onDrop(taskId, source)
    → KanbanColumnSupport.statusOf(source) でドロップ先のステータス取得
    → changeTaskStatus(taskId, status)

changeTaskStatus(taskId, newStatus)
  → TaskStatusChanger.change(...)（カレンダーの矢印ボタンも同じ）
    → SwingWorker.doInBackground() (バックグラウンドで保存)
      → taskService.changeStatus(taskId, newStatus)
        → 子がいれば HAS_CHILDREN を返して何もしない
        → task.setStatus(newStatus)
        → task.setProgress(TaskBusinessRules.resolveProgress(newStatus, task.getProgress()))
        → updateTask(task)
          → TaskDao.updateTasks(List.of(task))
            → TaskCrudDao.updateAll(tasks)
          → TaskEventBus.post(TASK_UPDATED, task)
            → 各 Panel が refresh() を実行
    → SwingWorker.done() (EDTで結果処理)
      → HAS_CHILDREN なら NoticeBar.info() で理由を表示
      → 失敗したときはエラーを表示
```

できたチェック:

- [ ] 矢印ボタンもドラッグも `changeTaskStatus()` に来ると言える
- [ ] 子がいるタスクを止める判断と進捗率の計算は、画面ではなく `TaskService.changeStatus()` にあると言える
- [ ] `onDrop()` がドロップ先のステータスを取得して `changeTaskStatus()` を呼び出していると言える
- [ ] ステータス変更時に `TaskBusinessRules.resolveProgress()` で進捗率も更新されていると言える

### 操作L: カレンダーで月／週を切り替える

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/calendar/CalendarPanel.java` |
| 関連 | `CalendarNavToolbar` / `CalendarMonthViewBuilder` / `CalendarWeekViewBuilder` |
| 関連 | `CalendarTaskFilter` / `CalendarPeriodLabels` |
| テスト | `CalendarPanelTest` / `CalendarTaskFilterTest` |

**コードの流れ（カレンダー表示切替）:**

```
CalendarPanel コンストラクタ
  → currentDate = LocalDate.now() (現在の日付)
  → selectedDate = LocalDate.now()
  → currentViewMode = ViewMode.MONTH
  → CalendarNavToolbar.create() でナビゲーションツールバー作成
    → Actions で isMonthView(), onPrev(), onNext(), onToday(), onToggleView() を定義
  → TaskEventBus.getInstance().register(taskEventListener) (イベントリスナー登録)
  → refreshCalendar() (初期表示)
    → taskService.getAllTasks() で全タスク取得
    → CalendarTaskFilter で期間内のタスクをフィルタリング
    → currentViewMode に応じて:
      → MONTH: CalendarMonthViewBuilder.build() で月ビュー構築
      → WEEK: CalendarWeekViewBuilder.build() で週ビュー構築
    → 日付セルにタスクカードを追加

ユーザーが「月/週」切り替えボタンをクリック
  → onToggleView()
    → currentViewMode = (currentViewMode == MONTH) ? WEEK : MONTH
    → refreshCalendar()
```

できたチェック:

- [ ] `CalendarNavToolbar.Actions` で月/週切り替えのアクションが定義されていると言える
- [ ] `refreshCalendar()` が `currentViewMode` に応じて異なるビルダーを呼び出していると言える
- [ ] `CalendarTaskFilter.tasksForDate()` が子を持つプロジェクト・工程を外す理由（毎日のマスが埋まるから）を言える

### 操作M: ガントのバーを見る（ドラッグは後で）

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/gantt/GanttPanel.java`（全体構造だけ） |
| 関連 | `GanttPainter` / `GanttTaskFilter` / `GanttSideViews` |
| 後回し | `GanttMouseController` / `GanttBarDragMath`（複雑） |
| テスト | `GanttTaskFilterTest` / `GanttBarDragMathTest` / `GanttTodayCenteringTest` |

**コードの流れ（ガントチャート表示）:**

（表示する期間は `GanttDisplayDates` が決め、今日の前後30日を必ず含めます。初めて画面に出たとき `GanttPanel.centerOnToday()` が今日を真ん中に送ります。地は `GanttPainter.drawRowStripes()`（1行おきの色）と `overlayDayColumns()`（土日・今日の列）が塗ります）

```
GanttPanel コンストラクタ
  → dayWidth = GanttMetrics.DEFAULT_DAY_WIDTH
  → GanttMouseController 作成（ドラッグ操作用）
  → addHierarchyListener() (初めて画面に出たら centerOnToday())
  → TaskEventBus.getInstance().register(taskEventListener) (イベントリスナー登録)
  → refreshTaskCache() (初期表示用にタスクを読む)
    → taskService.getAllTasks() で全タスク取得
    → GanttTaskFilter でフィルタリング
MainFrame
  → createTaskNameColumn() / createDateHeader() / createCorner() で左の列・上の日付・左上の角を付ける
paintComponent()
  → GanttDisplayDates.calculateVisibleDateWindow() で表示する期間を決める（今日の前後30日は必ず入る）
  → GanttPainter.drawRowStripes() / overlayDayColumns() で地を塗る
  → GanttPainter.drawBar() で各タスクのバーを描画
    → 開始日・終了日に基づいてバーの位置と長さを計算
    → 進捗率の分だけ状態の色で塗り、数字は見えている部分に置く（GanttBarGeometry.progressLabel）
  → GanttPainter.drawTodayLine() で今日の線
```

できたチェック:

- [ ] `GanttPainter.drawBar()` がタスクの開始日・終了日に基づいてバーを描画していると言える
- [ ] `GanttSideViews` がタスク名列と日付ヘッダーを作成していると言える
- [ ] ガントを開いたとき今日が真ん中に来る理由を、`centerOnToday()` と `GanttViewportSupport.centeredScrollX()` で言える

---

## レベル4: 入出力・テーマ

### 操作O: インポート／エクスポート

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/TaskDataIoHelper.java`（全件／絞り込み結果の選択、取り込み前の確認を含む） |
| 必読 | `io/TaskCsv.java`（タスク ⇔ CSV、行ごとの検証） |
| 必読 | `persistence/TaskImportDao.java`（ID の振り直し） |
| 関連 | `io/CsvCodec.java`（引用符・改行・文字コード） / `io/TaskJson.java` |
| テスト | `TaskCsvTest` / `CsvCodecTest` / `TaskJsonTest` / `TaskDataIoHelperTest` / `TaskDaoIntegrationTest`（import〜） |

**コードの流れ（エクスポート）:**

```
ヘッダーの「エクスポート」をクリック
  → TaskDataIoHelper.exportTasks()
    → 絞り込み中なら「絞り込み結果／全件」を選ぶ（絞り込み結果には親も足す: withAncestors）
    → JFileChooser で保存先と形式（CSV / JSON）を選ぶ
    → TaskCsv.write()（BOM 付き UTF-8・親の直後に子の順）または TaskJson.write()
    → NoticeBar.info() で件数と保存先を表示
```

**コードの流れ（インポート）:**

```
ヘッダーの「インポート」をクリック
  → TaskDataIoHelper.importTasks()
    → JFileChooser でファイルを選ぶ
    → TaskCsv.read()（CSV）または TaskJson.read()（JSON）
      → CsvCodec.decode() で UTF-8 / Shift_JIS を判定 → CsvCodec.parse() で行に分ける
      → 1 行ずつ検証し、駄目な行は RowError（行番号と理由）へ
    → summarize() の内容で確認ダイアログ（追加 / すべて置き換え / キャンセル）
    → 置き換えなら、今のタスクを backups/before-import-日時.csv に書き出してから
    → taskService.importTasks(tasks, replace)
      → TaskDao.importTasks() でファイル内の親子関係を検証（1 件でも不正なら何も書かない）
        → TaskImportDao.importTasks()（1 トランザクション）
          → 置き換えなら自分のタスクを全削除
          → 親から順に INSERT し、振られた ID で子の親 ID を付け替える
      → TaskEventBus.post(REFRESH_ALL)
    → NoticeBar.info() で件数を表示
```

共有プロジェクトを選んでいるときは、`TaskDao` の代わりに `TeamTaskStore.importTasks()` → `TeamProject.importTasks()` を通ります。全部の行をチェックしてから、`TeamChangeLog.appendAll()` で記録ファイルに1回で書きます。

できたチェック:

- [ ] Excel で保存した CSV（Shift_JIS・`2026/10/24` 形式の日付）も読める理由を `CsvCodec.decode` と `TaskCsv.parseDate` で説明できる
- [ ] 取り込み時に ID を振り直す理由と、親を先に入れる理由を言える
- [ ] `TaskImportDao.importTasks()` が replaceExisting で全置換か追加を切り替えていると言える

### 操作O2: 期限超過の見え方を確認する

| 区分 | ファイル |
|------|----------|
| 必読 | `Task.isOverdue()` |
| 必読 | `ui/calendar/CalendarTaskCardFactory.java` または `ui/kanban/KanbanCardFactory.java` |
| テスト | `TaskOverdueTest` |

**コードの流れ（期限超過の判定と表示）:**

```
Task.isOverdue()
  → isCompleted() が true なら false
  → endDate が null なら false
  → endDate.isBefore(LocalDate.now()) で今日より前なら true

カレンダーでタスクカードを表示
  → CalendarTaskCardFactory.create(task, isCompact, actions)
    → task.isOverdue() で期限超過判定
    → 期限超過なら accent = 赤色 (new Color(220, 38, 38))
    → 期限超過ならボーダーを太くする
    → カードの背景色やボーダーに適用

カンバンでタスクカードを表示
  → KanbanCardFactory.create(task, actions)
    → task.isOverdue() で期限超過判定
    → 期限超過なら borderColor = 赤色 (new Color(220, 38, 38))
    → 期限超過ならボーダーを太くする
    → setToolTipText() で「期限を過ぎています」を表示
```

できたチェック:

- [ ] `Task.isOverdue()` が終了日と今日を比較して期限超過を判定していると言える
- [ ] カレンダーとカンバンのカードファクトリーが `isOverdue()` を呼び出して赤色を適用していると言える

### 操作P: ライト／ダーク切替

| 区分 | ファイル |
|------|----------|
| 必読 | `MainFrame` のテーマトグル |
| 必読 | `ui/AppTheme.java` / `ui/AppThemeControls.java` |
| 関連 | `ui/main/MainThemeSupport.java` |

**コードの流れ（テーマ切り替え）:**

```
MainFrame コンストラクタ
  → MainHeaderBar.create() でヘッダー作成
    → themeToggleButton 作成
    → themeToggleButton.addActionListener(e -> toggleTheme())

ユーザーがテーマ切り替えボタンをクリック
  → AppTheme.setDarkMode(toggle.isSelected())
      → setTheme() で PRIMARY / BACKGROUND / TEXT_PRIMARY などを入れ替える
      → applySwingDefaults() で新しく開く窓の既定色も入れ替える
      → Preferences.userNodeForPackage() で設定保存
  → applyThemeToWindow()
    → 各タブに MainThemeSupport.updateViewTheme(component)
      → 背景色・文字色を設定し、各画面の updateTheme() で描き直す
    → MainTabChrome.updateTabTextColors(mainTabbedPane) でタブ色更新
```

色の決まり:

- 画面のファイルに `new Color(...)` を直接書かない。ライトとダークで変わる色は `AppTheme` のメソッドから取る（`cardBackground()`・`controlBackground()`・`textStrong()`・`danger()`・`warning()`・`success()`・`progressTrack()`・`gridLine()`・`sundayText()` / `saturdayText()` など）
- 白い文字を載せる札やボタンの塗りは、ライトとダークで同じ `AppTheme.FILL_*` を使う
- `static final` に色を入れない。テーマを切り替えても変わらなくなるため

できたチェック:

- [ ] `AppTheme.setDarkMode()` が色を入れ替えて Preferences に保存していると言える
- [ ] `MainThemeSupport.updateViewTheme()` がタブの中身のテーマを更新していると言える
- [ ] 色をメソッドで取る理由（テーマを切り替えたときに追いつくため）を言える

---

## 自分で練習する課題（チェックしてから実装）

AI に丸投げせず、**先に直すファイルを自分で書いてから**着手する。

| 課題 | まず開くべき場所 |
|------|------------------|
| 検索プレースホルダの文言を変える | `messages.properties` / `messages_ja.properties` |
| デフォルトタブを変える | `MainFrame` のタブ記憶ロジック |
| 「完了を隠す」フィルタ | `TaskViewFilter` + 既存テストに1ケース |
| 期限超過の色や文言を変える | `Task.isOverdue()` → `CalendarTaskCardFactory` / `KanbanCardFactory` |
| エクスポート確認ダイアログの文言を変える | `TaskDataIoHelper` + `messages*.properties` |

### すでに実装済み（追うだけ・再実装しない）

| 機能 | 読む場所 |
|------|----------|
| 期限超過の強調 | `Task.isOverdue()` / `CalendarTaskCardFactory` / `KanbanCardFactory` |
| 適用中フィルタの表示 | `MainGlobalSearchBar.describeFilters` |
| 絞り込み結果のエクスポート | `TaskDataIoHelper`（全件かフィルタ結果かを確認） |
| Excel 向け CSV の書き出し・取り込み | `TaskCsv` / `CsvCodec` / `TaskImportDao` |
| 保存の失敗を画面に出す | `ErrorDialogUtil.findAppException` |
| 編集の保存は成功したあとだけ知らせる | `WbsPanel.openEditDialog` → `TaskService.updateTask` → `updateTasks` |
| 削除と並べ替えを途中で残さない | `TaskCrudDao.deleteIds` / `updateOrderIndexes` |
| ログインをやめても前のタスクを全部残す | `V6__drop_login.sql` / `DatabaseTest.upgradingFromLoginVersionKeepsEveryAccountsTasks` |
| サンプルの段階名の番号を外し、自分で付けた名前は触らない | `V7__unnumber_sample_stages.sql` / `DatabaseTest.upgradingRemovesNumbersOnlyFromSampleStageNames` |
| チーム共有の土台（教科書16章） | `team/TeamProject` → `TeamChangeLog`（読み書き）→ `TeamReplay`（組み立てと重なりの検出） |
| 共有プロジェクトを作る・参加する・閉じる・表示名 | `ui/team/ProjectSwitcher`（メニュー）→ `ui/team/TeamSession` / `TeamDialogs` / `team/TeamSettings` |
| メンバー・同期・重なりの表示 | `ProjectSwitcher.refresh` ← `TeamSession.Listener.statusChanged` / `TeamMembersDialog` |
| 担当者をメンバーから選ぶ | `TaskDialog` → `TaskService.assigneeOptions` → `TeamTaskStore.assigneeOptions` |
| チームの自動の読み直し | `TeamSession.poll`（5秒ごと）→ `TeamProject.reloadIfChanged` → `TeamChangeLog.fingerprint` → `Listener.dataChanged` → `TaskService.reload` |
| 共有プロジェクトを全タブに出す | `ProjectSwitcher` → `TeamSession.Listener.projectChanged` → `TaskService.useStore(teamSession.storeOf(...))` |
| 「今日」タブと期限の通知を全プロジェクトで出す | `ProjectSources.all` → `TaskSource` の一覧 → `TodayPanel.collect` / `DueReminder.dueAcross` → `DueTasks.merge`（プロジェクトごとに作ってからつなぐ） |
| 「自分の担当だけ」 | `TodayPanel.onlyMine`（切り替えを覚える）→ `TaskSource.isMine`（表示名と担当者を比べる）→ `DueTasks.of(..., include)` |
| 表示名を変えたら担当者も直す | `TeamSession.changeDisplayName` → `askReassign`（件数を数えて聞く）→ `TeamProject.reassign`（1回で書く）/ 比べ方は `Assignees.same` |
| 自分の担当が変わったら通知 | `TeamProject.reload`（届いた記録を取っておく）→ `TeamSession.poll` → `takeNotices` → `TeamNotice.forMe`（担当にした／変えた）→ `MainFrame.noticesArrived` → `DueReminder.showNotices` |
| アプリの版と、新しい版の記録 | `AppVersion.current`（manifest の版）/ `TeamEventCodec.NewerFormatException` → `TeamChangeLog.ReadResult.newerFormatLines` → `ProjectSwitcher.refresh`（「アプリの更新が必要」） |
| 最近のプロジェクトも裏で開いて見張る | `TeamSession.loadOthers` / `poll` → `Listener.otherProjectsChanged` → `ProjectSources.invalidate` + `TaskService.reload` |
| 共有プロジェクトへのまとめての取り込み | `TeamTaskStore.importTasks` → `TeamProject.importTasks`（全部チェックしてから）→ `TeamChangeLog.appendAll`（1回で書く） |
| 共有のタスクを画面の番号で扱う・見ていた版と比べる | `TeamTaskStore`（`idOf` / `Task.revision` → `seenState`）→ `TeamProject.updateTask(id, 変更, 見ていた状態)` |
| WBS で親を変える（深さは並べるときに決める） | `TeamProject.requireMovable` / `TeamReplay.visibleTasks`（`cycleTop` で輪をほどく） |
| 重なった変更の確認と戻す | `TeamSession.showConflicts` → `TeamConflictDialog` → `TeamConflictGroup` / `TeamProject.restoreTask` / `TeamSettings.acknowledge` |
| チームの変更履歴 | `TeamSession.showHistory` → `TeamProject.history` → `TeamHistory.build` → `TeamHistoryDialog` |
| タスクのコメント | `TaskDialog.commentsFor` → `TaskCommentsPanel.submit` → `TeamProject.addComment`（`COMMENT` の記録を足す）→ `TeamReplay.applyComment` → `TeamState.comments` / 編集・削除は `TaskCommentsPanel.startEditing` / `confirmDelete` → `TeamProject.editComment` / `deleteComment`（`canChange` で自分のものか確かめる）→ `TeamReplay.applyCommentChange` / 行の形は `TeamEventCodec.formatOf` / 件数は `TeamTaskStore.toTask`（`Task.comments`）→ `CommentCount` → `KanbanCardFactory` / `WbsTreeCellRenderer` / 通知は `TeamNotice.forMe`（`COMMENTED` / `REPLIED`） |
| 置き場所の初期値（共有ドライブ → マイドライブ） | `TeamSession.defaultLocation` → `team/GoogleDriveFolders` |
| 1台で2人目として起動する | `Main` → `TeamSettings.applyLaunchArguments`（`--team-profile=名前`）→ `TeamSettings.userDefault` |
| 同じ PC で2つ起動しても記録が壊れない | `TeamChangeLog.append`（PC 内のファイルでロック）/ `TeamChangeLogConcurrencyTest` |
| テスト | `TaskOverdueTest` / `MainGlobalSearchBarDescribeTest` / `TaskDaoIntegrationTest` / `TeamProjectTest` / `TeamChangeLogTest` / `TeamSettingsTest` / `TeamTaskStoreTest` / `DueTasksTest` / `DueReminderTest` / `TaskSourceTest` |

---

## レベル5: 面接と、あとから直す練習

操作を一通り触ったあとにやる。新しい機能は足さない。

### 操作Q: 追加の流れを、紙だけで説明する

| 区分 | ファイル |
|------|----------|
| 必読 | 操作Dの矢印（このファイルの上の方） |
| 必読 | `docs/beginner-handbook.md` の「面接で話す順番」 |
| テスト | `TaskDialogUiTest` を開き、どのクラスを呼んでいるかメモする |

できたチェック:

- [ ] 資料を見ずに、3分でアプリの形を説明できる
- [ ] 「画面は保存しない。Service が保存して、Bus が他の画面を更新する」と言える
- [ ] 「ログインはなく、PC の守りは Windows のログインに任せる」と言える

### 操作R: わざと小さい修正をして、戻せるようにする

例: 検索欄の案内文を1語変えて、テストを足さず `messages_ja.properties` だけ変える。そのあと元に戻す。

| 区分 | ファイル |
|------|----------|
| 必読 | `docs/beginner-handbook.md` の「直すときに最初に開くファイル」 |
| 必読 | 変える文言なら `messages_ja.properties` |
| 確認 | `mvn test` が成功すること |

できたチェック:

- [ ] 変える前に、開くファイルを自分で書けた
- [ ] テストの通り方を、コマンドで確認した

---

## 就活・説明用の台本（3分）

次を、このファイルを見ずに言えるようにする。詳しい文は handbook の「面接で話す順番」と同じです。

1. TaskNavi は、自分のパソコンで WBS／カンバン／ガント／カレンダーを切り替えるタスク管理です
2. 画面は入力だけです。保存は `TaskService`、データベースは `TaskDao`、更新の知らせは `TaskEventBus` です
3. 追った操作は「追加」（操作D）です。道は MainFrame → Helper → Dialog → Service → Bus です
4. 保存の失敗は画面に出ます。削除と並べ替えは、途中だけ残りません
5. 状態変更のルールは `TaskService.changeStatus` の1か所にあります。全件の読み込みは1回の変更で1回だけで、5つの画面で使い回します
6. テストは `mvn test` です。CI でも同じテストが動きます
7. アプリにログインはありません。データは PC の中なので、守りは Windows のログインに任せています

---

## 進捗メモ（自由記入）

| 日付 | やった操作 | わかったこと（1行） | まだ曖昧な点 |
|------|------------|-------------------|--------------|
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
