// packageとは、このファイルがどのグループ（パッケージ）に属しているかを宣言するもの
package org.tasknavi.ui;

import org.tasknavi.AppException;
import org.tasknavi.AppMessages;

// importとは、他のファイルやライブラリから機能を借りてくること
import javax.swing.*;
import java.awt.*;

// public final class ErrorDialogUtilとは、ErrorDialogUtilというクラスを宣言していること
// finalとは、このクラスを継承できないようにすること
public final class ErrorDialogUtil {
    // private ErrorDialogUtil()とは、プライベートなコンストラクタ
    // 外部からインスタンスを作れないようにする（ユーティリティクラスのパターン）
    private ErrorDialogUtil() {
    }

    // public static voidとは、どこからでも呼び出せる戻り値がない静的メソッド
    // Componentとは、画面の部品（ボタン、パネルなど）の親クラス
    public static void showError(Component parent, String message) {
        if (!NoticeBar.error(parent, message)) {
            JOptionPane.showMessageDialog(parent, message, AppMessages.get("common.dialog.error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    // public static voidとは、どこからでも呼び出せる戻り値がない静的メソッド
    // Throwableとは、Javaの全てのエラーと例外の親クラス
    public static void showError(Component parent, Throwable throwable) {
        // throwableがnullでないかつメッセージが空でなければ、そのメッセージを使う
        // そうでなければデフォルトのエラーメッセージを使う
        // && は論理積（AND）で、両方の条件がtrueの時にtrueになる
        // ! は否定（NOT）で、条件を反転させる
        // isBlank()とは、文字列が空か空白のみかを確認すること
        String message = throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()
                ? throwable.getMessage()
                : AppMessages.get("common.dialog.error.message");
        showError(parent, message);
    }

    /**
     * SwingWorker が包んだ例外から、利用者向けの {@link AppException} を取り出す。
     */
    public static AppException findAppException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AppException appException) {
                return appException;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * バックグラウンド処理の失敗を表示する。業務エラーは利用者向け文面、それ以外は fallback を使う。
     */
    public static void showBackgroundError(Component parent, Throwable throwable, String fallbackMessage) {
        AppException appException = findAppException(throwable);
        if (appException != null) {
            showError(parent, appException.getUserMessage());
            return;
        }
        showError(parent, fallbackMessage);
    }
}
