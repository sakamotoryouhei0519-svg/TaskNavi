package org.example.ui.auth;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * 認証画面間の遷移。
 */
public final class AuthNavigation {

    private AuthNavigation() {
    }

    public static void openLoginAndDispose(JFrame current) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
            if (current != null) {
                current.dispose();
            }
        });
    }
}

