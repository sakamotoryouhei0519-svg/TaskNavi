package org.tasknavi.ui.calendar;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.IconManager;
import org.tasknavi.Task;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 右側日別タスク欄のヘッダー生成と一覧更新。
 */
public final class CalendarDailySideSupport {

    public record Header(JPanel panel, JLabel titleLabel, JLabel countBadge) {
    }

    private CalendarDailySideSupport() {
    }

    public static Header createHeader(Runnable onAddTask) {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 10 12 10 12, gap 8", "[grow][right]"));
        panel.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 10, 1, 1, 1, 1),
                new EmptyBorder(2, 4, 2, 4)
        ));

        JPanel titlePanel = new JPanel(new MigLayout("insets 0, gap 6", "[][]"));
        titlePanel.setOpaque(false);

        JLabel lblDailyHeader = new JLabel("");
        lblDailyHeader.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 13f));
        lblDailyHeader.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblDailyCountBadge = new JLabel(AppMessages.format("calendar.count.format", "{0}件", 0));
        lblDailyCountBadge.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 11f));
        lblDailyCountBadge.setForeground(Color.WHITE);
        lblDailyCountBadge.setOpaque(true);
        lblDailyCountBadge.setBackground(AppTheme.PRIMARY);
        lblDailyCountBadge.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.PRIMARY_DARK, 10, 1, 1, 1, 1),
                new EmptyBorder(2, 6, 2, 6)
        ));

        titlePanel.add(lblDailyHeader);
        titlePanel.add(lblDailyCountBadge);

        JButton btnAddTaskForDay = IconManager.getIconButton(
                IconManager.IconType.ADD, AppMessages.get("calendar.button.add.task", "タスク追加"));
        btnAddTaskForDay.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD));
        btnAddTaskForDay.setBackground(AppTheme.PRIMARY);
        btnAddTaskForDay.setForeground(Color.WHITE);
        btnAddTaskForDay.setFocusPainted(false);
        btnAddTaskForDay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddTaskForDay.setBorder(AppTheme.createRoundedBorder(AppTheme.PRIMARY_DARK, 8, 1, 1, 1, 1));
        btnAddTaskForDay.setMargin(new Insets(4, 10, 4, 10));
        btnAddTaskForDay.addActionListener(e -> onAddTask.run());

        panel.add(titlePanel, "left");
        panel.add(btnAddTaskForDay, "right");

        return new Header(panel, lblDailyHeader, lblDailyCountBadge);
    }

    public static void updateTasks(
            JLabel titleLabel,
            JLabel countBadge,
            JPanel container,
            LocalDate selectedDate,
            List<Task> allTasks,
            CalendarTaskCardFactory.Actions cardActions) {
        if (titleLabel == null || container == null) {
            return;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(
                AppMessages.get("calendar.date.format.daily", "yyyy年MM月dd日 (E)"),
                AppMessages.getLocale());
        titleLabel.setText(selectedDate.format(dtf));

        List<Task> tasksForSelectedDate = CalendarTaskFilter.tasksForDate(allTasks, selectedDate);
        if (countBadge != null) {
            countBadge.setText(AppMessages.format("calendar.count.format", "{0}件", tasksForSelectedDate.size()));
        }

        container.removeAll();

        if (tasksForSelectedDate.isEmpty()) {
            JPanel emptyPanel = new JPanel(new MigLayout("fill, insets 40 20 40 20, align center", "[center]"));
            emptyPanel.setOpaque(false);

            JLabel emptyIcon = new JLabel("📝");
            emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

            JLabel emptyText = new JLabel(AppMessages.get("calendar.empty.title", "この日のタスクはありません"));
            emptyText.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 13f));
            emptyText.setForeground(AppTheme.TEXT_MUTED);

            JLabel emptySubText = new JLabel(AppMessages.get("calendar.empty.subtitle", "上の「タスク追加」から予定を登録できます"));
            emptySubText.setFont(AppTheme.FONT_SMALL.deriveFont(11f));
            emptySubText.setForeground(AppTheme.TEXT_MUTED);

            emptyPanel.add(emptyIcon, "wrap");
            emptyPanel.add(emptyText, "wrap");
            emptyPanel.add(emptySubText);
            container.add(emptyPanel, "grow");
        } else {
            for (Task task : tasksForSelectedDate) {
                container.add(CalendarTaskCardFactory.create(task, false, cardActions), "growx");
            }
        }
    }
}
