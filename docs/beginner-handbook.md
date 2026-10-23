# TaskNavi 初学者向け教科書

勉強を始める人は、先に **「12. 最初の2週間」** だけを上からやる。  
1〜11章と [study-checklist.md](./study-checklist.md) は辞書です。最初から最後まで読まない。

## 1. このプロジェクトで最初に押さえること
- 画面は Swing で作られている
- レイアウトは MigLayout が中心
- 画面ごとの役割がはっきり分かれている
- UI は `TaskService` 経由でデータを扱い、DB 直接操作は `Dao` に閉じる
- 画面間の同期は `TaskEventBus` で行う

## 2. 全体構成の考え方

フォルダは役割で分かれています。起動の `Main.java` だけ、いちばん上にあります。

- `model` はデータの形（`Task` など）
- `service` は保存やログインの仕事（`TaskService`）
- `persistence` はデータベース（`TaskDao`）
- `ui` は画面。中は WBS、カンバン、ガント、カレンダー、ログインに分かれる
- `event` は画面への知らせ（`TaskEventBus`）

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
### LoginFrame / RegisterFrame / ResetPasswordFrame（`org.tasknavi.ui.auth`）
- 認証画面のため、入力欄とボタン配置が重要
- 認証ロジックは `AuthService`（試行制限付き）
- 見た目の統一は `AppTheme` と `ui.auth.AuthFormWidgets` に寄せる
- 登録入力ポリシーは `ui.auth.AuthPasswordPolicy`

### MainFrame
- アプリ全体の土台
- 上部ヘッダー（ユーザー・インポート/エクスポート・テーマ・ログアウト）とツールバー（検索・フィルタ）
- メイン領域はタブ: WBS / カンバン / ガント / カレンダー
- 最後に開いたタブは Preferences で記憶・次回起動時に復元する
- グローバル検索は各タブの `SearchablePanel` に同期する
- 適用中フィルタの要約表示は `MainGlobalSearchBar.describeFilters`
- JSON エクスポートは全件か絞り込み結果かを選べる（`ui.TaskDataIoHelper`）
- ヘッダー／検索バー／テーマ適用／ツールバーボタン／追加フローは `ui.MainHeaderBar`・`MainGlobalSearchBar`・`MainThemeSupport`・`MainToolbarButtons`・`MainTaskAddHelper` に分割
- タブ装飾は `ui.MainTabChrome`

### WbsPanel / KanbanPanel / GanttPanel / CalendarPanel
- 主要業務画面（いずれも `TaskService` を受け取る）
- 本体はそれぞれ `org.tasknavi.ui.wbs` / `ui.kanban` / `ui.gantt` / `ui.calendar` に配置
- タスク一覧、状態、操作ボタンのつながりを見る
- 表示更新は `refresh` 系と `TaskEventListener` に注目する
- 期限超過は `Task.isOverdue()`（未完了かつ終了日が今日より前）。カレンダー／カンバンのカードで強調
- WBS のツリー構築・フィルタ・DnD・詳細フォーム／登録は同パッケージ内に分割
- カンバンのフィルタ・カード／列振り分けは同パッケージ内に分割
- カレンダーのナビ／月・週ビュー／日別サイド／カードは同パッケージ内に分割
- ガントの描画・ヒット・ドラッグ・サイド列は同パッケージ内に分割
- ステータス前後移動は `util.TaskStatusCycle`

### TaskDialog
- タスク作成・編集の中心（`org.tasknavi.ui.taskdialog.TaskDialog`）
- 種別は `EntryType`（PROJECT / PHASE / TASK）
- 日付入力は `DatePickerDialog` と連動する
- 新規作成の組み立ては `TaskEntryFactory` も参照
- 親選択肢／フォーム／保存検証は同パッケージ内のヘルパーに分割

### DatePickerDialog（`org.tasknavi.ui.DatePickerDialog`）
- 日付選択の再利用部品
- `LocalDate` の加減算と日付ボタン生成が重要

