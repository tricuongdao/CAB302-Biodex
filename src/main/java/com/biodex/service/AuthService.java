package com.biodex.service;

import com.biodex.dao.UserDAO;
import com.biodex.model.User;
import com.biodex.util.PasswordHasher;
import com.biodex.util.Validator;

import java.util.Optional;

public class AuthService {

    private static final String DUMMY_HASH = PasswordHasher.hash("no-such-account");

    private final UserDAO userDao;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDao) {
        this.userDao = userDao;
    }

    public LoginResult login(String usernameOrEmail, String password) {
        Optional<User> maybeUser = userDao.findByUsernameOrEmail(usernameOrEmail);

        if (maybeUser.isEmpty()) {
            PasswordHasher.verify(password == null ? "" : password, DUMMY_HASH);
            return LoginResult.invalidCredentials();
        }

        User user = maybeUser.get();

        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            return LoginResult.invalidCredentials();
        }

        return LoginResult.success(user);
    }

    public SignupResult signup(String username, String email, String password) {
        if (!Validator.isValidUsername(username)) {
            return SignupResult.invalidUsername();
        }
        if (!Validator.isValidEmail(email)) {
            return SignupResult.invalidEmail();
        }
        if (!Validator.isStrongPassword(password)) {
            return SignupResult.weakPassword();
        }
        if (userDao.usernameExists(username)) {
            return SignupResult.usernameTaken();
        }
        if (userDao.emailExists(email)) {
            return SignupResult.emailTaken();
        }

        User user = new User(username, email, PasswordHasher.hash(password));
        return SignupResult.success(userDao.insert(user));
    }
}