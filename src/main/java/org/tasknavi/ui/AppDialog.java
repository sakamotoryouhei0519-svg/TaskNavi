package org.tasknavi.ui;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;

/**
 * タスク編集ダイアログと同じ見た目の、確認・選択・入力用ダイアログ。
 */
public final class AppDialog {

    private AppDialog() {
    }

    record Built(JDialog dialog, JButton[] buttons, int[] choice) {
    }

    /** 「はい」「いいえ」で聞く。「はい」のときだけ true。 */
    public static boolean confirm(Component parent, String title, String message) {
        return choose(parent, title, message,
                AppMessages.get("notice.yes", "はい"),
                AppMessages.get("notice.no", "いいえ")) == 0;
    }

    /**
     * @return 押したボタンの番号。Esc や × で閉じたときは -1
     */
    public static int choose(Component parent, String title, String message, String... options) {
        return showAndWait(build(parent, title, messageBody(message), options));
    }

    /** 入力部品を並べたパネルを、OK／キャンセル付きで出す。OK のときだけ true。 */
    public static boolean showForm(Component parent, String title, JComponent content) {
        return showAndWait(build(parent, title, content,
                AppMessages.get("common.button.ok", "OK"),
                AppMessages.get("common.button.cancel", "キャンセル"))) == 0;
    }

    static Built build(Component parent, String title, JComponent body, String... options) {
        JDialog dialog = new JDialog(ownerOf(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new MigLayout("fill, insets 0, gap 0", "[grow]", "[grow][]"));
        dialog.setBackground(AppTheme.BACKGROUND);

        JPanel bodyPanel = new JPanel(new MigLayout("fill, insets 15 20 15 20", "[grow,fill]", "[grow]"));
        bodyPanel.setBackground(AppTheme.PANEL_BG);
        bodyPanel.add(body, "grow");

        int[] choice = {-1};
        JButton[] buttons = new JButton[options.length];
        int textWidth = 0;
        for (int i = 0; i < options.length; i++) {
            JButton button = new JButton(options[i]);
            AppTheme.styleDialogSecondaryButton(button);
            textWidth = Math.max(textWidth, button.getFontMetrics(button.getFont()).stringWidth(options[i]));
            int index = i;
            button.addActionListener(e -> {
                choice[0] = index;
                dialog.dispose();
            });
            buttons[i] = button;
        }

        JPanel buttonPanel = new JPanel(new MigLayout("insets 10, gap 10, align right"));
        buttonPanel.setBackground(AppTheme.BACKGROUND);
        Dimension buttonSize = new Dimension(textWidth + 36, 36);
        for (JButton button : buttons) {
            button.setPreferredSize(buttonSize);
            button.setMinimumSize(buttonSize);
            buttonPanel.add(button);
        }

        dialog.add(bodyPanel, "grow, wrap");
        dialog.add(buttonPanel, "grow");
        DialogFit.showAll(dialog);
        DialogKeys.install(dialog, buttons.length > 0 ? buttons[0] : null, dialog::dispose);
        return new Built(dialog, buttons, choice);
    }

    static JPanel messageBody(String message) {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 4, wrap", "[grow,fill]"));
        panel.setOpaque(false);
        String text = message == null ? "" : message;
        for (String line : text.split("\n", -1)) {
            JLabel label = new JLabel(line.isEmpty() ? " " : line);
            label.setFont(AppTheme.FONT_MAIN);
            label.setForeground(AppTheme.TEXT_PRIMARY);
            panel.add(label);
        }
        return panel;
    }

    private static int showAndWait(Built built) {
        built.dialog().setVisible(true);
        return built.choice()[0];
    }

    private static Window ownerOf(Component parent) {
        if (parent == null) {
            return null;
        }
        if (parent instanceof Window window) {
            return window;
        }
        return SwingUtilities.getWindowAncestor(parent);
    }
}
