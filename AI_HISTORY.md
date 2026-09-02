# AI History

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
