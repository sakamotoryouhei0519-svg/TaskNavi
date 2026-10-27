package org.tasknavi.ui;

import org.tasknavi.AppException;
import org.tasknavi.AppMessages;

import javax.swing.*;
import java.awt.*;

/**
 * エラーを画面に出す。メイン画面ではお知らせ帯（{@link NoticeBar}）、それ以外では窓で出す。
 */
public final class ErrorDialogUtil {
    private ErrorDialogUtil() {
    }

    public static void showError(Component parent, String message) {
        if (!NoticeBar.error(parent, message)) {
            JOptionPane.showMessageDialog(parent, message, AppMessages.get("common.dialog.error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showError(Component parent, Throwable throwable) {
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