### 検索（`org.tasknavi.ui.MainGlobalSearchBar`）
- 上の検索欄に文字を入れるたびに、今の画面を絞り込む
- キーワードはタスク名に加え担当者にもマッチする（`TaskViewFilter`）
- 各画面は `SearchablePanel.applySearchFilter` を実装する

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
- 本番起動の `TaskService` 組み立ては `TaskService.createDefault()`（UI が `new TaskDao()` しない）
- GitHub Actions（`.github/workflows/ci.yml`）で `mvn test` が走る。ローカルでも同じコマンドで確認する
- 画面土台の回帰は `MainFrameSmokeTest`（タブ切替・検索クリア・テーマ切替）を見る

## 8. ファイルの場所（辞書）

読む順番は12章に従う。ここは「あのクラスはどのファイルか」を探す表。

| やりたいこと | ファイル |
|---|---|
| 起動 | `Main.java` |
| ログイン | `ui/auth/LoginFrame.java` |
| タブと追加ボタン | `ui/MainFrame.java` |
| 追加の中身 | `ui/MainTaskAddHelper.java` |
| 追加と編集 | `service/TaskService.java` の `addTask` / `updateTask` |
| 画面への知らせ | `event/TaskEventBus.java` |
| データの形 | `model/Task.java` |
| WBS | `ui/wbs/WbsPanel.java` |
| データベース | `persistence/TaskDao.java` と `persistence/TaskCrudDao.java` |

## 9. まず覚えるべき設計感覚
- 画面は「表示するだけ」に寄せる
- ユースケースは `TaskService`、永続化は `Dao` に寄せる
- 共通処理は使い回す（`TaskBusinessRules` / `SearchFilterUtil` / `TaskViewFilter` など）
- UI は `AppTheme`、文言は `AppMessages` に統一する
- 確認や選択のダイアログは `AppDialog` を使う。タスク編集と同じ見た目になる
- フォルダは `model` / `service` / `persistence` / `ui`。画面の中は WBS やガントでさらに分かれる

## 10. 辞書の使い方

分からないクラス名が出たら、[study-checklist.md](./study-checklist.md) でその操作（操作A、操作D など）を探す。  
必読と書いてあるファイルだけ開く。関連は、必読が分かってからでよい。

## 11. データが守られている約束

画面は、データベースへ直接書きません。必ず `TaskService` を通ります。

- 保存に成功したときだけ、他の画面へ「変わったよ」と知らせます（`TaskEventBus`）
- 入力が不正なら、1件も書きません。画面には理由が出ます
- 編集の保存は `updateTask` です。中で `updateTasks` を1回呼び、成功したあとだけ他の画面へ知らせます
- 削除は、子タスクも含めて1回で消します。途中で失敗したら、全部元に戻します
- 並べ替えも1回で保存します
- パスワード再設定の番号は、データベースに5分間残ります。アプリを閉じても、その間は有効です
- タスクは、ログインした人のものです。別のアカウントのタスクは、一覧にも検索にも出ません

失敗の文面は `AppException` が持ちます。裏側の処理（`SwingWorker`）が包んでも、`ErrorDialogUtil.findAppException` が取り出して画面に出します。

## 12. 最初の2週間

平日だけ、1日に1個。全部で10日です。ファイルは全部読まない。  
IntelliJ でファイルを開き、`Ctrl+F` で下に書いた言葉を探す。メソッド名を `Ctrl+クリック` すると、その中身に飛べる。

ノートは1行でよい。その日の最後に、下の「今日の終わり」が言えたら終了。  
追加・編集・削除は、別々の保存講座ではありません。道は毎回 `TaskService` のあと `TaskEventBus` です。違うのはメソッド名だけです。

### 1日目: 起動する

