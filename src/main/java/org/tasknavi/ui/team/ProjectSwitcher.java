package org.tasknavi.ui.team;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppVersion;
import org.tasknavi.team.TeamMember;
import org.tasknavi.team.TeamProject;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.team.TeamSession;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 画面左上の「プロジェクト ▼」と、共有プロジェクトの様子（メンバー・同期・重なった変更）。
 * 選んだプロジェクトのタスクが、どのタブにも出る。
 */
public final class ProjectSwitcher {

    private static final String CHECK = "✓  ";
    private static final String NO_CHECK = "     ";
    private static final DateTimeFormatter CHECKED_AT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TeamSession session;
    private final JPanel panel = new JPanel(new MigLayout("insets 0, gap 6, hidemode 3", "[][][][]", "[]"));
    private final JButton button = new JButton();
    private final JButton membersButton = linkButton();
    private final JButton syncButton = linkButton();
    private final JButton conflictButton = new JButton();

    public ProjectSwitcher(TeamSession session) {
        this.session = session;
        panel.setOpaque(false);
        button.setFont(AppTheme.FONT_HEADER.deriveFont(Font.BOLD, 13f));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(AppMessages.get("project.switcher.tooltip", "表示するプロジェクトを切り替えます"));
        button.addActionListener(e -> showMenu());
        membersButton.addActionListener(e -> session.showMembers());
        syncButton.addActionListener(e -> {
            TeamProject project = session.project();
            if (project != null && (project.newerFormatLines() > 0 || project.newerAppMember() != null)) {
                session.showUpdateHelp();
            } else {
                session.reload();
            }
        });
        conflictButton.setFont(AppTheme.FONT_BOLD);
        conflictButton.setForeground(Color.WHITE);
        conflictButton.setBackground(AppTheme.FILL_WARNING);
        conflictButton.setOpaque(true);
        conflictButton.setFocusPainted(false);
        conflictButton.setBorder(new EmptyBorder(3, 10, 3, 10));
        conflictButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        conflictButton.setToolTipText(AppMessages.get("project.conflicts.tooltip",
                "互いに知らずに同じタスクを変えた箇所です。消えた方の値を確認できます"));
        conflictButton.addActionListener(e -> session.showConflicts());
        panel.add(button);
        panel.add(membersButton, "gapleft 4");
        panel.add(syncButton);
        panel.add(conflictButton, "gapleft 4");
        refresh();
    }

    public JComponent component() {
        return panel;
    }

    /** 開いているプロジェクトや同期の様子が変わったら呼ぶ。 */
    public void refresh() {
        TeamProject project = session.project();
        String label = project != null
                ? AppMessages.format("project.switcher.shared", "{0}（共有）", project.name())
                : AppMessages.get("project.personal", "自分だけのタスク");
        button.setText(label + "  ▼");
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(project != null ? AppTheme.PRIMARY : AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                new EmptyBorder(4, 10, 4, 10)));

        membersButton.setVisible(project != null);
        syncButton.setVisible(project != null);
        if (project == null) {
            conflictButton.setVisible(false);
            return;
        }
        membersButton.setText(AppMessages.format("team.members", "メンバー {0}人", project.members().size()));
        membersButton.setForeground(AppTheme.TEXT_MUTED);
        membersButton.setToolTipText(AppMessages.get("project.members.tooltip", "メンバーの一覧と、自分の表示名"));

