package org.tasknavi.ui.team;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.team.TeamProject;
import org.tasknavi.ui.AppDialog;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.ErrorDialogUtil;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * チームタブで使う入力ダイアログ。
 */
final class TeamDialogs {

    record CreateRequest(String name, Path folder) {
    }

    private TeamDialogs() {
    }

    /** @return 入力した表示名。やめたときは null */
    static String askDisplayName(Component parent, String current) {
        JTextField field = textField(current);
        JPanel form = form();
        form.add(label(AppMessages.get("team.name.label", "チームのメンバーに見える名前")));
        form.add(field, "w 280!");
        form.add(hint(AppMessages.get("team.name.hint", "変更の記録と、担当者の候補に使います。")), "gaptop 4");
        while (AppDialog.showForm(parent, AppMessages.get("team.name.title", "表示名"), form)) {
            String name = field.getText().trim();
            if (!name.isEmpty()) {
                return name;
            }
            ErrorDialogUtil.showError(parent, AppMessages.get("team.name.error", "表示名を入力してください。"));
        }
        return null;
    }

    /** @return 作る場所と名前。やめたときは null */
    static CreateRequest askCreate(Component parent, Path defaultLocation) {
        JTextField nameField = textField("");
        JTextField locationField = textField(defaultLocation != null ? defaultLocation.toString() : "");
        JButton browse = new JButton(AppMessages.get("team.create.browse", "参照…"));
        AppTheme.styleDialogSecondaryButton(browse);
        browse.addActionListener(e -> {
            File chosen = chooseFolder(parent, AppMessages.get("team.create.location", "置く場所"),
                    locationField.getText());
            if (chosen != null) {
                locationField.setText(chosen.getAbsolutePath());
            }
        });
        JLabel target = hint(" ");
        Runnable updateTarget = () -> {
            Path folder = targetFolder(nameField.getText(), locationField.getText());
            target.setText(folder == null ? " "
                    : AppMessages.format("team.create.target", "作成先: {0}", folder));
        };
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateTarget.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateTarget.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateTarget.run();
            }
        };
        nameField.getDocument().addDocumentListener(listener);
        locationField.getDocument().addDocumentListener(listener);

        JPanel location = new JPanel(new MigLayout("insets 0, gap 6", "[grow,fill][]", "[]"));
        location.setOpaque(false);
        location.add(locationField, "h 32!");
        location.add(browse, "h 32!");

        JPanel form = form();
        form.add(label(AppMessages.get("team.create.name", "プロジェクト名")));
        form.add(nameField, "w 420!, h 32!");
        form.add(label(AppMessages.get("team.create.location", "置く場所")), "gaptop 10");
        form.add(location, "w 420!");
        form.add(wrapping(AppMessages.get("team.create.hint",
                "Google ドライブの共有ドライブか、マイドライブの中を選びます。マイドライブに作ったら、"
                        + "できたフォルダをブラウザの Google ドライブでメンバーに「編集者」として共有してください。"), 420), "w 420!, gaptop 4");
        form.add(target, "w 420!, gaptop 8");
        updateTarget.run();

        String title = AppMessages.get("team.create.title", "共有プロジェクトを作る");
        while (AppDialog.showForm(parent, title, form)) {
            String name = nameField.getText().trim();
            Path folder = targetFolder(name, locationField.getText());
            if (name.isEmpty()) {
                ErrorDialogUtil.showError(parent, AppMessages.get("team.create.error.name", "プロジェクト名を入力してください。"));
            } else if (folder == null || !Files.isDirectory(folder.getParent())) {
                ErrorDialogUtil.showError(parent, AppMessages.get("team.create.error.location", "置く場所のフォルダが見つかりません。"));
            } else if (TeamProject.isProjectFolder(folder)) {
                ErrorDialogUtil.showError(parent, AppMessages.get("team.create.error.exists",
                        "そのフォルダには既に共有プロジェクトがあります。「共有フォルダから参加する」から開いてください。"));
            } else {
                return new CreateRequest(name, folder);
            }
        }
        return null;
    }

    /** @return 選んだフォルダ。やめたときは null */
    static Path askProjectFolder(Component parent, Path start) {
        File chosen = chooseFolder(parent, AppMessages.get("team.open.title", "参加する共有プロジェクトのフォルダを選ぶ"),
                start != null ? start.toString() : null);
        return chosen != null ? chosen.toPath() : null;
    }

    static Path targetFolder(String name, String location) {
        if (name == null || name.isBlank() || location == null || location.isBlank()) {
            return null;
        }
        try {
            return Path.of(location.trim()).resolve(TeamProject.folderNameFor(name));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static File chooseFolder(Component parent, String title, String start) {
        JFileChooser chooser = new JFileChooser(start == null || start.isBlank() ? null : start);
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

    private static JPanel form() {
        JPanel form = new JPanel(new MigLayout("insets 0, gap 4, wrap", "[grow,fill]", ""));
        form.setOpaque(false);
        return form;
    }

    private static JTextField textField(String text) {
        JTextField field = new JTextField(text);
        AppTheme.styleTextField(field);
        return field;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_BOLD);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        return label;
    }

    private static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.FONT_SMALL);
        label.setForeground(AppTheme.TEXT_MUTED);
        return label;
    }

    /** 折り返す説明文。先に幅を決めておかないと、高さが折り返し前の行数で計算される。 */
    static JTextArea wrapping(String text, int width) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setHighlighter(null);
        area.setFont(AppTheme.FONT_SMALL);
        area.setForeground(AppTheme.TEXT_MUTED);
        area.setBorder(null);
        area.setSize(width, Short.MAX_VALUE);
        return area;
    }
}
