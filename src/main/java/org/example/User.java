// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
package org.example;

// importとは、他のファイルやライブラリから機能を借りてくること
// LocalDateTimeとは、日付と時刻を扱うための型
import java.time.LocalDateTime;

// public class Userとは、Userというクラスを宣言していること
// クラスとは、ユーザー情報を管理する設計図のようなもの
public class User {
    // private finalとは、このクラス内でだけ使えて変更できない値を宣言すること
    // intとは、整数を扱う型
    private final int id;
    // Stringとは、文字列を扱う型
    private final String username;
    private final String email;
    private final String role;
    private final String displayName;
    // LocalDateTimeとは、日付と時刻を扱う型
    private final LocalDateTime lastLoginAt;

    // public Userとは、コンストラクタ（クラスを作る時に実行される初期化処理）のこと
    public User(int id, String username, String email, String role, String displayName, LocalDateTime lastLoginAt) {
        // this.id = idとは、引数で受け取ったidをフィールドのidに代入すること
        // thisとは、現在のオブジェクト自身を指すキーワード
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        // displayNameがnullでないかつ空白でなければdisplayName、そうでなければusernameを使う
        // && は論理積（AND）で、両方の条件がtrueの時にtrueになる
        // ! は否定（NOT）で、条件を反転させる
        // trim()とは、文字列の前後の空白を削除すること
        // isEmpty()とは、文字列が空かどうかを確認すること
        this.displayName = displayName != null && !displayName.trim().isEmpty() ? displayName : username;
        this.lastLoginAt = lastLoginAt;
    }

    // public intとは、整数を返すメソッド
    public int getId() {
        return id;
    }

    // public Stringとは、文字列を返すメソッド
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    // displayNameがnullならusernameを返すメソッド
    public String getDisplayNameOrUsername() {
        return displayName != null ? displayName : username;
    }

    // public LocalDateTimeとは、日付と時刻を返すメソッド
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
