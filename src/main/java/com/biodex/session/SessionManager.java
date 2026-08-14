package com.biodex.session;

import com.biodex.model.User;

/**
 * Who is signed in right now.
 *
 * <p>Sign in is two stages: a user who has passed the password check but still owes a two-factor
 * code is held as the pending user, and only becomes the current user once that code is accepted.
 */
public final class SessionManager {

    private static SessionManager instance;

    private User currentUser;
    private User pendingTwoFactorUser;

    private SessionManager() {
    }

    /** Returns the singleton instance. */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** The signed-in user, or null if nobody is signed in. */
    public User getCurrentUser() {
        return currentUser;
    }

    /** Signs a user in and clears any pending two-factor state. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.pendingTwoFactorUser = null;
    }

    /** True if a user is signed in. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** The user awaiting two-factor verification, or null if there is none. */
    public User getPendingTwoFactorUser() {
        return pendingTwoFactorUser;
    }

    /** Records a user who has passed the password check but not yet the two-factor code. */
    public void setPendingTwoFactorUser(User user) {
        this.pendingTwoFactorUser = user;
    }

    /** Forgets both the signed-in user and any pending two-factor user. Use this to sign out. */
    public void clear() {
        this.currentUser = null;
        this.pendingTwoFactorUser = null;
    }
}
