# TaskNavi 学習チェックリスト（操作 → 読むファイル）

このファイルは辞書です。毎日の手順は [beginner-handbook.md](./beginner-handbook.md) の「12. 最初の2週間」です。  
10日が終わるまで、下の操作を自分で選んで進まない。

| 日 | やること | このファイルの場所 |
|---|---|---|
| 1日目 | 起動 | 操作A |
| 2日目 | ログインと自分のタスク | 操作B |
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
| 関連 | `persistence/SampleDataUtil.java`（空DBならサンプル投入） |
| 関連 | `LoginFrame`（`ui/auth/LoginFrame.java`）※ skipLogin なしの場合 |

**コードの流れ（Main.java）:**

```
main()
  → 未処理例外ハンドラ設定
  → SwingUtilities.invokeLater (EDT上で実行)
    → UIManager.setLookAndFeel (Look & Feel設定)
    → applyGlobalUiDefaults() (テーマ適用)
    → createSplashWindow() (スプラッシュ作成)
    → splash.setVisible(true) (表示)
    → Timer(1000ms) で1秒待機
      → splash.dispose() (スプラッシュ消去)
      → shouldSkipLogin() で判定
        → true の場合:
          → autoLoginForTest()
            → UserDao.ensureDemoUser() (テストユーザー作成)
            → SampleDataUtil.prepareLedgerForUser() (サンプルデータ準備)
            → UserSession.login() (セッション設定)
          → openMainFrame()
            → TaskService.createDefault() (サービス作成)
            → new MainFrame() (メイン画面作成)
            → mainFrame.setVisible(true)
        → false の場合:
          → new LoginFrame() (ログイン画面作成)
          → loginFrame.setVisible(true)
```

できたチェック:

- [ ] `main` が何をしているか、3行で言える
- [ ] 「DB準備 → スプラッシュ → ログイン or メイン」の順が言える
- [ ] `shouldSkipLogin()` が `-Dtasknavi.skipLogin=true` で true を返すと言える
- [ ] スプラッシュが1秒後に消えて、ログイン画面かメイン画面が出る流れを言える

### 操作B: ログインしてメイン画面を開く

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/auth/LoginFrame.java` |
| 必読 | `service/AuthService.java` |
| 必読 | `service/TaskService.java` の `createDefault()` |
| 必読 | `ui/MainFrame.java`（コンストラクタ前半: ヘッダー・タブ追加） |
| 関連 | `persistence/UserDao.java` / `service/UserSession.java` |
| 関連 | `ui/MainHeaderBar.java` / `ui/MainToolbarButtons.java` |

**コードの流れ（LoginFrame → MainFrame）:**

```
LoginFrame.handleLogin()
  → 入力バリデーション（空欄チェック）
  → setLoginInProgress(true) (ボタンをビジー状態に)
  → SwingWorker.doInBackground() (バックグラウンドで実行)
    → AuthService.authenticate(username, password)
      → AuthAttemptGuard.loginKey() でキー生成
      → AuthAttemptGuard.isBlocked() でロック確認
      → UserDao.authenticate() (DBで認証)
        → 成功: AuthAttemptGuard.recordSuccess()
        → 失敗: AuthAttemptGuard.recordFailure() → ロック判定
      → 成功なら LoginResult.success(user)
      → 失敗なら LoginResult.invalid() または locked()
    → 成功なら SampleDataUtil.prepareLedgerForUser(userId) (サンプルデータ準備)
  → SwingWorker.done() (EDTで結果処理)
    → result.isSuccess() なら:
      → saveRememberedUsername() (ユーザー名保存)
      → UserSession.login(user) (セッション設定)
      → TaskService.createDefault()
        → UserSession.getCurrentUser() で現在ユーザー取得
        → new TaskDao(userId) (ユーザー専用DAO作成)
        → new TaskService(taskDao)
      → new MainFrame(taskService) (メイン画面作成)
        → ウィンドウ設定
        → MainHeaderBar.create() (ヘッダー作成)
        → createToolBarPanel() (ツールバー作成)
        → new WbsPanel(taskService)
        → new KanbanPanel(taskService)
        → new GanttPanel(taskService)
        → new CalendarPanel(taskService)
        → addMainTab() で4つのタブ追加
        → loadLastTabIndex() で最後のタブを復元
        → addChangeListener() でタブ切り替えリスナー設定
      → mainFrame.setVisible(true)
      → LoginFrame.this.dispose() (ログイン画面を閉じる)
    → 失敗なら:
      → エラーメッセージ表示
      → パスワードフィールドにフォーカス
