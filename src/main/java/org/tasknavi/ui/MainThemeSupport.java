package org.tasknavi.ui;

import org.tasknavi.AppTheme;
import org.tasknavi.SearchablePanel;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.Set;

/**
 * MainFrame 向けのテーマ再帰適用。
 */
public final class MainThemeSupport {

    private MainThemeSupport() {
    }

    public static void applyToContainer(Container container, Set<Component> skip, JToggleButton themeToggle) {
        if (container == null) {
            return;
        }
        for (Component child : container.getComponents()) {
            if (skip != null && skip.contains(child)) {
                continue;
            }
            if (child instanceof JPanel panel) {
                panel.setBackground(AppTheme.PANEL_BG);
            } else if (child instanceof JLabel label) {
                label.setForeground(AppTheme.TEXT_PRIMARY);
            } else if (child instanceof JButton button) {
                if (child == themeToggle) {
                    continue;
                }
                styleButton(button);
            } else if (child instanceof JRadioButton radio) {
                radio.setBackground(AppTheme.isDarkMode() ? AppTheme.PANEL_BG : Color.WHITE);
                radio.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);
                radio.setOpaque(false);
            } else if (child instanceof JComboBox<?> combo) {
                AppTheme.styleComboBox(combo);
                combo.repaint();
            } else if (child instanceof JTabbedPane tabbedPane) {
                tabbedPane.setBackground(AppTheme.BACKGROUND);
                tabbedPane.setForeground(AppTheme.TEXT_PRIMARY);
            } else if (child instanceof JTextField field) {
                AppTheme.styleTextField(field);
            } else if (child instanceof JScrollPane scrollPane) {
                scrollPane.setBackground(AppTheme.BACKGROUND);
                if (scrollPane.getViewport() != null) {
                    scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
                }
            } else if (child instanceof JTree tree) {
                tree.setBackground(AppTheme.PANEL_BG);
                tree.setForeground(AppTheme.TEXT_PRIMARY);
                tree.repaint();
            } else if (child instanceof JTable table) {
                table.setBackground(AppTheme.PANEL_BG);
                table.setForeground(AppTheme.TEXT_PRIMARY);
                table.setGridColor(AppTheme.BORDER_COLOR);
            }

            if (child instanceof Container nested) {
                applyToContainer(nested, skip, themeToggle);
            }
        }
    }

    public static void updateViewTheme(Component component) {
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBackground(AppTheme.BACKGROUND);
            if (scrollPane.getViewport() != null) {
                scrollPane.getViewport().setBackground(AppTheme.BACKGROUND);
                component = scrollPane.getViewport().getView();
            }
        }
        if (component instanceof SearchablePanel searchable) {
            searchable.updateTheme();
        }
        if (component instanceof JComponent view) {
            view.revalidate();
            view.repaint();
        }
    }

    private static void styleButton(JButton button) {
        Object variant = button.getClientProperty("app.button.variant");
        Color dangerColor = new Color(220, 38, 38);

        if ("toolbar-primary".equals(variant)) {
            button.setBackground(AppTheme.PRIMARY);
            button.setForeground(Color.WHITE);
            button.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY, 8, 2, 2, 2, 2));
            return;
        }
        if ("toolbar-outline-danger".equals(variant)) {
            button.setBackground(Color.WHITE);
            button.setForeground(dangerColor);
            button.setBorder(AppTheme.createRoundedBorder(dangerColor, 8, 2, 2, 2, 2));
            return;
        }
        if ("toolbar-outline-primary".equals(variant)) {
            button.setBackground(Color.WHITE);
            button.setForeground(AppTheme.PRIMARY);
            button.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY, 8, 2, 2, 2, 2));
            return;
        }
        if ("toolbar-outline".equals(variant) || "header".equals(variant)
                || "secondary".equals(variant) || "toolbar".equals(variant)) {
            button.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
            button.setForeground(AppTheme.TEXT_PRIMARY);
            button.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 2, 2, 2, 2));
            return;
        }
        if ("primary".equals(variant)) {
            button.setBackground(AppTheme.PRIMARY);
            button.setForeground(Color.WHITE);
            button.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY_DARK, 12, 2, 2, 2, 2));
            return;
        }
        button.setBackground(AppTheme.PANEL_BG);
        button.setForeground(AppTheme.TEXT_PRIMARY);
    }
}
