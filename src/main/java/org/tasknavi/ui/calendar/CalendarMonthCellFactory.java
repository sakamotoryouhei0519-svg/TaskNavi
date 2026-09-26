package org.tasknavi.ui.calendar;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.Task;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * 月表示の日付セルとミニタスクバッジ。
 */
public final class CalendarMonthCellFactory {

    public interface Actions {
        LocalDate selectedDate();

        Integer selectedTaskId();

        void onDateSelected(LocalDate date);

        void onTaskClicked(Task task, boolean doubleClick);
    }

    private CalendarMonthCellFactory() {
    }

    public static JPanel createDayCell(
            LocalDate date,
            boolean isCurrentMonth,
            boolean isToday,
            boolean isSelected,
            List<Task> tasksForDay,
            Actions actions) {
        JPanel cell = new JPanel(new BorderLayout(0, 4));
        Color cellBg = AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE;
        if (!isCurrentMonth) {
            cellBg = AppTheme.isDarkMode() ? new Color(20, 29, 44) : new Color(248, 250, 252);
        }
        cell.setBackground(cellBg);

        Border selectedBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.PRIMARY, 8, 2, 2, 2, 2),
                new EmptyBorder(3, 4, 3, 4)
        );
        Border todayBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(new Color(59, 130, 246), 8, 2, 2, 2, 2),
                new EmptyBorder(3, 4, 3, 4)
        );
        Border normalBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                new EmptyBorder(4, 5, 4, 5)
        );
        Border hoverBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.PRIMARY, 8, 1, 1, 1, 1),
                new EmptyBorder(4, 5, 4, 5)
        );

        if (isSelected) {
            cell.setBorder(selectedBorder);
            if (!AppTheme.isDarkMode()) {
                cell.setBackground(new Color(240, 247, 255));
            }
        } else if (isToday) {
            cell.setBorder(todayBorder);
        } else {
            cell.setBorder(normalBorder);
        }

        cell.setCursor(new Cursor(Cursor.HAND_CURSOR));

        int dayVal = date.getDayOfMonth();
        JLabel lblDayNumber = new JLabel(String.valueOf(dayVal), SwingConstants.CENTER);
        lblDayNumber.setFont(isToday ? AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 12f) : AppTheme.FONT_MAIN.deriveFont(12f));

        if (isToday) {
            lblDayNumber.setOpaque(true);
            lblDayNumber.setBackground(new Color(59, 130, 246));
            lblDayNumber.setForeground(Color.WHITE);
            lblDayNumber.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        } else if (!isCurrentMonth) {
            lblDayNumber.setForeground(AppTheme.TEXT_MUTED);
        } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lblDayNumber.setForeground(new Color(239, 68, 68));
        } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            lblDayNumber.setForeground(new Color(59, 130, 246));
        } else {
            lblDayNumber.setForeground(AppTheme.TEXT_PRIMARY);
        }

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(lblDayNumber, BorderLayout.WEST);

        if (!tasksForDay.isEmpty() && !isToday) {
            JLabel countDot = new JLabel(String.valueOf(tasksForDay.size()));
            countDot.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 9f));
            countDot.setForeground(AppTheme.TEXT_MUTED);
            topBar.add(countDot, BorderLayout.EAST);
        }

        cell.add(topBar, BorderLayout.NORTH);

        JPanel taskListPanel = new JPanel(new MigLayout("fillx, insets 0, gap 2, wrap", "[grow,fill]", "[]2"));
        taskListPanel.setOpaque(false);

        int maxShow = 2;
        for (int i = 0; i < Math.min(tasksForDay.size(), maxShow); i++) {
            taskListPanel.add(createTaskBadge(tasksForDay.get(i), actions));
        }

        if (tasksForDay.size() > maxShow) {
            JLabel moreLabel = new JLabel(AppMessages.format(
                    "calendar.more.tasks", "＋他 {0} 件", tasksForDay.size() - maxShow));
            moreLabel.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 9f));
            moreLabel.setForeground(AppTheme.PRIMARY);
            taskListPanel.add(moreLabel);
        }

        cell.add(taskListPanel, BorderLayout.CENTER);

        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                actions.onDateSelected(date);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!date.equals(actions.selectedDate())) {
                    cell.setBorder(hoverBorder);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!date.equals(actions.selectedDate())) {
                    cell.setBorder(isToday ? todayBorder : normalBorder);
                }
            }
        });

        return cell;
    }

    public static JPanel createTaskBadge(Task task, Actions actions) {
        JPanel badge = new JPanel(new BorderLayout(3, 0));
        Color statusColor = AppTheme.getStatusColor(task.getStatusCode());
        Color statusSoft = AppTheme.getStatusSoftColor(task.getStatusCode());

        badge.setBackground(AppTheme.isDarkMode() ? new Color(42, 50, 61) : statusSoft);
        badge.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(statusColor, 4, 1, 1, 1, 1),
                new EmptyBorder(1, 4, 1, 4)
        ));
        badge.setToolTipText(String.format(
                "%s (%d%% - %s)",
                task.getName(),
                task.getProgress(),
                AppMessages.statusDisplay(task.getStatusCode())));

        JLabel nameLbl = new JLabel(task.getName());
        nameLbl.setFont(AppTheme.FONT_SMALL.deriveFont(10f));
        nameLbl.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        JLabel progLbl = new JLabel(task.getProgress() + "%");
        progLbl.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 9f));
        progLbl.setForeground(statusColor);

        badge.add(nameLbl, BorderLayout.CENTER);
        badge.add(progLbl, BorderLayout.EAST);

        badge.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                actions.onTaskClicked(task, e.getClickCount() == 2);
            }
        });

        return badge;
    }
}
