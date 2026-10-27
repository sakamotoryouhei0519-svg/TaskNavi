package org.tasknavi.persistence;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasknavi.AppException;
import org.tasknavi.model.User;
import org.tasknavi.model.UserRole;
import org.tasknavi.service.VerificationManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * users テーブルの読み書き（登録・ログイン・パスワード再設定）。パスワードは BCrypt のハッシュだけを保存する。
 */
public class UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    /** @return 登録できたら true */
    public boolean registerUser(String username, String password, String email) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email);
            pstmt.setString(4, UserRole.USER.getValue());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("ユーザー登録エラー", e);
            return false;
        }
    }

    /** @return ユーザー名とパスワードが合っていれば true */
    public boolean authenticate(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (storedHash != null && storedHash.startsWith("$2")) {
                        return BCrypt.checkpw(password, storedHash);
                    }
                    // 平文で保存されたパスワードは受け付けない
                    logger.warn("非BCryptパスワードを検出したため認証を拒否しました: username={}", username);
                }
            }
        } catch (SQLException e) {
            logger.error("認証エラー", e);
        }
        return false;
    }

    /** メールで送った認証コードを確かめてから、新しいパスワードにする。 */
    public boolean resetPasswordWithVerification(String email, String verificationCode, String newPassword) {
        if (email == null || verificationCode == null || newPassword == null) {
            return false;
        }
        if (!VerificationManager.verifyCode(email, verificationCode)) {
            return false;
        }

        try {
            return Database.withTransaction(connection -> {
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                String sql = "UPDATE users SET password = ? WHERE email = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, hashedPassword);
                    pstmt.setString(2, email);
                    boolean success = pstmt.executeUpdate() > 0;
                    if (success) {
                        VerificationManager.clearCode(email);
                    }
                    return success;
                }
            });
        } catch (SQLException e) {
            logger.error("パスワードリセットエラー", e);
            return false;
        }
    }

    /**
     * ログイン省略用。同じユーザー名がいればそれを返し、いなければ作る。
     */
    public User ensureDemoUser(String username, String email) {
        User existing = findByUsername(username);
        if (existing != null) {
            return existing;
        }
        if (!registerUser(username, UUID.randomUUID().toString(), email)) {
            throw AppException.database("デモ用ユーザーを用意できませんでした。");
        }
        User created = findByUsername(username);
        if (created == null) {
            throw AppException.database("デモ用ユーザーを用意できませんでした。");
        }
        return created;
    }

    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, username, email, role FROM users WHERE username = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("username"),
                            null
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("ユーザー取得エラー", e);
        }
        return null;
    }

    public boolean isUsernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isEmailRegistered(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
