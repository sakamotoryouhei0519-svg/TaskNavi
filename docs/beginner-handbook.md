# TaskNavi 初学者向け教科書

勉強を始める人は、先に **「12. 最初の2週間」** だけを上からやる。  
1〜11章と [study-checklist.md](./study-checklist.md) は辞書です。最初から最後まで読まない。

## 1. このプロジェクトで最初に押さえること
- 画面は Swing で作っています。Java に最初から入っている画面の部品です
- 部品の並べ方は、ほとんど MigLayout で決めています
- 画面ごとに、受け持つ仕事が分かれています
- 画面はデータを `TaskService` に頼みます。データベースを触るのは `Dao` だけです
- ある画面で変えたことは、`TaskEventBus` が他の画面に知らせます

## 2. 全体構成の考え方

フォルダは役割で分かれています。起動の `Main.java` だけ、いちばん上にあります。

- `model` はデータの形（`Task` など）
- `service` は保存やログインの仕事（`TaskService`）
- `persistence` はデータベース（`TaskDao`）
- `ui` は画面。中は WBS、カンバン、ガント、カレンダー、ログインに分かれる
- `event` は画面への知らせ（`TaskEventBus`）

## 3. よく出てくる名前
- `JFrame` はウィンドウ、`JDialog` は小さな窓、`JPanel` は部品を入れる箱
- `MigLayout` は部品の並べ方
- `ActionListener` はボタンを押したときの処理
- `LocalDate` は日付
- `TaskService` は保存の窓口、`TaskDao` はデータベース
- `persistence` の中の `TaskRowMapper` / `TaskHierarchySupport` / `TaskSearchDao` / `TaskCrudDao` / `TaskImportDao` は、データベースの読み書きを分担しています
- `TaskEventBus` は他の画面への知らせ
- `AppTheme` は色、`AppMessages` は画面の文言
- `IconManager` はアイコン
- `SearchablePanel` は、上の検索欄で絞り込める画面の印

## 4. MigLayout の見方
- `fill`: 使える場所いっぱいに広げる
- `insets`: 外側の余白
- `gap`: 部品と部品のすき間
- `wrap`: そこで次の行に行く
- `grow`: 余った場所に合わせて伸びる
- `align right/left/center`: 右・左・真ん中に寄せる

## 5. 画面ごとの重要ポイント
### LoginFrame / RegisterFrame / ResetPasswordFrame（`ui/auth`）
- ログイン、新規登録、パスワード再設定の画面です
- ログインの判定は `AuthService` です。何度も間違えると、しばらく試せなくなります
- 見た目は `AppTheme` と `AuthFormWidgets` でそろえています
- 登録できるパスワードの決まりは `AuthPasswordPolicy` です

### MainFrame
- アプリ全体の土台です
- いちばん上の帯（ヘッダー）に、ユーザー名・インポート／エクスポート・テーマ・ログアウトがあります。その下の帯（ツールバー）に、検索と絞り込みがあります
- 真ん中はタブです: WBS / カンバン / ガント / カレンダー
- 最後に開いたタブを覚えていて、次に起動したときもそのタブから始まります（`Preferences`）
- 上の検索欄の文字は、各タブの `SearchablePanel` に渡ります
- 今かかっている絞り込みの説明文は、`MainGlobalSearchBar.describeFilters` が作ります
- JSON のエクスポートでは、全件か、絞り込んだ分だけかを選べます（`TaskDataIoHelper`）
- MainFrame が長くならないよう、部品ごとに別のファイルにしています
  - ヘッダー: `MainHeaderBar`
  - 検索欄: `MainGlobalSearchBar`
  - テーマの切り替え: `MainThemeSupport`
  - ツールバーのボタン: `MainToolbarButtons`
  - 追加の流れ: `MainTaskAddHelper`
  - タブの見た目: `MainTabChrome`

### WbsPanel / KanbanPanel / GanttPanel / CalendarPanel
- 仕事の中心になる4つの画面です。どれも `TaskService` を受け取ります
- 置き場所は `ui/wbs`、`ui/kanban`、`ui/gantt`、`ui/calendar` です
- 見るところは、タスクの一覧、状態、操作ボタンのつながりです
- 画面の描き直しは、`refresh` で始まるメソッドと `TaskEventListener` を見ます
- 期限切れは `Task.isOverdue()` です。終わっていなくて、終了日が今日より前なら期限切れです。カレンダーとカンバンのカードで目立たせます
- どの画面も、1つのファイルに全部は書いていません。同じフォルダの中で分けています
  - WBS: ツリーの組み立て、絞り込み、ドラッグでの並べ替え、右側の詳細、登録
  - カンバン: 絞り込み、カード、列への振り分け
  - カレンダー: 月や週の切り替え、月表示と週表示、日ごとの横の一覧、カード
  - ガント: 線の描画、クリックした場所の判定、ドラッグ、横の列
- 状態を1つ前・1つ後ろへ動かすのは `util/TaskStatusCycle` です

