package org.tasknavi.ui.wbs;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;
import org.tasknavi.ui.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * WBS 右側の表示専用パネル。編集はボタンか、名前の行のダブルクリックでダイアログを開いて行う。
 */
public final class WbsDetailForm {

    private final JPanel rootPanel;
    private final JPanel bodyPanel;
    private final JLabel emptyLabel;
    private final JButton editButton;
    private final JLabel nameLabel = new JLabel(" ");
    private final PillLabel statusBadge = new PillLabel();
    private final JLabel projectValue = new JLabel(" ");
    private final JLabel stageValue = new JLabel(" ");
    private final JLabel assigneeValue = new JLabel(" ");
    private final PillLabel priorityBadge = new PillLabel();
    private final JLabel startValue = new JLabel(" ");
    private final JLabel endValue = new JLabel(" ");
    private final JLabel progressPercent = new JLabel("0%");
    private final ProgressTrack progressTrack = new ProgressTrack();
    private final JLabel headingLabel = new JLabel();
    private final JLabel projectCaption = caption();
    private final JLabel stageCaption = caption();
    private final JLabel assigneeCaption = caption();
    private final JLabel priorityCaption = caption();
    private final JLabel startCaption = caption();
    private final JLabel endCaption = caption();
    private final JLabel progressCaption = caption();
    private final JLabel memoCaption = caption();
    private final JTextArea memoValue = new JTextArea();
    private final JPanel metaDivider = line();
    private final JPanel progressDivider = line();
    private final JPanel memoDivider = line();

