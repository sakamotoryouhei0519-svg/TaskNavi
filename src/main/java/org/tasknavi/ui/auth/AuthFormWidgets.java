package org.tasknavi.ui.auth;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.ui.AppIcon;
import org.tasknavi.ui.AppTheme;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 認証画面共通のフォーム部品・スタイル。
 * 画面は「背景の上に角丸カードを 1 枚置き、その中に縦一列でフォームを並べる」形でそろえる。
 */
public final class AuthFormWidgets {

    /** カード内の入力欄・ボタンの幅。ウィンドウ幅はここから pack で決まる。 */
    public static final int CONTENT_WIDTH = 300;
    public static final int FIELD_HEIGHT = 38;
    public static final int BUTTON_HEIGHT = 42;
    /** エラー欄の高さ（2 行分）を常に確保する制約。 */
    public static final String ERROR_AREA_CONSTRAINTS = "growx, wmin 0, gaptop 8, h 40!";

    private static final String ERROR_STATE = "auth.errorState";
    private static final char ECHO_CHAR = '•';

    private AuthFormWidgets() {
    }

    public static void applyWindowDefaults(JFrame frame, String title) {
        frame.setTitle(title);
        frame.setIconImages(AppIcon.windowIcons());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
    }

    /** 縦一列のフォーム用カード。行は呼び出し側が {@code gaptop} で間隔を付けて追加する。 */
    public static JPanel createCard() {
        return new CardPanel(new MigLayout("wrap, insets 28 28 24 28, gap 0", "[" + CONTENT_WIDTH + "!]"));
    }

    /** カードを背景の中央に置き、内容に合わせたサイズで画面中央に出す。 */
    public static void showCard(JFrame frame, JPanel card) {
        JPanel root = new JPanel(new MigLayout("insets 24, fill", "[center]", "[center]"));
        root.setBackground(AppTheme.BACKGROUND);
        root.add(card);
        frame.setContentPane(root);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public static JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("TaskNavi", SwingConstants.CENTER);
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        return titleLabel;
    }

    public static JLabel createSubtitleLabel(String text) {
        JLabel subtitleLabel = new JLabel(text, SwingConstants.CENTER);
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        return subtitleLabel;
    }

