package org.example.ui.auth;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.UiConstants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 認証画面共通のフォーム部品・スタイル。
 */
public final class AuthFormWidgets {

    public static final Color ERROR_COLOR = new Color(220, 53, 69);

    private AuthFormWidgets() {
    }

    public static void applyWindowDefaults(JFrame frame, String title, int width, int height) {
        frame.setTitle(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
    }

    public static JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("TaskNavi");
        titleLabel.setFont(AppTheme.FONT_TITLE);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return titleLabel;
    }

    public static JLabel createSubtitleLabel(String text) {
        JLabel subtitleLabel = new JLabel(text);
        subtitleLabel.setFont(AppTheme.FONT_MAIN);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        return subtitleLabel;
    }

    public static JLabel createFormLabel(String text) {
        return createFormLabel(text, UiConstants.FIELD_WIDTH_STANDARD, 20);
    }

    public static JLabel createFormLabel(String text, int maxWidth, int maxHeight) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(AppTheme.FONT_BOLD);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(maxWidth, maxHeight));
        return label;
    }

    public static JLabel createLinkLabel(String text, Runnable onClick) {
        JLabel link = new JLabel(text);
        link.setFont(AppTheme.FONT_MAIN);
        link.setForeground(AppTheme.PRIMARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
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

    public static JLabel createErrorLabel() {
        JLabel errorLabel = new JLabel();
        errorLabel.setFont(AppTheme.FONT_MAIN);
        errorLabel.setForeground(ERROR_COLOR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setVisible(false);
        return errorLabel;
    }

    public static JButton createPrimaryButton(String text, int width, int height) {
        JButton button = new JButton(text);
        button.setFont(AppTheme.FONT_HEADER);
        button.setBackground(AppTheme.PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(width, height));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    public static JTextField createStyledTextField() {
        return createStyledTextField(null, false);
    }

    public static JTextField createStyledTextField(String placeholderIgnored, boolean trackErrorState) {
        JTextField field = new JTextField();
        styleTextField(field, trackErrorState);
        return field;
    }

    public static JPasswordField createStyledPasswordField(boolean trackErrorState) {
        JPasswordField field = new JPasswordField();
        styleTextField(field, trackErrorState);
        return field;
    }

    public static void styleTextField(JTextField field, boolean trackErrorState) {
        field.setFont(AppTheme.FONT_MAIN);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(new Dimension(UiConstants.FIELD_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT));
        if (trackErrorState) {
            field.putClientProperty("errorState", false);
            field.setBorder(createDefaultBorder());
        } else {
            field.setBorder(AppTheme.createFieldBorder());
        }

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (trackErrorState && Boolean.TRUE.equals(field.getClientProperty("errorState"))) {
                    field.setBorder(createErrorBorder());
                } else {
                    field.setBorder(AppTheme.createFocusedFieldBorder());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (trackErrorState && Boolean.TRUE.equals(field.getClientProperty("errorState"))) {
                    field.setBorder(createErrorBorder());
                } else if (trackErrorState) {
                    field.setBorder(createDefaultBorder());
                } else {
                    field.setBorder(AppTheme.createFieldBorder());
                }
            }
        });
    }

    public static JPanel createPasswordTogglePanel(JPasswordField field) {
        return createPasswordTogglePanel(field, 0);
    }

    public static JPanel createPasswordTogglePanel(JPasswordField field, int gap) {
        JPanel wrapper = new JPanel(new MigLayout(
                "fill, insets 0, gap " + gap, "[grow][]", "[grow]"));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(UiConstants.FIELD_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JToggleButton toggle = createPasswordToggle(field);
        wrapper.add(field, "grow");
        wrapper.add(toggle);
        return wrapper;
    }

    public static JToggleButton createPasswordToggle(JPasswordField field) {
        JToggleButton toggle = new JToggleButton("🙈");
        toggle.setPreferredSize(new Dimension(UiConstants.ICON_BUTTON_SIZE_LARGE, UiConstants.FIELD_HEIGHT));
        toggle.setFocusPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1));
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setToolTipText(AppMessages.get("auth.password.toggle.tooltip"));
        toggle.addActionListener(e -> {
            if (toggle.isSelected()) {
                field.setEchoChar((char) 0);
                toggle.setText("🐵");
            } else {
                field.setEchoChar('•');
                toggle.setText("🙈");
            }
        });
        return toggle;
    }

    public static void setFieldErrorState(JTextField field, boolean hasError) {
        if (field == null) {
            return;
        }
        field.putClientProperty("errorState", hasError);
        if (hasError) {
            field.setBorder(createErrorBorder());
        } else if (field.hasFocus()) {
            field.setBorder(AppTheme.createFocusedFieldBorder());
        } else {
            field.setBorder(createDefaultBorder());
        }
    }

    public static void highlightInvalid(JTextField field, boolean invalid) {
        if (field == null) {
            return;
        }
        field.setBorder(invalid
                ? BorderFactory.createLineBorder(ERROR_COLOR, 2)
                : AppTheme.createFieldBorder());
    }

    public static void setPrimaryButtonBusy(JButton button, boolean busy, String busyText, String idleText) {
        if (button == null) {
            return;
        }
        button.setEnabled(!busy);
        button.setText(busy ? busyText : idleText);
        if (busy) {
            button.setBackground(new Color(132, 148, 166));
            button.setCursor(Cursor.getDefaultCursor());
        } else {
            button.setBackground(AppTheme.PRIMARY);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    public static Border createDefaultBorder() {
        return new CompoundBorder(
                new LineBorder(AppTheme.BORDER_COLOR, 1, true),
                new EmptyBorder(7, 12, 7, 12));
    }

    public static Border createErrorBorder() {
        return new CompoundBorder(
                new LineBorder(ERROR_COLOR, 2, true),
                new EmptyBorder(6, 11, 6, 11));
    }
}
