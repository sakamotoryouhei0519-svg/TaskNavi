package org.tasknavi.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * ダイアログを、中身が最初から全部見える大きさに合わせる。
 */
public final class DialogFit {

    private DialogFit() {
    }

    public static void showAll(Window window) {
        if (window == null) {
            return;
        }
        window.pack();
        Dimension packed = window.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(packed.width + 24, screen.width - 48);
        int height = Math.min(packed.height + 16, screen.height - 48);
        Dimension size = new Dimension(Math.max(width, 1), Math.max(height, 1));
        window.setMinimumSize(size);
        window.setSize(size);
        window.setLocationRelativeTo(window.getOwner());
    }
}
