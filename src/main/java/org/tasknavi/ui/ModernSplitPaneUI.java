package org.tasknavi.ui;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Graphics;

/**
 * 仕切りを背景と同じ色にして、真ん中に細い線を1本だけ引く分割ペイン。Metal の明るい仕切りがダークで浮かないようにする。
 */
public final class ModernSplitPaneUI extends BasicSplitPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new ModernSplitPaneUI();
    }

    static void install() {
        javax.swing.UIManager.put("SplitPaneUI", ModernSplitPaneUI.class.getName());
        javax.swing.UIManager.put(ModernSplitPaneUI.class.getName(), ModernSplitPaneUI.class);
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        splitPane.setBorder(null);
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this) {
            @Override
            public void paint(Graphics g) {
                g.setColor(AppTheme.BACKGROUND);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(AppTheme.BORDER_COLOR);
                if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                    g.fillRect(getWidth() / 2, 0, 1, getHeight());
                } else {
                    g.fillRect(0, getHeight() / 2, getWidth(), 1);
                }
            }
        };
    }
}
