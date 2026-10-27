package org.tasknavi.ui.team;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamComment;
import org.tasknavi.team.TeamProject;
import org.tasknavi.ui.AppDialog;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.NoticeBar;
import org.tasknavi.ui.WidthTrackingPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 共有プロジェクトのタスクのコメント欄。書く・直す・消すは、すぐ共有フォルダに書き足す（編集画面の「保存」とは別）。
 * 直す・消すは、この PC で書いた自分のコメントだけ。
 */
public final class TaskCommentsPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter AT = DateTimeFormatter.ofPattern("M/d HH:mm");

    private interface Write {
        void run() throws IOException;
    }

    private final transient TeamProject project;
    private final String taskId;
    private final JPanel list = new WidthTrackingPanel(new MigLayout("fillx, insets 6, gap 0, wrap", "[grow,fill]", "[]8"));
    private final JScrollPane scroll = new JScrollPane(list);
    private final JTextField input = new JTextField(20);
    private final JButton post = new JButton(AppMessages.get("comment.post", "書く"));
    private final JButton cancel = new JButton(AppMessages.get("comment.cancel", "やめる"));
    /** 直しているコメントの記録 ID。書いているときは null。 */
    private String editingId;

    public TaskCommentsPanel(TeamProject project, String taskId) {
        super(new MigLayout("insets 0, gap 5, wrap", "[grow,fill][][]", "[grow,fill][]"));
        this.project = project;
        this.taskId = taskId;
        setOpaque(false);

        Color background = AppTheme.getCurrentTheme().textFieldBackground();
        list.setBackground(background);
        scroll.getViewport().setBackground(background);
        scroll.setBorder(AppTheme.createFieldBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(280, 130));
        add(scroll, "span 3, grow");

        AppTheme.styleTextField(input);
        input.setToolTipText(AppMessages.get("comment.input.hint",
                "Enter で書き込みます。自分のコメントは、あとから直したり消したりできます"));
        input.addActionListener(e -> submit());
        AppTheme.styleDialogSecondaryButton(post);
        post.addActionListener(e -> submit());
        AppTheme.styleDialogSecondaryButton(cancel);
        cancel.addActionListener(e -> stopEditing());
        cancel.setVisible(false);
        add(input, "growx");
        add(post);
        add(cancel, "hidemode 3");

        refresh();
    }

    /** 名前と時刻。直したものには印を付ける。 */
    static String headerText(TeamComment comment) {
        String header = comment.author() + "  " + comment.at().atZone(ZoneId.systemDefault()).format(AT);
        return comment.edited() && !comment.deleted()
                ? header + "  " + AppMessages.get("comment.edited", "（編集済み）") : header;
    }

    static String bodyText(TeamComment comment) {
        return comment.deleted() ? AppMessages.get("comment.deleted", "（削除されました）") : comment.text();
    }

    private void refresh() {
        list.removeAll();
        List<TeamComment> comments = project.state().comments(taskId);
        if (comments.isEmpty()) {
            list.add(mutedLabel(AppMessages.get("comment.empty", "まだコメントはありません")));
        }
        for (TeamComment comment : comments) {
            list.add(row(comment), "wmin 0");
        }
        list.revalidate();
        list.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
    }

    private JPanel row(TeamComment comment) {
        JPanel row = new JPanel(new MigLayout("fillx, insets 0, gap 6 2, wrap 3", "[grow,fill][][]"));
        row.setOpaque(false);
        row.add(mutedLabel(headerText(comment)), "wmin 0");
        if (project.canChange(comment)) {
            row.add(linkButton(AppMessages.get("comment.edit", "直す"), AppTheme.PRIMARY, () -> startEditing(comment)));
            row.add(linkButton(AppMessages.get("comment.delete", "消す"), AppTheme.danger(), () -> confirmDelete(comment)));
        } else {
            row.add(new JLabel(), "span 2");
        }
        JTextArea body = new JTextArea(bodyText(comment));
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setBorder(null);
        body.setFont(comment.deleted() ? AppTheme.FONT_MAIN.deriveFont(Font.ITALIC) : AppTheme.FONT_MAIN);
        body.setForeground(comment.deleted() ? AppTheme.TEXT_MUTED : AppTheme.textStrong());
        body.addComponentListener(new ComponentAdapter() {
            private int width = -1;

            @Override
            public void componentResized(ComponentEvent e) {
                // 折り返した高さは幅が決まってから分かるので、幅が変わったら並べ直す
                if (body.getWidth() != width) {
                    width = body.getWidth();
                    list.revalidate();
                }
            }
        });
        row.add(body, "span 3, wmin 0");
        return row;
    }

    private static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(AppTheme.TEXT_MUTED);
        return label;
    }

    private static JButton linkButton(String text, Color color, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(AppTheme.FONT_SMALL);
        button.setForeground(color);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }

    private void startEditing(TeamComment comment) {
        editingId = comment.eventId();
        input.setText(comment.text());
        post.setText(AppMessages.get("comment.saveEdit", "保存"));
        cancel.setVisible(true);
        revalidate();
        input.requestFocusInWindow();
        input.selectAll();
    }

    private void stopEditing() {
        editingId = null;
        input.setText("");
        post.setText(AppMessages.get("comment.post", "書く"));
        cancel.setVisible(false);
        revalidate();
    }

    private void confirmDelete(TeamComment comment) {
        if (!AppDialog.confirm(this, AppMessages.get("comment.delete.title", "コメントを消す"),
                AppMessages.get("comment.delete.confirm", "このコメントを消しますか？一覧には「削除されました」と残ります。"))) {
            return;
        }
        if (comment.eventId().equals(editingId)) {
            stopEditing();
        }
        run(() -> project.deleteComment(taskId, comment.eventId()), () -> { });
    }

    private void submit() {
        String text = input.getText().strip();
        if (text.isEmpty()) {
            return;
        }
        String target = editingId;
        if (target != null) {
            run(() -> project.editComment(taskId, target, text), this::stopEditing);
        } else {
            run(() -> project.addComment(taskId, text), () -> input.setText(""));
        }
    }

    /** 裏側で書き、終わったら一覧を描き直す。 */
    private void run(Write write, Runnable onSuccess) {
        input.setEnabled(false);
        post.setEnabled(false);
        cancel.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                write.run();
                return null;
            }

            @Override
            protected void done() {
                input.setEnabled(true);
                post.setEnabled(true);
                cancel.setEnabled(true);
                try {
                    get();
                    onSuccess.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    NoticeBar.error(TaskCommentsPanel.this, e.getCause() instanceof IllegalArgumentException
                            ? e.getCause().getMessage()
                            : AppMessages.get("comment.error", "コメントを書き込めませんでした。Google ドライブがつながっているか確認してください。"));
                }
                refresh();
                input.requestFocusInWindow();
            }
        }.execute();
    }
}
