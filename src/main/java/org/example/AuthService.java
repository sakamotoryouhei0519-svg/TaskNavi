package org.example;

import org.example.util.AuthAttemptGuard;

/**
 * 認証ユースケース。試行制限と検証付きパスワード再設定を集約する。
 */
public final class AuthService {
    private static final UserDao userDao = new UserDao();

    private AuthService() {
    }

    public enum LoginStatus {
        SUCCESS,
        INVALID_CREDENTIALS,
        LOCKED
    }

    public static final class LoginResult {
        private final LoginStatus status;
        private final User user;
        private final long remainingLockSeconds;

        private LoginResult(LoginStatus status, User user, long remainingLockSeconds) {
            this.status = status;
            this.user = user;
            this.remainingLockSeconds = remainingLockSeconds;
        }

        public static LoginResult success(User user) {
            return new LoginResult(LoginStatus.SUCCESS, user, 0);
        }

        public static LoginResult invalid() {
            return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, 0);
        }

        public static LoginResult locked(long remainingSeconds) {
            return new LoginResult(LoginStatus.LOCKED, null, remainingSeconds);
        }

        public LoginStatus getStatus() {
            return status;
        }

        public User getUser() {
            return user;
        }

        public long getRemainingLockSeconds() {
            return remainingLockSeconds;
        }

        public boolean isSuccess() {
            return status == LoginStatus.SUCCESS;
        }
    }

    public static LoginResult authenticate(String username, String password) {
        String key = AuthAttemptGuard.loginKey(username);
        if (AuthAttemptGuard.isBlocked(key)) {
            return LoginResult.locked(AuthAttemptGuard.remainingLockSeconds(key));
        }

        if (!userDao.authenticate(username, password)) {
            AuthAttemptGuard.recordFailure(key);
            if (AuthAttemptGuard.isBlocked(key)) {
                return LoginResult.locked(AuthAttemptGuard.remainingLockSeconds(key));
            }
            return LoginResult.invalid();
        }

        AuthAttemptGuard.recordSuccess(key);
        return LoginResult.success(userDao.findByUsername(username));
    }

    public static boolean register(String username, String password, String email) {
        return userDao.registerUser(username, password, email);
    }

    public static boolean isEmailRegistered(String email) {
        return userDao.isEmailRegistered(email);
    }

    public static boolean canSendResetCode(String email) {
        return !AuthAttemptGuard.isBlocked(AuthAttemptGuard.sendCodeKey(email));
    }

    public static long remainingSendLockSeconds(String email) {
        return AuthAttemptGuard.remainingLockSeconds(AuthAttemptGuard.sendCodeKey(email));
    }

    public static void recordSendCodeFailure(String email) {
        AuthAttemptGuard.recordFailure(AuthAttemptGuard.sendCodeKey(email));
    }

    public static void recordSendCodeSuccess(String email) {
        AuthAttemptGuard.recordSuccess(AuthAttemptGuard.sendCodeKey(email));
    }

    public static boolean canVerifyResetCode(String email) {
        return !AuthAttemptGuard.isBlocked(AuthAttemptGuard.verifyKey(email));
    }

    public static long remainingVerifyLockSeconds(String email) {
        return AuthAttemptGuard.remainingLockSeconds(AuthAttemptGuard.verifyKey(email));
    }

    /**
     * コードを検証するだけ（消費しない）。失敗時は試行回数を加算する。
     */
    public static boolean checkVerificationCode(String email, String code) {
        String key = AuthAttemptGuard.verifyKey(email);
        if (AuthAttemptGuard.isBlocked(key)) {
            return false;
        }
        boolean ok = VerificationManager.verifyCode(email, code);
        if (ok) {
            AuthAttemptGuard.recordSuccess(key);
            return true;
        }
        AuthAttemptGuard.recordFailure(key);
        return false;
    }

    /**
     * 認証コードを再検証してパスワードを更新する（成功時にコードを消費）。
     */
    public static boolean resetPassword(String email, String verificationCode, String newPassword) {
        String key = AuthAttemptGuard.verifyKey(email);
        if (AuthAttemptGuard.isBlocked(key)) {
            return false;
        }
        boolean ok = userDao.resetPasswordWithVerification(email, verificationCode, newPassword);
        if (ok) {
            AuthAttemptGuard.recordSuccess(key);
            AuthAttemptGuard.recordSuccess(AuthAttemptGuard.sendCodeKey(email));
        } else {
            AuthAttemptGuard.recordFailure(key);
        }
        return ok;
    }
}
