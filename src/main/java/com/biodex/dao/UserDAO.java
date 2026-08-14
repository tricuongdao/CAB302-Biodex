package com.biodex.dao;

import com.biodex.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Reads and writes the {@code users} table.
 *
 * <p>This is the worked example every other DAO should follow: extend {@link BaseDao}, supply
 * queries and a row mapper, and let the base class deal with connections and exceptions.
 */
public class UserDAO extends BaseDao {

    private static final String SELECT_COLUMNS =
            "SELECT user_id, username, email, password_hash, created_at FROM users";

    /** Uses the shared application connection. */
    public UserDAO() {
        super();
    }

    /** Uses the given connection. Tests pass an in-memory one here. */
    public UserDAO(Connection connection) {
        super(connection);
    }

    /** Finds the account matching a sign-in identifier, which may be either a username or email. */
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return queryOne(
                SELECT_COLUMNS + " WHERE username = ? OR email = ?",
                statement -> {
                    statement.setString(1, usernameOrEmail);
                    statement.setString(2, usernameOrEmail);
                },
                UserDAO::mapRow);
    }

    /** Finds the account registered with an email address. */
    public Optional<User> findByEmail(String email) {
        return queryOne(
                SELECT_COLUMNS + " WHERE email = ?",
                statement -> statement.setString(1, email),
                UserDAO::mapRow);
    }

    /**
     * Inserts a new account and stamps the generated id onto the given user. The database trigger
     * creates the matching {@code user_settings} row.
     *
     * @return the same user, with {@code userId} populated
     */
    public User insert(User user) {
        int userId = insertReturningKey(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                statement -> {
                    statement.setString(1, user.getUsername());
                    statement.setString(2, user.getEmail());
                    statement.setString(3, user.getPasswordHash());
                });
        user.setUserId(userId);
        return user;
    }

    /**
     * Replaces the stored password hash for an account.
     *
     * @return true if a row was updated
     */
    public boolean updatePasswordHash(int userId, String passwordHash) {
        int rows = update(
                "UPDATE users SET password_hash = ? WHERE user_id = ?",
                statement -> {
                    statement.setString(1, passwordHash);
                    statement.setInt(2, userId);
                });
        return rows > 0;
    }

    private static User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getInt("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setCreatedAt(resultSet.getString("created_at"));
        return user;
    }
}
