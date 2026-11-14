# TaskNavi 設計と技術の早見表

全体の形・技術・ファイルの場所を1枚で見るための表です。理由や細かい動きは書かず、詳しい場所へのリンクにしています。

- 勉強の順番: [beginner-handbook.md](./beginner-handbook.md) の「12. 最初の2週間」
- 操作ごとの呼び出しの流れ: [study-checklist.md](./study-checklist.md)
- なぜこの作りにしたか・分かっている限界: [README](../README.md)

## 1. 技術スタック

版は `pom.xml` のものです。部品の版は Dependabot が毎週知らせるので、ここの数字より新しいことがあります。迷ったら `pom.xml` を見ます。

| 領域 | 使っているもの | 版 | 何に使うか |
|---|---|---|---|
| 言語 | Java | 25（`maven.compiler.release`） | LTS の最新。exe に Java ごと入れて配るため |
| 画面 | Swing | JDK に同梱 | 画面の部品 |
| 並べ方 | MigLayout（`miglayout-swing`） | 11.5.2 | 画面の部品の配置 |
| データベース | SQLite（`sqlite-jdbc`） | 3.53.4.0 | 自分だけのタスク（`tasknavi.db`） |
| 表の更新 | Flyway（`flyway-core`） | 13.11.1 | `V1`〜`V7` を順に当てる |
| JSON | Gson | 2.15.0 | JSON の書き出し・取り込み、共有の記録 |
| ログ | SLF4J + Logback | 2.0.20 / 1.6.6 | `%APPDATA%\TaskNavi\logs` |
| ビルド | Maven | CI は 3.9.6 | `mvn verify` |
| テスト | JUnit Jupiter | 6.1.3 | `src/test/java` |
| 網羅率 | JaCoCo | 0.8.16 | 割合が下がると CI が止まる |
| 配布 | jpackage + maven-assembly-plugin | JDK 25 | `TaskNavi.exe` と zip |
| CI | GitHub Actions / Qodana / Dependabot | — | `.github/` の下 |

アプリの版は `pom.xml` の `app.version`（今は 1.2.0）です。Maven の `version`（`1.0-SNAPSHOT`）とは別です。

## 2. 全体の形

```
Main（起動）
  └─ ui（画面。表示と入力だけ）
       │  読み書きは必ず TaskService を通す
       ▼
     service.TaskService（追加・編集・削除・状態の変更。全件の読み込みを使い回す）
       │                                   │ 成功したあとだけ
       ▼                                   ▼
     persistence.TaskStore（保存先の約束）   event.TaskEventBus → 各画面が描き直す
       ├─ persistence.TaskDao       自分だけのタスク（SQLite）
       └─ team.TeamTaskStore        共有プロジェクト（Google ドライブの共有フォルダ）
```

### パッケージの依存の向き

矢印の先だけを使います。下の層は `ui` を知りません。

| パッケージ | 使うもの |
|---|---|
| `model` | なし |
| `event` | `model` |
| `util` / `io` | `model`、`AppMessages` |
| `persistence` | `model`、`AppException`、`AppMessages` |
| `service` | `model`、`event`、`persistence` |
| `team` | `model`、`persistence`（`TaskStore`）、`AppException`、`AppMessages`、`AppVersion` |
| `ui` | 全部。ただし `persistence` は `TaskStore`（型として）と `AppDataLocation`（フォルダの場所）だけ |

`team` は `service` を使いません。`TeamTaskStore` が `TaskStore` を実装するだけで、`TaskService` から見ると `TaskDao` と同じに見えます。

## 3. パッケージとクラス

`src/main/java/org/tasknavi` の下です。全部は書いていません。入口になるクラスだけです。

### いちばん上

| クラス | 役割 |
|---|---|
| `Main` | 起動。スプラッシュを出している間にデータベースを準備 → `MainFrame`。どこでも受けなかった例外の受け口（`handleUncaught`） |
| `AppException` | 画面に出す失敗の文面を持つ例外 |
| `AppMessages` | 画面の文言（`messages*.properties`） |
| `AppVersion` | アプリの版（JAR の manifest から読む） |

### `model`（データの形）

| クラス | 役割 |
|---|---|
| `Task` | タスク1件。プロジェクト・フェーズ・タスクのどれもこれ |
| `TaskStatus` / `Priority` / `EntryType` | 状態・優先度・種類（プロジェクト／フェーズ／タスク） |
| `TaskBusinessRules` | 状態と進捗率の関係 |
| `TaskHierarchyUtil` | 子がいるかなど、階層の小さな判定 |
| `Assignees` | 担当者名の比べ方（前後の空白と大文字・小文字を無視） |

