package org.example;

public final class AuthService {
    private AuthService() {
    }

    public static User authenticate(String username, String password) {
        if (!UserDao.authenticate(username, password)) {
            return null;
        }
        return UserDao.findByUsername(username);
    }

    public static boolean register(String username, String password, String email) {
        return UserDao.registerUser(username, password, email);
    }

    public static boolean resetPassword(String email, String verificationCode, String newPassword) {
        return UserDao.resetPasswordWithVerification(email, verificationCode, newPassword);
    }
}
