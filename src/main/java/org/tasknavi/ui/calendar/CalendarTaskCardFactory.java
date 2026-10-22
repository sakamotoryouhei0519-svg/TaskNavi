package org.tasknavi.ui.calendar;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.model.Task;
import org.tasknavi.util.TaskStatusCycle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * カレンダー右側／週表示で使うタスクカード。
 */
public final class CalendarTaskCardFactory {

    public interface Actions {
        Integer selectedTaskId();

        void onTaskClicked(Task task, boolean doubleClick);

        void onStatusChange(Task task, String newStatus);

        void refresh();
    }

    private CalendarTaskCardFactory() {
    }

    public static JPanel create(Task task, boolean isCompact, Actions actions) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        Color baseBg = AppTheme.isDarkMode() ? new Color(42, 50, 61) : Color.WHITE;
        card.setBackground(baseBg);

        Color statusColor = AppTheme.getStatusColor(task.getStatusCode());
        Color accent = task.isOverdue()
                ? new Color(220, 38, 38)
                : AppTheme.BORDER_COLOR;
        Border normalBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(accent, 10, task.isOverdue() ? 2 : 1, task.isOverdue() ? 2 : 1, task.isOverdue() ? 2 : 1, task.isOverdue() ? 2 : 1),
                new EmptyBorder(8, 10, 8, 10)
        );
        Integer selectedTaskId = actions.selectedTaskId();
        card.setBorder(normalBorder);
        if (selectedTaskId != null && selectedTaskId.equals(task.getId())) {
            card.setBackground(AppTheme.selectedFill());
        }
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                actions.onTaskClicked(task, e.getClickCount() == 2);
                actions.refresh();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                Integer selected = actions.selectedTaskId();
                if (selected == null || !selected.equals(task.getId())) {
                    card.setBackground(AppTheme.hoverFill());
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                Integer selected = actions.selectedTaskId();
                card.setBackground(selected != null && selected.equals(task.getId())
                        ? AppTheme.selectedFill()
                        : baseBg);
            }
        });

        JPanel prioBar = new JPanel();
        prioBar.setPreferredSize(new Dimension(4, 0));
        prioBar.setBackground(AppTheme.getPriorityColor(task.getPriorityCode()));
        card.add(prioBar, BorderLayout.WEST);

        JLabel nameLabel = new JLabel(task.getName());
        nameLabel.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, isCompact ? 11f : 12f));
        nameLabel.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        JLabel progressLabel = new JLabel(task.getProgress() + "%");
        progressLabel.setFont(AppTheme.FONT_MAIN.deriveFont(Font.BOLD, isCompact ? 10f : 11f));
        progressLabel.setForeground(statusColor);

        JPanel topPanel = new JPanel(new BorderLayout(4, 0));
        topPanel.setOpaque(false);
        topPanel.add(nameLabel, BorderLayout.CENTER);
        topPanel.add(progressLabel, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new MigLayout("fillx, insets 0, gap 4, wrap", "[grow,fill]", "[]"));
        centerPanel.setOpaque(false);

        JComponent progressBar = createProgressBar(task.getProgress(), task.getStatusCode());
        if (progressBar != null) {
            centerPanel.add(progressBar);
        }

        JPanel metaPanel = new JPanel(new BorderLayout());
        metaPanel.setOpaque(false);

        String assignee = (task.getAssignee() != null && !task.getAssignee().isBlank())
                ? "👤 " + task.getAssignee()
                : "👤 " + AppMessages.get("label.unset", "未設定");
        JLabel assigneeLabel = new JLabel(assignee);
        assigneeLabel.setFont(AppTheme.FONT_SMALL.deriveFont(10f));
        assigneeLabel.setForeground(AppTheme.TEXT_MUTED);
        metaPanel.add(assigneeLabel, BorderLayout.WEST);

        JLabel statusBadge = new JLabel(AppMessages.statusDisplay(task.getStatus()));
        statusBadge.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 10f));
        statusBadge.setForeground(statusColor);
        metaPanel.add(statusBadge, BorderLayout.EAST);

        centerPanel.add(metaPanel);

        if (task.getStartDate() != null && task.getEndDate() != null) {
            String dateText = "📅 " + task.getStartDate() + " 〜 " + task.getEndDate();
            if (task.isOverdue()) {
                dateText = AppMessages.get("task.overdue.badge", "期限超過") + " · " + dateText;
            }
            JLabel dateRangeLabel = new JLabel(dateText);
            dateRangeLabel.setFont(AppTheme.FONT_SMALL.deriveFont(10f));
            dateRangeLabel.setForeground(task.isOverdue()
                    ? new Color(220, 38, 38)
                    : AppTheme.TEXT_MUTED);
            centerPanel.add(dateRangeLabel);
        }

        card.add(topPanel, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);

        if (!isCompact) {
            JPanel actionPanel = new JPanel(new MigLayout("insets 0, gap 4, align right", "[][]"));
            actionPanel.setOpaque(false);

            if (!task.isNotStarted()) {
                JButton prevBtn = arrowButton(AppMessages.get("kanban.button.prev", "←"));
                prevBtn.addActionListener(e ->
                        actions.onStatusChange(task, TaskStatusCycle.previous(task.getStatusCode())));
                actionPanel.add(prevBtn);
            }

            if (!task.isCompleted()) {
                JButton nextBtn = arrowButton(AppMessages.get("kanban.button.next", "→"));
                nextBtn.addActionListener(e ->
                        actions.onStatusChange(task, TaskStatusCycle.next(task.getStatusCode())));
                actionPanel.add(nextBtn);
            }

            card.add(actionPanel, BorderLayout.SOUTH);
        }

        return card;
    }

    private static JButton arrowButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFont(AppTheme.FONT_BOLD);
        button.setMargin(new Insets(2, 8, 2, 8));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 2, 2, 2, 2));
        return button;
    }

    public static JComponent createProgressBar(int progress, String status) {
        if (progress <= 0) {
            return null;
        }
        Color fillColor = AppTheme.getStatusColor(status);
        JComponent bar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int trackHeight = 4;
                    int y = (getHeight() - trackHeight) / 2;
                    g2.setColor(AppTheme.isDarkMode() ? new Color(60, 70, 85) : new Color(226, 232, 240));
                    g2.fillRoundRect(0, y, getWidth(), trackHeight, trackHeight, trackHeight);
                    int fillWidth = (int) Math.round(getWidth() * Math.min(100, Math.max(0, progress)) / 100.0);
                    if (fillWidth > 0) {
                        g2.setColor(fillColor);
                        g2.fillRoundRect(0, y, fillWidth, trackHeight, trackHeight, trackHeight);
                    }
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(80, 6);
            }
        };
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        return bar;
    }
}
