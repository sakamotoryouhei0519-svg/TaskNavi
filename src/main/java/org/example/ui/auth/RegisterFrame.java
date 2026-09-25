package org.example.ui.auth;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.AuthService;
import org.example.ErrorDialogUtil;
import org.example.UserDao;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.Cursor;

/**
 * 【新規ユーザー登録画面クラス】
 */
public class RegisterFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private final UserDao userDao = new UserDao();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RegisterFrame.class);

    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JLabel errorLabel;

    public RegisterFrame() {
        AuthFormWidgets.applyWindowDefaults(
                this, AppMessages.get("auth.register.window.title"), 400, 520);

        JPanel mainPanel = new JPanel(new MigLayout(
                "fillx, insets 25 40 25 40, wrap", "[grow,fill]",
                "[]15[]2[]8[]2[]8[]2[]8[]12"));
        mainPanel.setBackground(AppTheme.BACKGROUND);

        JLabel titleLabel = AuthFormWidgets.createTitleLabel();
        JLabel subtitleLabel = AuthFormWidgets.createSubtitleLabel(AppMessages.get("auth.register.subtitle"));

        JLabel userLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.username"), 320, 18);
        usernameField = AuthFormWidgets.createStyledTextField();

        JLabel emailLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.register.email"), 320, 18);
        emailField = AuthFormWidgets.createStyledTextField();

        JLabel passLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.password"), 320, 18);
        passwordField = AuthFormWidgets.createStyledPasswordField(false);
        JPanel passwordWrapper = AuthFormWidgets.createPasswordTogglePanel(passwordField, 6);

        JLabel confirmPassLabel = AuthFormWidgets.createFormLabel(
                AppMessages.get("auth.register.password.confirm"), 320, 18);
        confirmPasswordField = AuthFormWidgets.createStyledPasswordField(false);
        JPanel confirmPasswordWrapper = AuthFormWidgets.createPasswordTogglePanel(confirmPasswordField, 6);

        errorLabel = AuthFormWidgets.createErrorLabel();

        registerButton = AuthFormWidgets.createPrimaryButton(
                AppMessages.get("auth.register.button.account"), 320, 42);

        JLabel loginLink = AuthFormWidgets.createLinkLabel(
                AppMessages.get("auth.register.login.link"),
                () -> AuthNavigation.openLoginAndDispose(this));

        mainPanel.add(titleLabel, "align center");
        mainPanel.add(subtitleLabel, "align center, wrap");
        mainPanel.add(userLabel, "align center");
        mainPanel.add(usernameField, "align center, wrap");
        mainPanel.add(emailLabel, "align center");
        mainPanel.add(emailField, "align center, wrap");
        mainPanel.add(passLabel, "align center");
        mainPanel.add(passwordWrapper, "align center, wrap");
        mainPanel.add(confirmPassLabel, "align center");
        mainPanel.add(confirmPasswordWrapper, "align center, wrap");
        mainPanel.add(errorLabel, "align center, wrap");
        mainPanel.add(registerButton, "align center, wrap");
        mainPanel.add(loginLink, "align center");
        add(mainPanel);

        getRootPane().setDefaultButton(registerButton);
        registerButton.addActionListener(e -> handleRegister());
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        clearErrorState();

        String validationMessage = AuthPasswordPolicy.validateRegisterForm(
                username, email, password, confirmPassword);
        if (validationMessage != null) {
            highlightValidationFields(username, email, password, confirmPassword);
            showInlineError(validationMessage);
            return;
        }

        String uniquenessMessage = AuthPasswordPolicy.validateRegisterUniqueness(
                username, email, userDao::isUsernameExists, userDao::isEmailRegistered);
        if (uniquenessMessage != null) {
            if (uniquenessMessage.equals(AppMessages.get("auth.register.validation.username.taken"))) {
                AuthFormWidgets.highlightInvalid(usernameField, true);
            } else {
                AuthFormWidgets.highlightInvalid(emailField, true);
            }
            showInlineError(uniquenessMessage);
            return;
        }

        setRegisterButtonBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return userDao.registerUser(username, password, email);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(RegisterFrame.this,
                                AppMessages.get("auth.register.success.message"),
                                AppMessages.get("auth.register.success.title"),
                                JOptionPane.INFORMATION_MESSAGE);
                        AuthNavigation.openLoginAndDispose(RegisterFrame.this);
                    } else {
                        showInlineError(AppMessages.get("auth.register.error.failed"));
                    }
                } catch (Exception ex) {
                    showInlineError(AppMessages.get("auth.register.error.exception"));
                    logger.error("ユーザー登録処理失敗", ex);
                } finally {
                    setRegisterButtonBusy(false);
                }
            }
        }.execute();
    }

    private void highlightValidationFields(
            String username, String email, String password, String confirmPassword) {
        if (username.isEmpty()) {
            AuthFormWidgets.highlightInvalid(usernameField, true);
        } else if (email.isEmpty() || !AuthPasswordPolicy.isValidEmail(email)) {
            AuthFormWidgets.highlightInvalid(emailField, true);
        } else if (password.isEmpty() || !password.equals(confirmPassword)
                || !AuthPasswordPolicy.isStrongPassword(password)) {
            AuthFormWidgets.highlightInvalid(passwordField, true);
            AuthFormWidgets.highlightInvalid(confirmPasswordField, true);
        }
    }

    private void setRegisterButtonBusy(boolean busy) {
        AuthFormWidgets.setPrimaryButtonBusy(
                registerButton,
                busy,
                AppMessages.get("auth.register.in.progress"),
                AppMessages.get("auth.register.button.account"));
        if (!busy) {
            registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    private void showInlineError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearErrorState() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        AuthFormWidgets.highlightInvalid(usernameField, false);
        AuthFormWidgets.highlightInvalid(emailField, false);
        AuthFormWidgets.highlightInvalid(passwordField, false);
        AuthFormWidgets.highlightInvalid(confirmPasswordField, false);
    }
}
