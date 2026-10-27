package org.tasknavi.ui;

import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * 空の入力欄に、何を書く所かを薄い文字で出す。入力欄の paintComponent の最後で呼ぶ。
 * フォーカスがあっても空なら出す（書き始めるまで案内が見えるように）。
 */
public final class InputHint {

    private InputHint() {
    }

    public static void paint(JTextComponent field, Graphics g, String hint) {
        if (hint == null || hint.isEmpty() || field.getDocument().getLength() > 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(AppTheme.TEXT_MUTED);
            g2.setFont(field.getFont().deriveFont(Font.ITALIC));
            FontMetrics metrics = g2.getFontMetrics();
            Insets insets = field.getInsets();
            int y = field instanceof JTextArea
                    ? insets.top + metrics.getAscent()
                    : (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(hint, insets.left + 2, y);
        } finally {
            g2.dispose();
        }
    }
}
