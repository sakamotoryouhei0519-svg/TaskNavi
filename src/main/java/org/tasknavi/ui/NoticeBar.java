package org.tasknavi.ui;

import org.tasknavi.AppMessages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * 画面下に短く出す完了・エラー・削除確認。
 * 確認は「はい」を押すまで消えず、押すまで削除などの処理は走らない。
 */
public final class NoticeBar extends JPanel {

    private static final String KEY = "tasknavi.noticeBar";
    private static final int AUTO_HIDE_MS = 4200;

    private final JLabel messageLabel = new JLabel();
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JButton yesButton = new JButton();
    private final JButton noButton = new JButton();
    private final Timer hideTimer;
    private Runnable pendingYes;
    private boolean confirmShowing;

    private NoticeBar() {
        setLayout(new BorderLayout(12, 0));
        setBorder(new EmptyBorder(10, 16, 10, 12));
        messageLabel.setFont(AppTheme.FONT_MAIN);
        actions.setOpaque(false);
        yesButton.addActionListener(e -> {
            Runnable action = pendingYes;
            pendingYes = null;
            dismiss();
            if (action != null) {
                action.run();
            }
        });
        noButton.addActionListener(e -> {
            pendingYes = null;
            dismiss();
        });
        actions.add(yesButton);
        actions.add(noButton);
        add(messageLabel, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
        hideTimer = new Timer(AUTO_HIDE_MS, e -> dismiss());
        hideTimer.setRepeats(false);
    }

    public static void info(Component parent, String message) {
        show(parent, message, Kind.INFO, null);
    }

    public static boolean error(Component parent, String message) {
        return show(parent, message, Kind.ERROR, null);
    }

    /** 帯を出せたとき true。「はい」を押したときだけ onYes を実行する。 */
    public static boolean confirm(Component parent, String message, Runnable onYes) {
        return show(parent, message, Kind.CONFIRM, onYes);
    }

    private static boolean show(Component parent, String message, Kind kind, Runnable onYes) {
        Window window = parent instanceof Window owned ? owned : SwingUtilities.getWindowAncestor(parent);
        if (!(window instanceof RootPaneContainer root)) {
            return false;
        }
        JLayeredPane layered = root.getLayeredPane();
        NoticeBar bar = (NoticeBar) layered.getClientProperty(KEY);
        if (bar == null) {
            bar = new NoticeBar();
            layered.putClientProperty(KEY, bar);
            layered.add(bar, JLayeredPane.POPUP_LAYER);
            NoticeBar placed = bar;
            layered.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    placed.place(layered);
                }
            });
        }
        if (bar.confirmShowing && kind != Kind.CONFIRM) {
            return false;
        }
        bar.present(message == null ? "" : message, kind, onYes);
        bar.place(layered);
        layered.revalidate();
        layered.repaint();
        return true;
    }

    private void present(String message, Kind kind, Runnable onYes) {
        hideTimer.stop();
        pendingYes = kind == Kind.CONFIRM ? onYes : null;
        confirmShowing = kind == Kind.CONFIRM;
        messageLabel.setText(toHtml(message));
        yesButton.setText(AppMessages.get("notice.yes", "はい"));
        noButton.setText(AppMessages.get("notice.no", "いいえ"));
        AppTheme.styleDialogSecondaryButton(yesButton);
        AppTheme.styleDialogSecondaryButton(noButton);
        actions.setVisible(kind == Kind.CONFIRM);
        Color background = kind == Kind.ERROR ? errorBackground() : infoBackground();
        Color foreground = kind == Kind.ERROR ? errorForeground() : infoForeground();
        setBackground(background);
        setForeground(foreground);
        messageLabel.setForeground(foreground);
        setVisible(true);
        if (kind != Kind.CONFIRM) {
            hideTimer.restart();
        }
    }

    private void dismiss() {
        hideTimer.stop();
        confirmShowing = false;
        pendingYes = null;
        setVisible(false);
        if (getParent() instanceof JComponent parent) {
            parent.repaint();
        }
    }

    private void place(JLayeredPane layered) {
        if (!isVisible()) {
            return;
        }
        int width = Math.max(layered.getWidth(), 1);
        setSize(width, 1);
        int height = Math.max(getPreferredSize().height, 44);
        setBounds(0, Math.max(0, layered.getHeight() - height), width, height);
    }

    private static String toHtml(String message) {
        String escaped = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html>" + escaped + "</html>";
    }

    private static Color infoBackground() {
        return AppTheme.isDarkMode() ? new Color(30, 58, 95) : new Color(219, 234, 254);
    }

    private static Color infoForeground() {
        return AppTheme.isDarkMode() ? new Color(226, 232, 240) : new Color(15, 23, 42);
    }

    private static Color errorBackground() {
        return AppTheme.isDarkMode() ? new Color(127, 29, 29) : new Color(254, 226, 226);
    }

    private static Color errorForeground() {
        return AppTheme.isDarkMode() ? new Color(254, 226, 226) : new Color(127, 29, 29);
    }

    private enum Kind {
        INFO, ERROR, CONFIRM
    }
}