    public static JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEADING);
        label.setFont(AppTheme.FONT_BOLD);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        return label;
    }

    public static JLabel createLinkLabel(String text, Runnable onClick) {
        JLabel link = new JLabel(text, SwingConstants.CENTER);
        link.setFont(AppTheme.FONT_MAIN);
        link.setForeground(AppTheme.PRIMARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (onClick != null) {
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }
            });
        }
        return link;
    }

    /**
     * エラー表示欄。常に置いたまま文字だけ出し入れする（出し入れでレイアウトが動かないように）。
     * HTML の JLabel は日本語を折り返さないので、文字単位で折り返す読み取り専用テキストにする。
     */
    public static JTextArea createErrorArea() {
        JTextArea errorArea = new JTextArea();
        errorArea.setFont(AppTheme.FONT_MAIN);
        errorArea.setForeground(errorColor());
        errorArea.setLineWrap(true);
        errorArea.setEditable(false);
        errorArea.setFocusable(false);
        errorArea.setHighlighter(null);
        errorArea.setOpaque(false);
        errorArea.setBorder(new EmptyBorder(0, 0, 0, 0));
        return errorArea;
    }

    public static void showError(JTextArea errorArea, String message) {
        errorArea.setText(message == null ? "" : message);
    }

    public static void clearError(JTextArea errorArea) {
        errorArea.setText("");
    }

    public static Color errorColor() {
        return AppTheme.isDarkMode() ? new Color(248, 113, 113) : new Color(220, 38, 38);
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        AppTheme.styleDialogPrimaryButton(button);
        button.setFont(AppTheme.FONT_HEADER);
        button.setMargin(new Insets(0, 12, 0, 12));
        button.addPropertyChangeListener("enabled", e -> applyPrimaryButtonState(button));
        applyPrimaryButtonState(button);
        return button;
    }

    private static void applyPrimaryButtonState(JButton button) {
        if (button.isEnabled()) {
            button.setBackground(AppTheme.PRIMARY);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            button.setBackground(AppTheme.isDarkMode() ? new Color(51, 65, 85) : new Color(226, 232, 240));
            button.setCursor(Cursor.getDefaultCursor());
        }
    }

    public static void setPrimaryButtonBusy(JButton button, boolean busy, String busyText, String idleText) {
        if (button == null) {
            return;
        }
        button.setEnabled(!busy);
        button.setText(busy ? busyText : idleText);
    }

    public static JCheckBox createCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setFont(AppTheme.FONT_MAIN);
        checkBox.setForeground(AppTheme.TEXT_PRIMARY);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setIcon(AuthIcons.checkBox());
        checkBox.setIconTextGap(8);
        checkBox.setBorder(new EmptyBorder(0, 0, 0, 0));
        checkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return checkBox;
    }

    public static JTextField createStyledTextField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }

    public static JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setEchoChar(ECHO_CHAR);
        styleTextField(field);
        return field;
    }

    private static void styleTextField(JTextField field) {
        field.setFont(AppTheme.FONT_MAIN);
        AppTheme.applyTextColors(field);
        field.putClientProperty(ERROR_STATE, false);
        field.setBorder(AppTheme.createFieldBorder());
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                refreshBorder(field);
            }

            @Override
            public void focusLost(FocusEvent e) {
                refreshBorder(field);
            }
        });
        field.addPropertyChangeListener("enabled", e -> field.setBackground(field.isEnabled()
                ? AppTheme.getCurrentTheme().textFieldBackground()
                : AppTheme.isDarkMode() ? new Color(30, 41, 59) : new Color(241, 245, 249)));
    }

    /** パスワード欄と表示切替ボタンを横に並べた行。 */
    public static JPanel createPasswordRow(JPasswordField field, JToggleButton toggle) {
        JPanel row = new JPanel(new MigLayout(
                "insets 0, gap 6", "[grow,fill][" + FIELD_HEIGHT + "!]", "[" + FIELD_HEIGHT + "!]"));
        row.setOpaque(false);
        row.add(field, "grow");
        row.add(toggle, "grow");
        return row;
    }

    public static JToggleButton createPasswordToggle(JPasswordField field) {
        JToggleButton toggle = new JToggleButton();
        AppTheme.styleDialogSecondaryButton(toggle);
        toggle.setIcon(AuthIcons.eye(false));
        toggle.setSelectedIcon(AuthIcons.eye(true));
        toggle.setDisabledIcon(AuthIcons.eye(false));
        toggle.setMargin(new Insets(0, 0, 0, 0));
        toggle.setToolTipText(AppMessages.get("auth.password.toggle.tooltip"));
        toggle.addItemListener(e -> field.setEchoChar(
                e.getStateChange() == ItemEvent.SELECTED ? (char) 0 : ECHO_CHAR));
        field.addPropertyChangeListener("enabled", e -> toggle.setEnabled(field.isEnabled()));
        toggle.setEnabled(field.isEnabled());
        return toggle;
    }

    public static void setFieldErrorState(JTextField field, boolean hasError) {
        if (field == null) {
            return;
        }
        field.putClientProperty(ERROR_STATE, hasError);
        refreshBorder(field);
    }

    private static void refreshBorder(JComponent field) {
        if (Boolean.TRUE.equals(field.getClientProperty(ERROR_STATE))) {
            field.setBorder(createErrorBorder());
        } else if (field.hasFocus()) {
            field.setBorder(AppTheme.createFocusedFieldBorder());
        } else {
            field.setBorder(AppTheme.createFieldBorder());
        }
    }

    private static Border createErrorBorder() {
        return new CompoundBorder(
                AppTheme.createRoundedBorder(errorColor(), 12, 2, 2, 2, 2),
                new EmptyBorder(4, 10, 4, 10));
    }

    /** 背景を角丸で塗るカード。四角い不透明パネルだと角が背景からはみ出す。 */
    private static final class CardPanel extends JPanel {
        private static final int ARC = 20;

        private CardPanel(MigLayout layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.PANEL_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
                g2.setColor(AppTheme.isDarkMode() ? new Color(51, 65, 85) : new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