    public WbsDetailForm(Runnable onEdit) {
        headingLabel.setText(AppMessages.get("wbs.detail.title", "タスク詳細"));
        headingLabel.setFont(AppTheme.FONT_HEADER);

        editButton = new JButton("✎  " + AppMessages.get("wbs.button.edit", "編集"));
        editButton.setFont(AppTheme.FONT_BOLD);
        editButton.setFocusPainted(false);
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.setEnabled(false);
        editButton.addActionListener(e -> onEdit.run());

        nameLabel.setFont(AppTheme.FONT_TITLE);
        statusBadge.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD, 12f));
        statusBadge.setForeground(Color.WHITE);
        progressPercent.setFont(AppTheme.FONT_BOLD);

        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]", "[]"));
        header.setOpaque(false);
        header.add(headingLabel, "growx, wmin 0");
        header.add(editButton, "h 36!");

        JLabel docIcon = new JLabel("☰");
        docIcon.setFont(AppTheme.FONT_TITLE);
        docIcon.setForeground(AppTheme.PRIMARY);

        JPanel titleRow = new JPanel(new MigLayout("insets 0, gap 8", "[][][]", "[]"));
        titleRow.setOpaque(false);
        titleRow.add(docIcon);
        titleRow.add(nameLabel, "wmin 0");
        titleRow.add(statusBadge, "aligny center");
        editOnDoubleClick(onEdit, titleRow, docIcon, nameLabel, statusBadge);
        String editHint = AppMessages.get("wbs.detail.edit.hint", "ダブルクリックで編集");
        docIcon.setToolTipText(editHint);
        statusBadge.setToolTipText(editHint);

        JPanel meta = new JPanel(new MigLayout(
                "insets 0",
                "[grow,fill]16[grow,fill]16[grow,fill]",
                "[]4[]14[]4[]"));
        meta.setOpaque(false);
        projectCaption.setText(AppMessages.get("wbs.label.project.name", "プロジェクト名"));
        stageCaption.setText(AppMessages.get("wbs.label.stage.name", "工程名"));
        assigneeCaption.setText(AppMessages.get("label.assignee", "担当者"));
        priorityCaption.setText(AppMessages.get("label.priority", "優先度"));
        startCaption.setText(AppMessages.get("wbs.label.start", "開始日"));
        endCaption.setText(AppMessages.get("wbs.label.end", "終了日"));
        styleValue(projectValue);
        styleValue(stageValue);
        styleValue(assigneeValue);
        styleValue(startValue);
        styleValue(endValue);
        priorityBadge.setFont(AppTheme.FONT_BOLD);
        meta.add(projectCaption, "wmin 0");
        meta.add(stageCaption, "wmin 0");
        meta.add(assigneeCaption, "wmin 0, wrap");
        meta.add(projectValue, "wmin 0");
        meta.add(stageValue, "wmin 0");
        meta.add(assigneeValue, "wmin 0, wrap");
        meta.add(priorityCaption, "wmin 0");
        meta.add(startCaption, "wmin 0");
        meta.add(endCaption, "wmin 0, wrap");
        meta.add(priorityBadge, "left, growx 0");
        meta.add(startValue, "wmin 0");
        meta.add(endValue, "wmin 0");

        progressCaption.setText(AppMessages.get("wbs.label.progress.rate", "進捗率"));
        JPanel progressHeader = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        progressHeader.setOpaque(false);
        progressHeader.add(progressCaption);
        progressHeader.add(progressPercent);
        progressTrack.setPreferredSize(new Dimension(240, 10));
        progressTrack.setMaximumSize(new Dimension(240, 10));

        memoCaption.setText(AppMessages.get("label.memo", "メモ"));
        memoValue.setEditable(false);
        memoValue.setFocusable(false);
        memoValue.setOpaque(false);
        memoValue.setLineWrap(true);
        memoValue.setWrapStyleWord(true);
        memoValue.setFont(AppTheme.FONT_MAIN);
        memoValue.setBorder(null);

        bodyPanel = new JPanel(new MigLayout("insets 0", "[grow]", "[]10[]10[]8[]"));
        bodyPanel.setOpaque(false);
        bodyPanel.add(titleRow, "growx, wmin 0, wrap");
        bodyPanel.add(metaDivider, "growx, h 1!, wrap");
        bodyPanel.add(meta, "growx, wmin 0, wrap");
        bodyPanel.add(progressDivider, "growx, h 1!, wrap");
        bodyPanel.add(progressHeader, "left, wrap");
        bodyPanel.add(progressTrack, "w 0:240:240, h 10!, left, wrap");
        bodyPanel.add(memoDivider, "growx, h 1!, gaptop 6, wrap");
        bodyPanel.add(memoCaption, "left, wrap");
        bodyPanel.add(memoValue, "growx, wmin 0");
        bodyPanel.setVisible(false);

        emptyLabel = new JLabel(AppMessages.get(
                "wbs.detail.empty", "左側のツリーから項目を選ぶと、ここに内容が出ます。"));
        emptyLabel.setFont(AppTheme.FONT_MAIN);
        emptyLabel.setForeground(AppTheme.TEXT_MUTED);

        rootPanel = new JPanel(new MigLayout("insets 12 16 12 16", "[grow]", "[]12[]"));
        rootPanel.add(header, "growx, wrap");
        rootPanel.add(bodyPanel, "growx, wrap");
        rootPanel.add(emptyLabel, "growx, wmin 0");
        updateTheme();
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public void clear() {
        show(null, "", "", "");
    }

    public void show(Task task, String projectName, String stageName, String wbsCode) {
        boolean visible = task != null;
        editButton.setEnabled(visible);
        bodyPanel.setVisible(visible);
        emptyLabel.setVisible(!visible);
        if (!visible) {
            rootPanel.revalidate();
            rootPanel.repaint();
            return;
        }

        String name = task.getName() == null ? "" : task.getName();
        nameLabel.setText(wbsCode == null || wbsCode.isBlank() ? name : wbsCode + "  " + name);
        String editHint = AppMessages.get("wbs.detail.edit.hint", "ダブルクリックで編集");
        nameLabel.setToolTipText(name.isBlank() ? editHint : "<html>" + escapeHtml(name) + "<br>" + escapeHtml(editHint) + "</html>");
        statusBadge.setText(AppMessages.statusDisplay(task.getStatusCode()));
        statusBadge.setBackground(AppTheme.getStatusColor(task.getStatusCode()));

        projectValue.setText(textOrDash(projectName));
        projectValue.setToolTipText(projectName == null || projectName.isBlank() ? null : projectName);
        stageValue.setText(textOrDash(stageName));
        stageValue.setToolTipText(stageName == null || stageName.isBlank() ? null : stageName);
        assigneeValue.setText(assigneeHtml(task.getAssignee()));
        priorityBadge.setText(AppMessages.priorityDisplay(task.getPriorityCode()));
        priorityBadge.setBackground(AppTheme.getPriorityColor(task.getPriorityCode()));
        startValue.setText(task.getStartDate() != null ? task.getStartDate().toString() : "—");
        endValue.setText(task.getEndDate() != null ? task.getEndDate().toString() : "—");

        int progress = Math.max(0, Math.min(100, task.getProgress()));
        progressPercent.setText(progress + "%");
        progressTrack.setPercent(progress);
        memoValue.setText(textOrDash(task.getMemo()));
        memoValue.setCaretPosition(0);
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    public void updateTheme() {
        Color fg = AppTheme.TEXT_PRIMARY;
        Color muted = AppTheme.TEXT_MUTED;
        rootPanel.setBackground(AppTheme.PANEL_BG);
        headingLabel.setForeground(fg);
        nameLabel.setForeground(fg);
        projectValue.setForeground(fg);
        stageValue.setForeground(fg);
        assigneeValue.setForeground(fg);
        startValue.setForeground(fg);
        endValue.setForeground(fg);
        progressPercent.setForeground(fg);
        memoValue.setForeground(fg);
        emptyLabel.setForeground(muted);
        for (JLabel caption : new JLabel[]{
                projectCaption, stageCaption, assigneeCaption, priorityCaption,
                startCaption, endCaption, progressCaption, memoCaption}) {
            caption.setForeground(muted);
        }
        statusBadge.setForeground(Color.WHITE);
        priorityBadge.setForeground(Color.WHITE);
        editButton.setForeground(fg);
        editButton.setBackground(AppTheme.PANEL_BG);
        editButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        progressTrack.repaint();
        metaDivider.setBackground(AppTheme.BORDER_COLOR);
        progressDivider.setBackground(AppTheme.BORDER_COLOR);
        memoDivider.setBackground(AppTheme.BORDER_COLOR);
    }

    /** 名前の行のどこをダブルクリックしても、編集ボタンと同じく編集を開く。選んでいなければ何もしない。 */
    private void editOnDoubleClick(Runnable onEdit, JComponent... targets) {
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && editButton.isEnabled()) {
                    onEdit.run();
                }
            }
        };
        for (JComponent target : targets) {
            target.addMouseListener(handler);
            target.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static void styleValue(JLabel value) {
        value.setFont(AppTheme.FONT_BOLD);
    }

    private static JLabel caption() {
        JLabel label = new JLabel();
        label.setFont(AppTheme.FONT_SMALL);
        return label;
    }

    private static JPanel line() {
        JPanel line = new JPanel();
        line.setBackground(AppTheme.BORDER_COLOR);
        line.setPreferredSize(new Dimension(1, 1));
        return line;
    }

    private static String textOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String assigneeHtml(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return "—";
        }
        String color = String.format("#%06X", AppTheme.PRIMARY.getRGB() & 0xFFFFFF);
        return "<html><span style='color:" + color + ";'>●</span> " + escapeHtml(assignee) + "</html>";
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class PillLabel extends JLabel {
        private PillLabel() {
            setOpaque(false);
            setBorder(new EmptyBorder(4, 10, 4, 10));
            setHorizontalAlignment(CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ProgressTrack extends JComponent {
        private int percent;

        private void setPercent(int percent) {
            this.percent = percent;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(240, 10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int height = 8;
            int y = (getHeight() - height) / 2;
            int width = Math.max(0, getWidth() - 1);
            g2.setColor(AppTheme.progressTrack());
            g2.fillRoundRect(0, y, width, height, height, height);
            int filled = (int) Math.round(width * (percent / 100.0));
            if (filled > 0) {
                g2.setColor(AppTheme.PRIMARY);
                g2.fillRoundRect(0, y, Math.max(filled, height), height, height, height);
            }
            g2.dispose();
        }
    }
}
