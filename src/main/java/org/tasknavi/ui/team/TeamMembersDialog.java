package org.tasknavi.ui.team;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppVersion;
import org.tasknavi.team.TeamMember;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.DialogKeys;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** 共有プロジェクトのメンバー一覧と、自分の表示名の変更。 */
final class TeamMembersDialog {

    private static final DateTimeFormatter JOINED_AT = DateTimeFormatter.ofPattern("yyyy/M/d");

    private TeamMembersDialog() {
    }

    /** @param onRename 「表示名を変える」が押されたら、ダイアログを閉じてから呼ぶ */
    static void show(Component parent, List<TeamMember> members, String selfDeviceId, Runnable onRename) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                AppMessages.format("team.members", "メンバー {0}人", members.size()), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        String newest = members.stream().map(TeamMember::appVersion).filter(Objects::nonNull)
                .max(AppVersion::compare).orElse(null);
        JPanel content = new JPanel(new MigLayout("fillx, insets 16 20 12 20, gap 6, wrap", "[grow,fill]", ""));
        content.setBackground(AppTheme.PANEL_BG);
        content.add(TeamDialogs.wrapping(AppMessages.get("team.members.intro",
                "共有フォルダを開いた人がここに出ます。\nメンバーを増やすには、Google ドライブでこのフォルダを共有してください。"),
                440), "w 440!");
        for (TeamMember member : members) {
            boolean self = member.deviceId().equals(selfDeviceId);
            JPanel row = new JPanel(new MigLayout("insets 6 0 6 0, gap 8", "[grow,fill][][]", "[]"));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR));
            JLabel name = new JLabel(self
                    ? AppMessages.format("team.member.self", "{0}（あなた）", member.name())
                    : member.name());
            name.setFont(self ? AppTheme.FONT_BOLD : AppTheme.FONT_MAIN);
            name.setForeground(AppTheme.TEXT_PRIMARY);
            row.add(name, "wmin 0");
            if (member.appVersion() != null) {
                boolean behind = AppVersion.compare(member.appVersion(), newest) < 0;
                JLabel version = new JLabel(AppMessages.format("team.member.version", "版 {0}", member.appVersion()));
                version.setFont(behind ? AppTheme.FONT_SMALL.deriveFont(Font.BOLD) : AppTheme.FONT_SMALL);
                version.setForeground(behind ? AppTheme.warning() : AppTheme.TEXT_MUTED);
                row.add(version);
            }
            if (member.joinedAt() != null) {
                JLabel joined = new JLabel(AppMessages.format("team.member.joined", "{0} 参加",
                        member.joinedAt().atZone(ZoneId.systemDefault()).format(JOINED_AT)));
                joined.setFont(AppTheme.FONT_SMALL);
                joined.setForeground(AppTheme.TEXT_MUTED);
                row.add(joined);
            }
            content.add(row, "gaptop 2");
        }

        JButton rename = new JButton(AppMessages.get("team.button.rename.self", "自分の表示名を変える"));
        styleSecondary(rename);
        rename.addActionListener(e -> {
            dialog.dispose();
            onRename.run();
        });
        JButton close = new JButton(AppMessages.get("team.conflict.close", "閉じる"));
        styleSecondary(close);
        close.addActionListener(e -> dialog.dispose());
        JPanel buttons = new JPanel(new MigLayout("insets 10, gap 8", "[]push[]", "[]"));
        buttons.setBackground(AppTheme.BACKGROUND);
        buttons.add(rename, "h 36!");
        buttons.add(close, "h 36!");

        dialog.setLayout(new MigLayout("fill, insets 0, gap 0", "[grow]", "[grow][]"));
        dialog.add(content, "grow, wrap");
        dialog.add(buttons, "growx");
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        DialogKeys.install(dialog, close, dialog::dispose);
        dialog.setVisible(true);
    }

    private static void styleSecondary(JButton button) {
        AppTheme.styleDialogSecondaryButton(button);
        button.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                new EmptyBorder(6, 14, 6, 14)));
    }
}
