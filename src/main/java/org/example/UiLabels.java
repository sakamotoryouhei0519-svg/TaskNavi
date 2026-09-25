package org.example;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;

/**
 * ドメイン値を保持したまま表示だけローカライズする UI 補助。
 */
public final class UiLabels {
    private UiLabels() {
    }

    /** 日曜始まりの短い曜日名。 */
    public static String[] weekdaysSundayFirst() {
        return new String[]{
                AppMessages.get("weekday.sun", "日"),
                AppMessages.get("weekday.mon", "月"),
                AppMessages.get("weekday.tue", "火"),
                AppMessages.get("weekday.wed", "水"),
                AppMessages.get("weekday.thu", "木"),
                AppMessages.get("weekday.fri", "金"),
                AppMessages.get("weekday.sat", "土")
        };
    }

    /** 月曜始まりの短い曜日名（ISO / DayOfWeek.getValue()-1）。 */
    public static String[] weekdaysMondayFirst() {
        return new String[]{
                AppMessages.get("weekday.mon", "月"),
                AppMessages.get("weekday.tue", "火"),
                AppMessages.get("weekday.wed", "水"),
                AppMessages.get("weekday.thu", "木"),
                AppMessages.get("weekday.fri", "金"),
                AppMessages.get("weekday.sat", "土"),
                AppMessages.get("weekday.sun", "日")
        };
    }

    public static void installPriorityRenderer(JComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(AppMessages.priorityDisplay(String.valueOf(value)));
                }
                return c;
            }
        });
    }

    public static void installStatusRenderer(JComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(AppMessages.statusDisplay(String.valueOf(value)));
                }
                return c;
            }
        });
    }

    public static void installEntryTypeRenderer(JComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(AppMessages.entryTypeDisplay(String.valueOf(value)));
                }
                return c;
            }
        });
    }
}
