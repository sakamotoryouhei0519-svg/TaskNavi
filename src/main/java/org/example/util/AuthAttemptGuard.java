package org.example.util;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ログイン・認証コード検証などの試行回数を制限する。
 */
public final class AuthAttemptGuard {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;

    private static final Map<String, AttemptState> STATES = new ConcurrentHashMap<>();

    private AuthAttemptGuard() {
    }

    public static String loginKey(String username) {
        return "login:" + normalize(username);
    }

    public static String verifyKey(String email) {
        return "verify:" + normalize(email);
    }

    public static String sendCodeKey(String email) {
        return "send:" + normalize(email);
    }

    public static boolean isBlocked(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        AttemptState state = STATES.get(key);
        if (state == null) {
            return false;
        }
        if (state.lockedUntilMillis <= 0) {
            return false;
        }
        if (System.currentTimeMillis() >= state.lockedUntilMillis) {
            STATES.remove(key);
            return false;
        }
        return true;
    }

    /** ロック解除までの残り秒（ロック中でなければ 0）。 */
    public static long remainingLockSeconds(String key) {
        AttemptState state = STATES.get(key);
        if (state == null || state.lockedUntilMillis <= 0) {
            return 0;
        }
        long remainingMs = state.lockedUntilMillis - System.currentTimeMillis();
        if (remainingMs <= 0) {
            STATES.remove(key);
            return 0;
        }
        return Math.max(1, (remainingMs + 999) / 1000);
    }

    public static void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        STATES.compute(key, (k, current) -> {
            AttemptState next = current != null ? current : new AttemptState(0, 0);
            if (next.lockedUntilMillis > System.currentTimeMillis()) {
                return next;
            }
            int failures = next.failures + 1;
            if (failures >= MAX_FAILURES) {
                return new AttemptState(failures, System.currentTimeMillis() + LOCK_MILLIS);
            }
            return new AttemptState(failures, 0);
        });
    }

    public static void recordSuccess(String key) {
        if (key != null && !key.isBlank()) {
            STATES.remove(key);
        }
    }

    /** テスト用。 */
    public static void clearAll() {
        STATES.clear();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptState {
        private final int failures;
        private final long lockedUntilMillis;

        private AttemptState(int failures, long lockedUntilMillis) {
            this.failures = failures;
            this.lockedUntilMillis = lockedUntilMillis;
        }
    }
}
