package org.tasknavi.ui;

import org.tasknavi.AppTheme;
import org.tasknavi.IconManager;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * MainFrame タブのアイコン描画とタブ UI 装飾。
 */
public final class MainTabChrome {

    private MainTabChrome() {
    }

    public static Icon createTabIcon(IconManager.IconType type, Color color) {
        return new TabIcon(type, color);
    }

    public static void updateTabTextColors(JTabbedPane tabs) {
        if (tabs == null) {
            return;
        }
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Color color = i == selected ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED;
            tabs.setForegroundAt(i, color);
            Icon icon = tabs.getIconAt(i);
            if (icon instanceof TabIcon tabIcon) {
                tabIcon.setColor(color);
            }
        }
        tabs.repaint();
    }

    public static void applyTabbedPaneStyle(JTabbedPane tabs) {
        if (tabs == null) {
            return;
        }

        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                tabInsets = new Insets(7, 12, 7, 12);
                tabAreaInsets = new Insets(8, 14, 8, 14);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(AppTheme.PANEL_BG);
                    g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 14, 14);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(isSelected ? AppTheme.PRIMARY : AppTheme.BORDER_COLOR);
                    g2.setStroke(new BasicStroke(isSelected ? 1.6f : 1f));
                    g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 14, 14);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(AppTheme.BORDER_COLOR);
                    g2.fillRect(0, 0, tabs.getWidth(), 1);
                } finally {
                    g2.dispose();
                }
            }
        });
    }

    private static final class TabIcon implements Icon {
        private static final int SIZE = 18;

        private final IconManager.IconType type;
        private Color color;

        private TabIcon(IconManager.IconType type, Color color) {
            this.type = type;
            this.color = color;
        }

        private void setColor(Color color) {
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                switch (type) {
                    case TAB_WBS -> paintWbs(g2, x, y);
                    case TAB_KANBAN -> paintKanban(g2, x, y);
                    case TAB_GANTT -> paintGantt(g2, x, y);
                    case TAB_CALENDAR -> paintCalendar(g2, x, y);
                    default -> {
                    }
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintWbs(Graphics2D g2, int x, int y) {
            g2.drawLine(x + 4, y + 4, x + 4, y + 14);
            g2.drawLine(x + 4, y + 9, x + 9, y + 9);
            g2.drawLine(x + 9, y + 5, x + 9, y + 13);
            g2.fillRoundRect(x + 1, y + 2, 5, 4, 2, 2);
            g2.fillRoundRect(x + 9, y + 3, 7, 4, 2, 2);
            g2.fillRoundRect(x + 9, y + 11, 7, 4, 2, 2);
        }

        private void paintKanban(Graphics2D g2, int x, int y) {
            g2.drawRoundRect(x + 1, y + 2, 16, 14, 2, 2);
            g2.drawLine(x + 6, y + 3, x + 6, y + 15);
            g2.drawLine(x + 12, y + 3, x + 12, y + 15);
            g2.fillRect(x + 3, y + 5, 2, 4);
            g2.fillRect(x + 8, y + 5, 2, 6);
            g2.fillRect(x + 14, y + 5, 2, 3);
        }

        private void paintGantt(Graphics2D g2, int x, int y) {
            g2.drawLine(x + 2, y + 2, x + 2, y + 16);
            g2.drawLine(x + 2, y + 16, x + 17, y + 16);
            g2.fillRoundRect(x + 5, y + 4, 9, 3, 2, 2);
            g2.fillRoundRect(x + 8, y + 9, 8, 3, 2, 2);
            g2.fillRoundRect(x + 4, y + 13, 6, 2, 2, 2);
        }

        private void paintCalendar(Graphics2D g2, int x, int y) {
            IconManager.paintCalendarOutline(g2, x, y);
        }
    }
}
