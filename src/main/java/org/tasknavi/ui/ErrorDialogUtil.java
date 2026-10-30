package org.tasknavi.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppException;
import org.tasknavi.AppMessages;

import javax.swing.*;
import java.awt.*;

/**
 * エラーを画面に出す。メイン画面ではお知らせ帯（{@link NoticeBar}）、それ以外では窓で出す。
 */
public final class ErrorDialogUtil {
    private static final Logger logger = LoggerFactory.getLogger(ErrorDialogUtil.class);

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
     * 操作の失敗を表示し、ログに残す。業務エラー（入力の誤りなど）は利用者向け文面を出して info に、
     * それ以外（想定外の失敗）は fallback を出して、調べられるようにスタックトレースごと error に残す。
     */
    public static void showBackgroundError(Component parent, Throwable throwable, String fallbackMessage) {
        AppException appException = findAppException(throwable);
        if (appException != null) {
            logger.info("操作を受け付けませんでした: {}", appException.getUserMessage());
            showError(parent, appException.getUserMessage());
            return;
        }
        logger.error(fallbackMessage, throwable);
        showError(parent, fallbackMessage);
    }
}
