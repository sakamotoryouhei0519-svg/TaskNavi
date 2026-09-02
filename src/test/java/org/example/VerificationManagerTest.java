package org.example;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationManagerTest {

    @Test
    public void testGenerateAndVerifyCode() {
        String code = VerificationManager.generateCode();

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
        assertTrue(VerificationManager.verifyCode(code));

        VerificationManager.clearCode();
        assertFalse(VerificationManager.verifyCode(code));
    }

    @Test
    public void testExpiredCodeIsRejected() throws Exception {
        String code = VerificationManager.generateCode();
        setGeneratedAtMillis(System.currentTimeMillis() - (5 * 60 * 1000L) - 1L);

        assertFalse(VerificationManager.verifyCode(code));
        assertNull(getCurrentCode());

        VerificationManager.clearCode();
    }

    private void setGeneratedAtMillis(long value) throws Exception {
        Field field = VerificationManager.class.getDeclaredField("generatedAtMillis");
        field.setAccessible(true);
        field.setLong(null, value);
    }

    private String getCurrentCode() throws Exception {
        Field field = VerificationManager.class.getDeclaredField("currentCode");
        field.setAccessible(true);
        return (String) field.get(null);
    }
}
