package org.tasknavi.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * 細い角丸のつまみだけのスクロールバー。色は描くたびにテーマから取るので、ライトとダークを切り替えてもそのまま合う。
 */
public final class ModernScrollBarUI extends BasicScrollBarUI {

    private static final int THICKNESS = 12;
    private static final int PAD = 3;

    public static ComponentUI createUI(JComponent c) {
        return new ModernScrollBarUI();
    }

    /** {@code UIManager} は名前でクラスを探すので、exec:java のような別のクラスローダーでも見つかるよう Class も渡す。 */
    static void install() {
        javax.swing.UIManager.put("ScrollBarUI", ModernScrollBarUI.class.getName());
        javax.swing.UIManager.put(ModernScrollBarUI.class.getName(), ModernScrollBarUI.class);
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
        scrollbar.setBorder(null);
    }

    @Override
    protected void configureScrollBarColors() {
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return noButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return noButton();
    }

    private static JButton noButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        button.setFocusable(false);
        return button;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
                ? new Dimension(THICKNESS, 48)
                : new Dimension(48, THICKNESS);
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(32, 32);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor());
            int w = Math.max(1, bounds.width - PAD * 2);
            int h = Math.max(1, bounds.height - PAD * 2);
            int arc = Math.min(w, h);
            g2.fillRoundRect(bounds.x + PAD, bounds.y + PAD, w, h, arc, arc);
        } finally {
            g2.dispose();
        }
    }

    private Color thumbColor() {
        boolean active = isDragging || isThumbRollover();
        if (AppTheme.isDarkMode()) {
            return active ? new Color(148, 163, 184) : new Color(100, 116, 139);
        }
        return active ? new Color(100, 116, 139) : new Color(148, 163, 184);
    }
}
