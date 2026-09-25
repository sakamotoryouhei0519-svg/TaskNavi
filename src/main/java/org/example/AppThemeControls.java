package org.example;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
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
        button.setBackground(AppTheme.getCurrentTheme().primary());
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
        button.setBackground(Color.WHITE);
        button.setForeground(AppTheme.getCurrentTheme().textPrimary());
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

    static void styleTextField(JTextField field) {
        field.setFont(AppTheme.FONT_MAIN);
        field.setBackground(AppTheme.getCurrentTheme().textFieldBackground());
        if (AppTheme.isDarkMode()) {
            field.setForeground(new Color(226, 232, 240));
            field.setCaretColor(Color.WHITE);
        } else {
            field.setForeground(AppTheme.getCurrentTheme().textFieldForeground());
            field.setCaretColor(Color.BLACK);
        }
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

    static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(AppTheme.FONT_MAIN);
        combo.setBackground(AppTheme.getCurrentTheme().textFieldBackground());
        combo.setForeground(AppTheme.TEXT_PRIMARY);
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

    private static final class ThemedComboBoxUI extends BasicComboBoxUI {
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
                int thickness = insets.top;
                double offset = thickness / 2.0;
                g2.setStroke(new BasicStroke(thickness));
                g2.drawRoundRect(
                        (int) (x + offset), (int) (y + offset),
                        width - thickness, height - thickness, radius, radius);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(insets.top, insets.left, insets.bottom, insets.right);
        }
    }
}
