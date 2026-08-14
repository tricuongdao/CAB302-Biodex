package com.biodex.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"ada@example.com", "a.b+tag@sub.domain.co.uk", "user_1@qut.edu.au"})
    void acceptsWellFormedEmails(String email) {
        assertTrue(Validator.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ada", "ada@", "@example.com", "ada@example", "ada @example.com"})
    void rejectsMalformedEmails(String email) {
        assertFalse(Validator.isValidEmail(email));
    }

    @Test
    void rejectsNullEmail() {
        assertFalse(Validator.isValidEmail(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ada", "ada_lovelace", "user123", "abc"})
    void acceptsWellFormedUsernames(String username) {
        assertTrue(Validator.isValidUsername(username));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "has space", "has-dash", "way_too_long_a_username_here"})
    void rejectsMalformedUsernames(String username) {
        assertFalse(Validator.isValidUsername(username));
    }

    @Test
    void rejectsNullUsername() {
        assertFalse(Validator.isValidUsername(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password1", "Passw0rd", "aaaaaaa1", "1abcdefg", "p4ssword!"})
    void acceptsStrongPasswords(String password) {
        assertTrue(Validator.isStrongPassword(password));
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        assertFalse(Validator.isStrongPassword("pass1"));
    }

    @Test
    void rejectsPasswordWithoutANumber() {
        assertFalse(Validator.isStrongPassword("passwordonly"));
    }

    @Test
    void rejectsPasswordWithoutALetter() {
        assertFalse(Validator.isStrongPassword("12345678"));
    }

    @Test
    void rejectsNullPassword() {
        assertFalse(Validator.isStrongPassword(null));
    }
}
