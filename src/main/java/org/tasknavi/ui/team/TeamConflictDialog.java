package org.tasknavi.ui.team;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamConflict;
import org.tasknavi.team.TeamConflictGroup;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.DialogKeys;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 重なった変更を 1 件ずつ見せ、「消えた方に戻す」か「このままでよい」を選ばせる。
 * 戻す操作は書き込みが要るので、ダイアログを閉じてから呼び出し側が行う。
 */
final class TeamConflictDialog {

    /** 戻す操作。REVERT は消えた値を書き直す、RESTORE は削除されたタスクを戻す。 */
    record Choice(Kind kind, TeamConflictGroup group) {
        enum Kind { REVERT, RESTORE }
    }

    private static final DateTimeFormatter AT = DateTimeFormatter.ofPattern("M/d HH:mm");

    private TeamConflictDialog() {
    }

    /**
     * @param currentName タスク ID から今の名前を引く（重なった時点の名前は、その後変わっていることがある）
     * @param onAcknowledge 「このままでよい」を押したとき（設定に覚えるだけ）
     * @return 戻す操作を選んだらその内容。閉じただけなら null
     */
    static Choice show(Component parent, List<TeamConflictGroup> groups, Function<String, String> currentName,
                       Consumer<List<String>> onAcknowledge) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                AppMessages.get("team.conflict.title", "重なった変更"), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new MigLayout("fill, insets 0, gap 0", "[grow]", "[grow][]"));

        Choice[] choice = {null};
        List<TeamConflictGroup> remaining = new ArrayList<>(groups);
        JPanel list = new JPanel(new MigLayout("fillx, insets 16 18 16 18, gap 10, wrap", "[grow,fill]", ""));
        list.setBackground(AppTheme.PANEL_BG);

        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            list.removeAll();
            list.add(muted(AppMessages.get("team.conflict.intro",
                    "互いの変更を知らずに、同じ所が変えられました。今は後から記録された方を表示しています。")));
            if (remaining.isEmpty()) {
                list.add(muted(AppMessages.get("team.conflict.none", "確認が必要な重なりはありません。")), "gaptop 8");
            }
            for (TeamConflictGroup group : List.copyOf(remaining)) {
                list.add(card(group, currentName.apply(group.taskId()), () -> {
                    choice[0] = new Choice(group.kind() == TeamConflict.Kind.SAME_FIELD
                            ? Choice.Kind.REVERT : Choice.Kind.RESTORE, group);
                    dialog.dispose();
                }, () -> {
                    onAcknowledge.accept(group.keys());
                    remaining.remove(group);
                    rebuild[0].run();
                }));
            }
            list.revalidate();
            list.repaint();
        };
        rebuild[0].run();

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(620, Math.min(520, 140 + groups.size() * 120)));

        JButton ackAll = new JButton(AppMessages.get("team.conflict.ackAll", "すべて確認済みにする"));
        AppTheme.styleDialogSecondaryButton(ackAll);
        ackAll.addActionListener(e -> {
            List<String> keys = new ArrayList<>();
            remaining.forEach(group -> keys.addAll(group.keys()));
            onAcknowledge.accept(keys);
            remaining.clear();
            rebuild[0].run();
        });
        JButton close = new JButton(AppMessages.get("team.conflict.close", "閉じる"));
        AppTheme.styleDialogSecondaryButton(close);
        close.addActionListener(e -> dialog.dispose());
        JPanel buttons = new JPanel(new MigLayout("insets 10, gap 10, align right"));
        buttons.setBackground(AppTheme.BACKGROUND);
        for (JButton button : List.of(ackAll, close)) {
            button.setBorder(BorderFactory.createCompoundBorder(
                    AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                    new EmptyBorder(6, 14, 6, 14)));
            buttons.add(button, "h 36!");
        }

        dialog.add(scroll, "grow, wrap");
        dialog.add(buttons, "growx");
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        DialogKeys.install(dialog, close, dialog::dispose);
        dialog.setVisible(true);
        return choice[0];
    }

    private static JPanel card(TeamConflictGroup group, String taskName, Runnable onRestore, Runnable onAcknowledge) {
        JPanel card = new JPanel(new MigLayout("fillx, insets 10 14 10 14, gap 4, wrap", "[grow,fill]", ""));
        card.setBackground(AppTheme.isDarkMode() ? new Color(42, 50, 61) : Color.WHITE);
        card.setBorder(AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 10, 1, 1, 1, 1));

        String shownName = taskName != null && !taskName.isBlank() ? taskName : group.taskName();
        JLabel name = new JLabel(shownName.isBlank() ? "-" : shownName);
        name.setFont(AppTheme.FONT_HEADER);
        name.setForeground(AppTheme.TEXT_PRIMARY);
        card.add(name, "wmin 0");
        card.add(muted(describe(group)), "wmin 0");

        if (group.kind() == TeamConflict.Kind.SAME_FIELD) {
            for (TeamConflict item : group.items()) {
                JLabel line = new JLabel(AppMessages.format("team.conflict.field",
                        "{0}: 今「{1}」（{2}） / 消えた値「{3}」（{4}）",
                        TeamFieldText.label(item.field()), TeamFieldText.value(item.field(), item.keptValue()),
                        item.keptBy(), TeamFieldText.value(item.field(), item.lostValue()), item.lostBy()));
                line.setFont(AppTheme.FONT_MAIN);
                line.setForeground(AppTheme.TEXT_PRIMARY);
                card.add(line, "wmin 0, gapleft 8");
            }
        }

        JButton restore = new JButton(actionLabel(group));
        AppTheme.styleDialogPrimaryButton(restore);
        restore.setBorder(new EmptyBorder(6, 14, 6, 14));
        restore.addActionListener(e -> onRestore.run());
        JButton ack = new JButton(AppMessages.get("team.conflict.ack", "このままでよい"));
        AppTheme.styleDialogSecondaryButton(ack);
        ack.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.createRoundedBorder(AppTheme.BORDER_COLOR, 8, 1, 1, 1, 1),
                new EmptyBorder(5, 14, 5, 14)));
        ack.addActionListener(e -> onAcknowledge.run());
        JPanel actions = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        actions.setOpaque(false);
        actions.add(restore);
        actions.add(ack);
        card.add(actions, "gaptop 6");
        return card;
    }

    static String describe(TeamConflictGroup group) {
        String at = formatAt(group.at());
        return switch (group.kind()) {
            case SAME_FIELD -> AppMessages.format("team.conflict.sameField",
                    "{0} さんの変更が、それを知らない {1} さんの変更で上書きされました（{2}）。",
                    group.lostBy(), group.keptBy(), at);
            case EDIT_AFTER_DELETE -> AppMessages.format("team.conflict.editAfterDelete",
                    "{0} さんが削除したタスクを、それを知らない {1} さんが変更していました（{2}）。",
                    group.keptBy(), group.lostBy(), at);
            case DELETE_OVER_EDIT -> AppMessages.format("team.conflict.deleteOverEdit",
                    "{1} さんの変更を知らずに、{0} さんがタスクを削除しました（{2}）。",
                    group.keptBy(), group.lostBy(), at);
            case PARENT_DELETED -> AppMessages.format("team.conflict.parentDeleted",
                    "{1} さんが追加・変更したタスクですが、親を {0} さんが削除したため表示されていません（{2}）。",
                    group.keptBy(), group.lostBy(), at);
        };
    }

    private static String actionLabel(TeamConflictGroup group) {
        return switch (group.kind()) {
            case SAME_FIELD -> AppMessages.format("team.conflict.revert", "{0} さんの値に戻す", group.lostBy());
            case PARENT_DELETED -> AppMessages.get("team.conflict.restoreParent", "親ごと戻す");
            default -> AppMessages.get("team.conflict.restore", "タスクを戻す");
        };
    }

    private static String formatAt(Instant at) {
        return at != null ? at.atZone(ZoneId.systemDefault()).format(AT) : "-";
    }

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(AppTheme.TEXT_MUTED);
        return label;
    }
}
