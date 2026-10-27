package org.tasknavi.ui.auth;

import org.tasknavi.AppMessages;
import org.tasknavi.persistence.SampleDataUtil;
import org.tasknavi.service.AuthService;
import org.tasknavi.service.TaskService;
import org.tasknavi.service.UserSession;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.ErrorDialogUtil;
import org.tasknavi.ui.main.MainFrame;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

/**
 * ログイン画面。ログインできたらメインの窓を開く。
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String PREF_LAST_USERNAME = "tasknavi.last.username";

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JToggleButton showPasswordButton;
    private JTextArea errorArea;
    private JCheckBox rememberMeCheckBox;

    public LoginFrame() {
        AuthFormWidgets.applyWindowDefaults(this, AppMessages.get("auth.login.window.title"));

        JLabel titleLabel = AuthFormWidgets.createTitleLabel();
        JLabel subtitleLabel = AuthFormWidgets.createSubtitleLabel(AppMessages.get("auth.login.subtitle"));

        JLabel userLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.username"));
        usernameField = AuthFormWidgets.createStyledTextField();

        JLabel passLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.password"));
        passwordField = AuthFormWidgets.createStyledPasswordField();
        showPasswordButton = AuthFormWidgets.createPasswordToggle(passwordField);
        JPanel passwordRow = AuthFormWidgets.createPasswordRow(passwordField, showPasswordButton);

        errorArea = AuthFormWidgets.createErrorArea();

        rememberMeCheckBox = AuthFormWidgets.createCheckBox(AppMessages.get("auth.login.remember"));

        loginButton = AuthFormWidgets.createPrimaryButton(AppMessages.get("auth.login.button"));

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

        int fieldHeight = AuthFormWidgets.FIELD_HEIGHT;
        JPanel card = AuthFormWidgets.createCard();
        card.add(titleLabel, "growx");
        card.add(subtitleLabel, "growx, gaptop 4");
        card.add(userLabel, "growx, gaptop 24");
        card.add(usernameField, "growx, gaptop 6, h " + fieldHeight + "!");
        card.add(passLabel, "growx, gaptop 14");
        card.add(passwordRow, "growx, gaptop 6");
        card.add(rememberMeCheckBox, "gaptop 12");
        card.add(errorArea, AuthFormWidgets.ERROR_AREA_CONSTRAINTS);
        card.add(loginButton, "growx, gaptop 4, h " + AuthFormWidgets.BUTTON_HEIGHT + "!");
        card.add(registerLink, "growx, gaptop 18");
        card.add(forgotPasswordLink, "growx, gaptop 8");
        AuthFormWidgets.showCard(this, card);

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
        AuthFormWidgets.clearError(errorArea);
        AuthFormWidgets.setFieldErrorState(usernameField, false);
        AuthFormWidgets.setFieldErrorState(passwordField, false);
    }

    private void showInlineError(String message) {
        AuthFormWidgets.showError(errorArea, message);
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
                AuthService.LoginResult result = AuthService.authenticate(username, password);
                if (result != null && result.isSuccess() && result.getUser() != null) {
                    SampleDataUtil.prepareLedgerForUser(result.getUser().getId());
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    AuthService.LoginResult result = get();
                    setLoginInProgress(false);

                    if (result != null && result.isSuccess()) {
                        saveRememberedUsername(username, rememberMeCheckBox.isSelected());
                        UserSession.login(result.getUser());
                        MainFrame mainFrame = new MainFrame(TaskService.createDefault());
                        mainFrame.setVisible(true);
                        mainFrame.startDueReminders();
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