アプリを起動する。ログインして、タブが4つあることを見る。  
ログインを飛ばす起動は、実行設定に `-Dtasknavi.skipLogin=true` を付ける。

1. `Main.java` を開く
2. `main` を検索する
3. スプラッシュのあと、ログインを飛ばすか、ログイン画面へ進むことを見つける

今日の終わり: 「起動のあと、ログインした人の画面に進む」と言える。

### 2日目: ログインした人のタスクだけ

ログインしてメイン画面を開く。別のアカウントでは、一覧が違うことを意識する。

1. `ui/auth/LoginFrame.java` を開く
2. `UserSession.login` を検索する
3. その直後の `TaskService.createDefault` を `Ctrl+クリック` する
4. ログイン中のユーザーから台帳を作っていることを見る。今日はここまで

今日の終わり: 「画面は `new TaskDao()` しない。ログインした人の `TaskService` を使う」と言える。

### 3日目: タスクを1件追加する

画面の「追加」を押して、タスクを1件作る。カンバンにも同じ名前が出ることだけ見る。イベントの中身は明日。

1. `ui/MainFrame.java` を開く
2. `addTaskButton` を検索する
3. その行が `MainTaskAddHelper.showAndAdd` を呼んでいることを確認する
4. `showAndAdd` を `Ctrl+クリック` する
5. その中で `TaskDialog` と `taskService.addTask` を見つける

今日の終わり: 「追加ボタンは、ダイアログで入力させて、`TaskService.addTask` に渡している」と言える。

### 4日目: 他の画面が追いつく理由

昨日追加したタスクが、別のタブにも出る理由をコードで見る。

1. `service/TaskService.java` を開く
2. `addTask` を検索する
3. `taskDao.addTask` のあとに、`TaskEventBus` へ知らせている行を見つける
4. `post` を `Ctrl+クリック` して、`TaskEventBus.java` に入る
5. `ui/wbs/WbsPanel.java` で `taskEventListener` を検索し、知らせを受けたら `refreshWbs` していることを見る

ノートにこの1行を書く。

`追加ボタン → MainTaskAddHelper → TaskDialog → TaskService.addTask → TaskEventBus → 各画面`

今日の終わり: この1行を、ノートを見ずに言える。

### 5日目: 編集は同じ道

WBS でタスクを選び、名前を変えて保存する。カンバンの名前も変わることを見る。空の名前では保存できないことも見る。

1. `WbsPanel.java` で `openEditDialog` を検索する
2. 最後の方で `taskService.updateTask` を呼んでいることを見る
3. `service/TaskService.java` の `updateTask` を開く。中で `updateTasks` を呼んでいる
4. `updateTasks` の保存が終わったあとで `TaskEventBus` に知らせていることを見る

今日の終わり: 「編集は追加と同じ道。違うのは `addTask` ではなく `updateTask`。成功したときだけ、他の画面に知らせる」と言える。

### 6日目: 削除

ツールバーの削除で、今のタスクを消す。子がある場合は子も消える、と画面下の確認の帯に書いてあることを読む。「はい」を押すまで消えません。

1. `ui/MainFrame.java` で `deleteButton` を検索する
2. `handleDeleteSelectedItem` に飛ぶ
3. そこから `TaskDeleteHelper` に飛ぶ
4. `service/TaskService.java` の `deleteTask` を開き、消したあとにイベントを出していることを見る

今日の終わり: 「削除も画面が直接消さない。確認のあと Service が消してから知らせる」と言える。

### 7日目: 打ちながら検索

上の検索欄に文字を入れる。今開いている画面だけが、文字のたびに狭くなることを見る。Esc で文字が空になることも見る。

1. `ui/MainGlobalSearchBar.java` を開く
2. `applyToActive` を検索する
3. 文字が変わるたびに、今の画面の `applySearchFilter` を呼んでいることを見る

今日の終わり: 「検索は専用の画面を開かない。上の欄が、今のタブを絞り込む」と言える。

