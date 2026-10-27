package org.tasknavi.ui.auth;

import org.tasknavi.AppMessages;
import org.tasknavi.service.AuthService;
import org.tasknavi.service.EmailUtil;
import org.tasknavi.service.VerificationManager;
import org.tasknavi.ui.ErrorDialogUtil;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;

/**
 * パスワード再設定画面。メールで送った認証コードを確かめてから変える。
 */
public class ResetPasswordFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResetPasswordFrame.class);

    private JTextField emailField;
    private JTextField authCodeField;
    private JPasswordField newPasswordField;
    private JButton sendCodeButton;
    private JButton verifyButton;
    private JButton resetButton;
    private JToggleButton showPasswordButton;

    private String currentEmail = null;
    private String verifiedAuthCode = null;
    private boolean emailVerified = false;

    public ResetPasswordFrame() {
        AuthFormWidgets.applyWindowDefaults(this, AppMessages.get("auth.reset.window.title"));

        JLabel titleLabel = AuthFormWidgets.createTitleLabel();
        JLabel subtitleLabel = AuthFormWidgets.createSubtitleLabel(AppMessages.get("auth.reset.subtitle"));

        JLabel emailLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.register.email"));
        emailField = AuthFormWidgets.createStyledTextField();

        sendCodeButton = AuthFormWidgets.createPrimaryButton(AppMessages.get("auth.reset.button.send"));
        sendCodeButton.addActionListener(e -> handleSendAuthCode());

        JLabel authCodeLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.reset.code.label"));
        authCodeField = AuthFormWidgets.createStyledTextField();
        authCodeField.setEnabled(false);

        verifyButton = AuthFormWidgets.createPrimaryButton(AppMessages.get("auth.reset.button.verify"));
        verifyButton.setEnabled(false);
        verifyButton.addActionListener(e -> handleVerifyAuthCode());

        JLabel passLabel = AuthFormWidgets.createFormLabel(AppMessages.get("auth.reset.new.password"));
        newPasswordField = AuthFormWidgets.createStyledPasswordField();
        newPasswordField.setEnabled(false);
        showPasswordButton = AuthFormWidgets.createPasswordToggle(newPasswordField);
        JPanel passwordRow = AuthFormWidgets.createPasswordRow(newPasswordField, showPasswordButton);

        resetButton = AuthFormWidgets.createPrimaryButton(AppMessages.get("auth.reset.button.update"));
        resetButton.setEnabled(false);

        JLabel loginLink = AuthFormWidgets.createLinkLabel(
                AppMessages.get("auth.reset.login.link"),
                () -> AuthNavigation.openLoginAndDispose(this));

        String field = "growx, gaptop 6, h " + AuthFormWidgets.FIELD_HEIGHT + "!";
        String stepButton = "growx, gaptop 10, h " + AuthFormWidgets.FIELD_HEIGHT + "!";
        JPanel card = AuthFormWidgets.createCard();
        card.add(titleLabel, "growx");
        card.add(subtitleLabel, "growx, gaptop 4");
        card.add(emailLabel, "growx, gaptop 24");
        card.add(emailField, field);
        card.add(sendCodeButton, stepButton);
        card.add(authCodeLabel, "growx, gaptop 20");
        card.add(authCodeField, field);
        card.add(verifyButton, stepButton);
        card.add(passLabel, "growx, gaptop 20");
        card.add(passwordRow, "growx, gaptop 6");
        card.add(resetButton, "growx, gaptop 20, h " + AuthFormWidgets.BUTTON_HEIGHT + "!");
        card.add(loginLink, "growx, gaptop 18");
        AuthFormWidgets.showCard(this, card);

        resetButton.addActionListener(e -> handleResetPassword());
    }

    private void handleSendAuthCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.email.required"),
                    AppMessages.get("dialog.error.input"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!AuthService.canSendResetCode(email)) {
            JOptionPane.showMessageDialog(this, AppMessages.format(
                            "auth.reset.error.send.locked",
                            "送信の試行回数が上限に達しました。{0}秒後に再試行してください。",
                            AuthService.remainingSendLockSeconds(email)),
                    AppMessages.get("common.dialog.error.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!AuthService.isEmailRegistered(email)) {
            AuthService.recordSendCodeFailure(email);
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.email.unregistered"),
                    AppMessages.get("common.dialog.error.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        setSendCodeButtonBusy(true);
        emailField.setEnabled(false);
        String authCode = VerificationManager.generateCode(email);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return EmailUtil.sendAuthCode(email, authCode);
            }

            @Override
            protected void done() {
                try {
                    boolean emailSent = get();
                    if (emailSent) {
                        currentEmail = email;
                        verifiedAuthCode = null;
                        emailVerified = false;
                        AuthService.recordSendCodeSuccess(email);
                        JOptionPane.showMessageDialog(ResetPasswordFrame.this,
                                AppMessages.get("auth.reset.message.sent"),
                                AppMessages.get("auth.reset.title.send.success"),
                                JOptionPane.INFORMATION_MESSAGE);
                        authCodeField.setEnabled(true);
                        verifyButton.setEnabled(true);
                        sendCodeButton.setEnabled(false);
                        emailField.setEnabled(false);
                    } else {
                        AuthService.recordSendCodeFailure(email);
                        JOptionPane.showMessageDialog(ResetPasswordFrame.this,
                                AppMessages.get("auth.reset.error.email.send"),
                                AppMessages.get("auth.reset.title.send.failure"),
                                JOptionPane.ERROR_MESSAGE);
                        setSendCodeButtonBusy(false);
                        authCodeField.setEnabled(false);
                        verifyButton.setEnabled(false);
                        emailField.setEnabled(true);
                        VerificationManager.clearCode(email);
                    }
                } catch (Exception ex) {
                    AuthService.recordSendCodeFailure(email);
                    ErrorDialogUtil.showError(ResetPasswordFrame.this,
                            AppMessages.get("auth.reset.error.email.processing"));
                    logger.error("メール送信時の予期せぬエラー", ex);
                    setSendCodeButtonBusy(false);
                    authCodeField.setEnabled(false);
                    verifyButton.setEnabled(false);
                    emailField.setEnabled(true);
                    VerificationManager.clearCode(email);
                }
            }
        };
        worker.execute();
    }

    private void handleVerifyAuthCode() {
        String inputCode = authCodeField.getText().trim();

        if (inputCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.code.required"),
                    AppMessages.get("dialog.error.input"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (currentEmail == null) {
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.verify.required"),
                    AppMessages.get("dialog.error.input"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!AuthService.canVerifyResetCode(currentEmail)) {
            JOptionPane.showMessageDialog(this, AppMessages.format(
                            "auth.reset.error.verify.locked",
                            "認証コードの試行回数が上限に達しました。{0}秒後に再試行してください。",
                            AuthService.remainingVerifyLockSeconds(currentEmail)),
                    AppMessages.get("auth.reset.title.verify.failure"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (AuthService.checkVerificationCode(currentEmail, inputCode)) {
            emailVerified = true;
            verifiedAuthCode = inputCode;
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.message.verified"),
                    AppMessages.get("auth.reset.title.verify.success"), JOptionPane.INFORMATION_MESSAGE);
            newPasswordField.setEnabled(true);
            resetButton.setEnabled(true);
            verifyButton.setEnabled(false);
            authCodeField.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.code.invalid"),
                    AppMessages.get("auth.reset.title.verify.failure"), JOptionPane.ERROR_MESSAGE);
            if (!AuthService.canVerifyResetCode(currentEmail)) {
                resetWorkflowToInitialState();
            }
        }
    }

    private void handleResetPassword() {
        String newPassword = new String(newPasswordField.getPassword()).trim();

        if (currentEmail == null || !emailVerified || verifiedAuthCode == null) {
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.verify.required"),
                    AppMessages.get("dialog.error.input"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.password.required"),
                    AppMessages.get("dialog.error.input"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (newPassword.length() < 6) {
            JOptionPane.showMessageDialog(this, AppMessages.get("auth.reset.error.password.length"),
                    AppMessages.get("dialog.error.input"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        setResetButtonBusy(true);
        final String email = currentEmail;
        final String code = verifiedAuthCode;
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return AuthService.resetPassword(email, code, newPassword);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(ResetPasswordFrame.this,
                                AppMessages.get("auth.reset.message.updated"),
                                AppMessages.get("auth.reset.title.update.success"),
                                JOptionPane.INFORMATION_MESSAGE);
                        AuthNavigation.openLoginAndDispose(ResetPasswordFrame.this);
                    } else {
                        JOptionPane.showMessageDialog(ResetPasswordFrame.this,
                                AppMessages.get("auth.reset.error.update"),
                                AppMessages.get("auth.reset.title.update.failure"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ErrorDialogUtil.showError(ResetPasswordFrame.this,
                            AppMessages.get("auth.reset.error.update.processing"));
                    logger.error("パスワード更新時の予期せぬエラー", ex);
                } finally {
                    setResetButtonBusy(false);
                }
            }
        };
        worker.execute();
    }

    private void setSendCodeButtonBusy(boolean busy) {
        AuthFormWidgets.setPrimaryButtonBusy(
                sendCodeButton,
                busy,
                AppMessages.get("auth.reset.send.in.progress"),
                AppMessages.get("auth.reset.button.send"));
    }

    private void setResetButtonBusy(boolean busy) {
        AuthFormWidgets.setPrimaryButtonBusy(
                resetButton,
                busy,
                AppMessages.get("auth.reset.update.in.progress"),
                AppMessages.get("auth.reset.button.update"));
    }

    private void resetWorkflowToInitialState() {
        currentEmail = null;
        verifiedAuthCode = null;
        emailVerified = false;
        emailField.setEnabled(true);
        emailField.setText("");
        authCodeField.setText("");
        authCodeField.setEnabled(false);
        verifyButton.setEnabled(false);
        newPasswordField.setText("");
        newPasswordField.setEnabled(false);
        resetButton.setEnabled(false);
        setSendCodeButtonBusy(false);
        showPasswordButton.setSelected(false);
    }
}