### `service`（仕事の窓口）

| クラス | 役割 |
|---|---|
| `TaskService` | 画面が使う唯一の窓口。`addTask` / `updateTask(s)` / `deleteTask` / `restoreDeleted` / `changeStatus` / `importTasks` / `useStore` / `reload` |
| `DueTasks` | 期限切れ・今日まで・7日以内の振り分け（「今日」タブと通知で共通） |
| `TaskSource` | 「今日」タブで使う、プロジェクトごとの `TaskService` と名前の組 |

### `persistence`（自分だけのタスクの保存）

| クラス | 役割 |
|---|---|
| `TaskStore` | 保存先の約束（インターフェース） |
| `TaskDao` | `TaskStore` の SQLite 版。入力チェックと親の進捗の計算をしてから下に渡す |
| `TaskCrudDao` | 追加・更新・削除の SQL。複数件はトランザクションで1回に |
| `TaskImportDao` | 取り込み。ID を振り直し、親を先に保存する |
| `TaskRowMapper` / `TaskStatementBinder` | 行 ⇔ `Task` の変換 |
| `TaskHierarchySupport` | 子孫の収集、親の進捗の計算、階層の正しさ |
| `TaskWriteValidator` | 書く前の入力チェック |
| `Database` | 接続、Flyway、起動時の準備、トランザクション |
| `DatabaseBackup` | 1日1回のコピー（7日分） |
| `AppDataLocation` | データ・バックアップ・ログのフォルダ |
| `SampleDataUtil` | データベースを初めて作ったときだけ見本を入れる |
| `LogDirectoryDefiner` | Logback にログのフォルダを教える |

### `team`（共有プロジェクトの記録）

| クラス | 役割 |
|---|---|
| `TeamProject` | 入口。作る・開く・追加・変更・削除・読み直し |
| `TeamTaskStore` | `TaskStore` の共有版。UUID の ID を画面用の int に振る |
| `TeamEvent` / `TeamField` / `TeamEventCodec` | 記録の1行・項目・JSON への変換と形の番号 |
| `TeamChangeLog` | 自分のファイルへの追記と、全員分の読み込み |
| `TeamReplay` / `TeamState` | 記録を順に当てはめて今の一覧を作る |
| `TeamConflict` / `TeamConflictGroup` | 重なった変更 |
| `TeamHistory` | 変更履歴 |
| `TeamComment` / `TeamNotice` | コメントと、自分あての通知 |
| `TeamMembers` / `TeamMember` / `TeamProjectFile` | `members` フォルダと `project.json` |
| `TeamTaskRules` / `TeamImport` | 書く前の確かめと、まとめての取り込み |
| `TeamFiles` | 一時ファイルからの付け替え、読む大きさの上限 |
| `TeamSettings` | 端末 ID・表示名・最近のプロジェクト（Windows の設定に保存） |
| `GoogleDriveFolders` | 作る場所の初期値を探す |

### `io` / `util` / `event`

| クラス | 役割 |
|---|---|
| `io/TaskCsv` / `io/CsvCodec` | CSV の読み書き（BOM 付き UTF-8 で書き、UTF-8 と Shift_JIS を読む） |
| `io/TaskJson` | JSON の読み書き |
| `util/TaskViewFilter` | 検索と状態の絞り込み（全タブで共通） |
| `util/ProjectMemberFilter` | プロジェクトでの絞り込み |
| `util/TaskStatusCycle` | 状態を1つ前・後ろへ |
| `util/SearchFilterUtil` | 絞り込みの小さな共通処理 |
| `event/TaskEventBus` / `TaskEvent` / `TaskEventListener` | 画面への知らせ |

### `ui`（画面）

| 場所 | 入口 | 中身 |
|---|---|---|
| `ui/main` | `MainFrame` | ヘッダー、ツールバー、検索欄、タブ、テーマ、追加の流れ、`ProjectSources` |
| `ui/today` | `TodayPanel` | 「今日」タブ。`DueReminder` が Windows の通知を出す |
| `ui/wbs` | `WbsPanel` | ツリーの組み立て・絞り込み・ドラッグ・右側の詳細 |
| `ui/kanban` | `KanbanPanel` | 列・カード・担当者で分ける |
| `ui/gantt` | `GanttPanel` | 描画（`GanttPainter`）・マウス・ドラッグ・横の列 |
| `ui/calendar` | `CalendarPanel` | 月・週の表示、日ごとの一覧 |
| `ui/taskdialog` | `TaskDialog` | 作る・直す窓。入力欄・親の選択・保存前のチェック |
| `ui/team` | `TeamSession` | 開いている共有プロジェクトの係。`ProjectSwitcher`（左上のボタン）、各ダイアログ、`TeamBackground`（裏側の列）、`TeamPoll`（5秒ごと） |
| `ui` 直下 | — | どの画面でも使う部品。`AppTheme`（色と書体）、`AppDialog`、`NoticeBar`（下の帯）、`ErrorDialogUtil`、`SearchablePanel`、`TaskStatusChanger`、`TaskEditHelper`、`TaskDeleteHelper`、`TaskDataIoHelper`、`DatePickerDialog` など |

