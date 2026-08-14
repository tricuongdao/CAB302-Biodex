package com.biodex.dao;

import com.biodex.db.InMemoryDatabase;
import com.biodex.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserDAOTest {

    private Connection connection;
    private UserDAO userDAO;

    @BeforeEach
    void setUp() throws SQLException {
        connection = InMemoryDatabase.open();
        userDAO = new UserDAO(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void insertAssignsGeneratedId() {
        User user = userDAO.insert(new User("ada", "ada@example.com", "hash"));

        assertTrue(user.getUserId() > 0, "insert should stamp the generated id onto the user");
    }

    @Test
    void findByUsernameOrEmailMatchesEitherIdentifier() {
        userDAO.insert(new User("ada", "ada@example.com", "hash"));

        Optional<User> byUsername = userDAO.findByUsernameOrEmail("ada");
        Optional<User> byEmail = userDAO.findByUsernameOrEmail("ada@example.com");

        assertTrue(byUsername.isPresent(), "should find the account by username");
        assertTrue(byEmail.isPresent(), "should find the account by email");
        assertEquals(byUsername.get().getUserId(), byEmail.get().getUserId());
        assertEquals("ada@example.com", byUsername.get().getEmail());
    }

    @Test
    void findByUsernameOrEmailReturnsEmptyForUnknownIdentifier() {
        assertTrue(userDAO.findByUsernameOrEmail("nobody").isEmpty());
    }

    @Test
    void findByEmailReturnsTheStoredAccount() {
        userDAO.insert(new User("ada", "ada@example.com", "hash"));

        Optional<User> found = userDAO.findByEmail("ada@example.com");

        assertTrue(found.isPresent());
        assertEquals("ada", found.get().getUsername());
        assertEquals("hash", found.get().getPasswordHash());
    }

    @Test
    void findByEmailReturnsEmptyForUnknownAddress() {
        assertTrue(userDAO.findByEmail("nobody@example.com").isEmpty());
    }

    @Test
    void updatePasswordHashReplacesTheStoredHash() {
        User user = userDAO.insert(new User("ada", "ada@example.com", "old-hash"));

        boolean updated = userDAO.updatePasswordHash(user.getUserId(), "new-hash");

        assertTrue(updated);
        assertEquals("new-hash", userDAO.findByEmail("ada@example.com").orElseThrow().getPasswordHash());
    }

    @Test
    void updatePasswordHashReportsWhenNoAccountMatched() {
        assertFalse(userDAO.updatePasswordHash(404, "new-hash"));
    }

    @Test
    void duplicateUsernameIsRejected() {
        userDAO.insert(new User("ada", "ada@example.com", "hash"));

        assertThrows(DataAccessException.class,
                () -> userDAO.insert(new User("ada", "other@example.com", "hash")));
    }

    @Test
    void insertingAUserCreatesDefaultSettingsRow() throws SQLException {
        User user = userDAO.insert(new User("ada", "ada@example.com", "hash"));

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT theme, two_factor_enabled FROM user_settings WHERE user_id = ?")) {
            statement.setInt(1, user.getUserId());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "the trigger should create a user_settings row");
                assertEquals("light", resultSet.getString("theme"));
                assertEquals(0, resultSet.getInt("two_factor_enabled"));
            }
        }
    }
}
