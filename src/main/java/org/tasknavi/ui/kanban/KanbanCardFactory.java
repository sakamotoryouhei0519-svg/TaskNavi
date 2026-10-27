package org.tasknavi.ui.kanban;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.util.TaskStatusCycle;

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
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
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

        void onDrop(int taskId, java.awt.Component source);

        void refresh();
    }

    private KanbanCardFactory() {
    }

    public static JPanel create(Task task, Actions actions) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        Color baseBg = AppTheme.isDarkMode() ? new Color(42, 50, 61) : new Color(255, 255, 255);
        card.setBackground(baseBg);

        Color statusColor = AppTheme.getStatusColor(task.getStatusCode());
        Color borderColor = task.isOverdue() ? new Color(220, 38, 38) : statusColor;
        Border normalBorder = BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(borderColor, 12, task.isOverdue() ? 2 : 1, task.isOverdue() ? 2 : 1, task.isOverdue() ? 2 : 1, task.isOverdue() ? 2 : 1),
                new EmptyBorder(10, 12, 10, 12)
        );
        card.setBorder(normalBorder);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText(task.isOverdue()
                ? AppMessages.get("task.overdue.tooltip", "期限を過ぎています")
                : AppMessages.get("kanban.tooltip.drag"));
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

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    String text = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    actions.onDrop(Integer.parseInt(text.trim()), support.getComponent());
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        });

        java.awt.event.MouseAdapter mouseHandler = new java.awt.event.MouseAdapter() {
            private Point pressPoint;
            private boolean dragStarted;

            @Override
            public void mousePressed(MouseEvent e) {
                pressPoint = SwingUtilities.isLeftMouseButton(e) ? e.getPoint() : null;
                dragStarted = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressPoint == null || dragStarted || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int threshold = DragSource.getDragThreshold();
                if (Math.abs(e.getX() - pressPoint.x) > threshold || Math.abs(e.getY() - pressPoint.y) > threshold) {
                    dragStarted = true;
                    actions.onSelect(task);
                    card.getTransferHandler().exportAsDrag(card, e, TransferHandler.MOVE);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                actions.onSelect(task);
                if (e.getClickCount() == 2) {
                    actions.onOpenEdit(task);
                }
                actions.refresh();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Integer selected = actions.selectedTaskId();
                if (selected == null || !selected.equals(task.getId())) {
                    card.setBackground(AppTheme.hoverFill());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Integer selected = actions.selectedTaskId();
                card.setBackground(selected != null && selected.equals(task.getId())
                        ? AppTheme.selectedFill()
                        : baseBg);
            }
        };
        card.addMouseListener(mouseHandler);
        card.addMouseMotionListener(mouseHandler);

        Integer selectedTaskId = actions.selectedTaskId();
        if (selectedTaskId != null && selectedTaskId.equals(task.getId())) {
            card.setBackground(AppTheme.selectedFill());
        }

        JLabel nameLabel = new JLabel(task.getName());
        nameLabel.setFont(AppTheme.FONT_HEADER);
        nameLabel.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        JLabel progressLabel = new JLabel(task.getProgress() + "%");
        progressLabel.setFont(AppTheme.FONT_MAIN);
        progressLabel.setForeground(AppTheme.isDarkMode() ? new Color(96, 165, 250) : AppTheme.PRIMARY);

        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setOpaque(false);
        topPanel.add(nameLabel, BorderLayout.CENTER);
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
            JButton prevBtn = arrowButton(AppMessages.get("kanban.button.prev"));
            prevBtn.addActionListener(e ->
                    actions.onStatusChange(task, TaskStatusCycle.previous(task.getStatusCode())));
            buttonPanel.add(prevBtn);
        }

        if (!task.isCompleted()) {
            JButton nextBtn = arrowButton(AppMessages.get("kanban.button.next"));
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

    private static JButton arrowButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFont(AppTheme.FONT_BOLD);
        button.setMargin(new Insets(2, 8, 2, 8));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 2, 2, 2, 2));
        return button;
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
