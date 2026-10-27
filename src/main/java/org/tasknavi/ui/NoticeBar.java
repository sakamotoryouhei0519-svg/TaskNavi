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
 * 画面下に短く出す完了・エラー・削除確認・取り消し。
 * 確認は「はい」を押すまで消えず、押すまで削除などの処理は走らない。
 * 取り消しのボタンは、帯が出ている間だけ押せる。
 */
public final class NoticeBar extends JPanel {

    private static final String KEY = "tasknavi.noticeBar";
    private static final int AUTO_HIDE_MS = 4200;
    private static final int ACTION_HIDE_MS = 10000;

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
        show(parent, message, Kind.INFO, null, null);
    }

    public static boolean error(Component parent, String message) {
        return show(parent, message, Kind.ERROR, null, null);
    }

    /** 帯を出せたとき true。「はい」を押したときだけ onYes を実行する。 */
    public static boolean confirm(Component parent, String message, Runnable onYes) {
        return show(parent, message, Kind.CONFIRM, onYes, null);
    }

    /** ボタンを1つ付けた完了の帯。帯が消えたあとは onAction は呼ばれない。 */
    public static boolean infoWithAction(Component parent, String message, String actionLabel, Runnable onAction) {
        return show(parent, message, Kind.ACTION, onAction, actionLabel);
    }

    private static boolean show(Component parent, String message, Kind kind, Runnable onYes, String actionLabel) {
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
        bar.present(message == null ? "" : message, kind, onYes, actionLabel);
        bar.place(layered);
        layered.revalidate();
        layered.repaint();
        return true;
    }

    private void present(String message, Kind kind, Runnable onYes, String actionLabel) {
        hideTimer.stop();
        boolean hasButton = kind == Kind.CONFIRM || kind == Kind.ACTION;
        pendingYes = hasButton ? onYes : null;
        confirmShowing = kind == Kind.CONFIRM;
        messageLabel.setText(toHtml(message));
        yesButton.setText(kind == Kind.ACTION ? actionLabel : AppMessages.get("notice.yes", "はい"));
        noButton.setText(AppMessages.get("notice.no", "いいえ"));
        AppTheme.styleDialogSecondaryButton(yesButton);
        AppTheme.styleDialogSecondaryButton(noButton);
        noButton.setVisible(kind == Kind.CONFIRM);
        actions.setVisible(hasButton);
        Color background = kind == Kind.ERROR ? AppTheme.errorBackground() : AppTheme.infoBackground();
        Color foreground = kind == Kind.ERROR ? AppTheme.errorForeground() : AppTheme.infoForeground();
        setBackground(background);
        setForeground(foreground);
        messageLabel.setForeground(foreground);
        setVisible(true);
        if (kind != Kind.CONFIRM) {
            hideTimer.setInitialDelay(kind == Kind.ACTION ? ACTION_HIDE_MS : AUTO_HIDE_MS);
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

    private enum Kind {
        INFO, ERROR, CONFIRM, ACTION
    }
}
