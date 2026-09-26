package org.tasknavi;

import org.tasknavi.util.AuthAttemptGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthAttemptGuardTest {

    @AfterEach
    void tearDown() {
        AuthAttemptGuard.clearAll();
        VerificationManager.clearAll();
    }

    @Test
    void shouldLockAfterRepeatedFailures() {
        String key = AuthAttemptGuard.loginKey("alice");
        for (int i = 0; i < 5; i++) {
            assertFalse(AuthAttemptGuard.isBlocked(key));
            AuthAttemptGuard.recordFailure(key);
        }
        assertTrue(AuthAttemptGuard.isBlocked(key));
        assertTrue(AuthAttemptGuard.remainingLockSeconds(key) > 0);
    }

    @Test
    void successShouldClearFailures() {
        String key = AuthAttemptGuard.loginKey("bob");
        AuthAttemptGuard.recordFailure(key);
        AuthAttemptGuard.recordFailure(key);
        AuthAttemptGuard.recordSuccess(key);
        assertFalse(AuthAttemptGuard.isBlocked(key));
    }

    @Test
    void checkVerificationCodeShouldCountFailures() {
        String email = "lock@example.com";
        VerificationManager.generateCode(email);
        for (int i = 0; i < 5; i++) {
            assertFalse(AuthService.checkVerificationCode(email, "000000"));
        }
        assertFalse(AuthService.canVerifyResetCode(email));
        assertTrue(AuthService.remainingVerifyLockSeconds(email) > 0);
    }
}
