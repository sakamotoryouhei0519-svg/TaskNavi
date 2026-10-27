package org.tasknavi.service;

import org.tasknavi.AppException;
import org.tasknavi.persistence.Database;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * メール単位で認証コードを発行・検証する。コードは SQLite に保存し、5分間有効。
 */
public final class VerificationManager {
    private static final long CODE_VALIDITY_MILLIS = 5 * 60 * 1000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private VerificationManager() {
        throw new AssertionError("Utility class");
    }

    public static String generateCode(String email) {
        String key = normalizeEmail(email);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        String value = String.valueOf(code);
        long now = System.currentTimeMillis();
        String sql = "INSERT INTO verification_codes (email, code, generated_at) VALUES (?, ?, ?) "
                + "ON CONFLICT(email) DO UPDATE SET code = excluded.code, generated_at = excluded.generated_at";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setLong(3, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw AppException.database("認証コードを保存できませんでした。", e);
        }
        return value;
    }

    public static boolean verifyCode(String email, String inputCode) {
        String key = normalizeEmail(email);
        if (key.isEmpty() || inputCode == null || inputCode.trim().isEmpty()) {
            return false;
        }

        purgeExpired();
        String sql = "SELECT code, generated_at FROM verification_codes WHERE email = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                long generatedAt = rs.getLong("generated_at");
                if (isExpired(generatedAt)) {
                    clearCode(key);
                    return false;
                }
                return rs.getString("code").equals(inputCode.trim());
            }
        } catch (SQLException e) {
            throw AppException.database("認証コードを確認できませんでした。", e);
        }
    }

    public static void clearCode(String email) {
        String key = normalizeEmail(email);
        if (key.isEmpty()) {
            return;
        }
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM verification_codes WHERE email = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw AppException.database("認証コードを削除できませんでした。", e);
        }
    }

    public static void clearAll() {
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM verification_codes")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw AppException.database("認証コードを削除できませんでした。", e);
        }
    }

    static int sizeForTesting() {
        purgeExpired();
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM verification_codes");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw AppException.database("認証コードを確認できませんでした。", e);
        }
    }

    static void expireForTesting(String email) {
        String key = normalizeEmail(email);
        long expiredAt = System.currentTimeMillis() - CODE_VALIDITY_MILLIS - 1L;
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE verification_codes SET generated_at = ? WHERE email = ?")) {
            ps.setLong(1, expiredAt);
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw AppException.database("認証コードを更新できませんでした。", e);
        }
    }

    private static void purgeExpired() {
        long cutoff = System.currentTimeMillis() - CODE_VALIDITY_MILLIS;
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM verification_codes WHERE generated_at < ?")) {
            ps.setLong(1, cutoff);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw AppException.database("期限切れの認証コードを削除できませんでした。", e);
        }
    }

    private static boolean isExpired(long generatedAtMillis) {
        return System.currentTimeMillis() - generatedAtMillis > CODE_VALIDITY_MILLIS;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
