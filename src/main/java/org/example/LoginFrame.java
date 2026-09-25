package org.example;

import net.miginfocom.swing.MigLayout;
import org.example.ui.auth.AuthFormWidgets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

/**
 * 【ログイン画面クラス】
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String PREF_LAST_USERNAME = "tasknavi.last.username";

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JToggleButton showPasswordButton;
    private JLabel errorLabel;
    private JCheckBox rememberMeCheckBox;

    public LoginFrame() {
        AuthFormWidgets.applyWindowDefaults(
                this,
                AppMessages.get("auth.login.window.title"),
                UiConstants.WINDOW_WIDTH_LOGIN,
                UiConstants.WINDOW_HEIGHT_LOGIN);

        JPanel mainPanel = new JPanel(new MigLayout(
                "fillx, insets 30 40 30 40, wrap", "[grow,fill]",
                "[]20[]4[]12[]4[]8[]8[]12[]15[]8"));
        mainPanel.setBackground(AppTheme.BACKGROUND);

        JLabel titleLabel = AuthFormWidgets.createTitleLabel();
        JLabel subtitleLabel = AuthFormWidgets.createSubtitleLabel(AppMessages.get("auth.login.subtitle"));

        JLabel userLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.username"));
        usernameField = AuthFormWidgets.createStyledTextField(
                AppMessages.get("auth.login.placeholder.username"), true);

        JLabel passLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.password"));
        JPanel passwordWrapper = new JPanel(new MigLayout("fill, insets 0, gap 0", "[grow][]", "[grow]"));
        passwordWrapper.setOpaque(false);
        passwordWrapper.setMaximumSize(new Dimension(UiConstants.FIELD_WIDTH_STANDARD, UiConstants.FIELD_HEIGHT));
        passwordWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = AuthFormWidgets.createStyledPasswordField(true);
        showPasswordButton = AuthFormWidgets.createPasswordToggle(passwordField);
        showPasswordButton.setBorder(new LineBorder(AppTheme.BORDER_COLOR, 1));
        passwordWrapper.add(passwordField, "grow");
        passwordWrapper.add(showPasswordButton);

        errorLabel = AuthFormWidgets.createErrorLabel();

        rememberMeCheckBox = new JCheckBox(AppMessages.get("auth.login.remember"));
        rememberMeCheckBox.setFont(AppTheme.FONT_MAIN);
        rememberMeCheckBox.setOpaque(false);
        rememberMeCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        rememberMeCheckBox.setFocusPainted(false);

        loginButton = AuthFormWidgets.createPrimaryButton(
                AppMessages.get("auth.login.button"),
                UiConstants.FIELD_WIDTH_STANDARD,
                UiConstants.BUTTON_HEIGHT_LARGE);

        JLabel registerLink = AuthFormWidgets.createLinkLabel(
                AppMessages.htmlUnderline(AppMessages.get("auth.login.link.register")), null);
        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new RegisterFrame().setVisible(true);
                dispose();
            }
        });

        JLabel forgotPasswordLink = AuthFormWidgets.createLinkLabel(
                AppMessages.htmlUnderline(AppMessages.get("auth.login.link.forgot")), null);
        forgotPasswordLink.setForeground(AppTheme.TEXT_MUTED);
        forgotPasswordLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new ResetPasswordFrame().setVisible(true);
                dispose();
            }
        });

        mainPanel.add(titleLabel, "align center");
        mainPanel.add(subtitleLabel, "align center, wrap");
        mainPanel.add(userLabel, "align center");
        mainPanel.add(usernameField, "align center, wrap");
        mainPanel.add(passLabel, "align center");
        mainPanel.add(passwordWrapper, "align center, wrap");
        mainPanel.add(rememberMeCheckBox, "align center, wrap");
        mainPanel.add(errorLabel, "align center, wrap");
        mainPanel.add(loginButton, "align center, wrap");
        mainPanel.add(registerLink, "align center, wrap");
        mainPanel.add(forgotPasswordLink, "align center");
        add(mainPanel);

        String savedUsername = loadSavedUsername();
        if (!savedUsername.isEmpty()) {
            usernameField.setText(savedUsername);
            rememberMeCheckBox.setSelected(true);
        }

        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e -> handleLogin());
        DocumentListener clearError = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearInlineError();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearInlineError();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearInlineError();
            }
        };
        usernameField.getDocument().addDocumentListener(clearError);
        passwordField.getDocument().addDocumentListener(clearError);
    }

    private void clearInlineError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        AuthFormWidgets.setFieldErrorState(usernameField, false);
        AuthFormWidgets.setFieldErrorState(passwordField, false);
    }

    private void showInlineError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        AuthFormWidgets.setFieldErrorState(usernameField, usernameField.getText().trim().isEmpty());
        AuthFormWidgets.setFieldErrorState(passwordField, passwordField.getPassword().length == 0);
    }

    private void setLoginInProgress(boolean inProgress) {
        AuthFormWidgets.setPrimaryButtonBusy(
                loginButton,
                inProgress,
                AppMessages.get("auth.login.in.progress"),
                AppMessages.get("auth.login.button"));
    }

    private String loadSavedUsername() {
        Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
        return prefs.get(PREF_LAST_USERNAME, "");
    }

    private void saveRememberedUsername(String username, boolean enabled) {
        Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
        if (enabled && username != null && !username.isBlank()) {
            prefs.put(PREF_LAST_USERNAME, username);
        } else {
            prefs.remove(PREF_LAST_USERNAME);
        }
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        clearInlineError();

        boolean hasUsernameError = username.isEmpty();
        boolean hasPasswordError = password.isEmpty();
        if (hasUsernameError || hasPasswordError) {
            if (hasUsernameError) {
                AuthFormWidgets.setFieldErrorState(usernameField, true);
            }
            if (hasPasswordError) {
                AuthFormWidgets.setFieldErrorState(passwordField, true);
            }
            showInlineError(AppMessages.get("auth.login.error.empty"));
            if (hasUsernameError) {
                usernameField.requestFocusInWindow();
            } else {
                passwordField.requestFocusInWindow();
            }
            return;
        }

        setLoginInProgress(true);
        SwingWorker<AuthService.LoginResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthService.LoginResult doInBackground() {
                return AuthService.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    AuthService.LoginResult result = get();
                    setLoginInProgress(false);

                    if (result != null && result.isSuccess()) {
                        saveRememberedUsername(username, rememberMeCheckBox.isSelected());
                        UserSession.login(result.getUser());
                        TaskService taskService = new TaskService(new TaskDao());
                        new MainFrame(taskService).setVisible(true);
                        LoginFrame.this.dispose();
                    } else if (result != null && result.getStatus() == AuthService.LoginStatus.LOCKED) {
                        AuthFormWidgets.setFieldErrorState(usernameField, true);
                        AuthFormWidgets.setFieldErrorState(passwordField, true);
                        showInlineError(AppMessages.format(
                                "auth.login.error.locked",
                                "試行回数が上限に達しました。{0}秒後に再試行してください。",
                                result.getRemainingLockSeconds()));
                        passwordField.requestFocusInWindow();
                    } else {
                        AuthFormWidgets.setFieldErrorState(usernameField, true);
                        AuthFormWidgets.setFieldErrorState(passwordField, true);
                        showInlineError(AppMessages.get("auth.login.error.invalid"));
                        passwordField.selectAll();
                        passwordField.requestFocusInWindow();
                    }
                } catch (Exception ex) {
                    setLoginInProgress(false);
                    AuthFormWidgets.setFieldErrorState(usernameField, true);
                    AuthFormWidgets.setFieldErrorState(passwordField, true);
                    ErrorDialogUtil.showError(LoginFrame.this, ex);
                    showInlineError(AppMessages.get("auth.login.error.failed"));
                }
            }
        };
        worker.execute();
    }
}
