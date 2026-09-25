package org.example.ui.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPasswordPolicyTest {

    @Test
    void emailAndPasswordRules() {
        assertTrue(AuthPasswordPolicy.isValidEmail("user@example.com"));
        assertFalse(AuthPasswordPolicy.isValidEmail("bad"));
        assertTrue(AuthPasswordPolicy.isStrongPassword("Abcdef1!"));
        assertFalse(AuthPasswordPolicy.isStrongPassword("short1!"));
        assertFalse(AuthPasswordPolicy.isStrongPassword("onlyletters"));
    }

    @Test
    void registerFormValidation() {
        assertNotNull(AuthPasswordPolicy.validateRegisterForm("", "a@b.co", "Abcdef1!", "Abcdef1!"));
        assertNotNull(AuthPasswordPolicy.validateRegisterForm("u", "bad", "Abcdef1!", "Abcdef1!"));
        assertNotNull(AuthPasswordPolicy.validateRegisterForm("u", "a@b.co", "Abcdef1!", "mismatch"));
        assertNull(AuthPasswordPolicy.validateRegisterForm("u", "a@b.co", "Abcdef1!", "Abcdef1!"));
        assertNotNull(AuthPasswordPolicy.validateRegisterUniqueness(
                "u", "a@b.co", name -> true, email -> false));
        assertNull(AuthPasswordPolicy.validateRegisterUniqueness(
                "u", "a@b.co", name -> false, email -> false));
    }
}