        String at = session.lastChecked() != null
                ? session.lastChecked().atZone(ZoneId.systemDefault()).format(CHECKED_AT) : "-";
        TeamMember newer = project.newerAppMember();
        if (session.busy()) {
            syncButton.setText(AppMessages.get("team.loading", "読み込み中…"));
            syncButton.setIcon(new Dot(AppTheme.TEXT_MUTED));
            syncButton.setForeground(AppTheme.TEXT_MUTED);
        } else if (project.newerFormatLines() > 0) {
            syncButton.setText(AppMessages.get("project.sync.newer", "アプリの更新が必要"));
            syncButton.setIcon(new Dot(AppTheme.warning()));
            syncButton.setForeground(AppTheme.warning());
        } else if (newer != null) {
            syncButton.setText(AppMessages.format("project.sync.update", "新しい版 {0} があります", newer.appVersion()));
            syncButton.setIcon(new Dot(AppTheme.warning()));
            syncButton.setForeground(AppTheme.warning());
        } else if (project.unreadableLines() > 0) {
            syncButton.setText(AppMessages.get("project.sync.partial", "一部を同期中"));
            syncButton.setIcon(new Dot(AppTheme.warning()));
            syncButton.setForeground(AppTheme.TEXT_MUTED);
        } else if (session.lastCheckFailed()) {
            syncButton.setText(AppMessages.get("project.sync.failed", "共有フォルダを読めません"));
            syncButton.setIcon(new Dot(AppTheme.warning()));
            syncButton.setForeground(AppTheme.warning());
        } else {
            syncButton.setText(AppMessages.get("project.sync.ok", "同期中"));
            syncButton.setIcon(new Dot(AppTheme.success()));
            syncButton.setForeground(AppTheme.TEXT_MUTED);
        }
        String tooltip = AppMessages.format("project.sync.tooltip",
                "{0} 秒ごとに共有フォルダを確認しています（最終確認 {1}）。押すと今すぐ読み直します",
                TeamSession.POLL_MS / 1000, at);
        if (project.unreadableLines() > 0) {
            tooltip = AppMessages.format("team.unreadable",
                    "同期の途中で読めなかった記録が {0} 行あります。少し待ってから読み直してください。",
                    project.unreadableLines()) + " " + tooltip;
        }
        if (newer != null || project.newerFormatLines() > 0) {
            tooltip = AppMessages.get("team.update.click", "押すと入れ替え方が出ます");
            if (newer != null) {
                tooltip = AppMessages.format("team.update.tooltip", "{0} さんが {1} を使っています（この PC は {2}）。",
                        newer.name(), newer.appVersion(), AppVersion.current()) + " " + tooltip;
            }
            if (project.newerFormatLines() > 0) {
                tooltip = AppMessages.format("team.newerFormat",
                        "新しい版の TaskNavi で書かれた記録が {0} 行あり、この版（{1}）では読めません。アプリを新しくしてください。",
                        project.newerFormatLines(), AppVersion.current()) + " " + tooltip;
            }
        }
        syncButton.setToolTipText(tooltip);

        int conflicts = session.unacknowledgedConflictCount();
        conflictButton.setVisible(conflicts > 0);
        conflictButton.setText(AppMessages.format("project.conflicts", "重なった変更 {0}件", conflicts));
        panel.revalidate();
        panel.repaint();
    }

    private void showMenu() {
        TeamProject current = session.project();
        JPopupMenu menu = new JPopupMenu();

        menu.add(item((current == null ? CHECK : NO_CHECK) + AppMessages.get("project.personal", "自分だけのタスク"),
                session::close));
        List<Path> recent = session.recentProjects();
        Path currentDir = current != null ? current.directory().toAbsolutePath().normalize() : null;
        for (Path dir : recent) {
            boolean selected = dir.equals(currentDir);
            JMenuItem item = item((selected ? CHECK : NO_CHECK) + dir.getFileName(),
                    selected ? () -> { } : () -> session.open(dir));
            item.setToolTipText(dir.toString());
            menu.add(item);
        }

        menu.addSeparator();
        menu.add(item(AppMessages.get("project.switcher.create", "＋ 新しい共有プロジェクトを作る…"), session::createProject));
        menu.add(item(AppMessages.get("project.switcher.join", "共有フォルダから参加する…"), session::chooseAndOpen));
        if (current != null) {
            menu.addSeparator();
            menu.add(item(AppMessages.format("project.menu.members", "メンバー（{0}人）…", current.members().size()),
                    session::showMembers));
            int conflicts = session.unacknowledgedConflictCount();
            JMenuItem conflictItem = item(AppMessages.format("project.menu.conflicts", "重なった変更（{0}件）…", conflicts),
                    session::showConflicts);
            conflictItem.setEnabled(conflicts > 0);
            menu.add(conflictItem);
            menu.add(item(AppMessages.get("project.menu.history", "変更履歴…"), session::showHistory));
            menu.add(item(AppMessages.get("project.menu.reload", "今すぐ読み直す"), session::reload));
        }
        menu.addSeparator();
        String name = session.displayName();
        menu.add(item(name.isEmpty()
                ? AppMessages.get("project.menu.rename.unset", "共有で使う表示名を決める…")
                : AppMessages.format("project.menu.rename", "表示名「{0}」を変える…", name), session::changeDisplayName));
        menu.show(button, 0, button.getHeight());
    }

    private static JMenuItem item(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    private static JButton linkButton() {
        JButton link = new JButton();
        link.setFont(AppTheme.FONT_SMALL);
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setBorder(new EmptyBorder(2, 4, 2, 4));
        link.setIconTextGap(5);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return link;
    }

    /** 同期の様子を示す小さな丸。 */
    private record Dot(Color color) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + 1, 8, 8);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 8;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }
}
