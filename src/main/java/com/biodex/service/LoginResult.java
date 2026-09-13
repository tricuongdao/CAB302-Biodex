package com.biodex.service;

import com.biodex.model.User;

public final class LoginResult {

    public enum Status { SUCCESS, INVALID_CREDENTIALS }

    private final Status status;
    private final User user;

    private LoginResult(Status status, User user) {
        this.status = status;
        this.user = user;
    }

    public static LoginResult success(User user) {
        return new LoginResult(Status.SUCCESS, user);
    }

    public static LoginResult invalidCredentials() {
        return new LoginResult(Status.INVALID_CREDENTIALS, null);
    }

    public Status getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }
}