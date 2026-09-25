package org.example.ui.kanban;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.Task;
import org.example.util.TaskStatusCycle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;

/**
 * カンバンカード UI の生成。
 */
public final class KanbanCardFactory {

    public interface Actions {
        Integer selectedTaskId();

        void onSelect(Task task);

        void onOpenEdit(Task task);

        void onStatusChange(Task task, String newStatus);

        void refresh();
    }

    private KanbanCardFactory() {
    }

    public static JPanel create(Task task, Actions actions) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(AppTheme.isDarkMode() ? new Color(42, 50, 61) : new Color(255, 255, 255));

        Color statusColor = AppTheme.getStatusColor(task.getStatusCode());
        Border normalBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(statusColor, 12, 1, 1, 1, 1),
                new EmptyBorder(10, 12, 10, 12)
        );
        Border hoverBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(statusColor, 12, 2, 2, 2, 2),
                new EmptyBorder(9, 11, 9, 11)
        );
        card.setBorder(normalBorder);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText(AppMessages.get("kanban.tooltip.drag"));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                return new StringSelection(String.valueOf(task.getId()));
            }

            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }
        });

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    actions.onSelect(task);
                    actions.refresh();
                    card.getTransferHandler().exportAsDrag(card, e, TransferHandler.MOVE);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                actions.onSelect(task);
                if (e.getClickCount() == 2) {
                    actions.onOpenEdit(task);
                }
                actions.refresh();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Integer selected = actions.selectedTaskId();
                if (selected == null || !selected.equals(task.getId())) {
                    card.setBorder(normalBorder);
                }
            }
        });

        Integer selectedTaskId = actions.selectedTaskId();
        if (selectedTaskId != null && selectedTaskId.equals(task.getId())) {
            card.setBorder(hoverBorder);
        }

        JLabel nameLabel = new JLabel(task.getName());
        nameLabel.setFont(AppTheme.FONT_HEADER);
        nameLabel.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        JLabel progressLabel = new JLabel(task.getProgress() + "%");
        progressLabel.setFont(AppTheme.FONT_MAIN);
        progressLabel.setForeground(AppTheme.isDarkMode() ? new Color(96, 165, 250) : AppTheme.PRIMARY);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(progressLabel, BorderLayout.EAST);

        String assigneeText = "\uD83D\uDC64 " + (task.getAssignee() != null ? task.getAssignee() : AppMessages.get("label.unset"));
        JLabel assigneeLabel = new JLabel(assigneeText);
        assigneeLabel.setFont(AppTheme.FONT_MAIN);
        assigneeLabel.setForeground(AppTheme.isDarkMode() ? new Color(148, 163, 184) : AppTheme.TEXT_MUTED);

        JPanel centerPanel = new JPanel(new MigLayout("fillx, insets 0, gap 6, wrap", "[grow,fill]", "[]"));
        centerPanel.setOpaque(false);

        JComponent progressBar = createProgressBar(task.getProgress(), task.getStatusCode());
        if (progressBar != null) {
            centerPanel.add(progressBar);
        }
        centerPanel.add(assigneeLabel);

        JPanel buttonPanel = new JPanel(new MigLayout("insets 0, gap 4, align right", "[][]"));
        buttonPanel.setOpaque(false);

        if (!task.isNotStarted()) {
            JButton prevBtn = new JButton(AppMessages.get("kanban.button.prev"));
            prevBtn.setFont(AppTheme.FONT_MAIN);
            prevBtn.setMargin(new Insets(2, 6, 2, 6));
            prevBtn.setFocusPainted(false);
            prevBtn.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1));
            prevBtn.addActionListener(e ->
                    actions.onStatusChange(task, TaskStatusCycle.previous(task.getStatusCode())));
            buttonPanel.add(prevBtn);
        }

        if (!task.isCompleted()) {
            JButton nextBtn = new JButton(AppMessages.get("kanban.button.next"));
            nextBtn.setFont(AppTheme.FONT_MAIN);
            nextBtn.setMargin(new Insets(2, 6, 2, 6));
            nextBtn.setFocusPainted(false);
            nextBtn.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1));
            nextBtn.addActionListener(e ->
                    actions.onStatusChange(task, TaskStatusCycle.next(task.getStatusCode())));
            buttonPanel.add(nextBtn);
        }

        JPanel priorityIndicator = new JPanel();
        priorityIndicator.setBackground(AppTheme.getPriorityColor(task.getPriorityCode()));

        card.add(priorityIndicator, BorderLayout.WEST);
        card.add(topPanel, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        return card;
    }

    private static JComponent createProgressBar(int progress, String status) {
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
                    int trackHeight = 6;
                    int y = (getHeight() - trackHeight) / 2;
                    g2.setColor(new Color(230, 232, 236));
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
                return new Dimension(100, 10);
            }
        };
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        return bar;
    }
}
