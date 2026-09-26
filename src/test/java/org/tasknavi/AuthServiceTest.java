package org.tasknavi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @BeforeAll
    static void setUpDatabase() {
        DatabaseTestConfig.useIsolatedDatabase();
    }

    @Test
    void authenticateRejectsUnknownUser() {
        String user = "nouser-" + UUID.randomUUID().toString().substring(0, 8);
        AuthService.LoginResult result = AuthService.authenticate(user, "wrong-password");
        assertFalse(result.isSuccess());
        assertEquals(AuthService.LoginStatus.INVALID_CREDENTIALS, result.getStatus());
    }

    @Test
    void authenticateLocksAfterRepeatedFailures() {
        String user = "lock-" + UUID.randomUUID().toString().substring(0, 8);
        AuthService.LoginStatus last = AuthService.LoginStatus.INVALID_CREDENTIALS;
        for (int i = 0; i < 12; i++) {
            last = AuthService.authenticate(user, "bad").getStatus();
            if (last == AuthService.LoginStatus.LOCKED) {
                break;
            }
        }
        assertEquals(AuthService.LoginStatus.LOCKED, last);
        assertTrue(AuthService.authenticate(user, "bad").getRemainingLockSeconds() > 0);
    }
}
