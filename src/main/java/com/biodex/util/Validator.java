package com.biodex.util;

import java.util.regex.Pattern;

/**
 * Input rules shared by every page.
 *
 * <p>These are the single definitions of "valid" in Biodex — signup, login, password reset and
 * settings all call these methods rather than writing their own checks, so the rules can only ever
 * disagree in one place.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Letters, digits and underscores, 3 to 20 characters. */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    /** Shortest password Biodex accepts. */
    public static final int MINIMUM_PASSWORD_LENGTH = 8;

    private Validator() {
    }

    /** True if the value looks like an email address. */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /** True if the value is a usable username: 3-20 letters, digits or underscores. */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * True if the password is strong enough: at least {@value #MINIMUM_PASSWORD_LENGTH}
     * characters, with at least one letter and at least one number.
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false;
    }
}