```

できたチェック:

- [ ] ログイン成功後、誰が `MainFrame` を `new` しているか言える
- [ ] UI が `new TaskDao()` せず `TaskService.createDefault()` を使う理由を言える
- [ ] `AuthService.authenticate()` が `AuthAttemptGuard` で試行制限していると言える
- [ ] `TaskService.createDefault()` が `UserSession.getCurrentUser()` からユーザーIDを取得していると言える

### 操作C: タブを切り替える（WBS / カンバン / ガント / カレンダー）

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/MainFrame.java`（`JTabbedPane` と `addChangeListener`） |
| 必読 | `ui/SearchablePanel.java`（各タブが実装する約束） |
| 関連 | `ui/MainTabChrome.java`（見た目） |
| 関連 | `ui/MainGlobalSearchBar.java` の `syncActive()` |

**コードの流れ（MainFrame タブ切り替え）:**

```
MainFrame コンストラクタ
  → new JTabbedPane() (タブペイン作成)
  → new WbsPanel(taskService)
  → new KanbanPanel(taskService)
  → new GanttPanel(taskService)
  → new CalendarPanel(taskService)
  → addMainTab() で4つのパネルをタブに追加
  → loadLastTabIndex() で前回のタブインデックスを復元
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
      → saveLastTabIndex(mainTabbedPane.getSelectedIndex())
        → Preferences に現在のタブインデックスを保存
    })
  → MainTabChrome.updateTabTextColors(mainTabbedPane) (初期表示時の色設定)

ユーザーがタブをクリック
  → ChangeListener が発火
  → 上記の4つの処理が実行される
```

できたチェック:

- [ ] タブ本体のクラス名を4つ言える
- [ ] 「最後のタブを Preferences で覚える」場所を `MainFrame` 内で指せる
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
| 必読 | `ui/MainFrame.java`（追加ボタンの `ActionListener`） |
| 必読 | `ui/MainTaskAddHelper.java` |
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
          → TaskCrudDao.add(newTask, ownerId)
            → DatabaseUtil.getConnection() でDB接続取得
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

ユーザーがWBSでタスクを選択して「編集」ボタンをクリック
  → WbsDetailForm の編集ボタンがクリックされる
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
            → TaskCrudDao.updateAll(tasks, ownerId)
              → DatabaseUtil.withTransaction() でトランザクション開始
              → 各タスクを UPDATE SQL で更新
              → getAll(connection, ownerId) で全タスク再取得
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
                → TaskCrudDao.deleteIds(targetIds, ownerId)
                  → DatabaseUtil.withTransaction() でトランザクション開始
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
| 必読 | `ui/MainGlobalSearchBar.java` |
| 必読 | `SearchablePanel.applySearchFilter` |
| 必読 | `util/TaskViewFilter.java`（名前 **または担当者**） |
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
              → status が null または task.getStatusCode().equals(status)
            → フィルタ済みタスクでツリー再構築
      → refreshFilterSummary()
        → describeActiveFilters() でフィルタ要約文字列作成
        → filterSummaryLabel に表示
```

できたチェック:

- [ ] なぜ Panel ごとに別ロジックを書かず `TaskViewFilter` に寄せるのか言える
- [ ] `DocumentListener` が入力のたびに `applyToActive()` を呼び出していると言える
- [ ] `TaskViewFilter.matches()` が名前または担当者でのマッチングを行っていると言える

### 操作I: フィルタダイアログ／プロジェクト絞り込み

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/MainGlobalSearchBar.java` |
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
| テスト | `WbsTreeFilterTest` / `WbsFormValidatorTest` |

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
| 関連 | `KanbanCardFactory` / `KanbanColumnSupport` / `KanbanTaskFilter` |
| 関連 | `util/TaskStatusCycle.java` / `ui/TaskStatusChanger.java` / `service/TaskService.java` の `changeStatus` |
| テスト | `KanbanTaskFilterTest` / `TaskServiceStatusChangeTest` |

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
            → TaskCrudDao.updateAll(tasks, ownerId)
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

