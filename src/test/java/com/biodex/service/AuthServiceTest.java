package com.biodex.service;

import com.biodex.dao.UserDAO;
import com.biodex.db.DatabaseConnection;
import com.biodex.db.SchemaInitialiser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthServiceTest {

    private Connection connection;
    private AuthService authService;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseConnection.enableForeignKeys(connection);
        SchemaInitialiser.initialise(connection);
        authService = new AuthService(new UserDAO(connection));
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void signupThenLoginSucceeds() {
        SignupResult signup = authService.signup("cuong", "cuong@qut.edu.au", "password1");
        assertEquals(SignupResult.Status.SUCCESS, signup.getStatus());

        LoginResult login = authService.login("cuong", "password1");
        assertEquals(LoginResult.Status.SUCCESS, login.getStatus());
        assertEquals("cuong", login.getUser().getUsername());
    }

    @Test
    void loginWithWrongPasswordFails() {
        authService.signup("cuong", "cuong@qut.edu.au", "password1");
        LoginResult login = authService.login("cuong", "wrongpassword");
        assertEquals(LoginResult.Status.INVALID_CREDENTIALS, login.getStatus());
    }

    @Test
    void loginWithUnknownUserFails() {
        LoginResult login = authService.login("nobody", "whatever1");
        assertEquals(LoginResult.Status.INVALID_CREDENTIALS, login.getStatus());
    }

    @Test
    void signupRejectsDuplicateUsername() {
        authService.signup("cuong", "a@qut.edu.au", "password1");
        SignupResult second = authService.signup("cuong", "b@qut.edu.au", "password1");
        assertEquals(SignupResult.Status.USERNAME_TAKEN, second.getStatus());
    }

    @Test
    void signupRejectsWeakPassword() {
        SignupResult result = authService.signup("newuser", "new@qut.edu.au", "abc");
        assertEquals(SignupResult.Status.WEAK_PASSWORD, result.getStatus());
    }
}