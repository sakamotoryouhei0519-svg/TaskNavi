package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationManagerTest {

    @AfterEach
    void tearDown() {
        VerificationManager.clearAll();
    }

    @Test
    public void testGenerateAndVerifyCode() {
        String email = "user@example.com";
        String code = VerificationManager.generateCode(email);

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
        assertTrue(VerificationManager.verifyCode(email, code));
        assertFalse(VerificationManager.verifyCode("other@example.com", code));

        VerificationManager.clearCode(email);
        assertFalse(VerificationManager.verifyCode(email, code));
    }

    @Test
    public void testExpiredCodeIsRejected() {
        String email = "expire@example.com";
        String code = VerificationManager.generateCode(email);
        VerificationManager.expireForTesting(email);

        assertFalse(VerificationManager.verifyCode(email, code));
        assertEquals(0, VerificationManager.sizeForTesting());
    }

    @Test
    public void codesAreIsolatedPerEmail() {
        String codeA = VerificationManager.generateCode("a@example.com");
        String codeB = VerificationManager.generateCode("b@example.com");

        assertTrue(VerificationManager.verifyCode("a@example.com", codeA));
        assertTrue(VerificationManager.verifyCode("b@example.com", codeB));
        assertFalse(VerificationManager.verifyCode("a@example.com", codeB));
    }
}