## 4. データの形

### `Task`

| 項目 | 型 | 中身 |
|---|---|---|
| `id` | int | 自分だけのタスクは DB の番号。共有は `TeamTaskStore` が振る番号 |
| `parentId` | Integer | 親。プロジェクトは null |
| `level` | int | 1 プロジェクト / 2 フェーズ / 3 タスク。親の1つ下だけ |
| `orderIndex` | int | 兄弟の中での順番 |
| `name` / `assignee` / `memo` | String | 名前・担当者・メモ（複数行） |
| `startDate` / `endDate` | LocalDate | 開始日・終了日（無くてもよい） |
| `progress` | int | 0〜100 |
| `status` | `TaskStatus` | `NOT_STARTED` / `IN_PROGRESS` / `COMPLETED` |
| `priority` | `Priority` | `HIGH` / `MEDIUM` / `LOW` |
| `revision` | long | 共有のみ。読んだときの版（重なりの判定用） |
| `comments` | List | 共有のみ。件数と検索のためだけ。保存しない |

決まり:

- 状態を変えると進捗も合わせる。未着手は 0、完了は 100、進行中は今の値（0 か 100 なら 50）（`TaskBusinessRules.resolveProgress`）
- 子がいるタスクの進捗は子の平均。子が全部完了なら完了、平均が 0 より大きければ進行中（`TaskHierarchySupport.rollupParentProgress`）。子がいるタスクの状態は直接変えられない（`TaskService.changeStatus` が `HAS_CHILDREN` を返す）
- 期限切れは「完了していなくて、終了日が今日より前」（`Task.isOverdue`）

### SQLite（`tasknavi.db`）

今の表は `tasks` の1つだけです。

| 列 | 型 | 初期値 |
|---|---|---|
| `id` | INTEGER PRIMARY KEY AUTOINCREMENT | |
| `parent_id` | INTEGER | |
| `level` | INTEGER | 1 |
| `order_index` | INTEGER | 0 |
| `name` / `assignee` / `memo` | TEXT | |
| `start_date` / `end_date` | TEXT（`2026-10-24` の形） | |
| `progress` | INTEGER | 0 |
| `status` | TEXT（英語のコード） | |
| `priority` | TEXT（英語のコード） | |

索引は `parent_id`・`status`・`assignee`。

| 更新 | したこと |
|---|---|
| `V1__create_tables.sql` | `users` と `tasks` を作る |
| `V2__codify_status_priority.sql` | 状態・優先度を日本語から英語のコードに |
| `V3__verification_codes.sql` | パスワード再設定コードの表（V6 で消した） |
| `V4__task_owner.sql` | タスクの持ち主の列（V6 で消した） |
| `V5__task_memo.sql` | `memo` 列 |
| `V6__drop_login.sql` | ログインをやめた。`users`・`verification_codes`・`owner_id` を外す |
| `V7__unnumber_sample_stages.sql` | 見本のフェーズ名の「1. 」などを外す |

表を変えるときは `V8__...sql` を足します。当てたあとのファイルは書き換えません。

### 共有プロジェクトの記録

```
プロジェクトのフォルダ\
   ├─ project.json            プロジェクト名
   ├─ members\<端末ID>.json   表示名とアプリの版（その PC だけが書く）
   └─ changes\<端末ID>.jsonl  変更の記録（その PC だけが1行ずつ足す）
```

記録の1行は `TeamEvent` です。

| 項目 | 中身 |
|---|---|
| `eventId` / `taskId` | 記録とタスクの ID（UUID） |
| `op` | `CREATE` / `UPDATE` / `DELETE` / `RESTORE` / `COMMENT` / `COMMENT_EDIT` / `COMMENT_DELETE` |
| `clock` | 並べる順。見た記録の最大 + 1。PC の時計は使わない |
| `deviceId` / `author` / `at` | 書いた PC・表示名・時刻 |
| `values` | 変えた項目（`TeamField`: parent, level, order, name, assignee, start, end, status, priority, progress, memo, comment） |
| `base` | 項目ごとに「見ていた値を書いた記録の ID」。今の値とずれたら重なり |
| `baseEventId` | コメントの編集・削除で、どのコメントかを指す |