### TaskDialog
- タスクを作るときと直すときの窓です（`ui/taskdialog/TaskDialog`）
- 種類は `EntryType` で、プロジェクト・フェーズ・タスクの3つです
- 日付の欄は `DatePickerDialog` と一緒に動きます
- 新しく作るタスクの中身は `TaskEntryFactory` も見ます
- 親の選択肢、入力欄、保存前のチェックは、同じフォルダの別ファイルにしています

### DatePickerDialog（`ui/DatePickerDialog`）
- 日付を選ぶ小さな窓です。いろいろな画面で使い回します
- 見るところは、`LocalDate` で月を進めたり戻したりするところと、日付のボタンを作るところです

### 検索（`ui/MainGlobalSearchBar`）
- 上の検索欄に1文字入れるたびに、今の画面を絞り込みます
- タスク名だけでなく、担当者の名前でも見つかります（`TaskViewFilter`）
- 各画面は `SearchablePanel` の `applySearchFilter` を持っています

## 6. よく見る書き方
- `getSelectedItem()` で、コンボボックスで選ばれている項目を取る
- `setText` で入力欄に文字を入れ、`getText` で取り出す
- `addActionListener` で、ボタンを押したときの処理を登録する
- `setVisible(true)` で窓を出す
- `dispose()` で窓を閉じる
- `LocalDate.parse()` で文字を日付に変える。変えられなければ、日付の書き方がおかしい
- 状態などの画面の文字は `AppMessages` から取ります。データベースには `NOT_STARTED` のような英語のコードで保存します

## 7. まちがえやすいところ
- `null` かどうかを確かめずに使うと、アプリが落ちます
- `JComboBox` の中の値（コード）と、画面に出す文字がずれると、おかしな動きになります
- `wrap` を忘れると、部品が横にずっと並びます
- ダークモードでは、背景の色と文字の色の両方を確かめます
- 画面から `TaskDao` を呼ばない。必ず `TaskService` を通します
- 本番の `TaskService` は `TaskService.createDefault()` で作ります。画面が `new TaskDao()` することはありません
- GitHub Actions（`.github/workflows/ci.yml`）で `mvn test` が動きます。自分のパソコンでも同じコマンドで確かめます
- 画面の土台を壊していないかは `MainFrameSmokeTest` で確かめます。タブの切り替え、検索欄を空にすること、テーマの切り替えを試しています

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

## 9. 設計の決まり
- 画面は、見せることと入力を受けることだけにします
- 「追加する」「消す」などの仕事は `TaskService`、データベースへの読み書きは `Dao` に置きます
- 何度も使う処理は1か所にまとめます（`TaskBusinessRules` / `SearchFilterUtil` / `TaskViewFilter` など）
- 色は `AppTheme`、文言は `AppMessages` から取ります
- 確認や選択の窓は `AppDialog` を使います。タスク編集の窓と同じ見た目になります
- フォルダは `model` / `service` / `persistence` / `ui` です。画面のフォルダは、WBS やガントごとにさらに分かれます

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
- 読み込んだ全件は、次の書き込みまで `TaskService` が使い回します。書き込みは必ず `TaskService` を通るので、古いままにはなりません。ツールバーの更新ボタンで読み直せます

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

1. `ui/kanban/KanbanPanel.java` で `changeTaskStatus` を検索する。矢印ボタンもドラッグも、ここに来る
2. `TaskStatusChanger.change` を `Ctrl+クリック` し、裏側で `taskService.changeStatus` を呼んでいることを見る
3. `service/TaskService.java` の `changeStatus` を開く。子がいれば `hasChildren` で止め、いなければ `updateTask` で保存している
4. 子がいるタスクを動かすと、画面下の帯に理由が出ることを見る

今日の終わり: 「カンバンも自分でデータベースを触らない。状態変更は `TaskService.changeStatus` で、中は `updateTask`」と言える。

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
4. 状態を変えるルールは、カンバンとカレンダーの3か所にあったものを、`TaskService.changeStatus` の1か所に集めました。全件の読み込みも、1回の変更につき1回にしました
5. テストは `mvn test` です。GitHub Actions でも同じテストが動きます
6. ログインした人ごとに台帳が分かれます。同じパソコンでも、別のアカウントのタスクは見えません

聞かれたら、操作Dの矢印を紙に書いて説明する。4番の続きを聞かれたら、11章の最後の約束（読み込みの使い回し）を話す。

### 聞かれやすい質問

1文で答えて、続きを聞かれたら話す。

- **なぜ Swing ですか**: Java に最初から入っていて、他に何も入れずに画面を作れるからです。1台のパソコンで動くアプリなので、Web にする必要がありませんでした。保存中に画面が固まらないよう、裏側で動かす（`SwingWorker`）ことも入れています
- **なぜ SQLite ですか**: データベースが1つのファイルで済み、サーバーを立てなくてよいからです。表の形を変えるときは Flyway を使い、`V1` から `V4` まで順番に当てています
- **`UserRole` を保存しているのに、使っていないのはなぜですか**: 今は登録した人を全員「一般」で保存しています。どのボタンを管理者だけにするかを決めてから、画面に使う予定です。15章の「次にやること」に入れています

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
- タスクが何万件になっても軽い読み方にすること。今は変更のたびに全件を1回読み直し、4つの画面で使い回します。件数が少ないうちはこれで足ります
