package org.tasknavi.ui;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * AppTheme 向けのボタン／フィールド／枠線ファクトリ。
 */
final class AppThemeControls {

    private AppThemeControls() {
    }

    static Border createRoundedBorder(Color borderColor, int radius, int top, int left, int bottom, int right) {
        return new RoundedBorder(borderColor, radius, top, left, bottom, right);
    }

    static Border createSoftPanelBorder() {
        return createRoundedBorder(AppTheme.getCurrentTheme().borderColor(), 12, 2, 2, 2, 2);
    }

    static Border createFieldBorder() {
        return new CompoundBorder(
                createRoundedBorder(AppTheme.getCurrentTheme().borderColor(), 12, 2, 2, 2, 2),
                new EmptyBorder(4, 10, 4, 10));
    }

    static Border createFocusedFieldBorder() {
        return new CompoundBorder(
                createRoundedBorder(AppTheme.getCurrentTheme().primary(), 12, 2, 2, 2, 2),
                new EmptyBorder(4, 10, 4, 10));
    }

    static JButton createPrimaryButton(String text) {
        JButton button = new RoundedFillButton(text, 12);
        button.putClientProperty("app.button.variant", "primary");
        button.setFont(AppTheme.FONT_HEADER);
        button.setBackground(AppTheme.primaryFill());
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(createRoundedBorder(AppTheme.getCurrentTheme().primaryDark(), 12, 2, 2, 2, 2));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setRolloverEnabled(true);
        return button;
    }

