package com.biodex.util;

import java.security.SecureRandom;

/**
 * Generates the one-time codes used for password resets and two-factor sign in.
 *
 * <p>The generated code is what gets shown or emailed to the user; only its
 * {@link PasswordHasher#hash(String) hash} is stored.
 */
public final class CodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    /** Returns a random six digit code, zero padded, e.g. {@code "047312"}. */
    public static String sixDigit() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
