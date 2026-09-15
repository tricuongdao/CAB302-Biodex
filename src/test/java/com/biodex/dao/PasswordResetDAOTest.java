package com.biodex.dao;

import com.biodex.db.InMemoryDatabase;
import com.biodex.model.PasswordResetCode;
import com.biodex.model.User;
import com.biodex.util.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetDAOTest {

    private Connection connection;
    private UserDAO userDAO;
    private PasswordResetDAO resetDAO;
    private User user;

    @BeforeEach
    void setUp() throws SQLException {
        connection = InMemoryDatabase.open();
        userDAO = new UserDAO(connection);
        resetDAO = new PasswordResetDAO(connection);
        user = userDAO.insert(new User("ada", "ada@example.com", "password-hash"));
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void createStoresAHashInsteadOfThePlainCode() {
        PasswordResetCode created = resetDAO.createResetCode(
                user.getUserId(), "047312", Duration.ofMinutes(15));

        assertTrue(created.getCodeId() > 0);
        assertNotEquals("047312", created.getCodeHash());
        assertTrue(PasswordHasher.verify("047312", created.getCodeHash()));
    }

    @Test
    void validCodeCanOnlyBeVerifiedOnce() {
        resetDAO.createResetCode(user.getUserId(), "047312", Duration.ofMinutes(15));

        assertTrue(resetDAO.verifyCode("ada@example.com", "047312"));
        assertFalse(resetDAO.verifyCode("ada@example.com", "047312"));
    }

    @Test
    void incorrectCodeIsRejected() {
        resetDAO.createResetCode(user.getUserId(), "047312", Duration.ofMinutes(15));

        assertFalse(resetDAO.verifyCode("ada@example.com", "123456"));
        assertTrue(resetDAO.verifyCode("ada@example.com", "047312"));
    }

    @Test
    void codeIsLockedAfterFiveIncorrectAttempts() {
        resetDAO.createResetCode(user.getUserId(), "047312", Duration.ofMinutes(15));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(resetDAO.verifyCode("ada@example.com", "123456"));
        }

        assertFalse(resetDAO.verifyCode("ada@example.com", "047312"));
        assertTrue(resetDAO.findLatestUnusedForUser(user.getUserId()).isEmpty());
    }

    @Test
    void expiredCodeIsRejected() {
        resetDAO.createResetCode(user.getUserId(), "047312", Duration.ofMillis(1));

        assertFalse(resetDAO.verifyCode("ada@example.com", "047312"));
    }

    @Test
    void creatingANewCodeInvalidatesThePreviousCode() {
        resetDAO.createResetCode(user.getUserId(), "111111", Duration.ofMinutes(15));
        resetDAO.createResetCode(user.getUserId(), "222222", Duration.ofMinutes(15));

        assertFalse(resetDAO.verifyCode("ada@example.com", "111111"));
        assertTrue(resetDAO.verifyCode("ada@example.com", "222222"));
    }

    @Test
    void unknownEmailIsRejected() {
        assertFalse(resetDAO.verifyCode("nobody@example.com", "047312"));
    }
}
