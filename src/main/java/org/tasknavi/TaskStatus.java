// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
package org.tasknavi;

// public enum TaskStatusとは、TaskStatusという列挙型を宣言していること
// enumとは、決まった値の集合を定義する型（例：曜日、月、状態など）
// タスクの状態（未着手・進行中・完了）を定義しています
public enum TaskStatus {
    // 列挙定数（定義された値）をカンマで区切って列挙
    // "未着手"はコンストラクタに渡す引数
    NOT_STARTED("未着手"),
    IN_PROGRESS("進行中"),
    COMPLETED("完了");

    // private finalとは、この列挙型内でだけ使えて変更できない値を宣言すること
    // Stringとは、文字列を扱う型
    private final String label;

    // TaskStatusとは、enumのコンストラクタ（列挙定数を作る時に実行される初期化処理）
    TaskStatus(String label) {
        // this.label = labelとは、引数で受け取ったlabelをフィールドのlabelに代入すること
        this.label = label;
    }

    // public Stringとは、文字列を返すメソッド
    public String getLabel() {
        return label;
    }

    // public static TaskStatusとは、TaskStatus型を返す静的メソッド
    // staticとは、インスタンスを作らなくても呼び出せるメソッド
    public static TaskStatus fromString(String value) {
        // valueがnullまたは空白なら「未着手」を返す
        if (value == null || value.trim().isEmpty()) {
            return NOT_STARTED;
        }

        // trim()とは、文字列の前後の空白を削除すること
        String normalized = value.trim();
        // for-eachとは、列挙型の全ての値を1つずつ取り出して繰り返すこと
        // values()とは、列挙型の全ての定数を配列で取得すること
        for (TaskStatus status : values()) {
            // equalsとは、文字列が等しいかを確認すること
            // name()とは、列挙定数の名前（NOT_STARTEDなど）を取得すること
            // equalsIgnoreCaseとは、大文字小文字を区別せずに比較すること
            if (status.label.equals(normalized) || status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        // 一致するものがなければ「未着手」を返す
        return NOT_STARTED;
    }
}
