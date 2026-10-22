package org.tasknavi.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/**
 * ダイアログ共通のキー操作。Enter は既定ボタン、Esc はキャンセル。
 */
public final class DialogKeys {

    private DialogKeys() {
    }

    public static void install(JDialog dialog, JButton defaultButton, Runnable onCancel) {
        if (dialog == null) {
            return;
        }
        if (defaultButton != null) {
            dialog.getRootPane().setDefaultButton(defaultButton);
        }
        if (onCancel == null) {
            return;
        }
        dialog.getRootPane().registerKeyboardAction(
                e -> onCancel.run(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
