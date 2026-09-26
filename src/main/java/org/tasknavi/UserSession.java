package org.tasknavi;

/**
 * 【ユーザーセッション管理クラス】
 * ログイン中のユーザー情報を保持するシングルトンクラスです。
 *
 * 【重要単語の解説】
 * - セッション: ユーザーがログインしてからログアウトするまでの期間
 * - static: クラス全体で共有される変数・メソッド
 * - シングルトン: アプリ全体で1つのインスタンスのみを持つ設計
 *
 * 【コードの読み方】
 * 1. static変数で現在のユーザーを保持
 * 2. login()メソッドでユーザーを設定
 * 3. logout()メソッドでユーザーをクリア
 * 4. getCurrentUser()で現在のユーザーを取得
 * 5. isLoggedIn()でログイン状態を判定
 */
public class UserSession {

    private static final Object SESSION_LOCK = new Object();
    private static final UserDao userDao = new UserDao();
    // ==========================================
    //  【フィールド】
    // ==========================================
    private static volatile User currentUser = null;

    public static void login(User user) {
       synchronized (SESSION_LOCK) {
           currentUser = user;
       }
    }

    public static void login(String username) {
       if (username == null || username.trim().isEmpty()) {
           logout();
           return;
       }
        User user = userDao.findByUsername(username);
        login(user);
    }

    public static void logout() {
       synchronized (SESSION_LOCK) {
           currentUser = null;
       }
    }

    public static User getCurrentUser() {
       synchronized (SESSION_LOCK) {
           return currentUser;
       }
    }

    public static String getCurrentUsername() {
       User user = getCurrentUser();
       return user != null ? user.getUsername() : null;
    }

    public static boolean isLoggedIn() {
       synchronized (SESSION_LOCK) {
           return currentUser != null;
       }
    }
}