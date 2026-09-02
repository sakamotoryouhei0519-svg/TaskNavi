package org.example;

import javax.swing.*;
import java.awt.*;

public final class ErrorDialogUtil {
    private ErrorDialogUtil() {
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    public static void showError(Component parent, Throwable throwable) {
        String message = throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()
                ? throwable.getMessage()
                : "処理中にエラーが発生しました。";
        showError(parent, message);
    }
}
