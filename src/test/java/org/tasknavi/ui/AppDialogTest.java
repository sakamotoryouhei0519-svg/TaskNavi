package org.tasknavi.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class AppDialogTest {

    @Test
    void buttonsShareTaskDialogSizeAndReportTheirIndex() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppDialog.Built built = AppDialog.build(null, "title", AppDialog.messageBody("message"),
                    "絞り込み結果 (3件)", "全件 (10件)", "キャンセル");
            JButton[] buttons = built.buttons();

            assertEquals(3, buttons.length);
            assertEquals(36, buttons[0].getPreferredSize().height);
            assertEquals(buttons[0].getPreferredSize(), buttons[2].getPreferredSize());
            assertSame(buttons[0], built.dialog().getRootPane().getDefaultButton());
            assertEquals(-1, built.choice()[0]);

            buttons[1].doClick();

            assertEquals(1, built.choice()[0]);
            assertFalse(built.dialog().isDisplayable());
        });
    }

    @Test
    void escapeClosesWithoutChoosing() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AppDialog.Built built = AppDialog.build(null, "title", AppDialog.messageBody("message"), "はい", "いいえ");
            KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

            built.dialog().getRootPane()
                    .getActionForKeyStroke(escape)
                    .actionPerformed(new ActionEvent(built.dialog(), ActionEvent.ACTION_PERFORMED, "esc"));

            assertEquals(-1, built.choice()[0]);
            assertFalse(built.dialog().isDisplayable());
        });
    }

    @Test
    void messageLinesBecomeSeparateLabels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel body = AppDialog.messageBody("1行目\n2行目");

            assertEquals(2, body.getComponentCount());
            assertEquals("1行目", ((JLabel) body.getComponent(0)).getText());
            assertEquals("2行目", ((JLabel) body.getComponent(1)).getText());
            assertFalse(body.isOpaque());
        });
    }
}
