package com.biodex.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Hashes secrets for storage. Used for account passwords and for the one-time password reset and
 * two-factor codes — nothing of that kind is ever written to the database in plaintext.
 *
 * <p>Backed by PBKDF2 from the JDK, so Biodex needs no extra dependency. Each hash carries its own
 * random salt and the iteration count that produced it, in the form
 * {@code iterations:salt:hash}, with salt and hash Base64 encoded.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String SEPARATOR = ":";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Hashes a plaintext secret with a fresh random salt.
     *
     * @return the encoded hash, safe to store
     */
    public static String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Cannot hash a null value");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(plaintext, salt, ITERATIONS);

        Base64.Encoder encoder = Base64.getEncoder();
        return ITERATIONS + SEPARATOR + encoder.encodeToString(salt) + SEPARATOR + encoder.encodeToString(key);
    }

    /**
     * Checks a plaintext secret against a stored hash.
     *
     * @return true if they match; false if they do not, or the stored value is malformed
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split(SEPARATOR);
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = derive(plaintext, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String plaintext, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(plaintext.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to hash with " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }
}