### 操作M: ガントのバーを見る（ドラッグは後で）

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/gantt/GanttPanel.java`（全体構造だけ） |
| 関連 | `GanttPainter` / `GanttTaskFilter` / `GanttSideViews` |
| 後回し | `GanttMouseController` / `GanttBarDragMath`（複雑） |
| テスト | `GanttTaskFilterTest` / `GanttBarDragMathTest` |

**コードの流れ（ガントチャート表示）:**

```
GanttPanel コンストラクタ
  → visibleStartDate = LocalDate.now().minusDays(14)
  → visibleEndDate = LocalDate.now().plusDays(30)
  → dayWidth = GanttMetrics.DEFAULT_DAY_WIDTH
  → GanttMouseController 作成（ドラッグ操作用）
  → TaskEventBus.getInstance().register(taskEventListener) (イベントリスナー登録)
  → refreshGantt() (初期表示)
    → taskService.getAllTasks() で全タスク取得
    → GanttTaskFilter でフィルタリング
    → GanttSideViews.createTaskNameColumn() でタスク名列作成
    → createDateHeader() で日付ヘッダー作成
    → paintComponent() でガントバー描画
      → GanttPainter.paintBars() で各タスクのバーを描画
      → 開始日・終了日に基づいてバーの位置と長さを計算
      → 進捗率に応じてバーの色を変更
```

できたチェック:

- [ ] `GanttPainter.paintBars()` がタスクの開始日・終了日に基づいてバーを描画していると言える
- [ ] `GanttSideViews` がタスク名列と日付ヘッダーを作成していると言える

---

## レベル4: 認証・入出力・テーマ

### 操作N: 新規登録／パスワード再設定画面を開く

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/auth/RegisterFrame.java` または `ResetPasswordFrame.java` |
| 必読 | `ui/auth/AuthFormWidgets.java` / `AuthPasswordPolicy.java` |
| 関連 | `ui/auth/AuthNavigation.java` |
| 関連 | `service/AuthService.java` / `service/VerificationManager.java` |

**コードの流れ（新規登録）:**

```
LoginFrame で「新規登録」リンクをクリック
  → AuthNavigation.openRegisterAndDispose(loginFrame)
    → new RegisterFrame() (登録画面作成)
    → registerFrame.setVisible(true)
    → loginFrame.dispose()

RegisterFrame コンストラクタ
  → AuthFormWidgets.applyWindowDefaults() でウィンドウ設定
  → ユーザー名、メール、パスワード、確認パスワードのフィールド作成
  → registerButton.addActionListener(e -> handleRegister())

ユーザーが情報を入力して「アカウント作成」をクリック
  → handleRegister()
    → 入力値取得
    → AuthPasswordPolicy.validateRegisterForm() でバリデーション
    → SwingWorker.doInBackground() (バックグラウンドで登録)
      → AuthService.register(username, email, password)
        → UserDao.create() でユーザー作成
        → パスワードハッシュ化して保存
    → SwingWorker.done() (EDTで結果処理)
      → 成功なら AuthNavigation.openLoginAndDispose(this)
      → 失敗なら errorLabel にエラー表示
```

**コードの流れ（パスワード再設定）:**

```
LoginFrame で「パスワードを忘れた」リンクをクリック
  → AuthNavigation.openResetPasswordAndDispose(loginFrame)
    → new ResetPasswordFrame() (再設定画面作成)
    → resetFrame.setVisible(true)
    → loginFrame.dispose()

ユーザーがメールを入力して「認証コード送信」をクリック
  → handleSendAuthCode()
    → VerificationManager.generateAuthCode() で認証コード生成
    → EmailUtil.sendAuthCode() でメール送信（デモではログ出力）

ユーザーが認証コードを入力して「認証」をクリック
  → handleVerifyAuthCode()
    → VerificationManager.verifyAuthCode() で認証コード検証
    → emailVerified = true
    → newPasswordField.setEnabled(true)
    → resetButton.setEnabled(true)

ユーザーが新パスワードを入力して「パスワード更新」をクリック
  → handleResetPassword()
    → AuthPasswordPolicy.validatePassword() でパスワードバリデーション
    → SwingWorker.doInBackground()
      → AuthService.resetPassword(email, newPassword)
        → UserDao.updatePassword() でパスワード更新
    → SwingWorker.done()
      → 成功なら AuthNavigation.openLoginAndDispose(this)
```

できたチェック:

- [ ] `AuthNavigation` がログイン画面と登録画面の切り替えを行っていると言える
- [ ] `AuthPasswordPolicy` がパスワードのバリデーションを行っていると言える

