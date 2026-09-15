package com.biodex.service;

import com.biodex.model.User;

public final  class SignupResult {

    public enum Status { SUCCESS, USERNAME_TAKEN, EMAIL_TAKEN, INVALID_USERNAME, INVALID_EMAIL, WEAK_PASSWORD }

    private final Status status;
    private final User user;

    private SignupResult(Status status, User user) {
        this.status = status;
        this.user = user;
    }

    public static SignupResult success(User user) { return new SignupResult(Status.SUCCESS, user); }
    public static SignupResult usernameTaken() { return new SignupResult(Status.USERNAME_TAKEN, null); }
    public static SignupResult emailTaken() { return new SignupResult(Status.EMAIL_TAKEN, null); }
    public static SignupResult invalidUsername() { return new SignupResult(Status.INVALID_USERNAME, null); }
    public static SignupResult invalidEmail() { return new SignupResult(Status.INVALID_EMAIL, null); }
    public static SignupResult weakPassword() { return new SignupResult(Status.WEAK_PASSWORD, null); }

    public Status getStatus() { return status; }
    public User getUser() { return user; }

}