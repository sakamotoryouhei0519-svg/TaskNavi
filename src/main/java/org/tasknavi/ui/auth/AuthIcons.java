package org.tasknavi.ui.auth;

import org.tasknavi.ui.AppTheme;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/**
 * 認証画面用の描画アイコン。絵文字は環境で見た目が変わるので、テーマ色で自前描画する。
 */
final class AuthIcons {

    private AuthIcons() {
    }

    /** パスワード表示切替。{@code crossed} が true なら斜線入り（＝隠す）。 */
    static Icon eye(boolean crossed) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.translate(x, y);
                    g2.setColor(c != null && !c.isEnabled() ? AppTheme.BORDER_COLOR : AppTheme.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D outline = new Path2D.Double();
                    outline.moveTo(1, 9);
                    outline.quadTo(9, 1, 17, 9);
                    outline.quadTo(9, 17, 1, 9);
                    g2.draw(outline);
                    g2.drawOval(6, 6, 6, 6);
                    if (crossed) {
                        g2.drawLine(3, 16, 15, 2);
                    }
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return 18;
            }

            @Override
            public int getIconHeight() {
                return 18;
            }
        };
    }

    /** チェックボックス。選択状態はボタンのモデルから読む。 */
    static Icon checkBox() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                boolean selected = c instanceof AbstractButton button && button.isSelected();
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.translate(x, y);
                    if (selected) {
                        g2.setColor(AppTheme.PRIMARY);
                        g2.fillRoundRect(0, 0, 16, 16, 6, 6);
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawPolyline(new int[]{4, 7, 12}, new int[]{8, 11, 5}, 3);
                    } else {
                        g2.setColor(AppTheme.getCurrentTheme().textFieldBackground());
                        g2.fillRoundRect(0, 0, 16, 16, 6, 6);
                        g2.setColor(AppTheme.BORDER_COLOR);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(1, 1, 14, 14, 5, 5);
                    }
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }
}
