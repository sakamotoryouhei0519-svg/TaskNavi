package org.tasknavi.ui.taskdialog;

import org.tasknavi.AppMessages;
import org.tasknavi.ui.AppTheme;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * TaskDialog 向けコンボボックス見た目。
 */
public final class TaskDialogUiStyle {

    private TaskDialogUiStyle() {
    }

    public static void styleComboBox(JComboBox<String> combo) {
        AppTheme.styleComboBox(combo);
        combo.setFont(AppTheme.FONT_MAIN);
        combo.setBackground(AppTheme.controlBackground());
        combo.setForeground(AppTheme.textStrong());

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                applyDarkSelection(c, isSelected);
                return c;
            }
        });
    }

    public static ListCellRenderer<? super String> createTypeRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String domainType) {
                    setText(AppMessages.entryTypeDisplay(domainType));
                }
                applyDarkSelection(c, isSelected);
                return c;
            }
        };
    }

    private static void applyDarkSelection(Component c, boolean isSelected) {
        if (!AppTheme.isDarkMode()) {
            return;
        }
        if (isSelected) {
            c.setBackground(AppTheme.PRIMARY);
            c.setForeground(Color.WHITE);
        } else {
            c.setBackground(AppTheme.controlBackground());
            c.setForeground(AppTheme.textStrong());
        }
    }
}