並べる順は `clock` → `deviceId` → `eventId` です（`TeamEvent.ORDER`）。どの PC でも同じ順になります。詳しくは handbook 16章。

## 5. スレッド

| どこで | 何をする |
|---|---|
| EDT（Swing の画面の係） | 画面の作成と描き直し。`TaskEventBus.post` は、EDT 以外から呼ばれても `invokeLater` で EDT に渡す |
| `SwingWorker` | 保存・削除・状態の変更。結果は `done()` で受け、失敗は `ErrorDialogUtil.showBackgroundError` が画面とログに出す |
| `team-io`（`TeamBackground`） | 共有フォルダの読み書きを1本の列に並べる。書き込みの途中に読み直しが割り込まない |
| 5秒ごと（`TeamPoll`） | 共有フォルダのファイルの大きさと時刻を見て、変わっていたら読み直す |

`TaskService` の全件の読み込み（スナップショット）は `snapshotLock` で守り、渡すたびに複製します。

## 6. ファイルの置き場所

| もの | 場所 |
|---|---|
| データベース | `%APPDATA%\TaskNavi\tasknavi.db` |
| バックアップ | `%APPDATA%\TaskNavi\backups`（7日分。取り込みで置き換える前の CSV も） |
| ログ | `%APPDATA%\TaskNavi\logs`（14日分） |
| 共有プロジェクト | 自分で選んだ Google ドライブのフォルダ |
| 端末 ID・表示名など | Windows の設定（Java の Preferences。`TeamSettings`） |

データのフォルダは `-Dtasknavi.dataDir` → 環境変数 `TASKNAVI_DATA_DIR` → `%APPDATA%\TaskNavi` → `~/.tasknavi` の順で決まります（`AppDataLocation`）。テストは `target/test-data` を使い、本物のデータには触れません。

## 7. ビルド・テスト・配布

| やること | コマンド |
|---|---|
| テストと網羅率の確認 | `mvn -B verify`（表は `target/site/jacoco/index.html`） |
| テストだけ | `mvn test` |
| 動かす | `mvn compile exec:java` |
| 依存込みの JAR | `mvn -B -DskipTests package` |
| exe と zip | `mvn -Pdist -DskipTests verify`（JDK 25 でないと止まる） |

- テストは `src/test/java` に、`src/main/java` と同じパッケージで置きます。テストは日本語に固定して動かします（`pom.xml` の `argLine`）
- 網羅率がこれを下回ると `mvn verify` が止まります: 全体 60%、`team` 90%、`ui/team` 25%
- CI（`.github/workflows/ci.yml`）: push と PR で、Ubuntu の Xvfb 上で `mvn -B verify`。網羅率の表を Summary に出し、HTML を `coverage-report` として14日残す。失敗したテストは注釈で出す
- Qodana（`.github/workflows/qodana_code_quality.yml`）: push と PR でコードの検査
- Dependabot（`.github/dependabot.yml`）: Maven の部品は毎週、GitHub Actions は毎月
- 開発用の道具は `src/test/java/org/tasknavi/tools`: `ScreenshotTool`（README の写真）、`TestDataTool`（試す専用のデータ）、`GanttBenchmark`（ガントの速さ）。動かし方は README の「開発メモ」
- 配布物: `target/jpackage/dist/TaskNavi/TaskNavi.exe` と `target/TaskNavi-<app.version>-windows.zip`。アイコンと zip の中身は `src/main/packaging`

## 8. 変えるときの決まり

詳しくは handbook の 7章・9章・11章です。

- 画面から `TaskDao` や共有フォルダを直接触らない。`TaskService` を通す
- 保存に成功したあとだけ `TaskEventBus` で知らせる
- 何件かまとめて書くときは、1件でもだめなら何も書かない
- 色は `AppTheme`、書体は `AppTheme.FONT_*`、文言は `AppMessages`。キーは `messages.properties`・`messages_ja.properties`・`messages_en.properties` の3つに足す
- 表を変えるときは Flyway の `V8__...` を足す。共有の記録の形を変えるときは `TeamEventCodec.FORMAT` を上げる
- 使わなくなったコードは消す。クラスが大きくなったら役割ごとに分ける