### 8日目: WBS は表示、編集はダイアログ

WBS でタスクを選ぶ。右側は読めるだけで、その場では名前を書き換えられないことを見る。編集ボタンから、5日目と同じダイアログが開く。

1. `ui/wbs/WbsPanel.java` で `openEditDialog` を検索する
2. 詳細側は `WbsDetailForm` だと見る。フォームの中までは読まない
3. 編集ボタンが `openEditDialog` につながっていることだけ確認する

今日の終わり: 「WBS の右側は表示。変更は編集ダイアログから `updateTask`」と言える。

### 9日目: カンバンで状態を変える

カンバンでカードを別の列へ移す。カードの矢印ボタン（← →）でも、ドラッグでもよい。WBS に戻しても、状態が変わっていることを見る。

1. `ui/kanban/KanbanPanel.java` で `changeTaskStatus` を検索する。矢印ボタンはここに来る
2. その中で `taskService.updateTask` を呼んでいる行を見つける
3. 子がいるタスクは、ここで状態を変えないことも、近くの `hasChildren` で見る
4. ドラッグは、すぐ上の `moveTaskToStatus` に来る。中身は同じく `hasChildren` と `updateTask` だと見る

今日の終わり: 「カンバンも自分でデータベースを触らない。状態変更は `updateTask`」と言える。

### 10日目: 声に出して話す

コードは開かない。13章の「面接で話す順番」を一度読む。閉じて、3分で話してみる。

詰まったら、4日目のノートの1行だけを見る。それでも詰まったら、その日に戻る。ガントのドラッグには進まない。

10日が終わってから、カレンダーかガントを1画面だけ見る。見方は9日目と同じです。操作して、そのパネルで `TaskService` を呼んでいる行だけ探す。

分からないときは、ファイル全体を読ませない。次のように聞く。

> 「タスク追加で、MainFrame から TaskEventBus までの呼び出し順を、クラス名だけで教えて」

## 13. 面接で話す順番（3分）

1. TaskNavi は、自分のパソコンで動くタスク管理です。WBS、カンバン、ガント、カレンダーで、同じタスクを見ます
2. 画面は表示と入力だけです。保存は `TaskService`、データベースは `TaskDao`、画面の更新は `TaskEventBus` です
3. 自分が直したところは、保存の失敗が画面下の帯に出ること、選択が残ること、保存は成功したあとだけ他の画面に知らせること、削除と並べ替えを途中で残さないことです
4. テストは `mvn test` です。GitHub Actions でも同じテストが動きます
5. ログインした人ごとに台帳が分かれます。同じパソコンでも、別のアカウントのタスクは見えません

聞かれたら、操作Dの矢印を紙に書いて説明する。

## 14. 直すときに最初に開くファイル

| 直したいこと | 最初に開く |
|---|---|
| ボタンやタブの位置 | `ui/MainFrame` と `ui/MainHeaderBar` など |
| 追加・保存・削除の流れ | `service/TaskService` |
| データベースへの書き方 | `persistence/TaskDao` と `persistence/TaskCrudDao` |
| 入力チェック | `persistence/TaskWriteValidator` と `model/TaskBusinessRules` |
| 画面に出るエラー | `AppException` と `ui/ErrorDialogUtil` |
| WBS の見た目と選択 | `ui/wbs/WbsPanel` |
| 検索の条件 | `util/TaskViewFilter` |
| 文言 | `messages_ja.properties` |
| 壊していないか | `mvn test` |

ルールは1つです。画面から `TaskDao` を直接呼ばない。

## 15. 今はやっていないこと

ここは未完成ではなく、今の版の範囲です。面接では「次にやること」として話します。

- `UserRole`（管理者と一般）を、ボタンの使用可否に使うこと。列としては保存しています
- タスクが何万件になっても軽い読み方にすること。今は更新のたびに全件を読みます。件数が少ないうちはこれで足ります
