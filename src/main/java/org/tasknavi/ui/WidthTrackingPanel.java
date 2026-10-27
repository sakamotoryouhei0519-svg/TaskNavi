package org.tasknavi.ui;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * JScrollPane の中で、横幅だけを見える幅に合わせるパネル。
 * 中身は縦にだけスクロールし、横にはみ出さない。
 */
public class WidthTrackingPanel extends JPanel implements Scrollable {

    private static final int UNIT_INCREMENT = 16;

    public WidthTrackingPanel(LayoutManager layout) {
        super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(UNIT_INCREMENT, visibleRect.height - UNIT_INCREMENT);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport
                && viewport.getHeight() > getPreferredSize().height;
    }
}
