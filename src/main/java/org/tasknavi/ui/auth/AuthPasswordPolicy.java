package org.tasknavi.ui.auth;

import org.tasknavi.AppMessages;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 認証画面の入力ポリシー（メール形式・パスワード強度）。
 */
public final class AuthPasswordPolicy {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private AuthPasswordPolicy() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");
        return hasLetter && hasDigit && hasSymbol;
    }

    /**
     * 登録フォームのローカル検証。DB 重複チェックは呼び出し側で行う。
     * @return エラーメッセージ。問題なければ null
     */
    public static String validateRegisterForm(
            String username,
            String email,
            String password,
            String confirmPassword) {
        if (username == null || username.isEmpty()) {
            return AppMessages.get("auth.register.validation.username.required");
        }
        if (email == null || email.isEmpty()) {
            return AppMessages.get("auth.register.validation.email.required");
        }
        if (!isValidEmail(email)) {
            return AppMessages.get("auth.register.validation.email.format");
        }
        if (password == null || password.isEmpty()) {
            return AppMessages.get("auth.register.validation.password.required");
        }
        if (!password.equals(confirmPassword)) {
            return AppMessages.get("auth.register.error.mismatch");
        }
        if (!isStrongPassword(password)) {
            return AppMessages.get("auth.register.validation.password.policy");
        }
        return null;
    }

    public static String validateRegisterUniqueness(
            String username,
            String email,
            Predicate<String> usernameExists,
            Predicate<String> emailRegistered) {
        if (usernameExists != null && usernameExists.test(username)) {
            return AppMessages.get("auth.register.validation.username.taken");
        }
        if (emailRegistered != null && emailRegistered.test(email)) {
            return AppMessages.get("auth.register.validation.email.taken");
        }
        return null;
    }
}
