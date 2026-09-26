package org.tasknavi;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * メール単位で認証コードを発行・検証するマネージャ。
 */
public final class VerificationManager {
    private static final long CODE_VALIDITY_MILLIS = 5 * 60 * 1000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Map<String, CodeEntry> CODES_BY_EMAIL = new ConcurrentHashMap<>();

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
        CODES_BY_EMAIL.put(key, new CodeEntry(value, System.currentTimeMillis()));
        return value;
    }

    public static boolean verifyCode(String email, String inputCode) {
        String key = normalizeEmail(email);
        if (key.isEmpty() || inputCode == null || inputCode.trim().isEmpty()) {
            return false;
        }

        purgeExpired();
        CodeEntry entry = CODES_BY_EMAIL.get(key);
        if (entry == null) {
            return false;
        }
        if (isExpired(entry)) {
            CODES_BY_EMAIL.remove(key);
            return false;
        }
        return entry.code().equals(inputCode.trim());
    }

    public static void clearCode(String email) {
        String key = normalizeEmail(email);
        if (!key.isEmpty()) {
            CODES_BY_EMAIL.remove(key);
        }
    }

    public static void clearAll() {
        CODES_BY_EMAIL.clear();
    }

    static int sizeForTesting() {
        purgeExpired();
        return CODES_BY_EMAIL.size();
    }

    static void expireForTesting(String email) {
        String key = normalizeEmail(email);
        CodeEntry entry = CODES_BY_EMAIL.get(key);
        if (entry != null) {
            CODES_BY_EMAIL.put(key, new CodeEntry(entry.code(), System.currentTimeMillis() - CODE_VALIDITY_MILLIS - 1L));
        }
    }

    private static void purgeExpired() {
        Iterator<Map.Entry<String, CodeEntry>> it = CODES_BY_EMAIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CodeEntry> entry = it.next();
            if (isExpired(entry.getValue())) {
                it.remove();
            }
        }
    }

    private static boolean isExpired(CodeEntry entry) {
        return System.currentTimeMillis() - entry.generatedAtMillis() > CODE_VALIDITY_MILLIS;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record CodeEntry(String code, long generatedAtMillis) {
    }
}
