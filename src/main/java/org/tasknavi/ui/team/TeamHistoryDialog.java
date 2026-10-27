package org.tasknavi.ui.team;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamEvent;
import org.tasknavi.team.TeamHistory;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.DialogKeys;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** チームプロジェクトの変更履歴。選択中のタスクがあれば、そのタスクだけに絞って開く。 */
final class TeamHistoryDialog {

    /** これより古いものは出さない。全部並べると、長く使ったプロジェクトで画面が重くなる。 */
    static final int MAX_ROWS = 300;
    private static final DateTimeFormatter AT = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm");

    private TeamHistoryDialog() {
    }

    /**
     * @param focusTaskId 絞り込むタスク（無ければ null）
     */
    static void show(Component parent, List<TeamHistory.Entry> history, String focusTaskId, String focusTaskName) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                AppMessages.get("team.history.title", "変更履歴"), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new MigLayout("fill, insets 0, gap 0", "[grow]", "[][grow][]"));

        JPanel list = new JPanel(new MigLayout("fillx, insets 12 18 16 18, gap 0, wrap", "[grow,fill]", ""));
        list.setBackground(AppTheme.PANEL_BG);

        JCheckBox onlyFocus = new JCheckBox(AppMessages.format("team.history.only", "「{0}」だけ", focusTaskName));
        onlyFocus.setSelected(focusTaskId != null);
        onlyFocus.setOpaque(false);
        onlyFocus.setFocusPainted(false);
        onlyFocus.setFont(AppTheme.FONT_MAIN);
        onlyFocus.setForeground(AppTheme.TEXT_PRIMARY);

        Runnable rebuild = () -> {
            list.removeAll();
            List<TeamHistory.Entry> shown = history.stream()
                    .filter(entry -> !onlyFocus.isSelected() || entry.taskId().equals(focusTaskId))
                    .toList();
            if (shown.isEmpty()) {
                list.add(muted(AppMessages.get("team.history.empty", "まだ変更はありません。")));
            }
            shown.stream().limit(MAX_ROWS).forEach(entry -> list.add(row(entry), "wmin 0"));
            if (shown.size() > MAX_ROWS) {
                list.add(muted(AppMessages.format("team.history.more",
                        "これより古い {0} 件は省略しています。", shown.size() - MAX_ROWS)), "gaptop 10");
            }
            list.revalidate();
            list.repaint();
        };
        onlyFocus.addActionListener(e -> rebuild.run());
        rebuild.run();

        JPanel top = new JPanel(new MigLayout("insets 12 18 4 18, gap 12", "[grow,fill][]", "[]"));
        top.setBackground(AppTheme.PANEL_BG);
        top.add(muted(AppMessages.get("team.history.intro",
                "新しい順です。時刻は書いた人の PC の時計です。")), "wmin 0");
        if (focusTaskId != null) {
            top.add(onlyFocus);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(640, 480));

        JButton close = new JButton(AppMessages.get("team.conflict.close", "閉じる"));
        AppTheme.styleDialogSecondaryButton(close);
        close.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                new EmptyBorder(6, 14, 6, 14)));
        close.addActionListener(e -> dialog.dispose());
        JPanel buttons = new JPanel(new MigLayout("insets 10, align right"));
        buttons.setBackground(AppTheme.BACKGROUND);
        buttons.add(close, "h 36!");

        dialog.add(top, "growx, wrap");
        dialog.add(scroll, "grow, wrap");
        dialog.add(buttons, "growx");
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        DialogKeys.install(dialog, close, dialog::dispose);
        dialog.setVisible(true);
    }

    private static JPanel row(TeamHistory.Entry entry) {
        JPanel row = new JPanel(new MigLayout("fillx, insets 8 0 8 0, gap 2, wrap", "[grow,fill]", ""));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR));

        JPanel head = new JPanel(new MigLayout("insets 0, gap 8", "[][][grow,fill]", "[]"));
        head.setOpaque(false);
        JLabel op = new JLabel(opLabel(entry.op()));
        op.setFont(AppTheme.FONT_SMALL);
        op.setForeground(Color.WHITE);
        op.setOpaque(true);
        op.setBackground(opColor(entry.op()));
        op.setBorder(new EmptyBorder(1, 6, 1, 6));
        head.add(op);
        JLabel name = new JLabel(entry.taskName().isBlank() ? "-" : entry.taskName());
        name.setFont(AppTheme.FONT_BOLD);
        name.setForeground(AppTheme.TEXT_PRIMARY);
        head.add(name, "wmin 0");
        String at = entry.at().atZone(ZoneId.systemDefault()).format(AT);
        head.add(muted(entry.author() + "  ·  " + at), "wmin 0");
        row.add(head, "wmin 0");

        for (TeamHistory.Change change : entry.changes()) {
            String text = entry.op() == TeamEvent.Op.CREATE || entry.op() == TeamEvent.Op.COMMENT
                    ? AppMessages.format("team.history.set", "{0}: {1}",
                    TeamFieldText.label(change.field()), TeamFieldText.value(change.field(), change.after()))
                    : AppMessages.format("team.history.change", "{0}: {1} → {2}",
                    TeamFieldText.label(change.field()), TeamFieldText.value(change.field(), change.before()),
                    TeamFieldText.value(change.field(), change.after()));
            JLabel line = new JLabel(text);
            line.setFont(AppTheme.FONT_MAIN);
            line.setForeground(AppTheme.TEXT_PRIMARY);
            row.add(line, "wmin 0, gapleft 8");
        }
        return row;
    }

    private static String opLabel(TeamEvent.Op op) {
        return switch (op) {
            case CREATE -> AppMessages.get("team.history.op.create", "追加");
            case UPDATE -> AppMessages.get("team.history.op.update", "変更");
            case DELETE -> AppMessages.get("team.history.op.delete", "削除");
            case RESTORE -> AppMessages.get("team.history.op.restore", "戻す");
            case COMMENT -> AppMessages.get("team.history.op.comment", "コメント");
        };
    }

    private static Color opColor(TeamEvent.Op op) {
        return switch (op) {
            case CREATE -> new Color(22, 163, 74);
            case UPDATE -> new Color(37, 99, 235);
            case DELETE -> new Color(220, 38, 38);
            case RESTORE -> new Color(147, 51, 234);
            case COMMENT -> new Color(13, 148, 136);
        };
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(AppTheme.TEXT_MUTED);
        return label;
    }
}
