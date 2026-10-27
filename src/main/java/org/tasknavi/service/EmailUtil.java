package org.tasknavi.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.tasknavi.AppMessages;

import java.util.Properties;

/**
 * パスワード再設定の認証コードをメールで送る（SMTP）。送信元は環境変数 TASKNAVI_EMAIL / TASKNAVI_EMAIL_PASSWORD。
 */
public class EmailUtil {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String SENDER_EMAIL_ENV = "TASKNAVI_EMAIL";
    private static final String SENDER_PASSWORD_ENV = "TASKNAVI_EMAIL_PASSWORD";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailUtil.class);

    /**
     * 認証コードを送信する。
     * 本番環境では環境変数 TASKNAVI_EMAIL / TASKNAVI_EMAIL_PASSWORD を設定して利用する。
     */
    public static boolean sendAuthCode(String toEmail, String authCode) {
        return sendAuthCode(toEmail, authCode, true);
    }

    /**
     * HTML 形式のメールで認証コードを送信する。
     */
    public static boolean sendAuthCode(String toEmail, String authCode, boolean htmlEnabled) {
        String senderEmail = resolveRequiredEnv(SENDER_EMAIL_ENV);
        String senderPassword = resolveRequiredEnv(SENDER_PASSWORD_ENV);

        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            logger.warn("メールの送信元が設定されていません。環境変数 {} と {} を設定してください。",
                    SENDER_EMAIL_ENV, SENDER_PASSWORD_ENV);
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, AppMessages.get("email.sender.name")));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(AppMessages.get("email.subject.auth.code"));

            String body = AppMessages.format("email.body.auth.code.html", "", authCode);

            if (htmlEnabled) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText(AppMessages.format("email.body.auth.code.text", "", authCode));
            }

            Transport.send(message);
            return true;
        } catch (Exception e) {
            logger.error("メール送信エラー", e);
            return false;
        }
    }

    public static boolean isConfigured() {
        return !resolveRequiredEnv(SENDER_EMAIL_ENV).isEmpty()
                && !resolveRequiredEnv(SENDER_PASSWORD_ENV).isEmpty();
    }

    private static String resolveRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return value == null ? "" : value.trim();
    }
}