### 操作O: インポート／エクスポート

| 区分 | ファイル |
|------|----------|
| 必読 | `ui/TaskDataIoHelper.java`（全件／絞り込み結果の選択を含む） |
| 必読 | `persistence/TaskImportDao.java` |
| 関連 | `util/JsonTaskSerializer.java` / `JsonTaskDeserializer.java` |
| テスト | `TaskImportDaoTest` / `JsonTaskSerializerTest` |

**コードの流れ（エクスポート）:**

```
MainFrame で「エクスポート」メニューを選択
  → TaskDataIoHelper.exportTasks()
    → JFileChooser で保存先ファイルを選択
    → taskService.getAllTasks() で全タスク取得
    → JsonTaskSerializer.serialize(tasks) でJSONシリアライズ
    → Files.write() でファイル書き込み
    → NoticeBar.info() で「エクスポートしました」を表示
```

**コードの流れ（インポート）:**

```
MainFrame で「インポート」メニューを選択
  → TaskDataIoHelper.importTasks()
    → JFileChooser でインポートファイルを選択
    → Files.readAllBytes() でファイル読み込み
    → JsonTaskDeserializer.deserialize() でJSONデシリアライズ
    → 置換確認ダイアログ表示（既存データを上書きするか）
    → SwingWorker.doInBackground()
      → taskDao.importTasks(tasks, replaceExisting)
        → TaskImportDao.importTasks()
          → replaceExisting なら全タスク削除
          → 各タスクを TaskCrudDao.add() で追加
          → 親子関係を考慮して順序を調整
    → SwingWorker.done()
      → TaskEventBus.post(REFRESH_ALL)
      → NoticeBar.info() で「インポートしました」を表示
```

できたチェック:

- [ ] `JsonTaskSerializer` がタスクリストをJSONに変換していると言える
- [ ] `TaskImportDao.importTasks()` がreplaceExistingフラグで全置換か追加を制御していると言える

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
| 関連 | `ui/MainThemeSupport.java` |

**コードの流れ（テーマ切り替え）:**

```
MainFrame コンストラクタ
  → MainHeaderBar.create() でヘッダー作成
    → themeToggleButton 作成
    → themeToggleButton.addActionListener(e -> toggleTheme())

ユーザーがテーマ切り替えボタンをクリック
  → toggleTheme()
    → AppTheme.toggleDarkMode()
      → darkMode = !darkMode (フラグ反転)
      → Preferences.userNodeForPackage() で設定保存
    → MainThemeSupport.updateViewTheme(getActiveComponent())
      → AppTheme.applyTheme(component) でテーマ適用
        → Look & Feel を更新
        → 背景色、前景色、フォントなどを設定
        → component.revalidate() で再レイアウト
        → component.repaint() で再描画
    → MainTabChrome.updateTabTextColors(mainTabbedPane) でタブ色更新
```

できたチェック:

- [ ] `AppTheme.toggleDarkMode()` がフラグを反転してPreferencesに保存していると言える
- [ ] `MainThemeSupport.updateViewTheme()` がアクティブなコンポーネントのテーマを更新していると言える

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
| 絞り込み結果の JSON エクスポート | `TaskDataIoHelper`（全件かフィルタ結果かを確認） |
| 保存の失敗を画面に出す | `ErrorDialogUtil.findAppException` |
| 編集の保存は成功したあとだけ知らせる | `WbsPanel.openEditDialog` → `TaskService.updateTask` → `updateTasks` |
| 削除と並べ替えを途中で残さない | `TaskCrudDao.deleteIds` / `updateOrderIndexes` |
| 再設定コードを DB に残す | `VerificationManager` / `V3__verification_codes.sql` |
| テスト | `TaskOverdueTest` / `MainGlobalSearchBarDescribeTest` / `TaskDaoIntegrationTest` |

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
- [ ] 「ログインした人のタスクだけが見える」と言える

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
5. 状態変更のルールは `TaskService.changeStatus` の1か所にあります。全件の読み込みは1回の変更で1回だけで、4つの画面で使い回します
6. テストは `mvn test` です。CI でも同じテストが動きます
7. ログインした人ごとに台帳が分かれます。別のアカウントのタスクは見えません

---

## 進捗メモ（自由記入）

| 日付 | やった操作 | わかったこと（1行） | まだ曖昧な点 |
|------|------------|-------------------|--------------|
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
