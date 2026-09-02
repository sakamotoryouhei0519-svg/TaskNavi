package org.example;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * 【メール認証用の送信用ユーティリティ】
 * このクラスは、ユーザーがログインや登録時に入力した認証コードを
 * SMTP 経由でメールにして送る役割を持ちます。
 */
public class EmailUtil {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String SENDER_EMAIL_ENV = "TASKNAVI_EMAIL";
    private static final String SENDER_PASSWORD_ENV = "TASKNAVI_EMAIL_PASSWORD";

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
            System.err.println("Email configuration is missing. Set TASKNAVI_EMAIL and TASKNAVI_EMAIL_PASSWORD.");
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
            message.setFrom(new InternetAddress(senderEmail, "TaskNavi サポート"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("【TaskNavi】本人確認用の認証コード");

            String body = "TaskNaviをご利用いただきありがとうございます。<br><br>"
                    + "認証コード: <strong>" + authCode + "</strong><br><br>"
                    + "このコードを画面に入力して手続きを完了してください。";

            if (htmlEnabled) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText("TaskNaviをご利用いただきありがとうございます。\n\n"
                        + "認証コード: " + authCode + "\n\n"
                        + "このコードを画面に入力して手続きを完了してください。");
            }

            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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