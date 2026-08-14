package com.biodex.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void verifyAcceptsTheOriginalPassword() {
        String hash = PasswordHasher.hash("password1");

        assertTrue(PasswordHasher.verify("password1", hash));
    }

    @Test
    void verifyRejectsTheWrongPassword() {
        String hash = PasswordHasher.hash("password1");

        assertFalse(PasswordHasher.verify("password2", hash));
    }

    @Test
    void hashDoesNotContainThePlaintext() {
        assertFalse(PasswordHasher.hash("password1").contains("password1"));
    }

    @Test
    void sameInputHashesDifferentlyEachTime() {
        assertNotEquals(PasswordHasher.hash("password1"), PasswordHasher.hash("password1"),
                "each hash should carry its own random salt");
    }

    @Test
    void verifyRejectsAMalformedStoredHash() {
        assertFalse(PasswordHasher.verify("password1", "not-a-real-hash"));
    }

    @Test
    void verifyRejectsNulls() {
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("password1")));
        assertFalse(PasswordHasher.verify("password1", null));
    }

    @Test
    void verifyAcceptsAGeneratedSixDigitCode() {
        String code = CodeGenerator.sixDigit();

        assertTrue(PasswordHasher.verify(code, PasswordHasher.hash(code)));
    }
}
