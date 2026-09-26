package org.tasknavi.ui;

import org.tasknavi.AppTheme;
import org.tasknavi.IconManager;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * MainFrame ツールバー用ボタン生成。
 */
public final class MainToolbarButtons {

    private MainToolbarButtons() {
    }

    public static JButton create(String text, IconManager.IconType iconType, Color bg, Color fg, Color borderColor) {
        Object icon = IconManager.getIcon(iconType);
        JButton btn;
        if (icon instanceof ImageIcon) {
            btn = new AppTheme.RoundedFillButton(text, 8);
            btn.setIcon((ImageIcon) icon);
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        } else {
            btn = new AppTheme.RoundedFillButton(icon + "  " + text, 8);
        }
        btn.putClientProperty("app.button.variant", "toolbar");
        btn.setFont(AppTheme.FONT_HEADER);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(true);
        btn.setBorder(AppTheme.createRoundedBorder(borderColor, 8, 2, 2, 2, 2));
        btn.setMargin(new Insets(4, 12, 4, 12));
        btn.setRolloverEnabled(true);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setPreferredSize(new Dimension(
                Math.max(100, btn.getFontMetrics(btn.getFont()).stringWidth(btn.getText()) + 36), 36));
        btn.setMinimumSize(new Dimension(100, 36));
        return btn;
    }
}
