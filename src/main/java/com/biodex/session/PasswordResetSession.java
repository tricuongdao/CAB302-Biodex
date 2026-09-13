package com.biodex.session;

/**
 * Stores temporary information while a user completes the password reset flow.
 */
public final class PasswordResetSession {

    private static PasswordResetSession instance;

    private String email;
    private int userId;
    private boolean codeVerified;

    private PasswordResetSession() {
    }

    public static synchronized PasswordResetSession getInstance() {
        if (instance == null) {
            instance = new PasswordResetSession();
        }

        return instance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isCodeVerified() {
        return codeVerified;
    }

    public void setCodeVerified(boolean codeVerified) {
        this.codeVerified = codeVerified;
    }

    public void clear() {
        email = null;
        userId = 0;
        codeVerified = false;
    }
}