    static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("app.button.variant", "secondary");
        button.setFont(AppTheme.FONT_MAIN);
        button.setBackground(AppTheme.controlBackground());
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(createRoundedBorder(AppTheme.getCurrentTheme().borderColor(), 10, 2, 2, 2, 2));
        button.setMargin(new Insets(4, 14, 4, 14));
        button.setRolloverEnabled(true);
        button.setPreferredSize(new Dimension(
                Math.max(110, button.getFontMetrics(button.getFont()).stringWidth(text) + 28), 36));
        return button;
    }

    static void styleDialogPrimaryButton(AbstractButton button) {
        if (button == null) {
            return;
        }
        button.setUI(new DialogButtonUI(18));
        button.setFont(AppTheme.FONT_MAIN);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(false);
        button.setBackground(AppTheme.primaryFill());
        button.setForeground(Color.WHITE);
        button.setBorder(null);
        button.setMargin(new Insets(10, 22, 10, 22));
        button.putClientProperty("dialog.fill", Boolean.TRUE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    static void styleDialogSecondaryButton(AbstractButton button) {
        if (button == null) {
            return;
        }
        Color face = AppTheme.controlBackground();
        button.setUI(new DialogButtonUI(10));
        button.setFont(AppTheme.FONT_MAIN);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(false);
        button.setBackground(face);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setMargin(new Insets(6, 14, 6, 14));
        button.setBorder(createRoundedBorder(AppTheme.BORDER_COLOR, 10, 2, 2, 2, 2));
        button.putClientProperty("dialog.fill", Boolean.TRUE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    static void styleSegmentButton(AbstractButton button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setUI(new DialogButtonUI(8));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(null);
        button.setMargin(new Insets(5, 14, 5, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("dialog.fill", selected);
        if (selected) {
            button.setBackground(AppTheme.isDarkMode() ? AppTheme.hoverFill() : Color.WHITE);
            button.setForeground(AppTheme.TEXT_PRIMARY);
        } else {
            button.setForeground(AppTheme.TEXT_MUTED);
        }
    }

    static void applyTextColors(JTextComponent component) {
        component.setBackground(AppTheme.getCurrentTheme().textFieldBackground());
        component.setForeground(AppTheme.isDarkMode()
                ? AppTheme.textStrong() : AppTheme.getCurrentTheme().textFieldForeground());
        component.setCaretColor(AppTheme.isDarkMode() ? Color.WHITE : Color.BLACK);
        component.setDisabledTextColor(AppTheme.textDisabled());
        component.setSelectionColor(AppTheme.selectedFill());
        component.setSelectedTextColor(component.getForeground());
    }

    static void styleTextField(JTextField field) {
        field.setFont(AppTheme.FONT_MAIN);
        applyTextColors(field);
        field.setBorder(createFieldBorder());
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(createFocusedFieldBorder());
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(createFieldBorder());
            }
        });
        field.putClientProperty("app.styled.textfield", Boolean.TRUE);
    }

    static JScrollPane createStyledTextAreaScroll(JTextArea area) {
        Color background = AppTheme.getCurrentTheme().textFieldBackground();
        area.setFont(AppTheme.FONT_MAIN);
        applyTextColors(area);
        area.setBorder(new EmptyBorder(2, 0, 2, 0));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBackground(background);
        scroll.getViewport().setBackground(background);
        scroll.setBorder(createFieldBorder());
        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                scroll.setBorder(createFocusedFieldBorder());
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                scroll.setBorder(createFieldBorder());
            }
        });
        return scroll;
    }

    static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(AppTheme.FONT_MAIN);
        combo.setBackground(AppTheme.controlBackground());
        combo.setForeground(AppTheme.textStrong());
        combo.setBorder(createRoundedBorder(AppTheme.BORDER_COLOR, 7, 1, 1, 1, 1));
        combo.setUI(new ThemedComboBoxUI());
        combo.putClientProperty("app.styled.combobox", Boolean.TRUE);
    }

    static class RoundedFillButton extends JButton {
        private final int radius;

        RoundedFillButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getBackground();
                if (getModel().isPressed()) {
                    fill = fill.darker();
                } else if (getModel().isRollover()) {
                    fill = new Color(
                            Math.max(0, fill.getRed() - 10),
                            Math.max(0, fill.getGreen() - 10),
                            Math.max(0, fill.getBlue() - 10));
                }
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** 中の文字と一覧は描くたびに今のテーマの色にする。画面ごとの描き方（renderer）が古い色を持っていても上書きする。 */
    private static final class ThemedComboBoxUI extends BasicComboBoxUI {
        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(comboBox.getBackground());
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            @SuppressWarnings("unchecked")
            javax.swing.ListCellRenderer<Object> renderer = (javax.swing.ListCellRenderer<Object>) comboBox.getRenderer();
            Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setFont(comboBox.getFont());
            c.setForeground(comboBox.isEnabled() ? comboBox.getForeground() : AppTheme.textDisabled());
            c.setBackground(comboBox.getBackground());
            if (c instanceof JComponent jc) {
                jc.setOpaque(false);
            }
            currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height,
                    c instanceof javax.swing.JPanel);
        }

        @Override
        protected javax.swing.plaf.basic.ComboPopup createPopup() {
            return new javax.swing.plaf.basic.BasicComboPopup(comboBox) {
                @Override
                public void show() {
                    list.setBackground(AppTheme.PANEL_BG);
                    list.setForeground(AppTheme.TEXT_PRIMARY);
                    list.setSelectionBackground(AppTheme.PRIMARY);
                    list.setSelectionForeground(Color.WHITE);
                    setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1));
                    super.show();
                }
            };
        }

        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(AppTheme.BORDER_COLOR);
                        g2.drawLine(0, 4, 0, getHeight() - 5);
                        g2.setColor(AppTheme.TEXT_PRIMARY);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int centerX = getWidth() / 2;
                        int centerY = getHeight() / 2 - 1;
                        g2.drawLine(centerX - 4, centerY - 2, centerX, centerY + 2);
                        g2.drawLine(centerX, centerY + 2, centerX + 4, centerY - 2);
                    } finally {
                        g2.dispose();
                    }
                }
            };
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setOpaque(false);
            button.setPreferredSize(new Dimension(30, 0));
            return button;
        }
    }

    /**
     * 背景を角丸の内側だけ塗る。標準のボタン描画は四角で塗るので、枠の外にはみ出す。
     */
    private static final class DialogButtonUI extends javax.swing.plaf.basic.BasicButtonUI {
        private final int radius;

        private DialogButtonUI(int radius) {
            this.radius = radius;
        }

        @Override
        public void update(Graphics g, JComponent c) {
            if (!Boolean.FALSE.equals(c.getClientProperty("dialog.fill"))) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean outline = c instanceof AbstractButton button
                            && button.isBorderPainted()
                            && button.getBorder() != null;
                    int pad = outline ? 2 : 0;
                    g2.setColor(c.getBackground());
                    g2.fillRoundRect(
                            pad,
                            pad,
                            Math.max(1, c.getWidth() - pad * 2),
                            Math.max(1, c.getHeight() - pad * 2),
                            radius,
                            radius);
                } finally {
                    g2.dispose();
                }
            }
            paint(g, c);
        }

        /** 標準の無効表示は文字を浮き彫りにして読みにくいので、くすんだ単色で描く。 */
        @Override
        protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
            if (b.isEnabled()) {
                super.paintText(g, b, textRect, text);
                return;
            }
            FontMetrics metrics = b.getFontMetrics(b.getFont());
            g.setColor(AppTheme.textDisabled());
            BasicGraphicsUtils.drawStringUnderlineCharAt(
                    b, (Graphics2D) g, text, -1, textRect.x, textRect.y + metrics.getAscent());
        }
    }

    private static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final Insets insets;

        private RoundedBorder(Color color, int radius, int top, int left, int bottom, int right) {
            this.color = color;
            this.radius = radius;
            this.insets = new Insets(top, left, bottom, right);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int thickness = thickness();
                double offset = thickness / 2.0;
                g2.setStroke(new BasicStroke(thickness));
                g2.drawRoundRect(
                        (int) (x + offset), (int) (y + offset),
                        Math.max(1, width - thickness), Math.max(1, height - thickness), radius, radius);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int thickness = thickness();
            return new Insets(
                    Math.max(thickness, insets.top),
                    Math.max(thickness, insets.left),
                    Math.max(thickness, insets.bottom),
                    Math.max(thickness, insets.right));
        }

        private int thickness() {
            return Math.max(2, insets.top);
        }
    }
}
