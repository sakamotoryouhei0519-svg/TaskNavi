// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
package org.tasknavi;

// public enum UserRoleとは、UserRoleという列挙型を宣言していること
// enumとは、決まった値の集合を定義する型　曜日など
// ユーザーの権限（管理者・一般ユーザー）を定義しています
public enum
UserRole {
    ADMIN("ADMIN"),
    USER("USER");

    // private finalとは、この列挙型内でだけ使えて変更できない値を宣言すること
    private final String value;

    // ==========================================
    //  【Enum のコンストラクタ】
    // ==========================================
    // ① UserRole: クラス名・Enum名と完全に同じ名前（コンストラクタである証拠）
    //    - (String value): 外からデータを受け取る受け取り窓口（引数）
    //    ※【注意】Enumのコンストラクタは外から new で生成できないよう、
    //      public をつけず自動的に private 扱いにするのがルールです。
    // ② this.value: 今作ろうとしている UserRole オブジェクト自身のフィールド（変数）
    // ③ = value;: 引数で渡された右側の「生データ」を、左側の this.value に書き写して代入
    UserRole(String value) {
        this.value = value;
    }

    // public Stringとは、文字列を返すメソッド
    public String getValue() {
        return value;
    }

    // ==========================================
    //  【文字列から Enum を検索・取得するメソッド】
    // ==========================================
    // 【役割】画面やDBから渡された文字列（"ADMIN" など）を、安全な UserRole 列挙型に変換します。
    //
    // ① public static UserRole fromString(String value):
    //    - static: インスタンス化せずに `UserRole.fromString(...)` の形で直接呼び出せるメソッド
    //    - UserRole: 戻り値として、合致した UserRole 型の定数（またはデフォルト値）を返します
    //
    // ② if (value == null):
    //    - 渡された文字列が null の場合、NullPointerException エラーを防ぐため、
    //      安全な初期値（デフォルト値）として USER を返して即座に終了します
    //
    // ③ for (UserRole role : values()):
    //    - values(): UserRole に定義されている全ての選択肢（ADMIN, USER, GUEST など）を配列で取得
    //    - for-each: 全ての選択肢を1つずつ取り出して `role` 変数に代入し、順番にチェックします
    //
    // ④ if (role.value.equalsIgnoreCase(value.trim())):
    //    - value.trim(): 渡された文字列の前後に含まれる不要な空白（スペース）を削除して整形
    //    - equalsIgnoreCase: 大文字・小文字を無視して文字列が一致するか比較（例: "admin" も "ADMIN" も同等と判定）
    //    - 一致した場合は、その `role`（UserRole型）を返して検索を終了します
    //
    // ⑤ return USER;:
    //    - ループを最後まで回しても一致する選択肢が見つからなかった場合（不正な文字列など）、
    //      安全のためのフォールバック（代用値）としてデフォルトの USER を返します
    public static UserRole fromString(String value) {
        if (value == null) {
            return USER;
        }
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(value.trim())) {
                return role;
            }
        }
        return USER;
    }

    // public booleanとは、真偽値（true/false）を返すメソッド
    public boolean isAdmin() {
        // this == ADMINとは、現在の列挙定数がADMINかどうかを確認すること
        return this == ADMIN;
    }

    // public static booleanとは、真偽値を返す静的メソッド
    public static boolean isAdminRole(String value) {
        return fromString(value).isAdmin();
    }
}
