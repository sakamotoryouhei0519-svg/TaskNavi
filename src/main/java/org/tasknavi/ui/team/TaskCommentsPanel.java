package org.tasknavi.ui.team;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamComment;
import org.tasknavi.team.TeamProject;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.NoticeBar;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 共有プロジェクトのタスクのコメント欄。書いたらすぐ共有フォルダに書き足す（編集画面の「保存」とは別）。
 */
public final class TaskCommentsPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter AT = DateTimeFormatter.ofPattern("M/d HH:mm");

    private final transient TeamProject project;
    private final String taskId;
    private final JTextArea history = new JTextArea(5, 20);
    private final JTextField input = new JTextField(20);
    private final JButton post = new JButton(AppMessages.get("comment.post", "書く"));

    public TaskCommentsPanel(TeamProject project, String taskId) {
        super(new MigLayout("insets 0, gap 5, wrap", "[grow,fill][]", "[grow,fill][]"));
        this.project = project;
        this.taskId = taskId;
        setOpaque(false);

        history.setEditable(false);
        history.setLineWrap(true);
        history.setWrapStyleWord(true);
        JScrollPane scroll = AppTheme.createStyledTextAreaScroll(history);
        scroll.setPreferredSize(new Dimension(280, 110));
        add(scroll, "span 2, grow");

        AppTheme.styleTextField(input);
        input.setToolTipText(AppMessages.get("comment.input.tooltip", "Enter で書き込みます。あとから直したり消したりはできません"));
        input.addActionListener(e -> submit());
        AppTheme.styleDialogSecondaryButton(post);
        post.addActionListener(e -> submit());
        add(input, "growx");
        add(post);

        refresh();
    }

    /** 古い順。最後のコメントが見えるようにする。 */
    static String format(List<TeamComment> comments) {
        if (comments.isEmpty()) {
            return AppMessages.get("comment.empty", "まだコメントはありません");
        }
        StringBuilder text = new StringBuilder();
        for (TeamComment comment : comments) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append(comment.author()).append("  ")
                    .append(comment.at().atZone(ZoneId.systemDefault()).format(AT)).append('\n')
                    .append(comment.text());
        }
        return text.toString();
    }

    private void refresh() {
        history.setText(format(project.state().comments(taskId)));
        history.setCaretPosition(history.getDocument().getLength());
    }

    private void submit() {
        String text = input.getText().strip();
        if (text.isEmpty()) {
            return;
        }
        input.setEnabled(false);
        post.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                project.addComment(taskId, text);
                return null;
            }

            @Override
            protected void done() {
                input.setEnabled(true);
                post.setEnabled(true);
                try {
                    get();
                    input.setText("");
                    refresh();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    NoticeBar.error(TaskCommentsPanel.this, e.getCause() instanceof IllegalArgumentException
                            ? e.getCause().getMessage()
                            : AppMessages.get("comment.error", "コメントを書き込めませんでした。Google ドライブがつながっているか確認してください。"));
                }
                input.requestFocusInWindow();
            }
        }.execute();
    }
}
