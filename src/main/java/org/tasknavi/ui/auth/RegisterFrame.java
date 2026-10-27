package org.tasknavi.ui.auth;

import org.tasknavi.AppMessages;
import org.tasknavi.persistence.UserDao;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * ユーザー登録画面。
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
    private JTextArea errorArea;

    public RegisterFrame() {
        AuthFormWidgets.applyWindowDefaults(this, AppMessages.get("auth.register.window.title"));

        JLabel titleLabel = AuthFormWidgets.createTitleLabel();
        JLabel subtitleLabel = AuthFormWidgets.createSubtitleLabel(AppMessages.get("auth.register.subtitle"));

        JLabel userLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.username"));
        usernameField = AuthFormWidgets.createStyledTextField();

        JLabel emailLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.register.email"));
        emailField = AuthFormWidgets.createStyledTextField();

        JLabel passLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.login.password"));
        passwordField = AuthFormWidgets.createStyledPasswordField();
        JPanel passwordRow = AuthFormWidgets.createPasswordRow(
                passwordField, AuthFormWidgets.createPasswordToggle(passwordField));

        JLabel confirmPassLabel = AuthFormWidgets.createFormLabel(
                AppMessages.get("auth.register.password.confirm"));
        confirmPasswordField = AuthFormWidgets.createStyledPasswordField();
        JPanel confirmPasswordRow = AuthFormWidgets.createPasswordRow(
                confirmPasswordField, AuthFormWidgets.createPasswordToggle(confirmPasswordField));

        errorArea = AuthFormWidgets.createErrorArea();

        registerButton = AuthFormWidgets.createPrimaryButton(AppMessages.get("auth.register.button.account"));

        JLabel loginLink = AuthFormWidgets.createLinkLabel(
                AppMessages.get("auth.register.login.link"),
                () -> AuthNavigation.openLoginAndDispose(this));

        String field = "growx, gaptop 6, h " + AuthFormWidgets.FIELD_HEIGHT + "!";
        JPanel card = AuthFormWidgets.createCard();
        card.add(titleLabel, "growx");
        card.add(subtitleLabel, "growx, gaptop 4");
        card.add(userLabel, "growx, gaptop 24");
        card.add(usernameField, field);
        card.add(emailLabel, "growx, gaptop 14");
        card.add(emailField, field);
        card.add(passLabel, "growx, gaptop 14");
        card.add(passwordRow, "growx, gaptop 6");
        card.add(confirmPassLabel, "growx, gaptop 14");
        card.add(confirmPasswordRow, "growx, gaptop 6");
        card.add(errorArea, AuthFormWidgets.ERROR_AREA_CONSTRAINTS);
        card.add(registerButton, "growx, gaptop 4, h " + AuthFormWidgets.BUTTON_HEIGHT + "!");
        card.add(loginLink, "growx, gaptop 18");
        AuthFormWidgets.showCard(this, card);

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
                AuthFormWidgets.setFieldErrorState(usernameField, true);
            } else {
                AuthFormWidgets.setFieldErrorState(emailField, true);
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
            AuthFormWidgets.setFieldErrorState(usernameField, true);
        } else if (email.isEmpty() || !AuthPasswordPolicy.isValidEmail(email)) {
            AuthFormWidgets.setFieldErrorState(emailField, true);
        } else if (password.isEmpty() || !password.equals(confirmPassword)
                || !AuthPasswordPolicy.isStrongPassword(password)) {
            AuthFormWidgets.setFieldErrorState(passwordField, true);
            AuthFormWidgets.setFieldErrorState(confirmPasswordField, true);
        }
    }

    private void setRegisterButtonBusy(boolean busy) {
        AuthFormWidgets.setPrimaryButtonBusy(
                registerButton,
                busy,
                AppMessages.get("auth.register.in.progress"),
                AppMessages.get("auth.register.button.account"));
    }

    private void showInlineError(String message) {
        AuthFormWidgets.showError(errorArea, message);
    }

    private void clearErrorState() {
        AuthFormWidgets.clearError(errorArea);
        AuthFormWidgets.setFieldErrorState(usernameField, false);
        AuthFormWidgets.setFieldErrorState(emailField, false);
        AuthFormWidgets.setFieldErrorState(passwordField, false);
        AuthFormWidgets.setFieldErrorState(confirmPasswordField, false);
    }
}
