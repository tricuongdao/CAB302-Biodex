package com.biodex.dao;

import com.biodex.model.PasswordResetCode;
import com.biodex.util.PasswordHasher;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/** Reads and writes one-time codes used by the forgot-password flow. */
public class PasswordResetDAO extends BaseDao {

    private static final int MAX_ATTEMPTS = 5;

    private static final DateTimeFormatter SQLITE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SELECT_COLUMNS =
            "SELECT code_id, user_id, code_hash, expires_at, attempt_count, used, created_at "
                    + "FROM password_reset_codes";

    /** Uses the shared application connection. */
    public PasswordResetDAO() {
        super();
    }

    /** Uses the given connection. Tests pass an in-memory one here. */
    public PasswordResetDAO(Connection connection) {
        super(connection);
    }

    /**
     * Invalidates earlier codes and creates a new one for the user. Only a hash of the plaintext
     * code is stored.
     *
     * @return the created reset-code record with its generated id populated
     */
    public PasswordResetCode createResetCode(int userId, String plainCode, Duration validFor) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        Objects.requireNonNull(plainCode, "Plain code cannot be null");
        Objects.requireNonNull(validFor, "Validity duration cannot be null");
        if (validFor.isZero() || validFor.isNegative()) {
            throw new IllegalArgumentException("Validity duration must be positive");
        }

        invalidateAllCodesForUser(userId);

        String codeHash = PasswordHasher.hash(plainCode);
        String expiresAt = format(Instant.now().plus(validFor));
        int codeId = insertReturningKey(
                "INSERT INTO password_reset_codes (user_id, code_hash, expires_at) "
                        + "VALUES (?, ?, ?)",
                statement -> {
                    statement.setInt(1, userId);
                    statement.setString(2, codeHash);
                    statement.setString(3, expiresAt);
                });

        PasswordResetCode resetCode = new PasswordResetCode(userId, codeHash, expiresAt);
        resetCode.setCodeId(codeId);
        return resetCode;
    }

    /**
     * Checks the newest unused code for an email address. A matching code is marked as used before
     * this method returns, so it cannot be verified twice.
     */
    public boolean verifyCode(String email, String plainCode) {
        if (email == null || plainCode == null) {
            return false;
        }

        Optional<PasswordResetCode> storedCode = queryOne(
                "SELECT prc.code_id, prc.user_id, prc.code_hash, prc.expires_at, "
                        + "prc.attempt_count, prc.used, prc.created_at "
                        + "FROM password_reset_codes prc "
                        + "JOIN users u ON u.user_id = prc.user_id "
                        + "WHERE u.email = ? AND prc.used = 0 "
                        + "ORDER BY datetime(prc.created_at) DESC, prc.code_id DESC LIMIT 1",
                statement -> statement.setString(1, email),
                PasswordResetDAO::mapRow);

        if (storedCode.isEmpty()) {
            return false;
        }

        PasswordResetCode resetCode = storedCode.get();
        if (!parse(resetCode.getExpiresAt()).isAfter(Instant.now())) {
            markUsed(resetCode.getCodeId());
            return false;
        }

        if (!PasswordHasher.verify(plainCode, resetCode.getCodeHash())) {
            incrementAttempts(resetCode.getCodeId());
            return false;
        }

        return markUsed(resetCode.getCodeId());
    }

    /** Marks every unused password-reset code belonging to a user as used. */
    public void invalidateAllCodesForUser(int userId) {
        update(
                "UPDATE password_reset_codes SET used = 1 WHERE user_id = ? AND used = 0",
                statement -> statement.setInt(1, userId));
    }

    /** Finds the newest unused code for a user. Primarily useful to services and tests. */
    public Optional<PasswordResetCode> findLatestUnusedForUser(int userId) {
        return queryOne(
                SELECT_COLUMNS + " WHERE user_id = ? AND used = 0 "
                        + "ORDER BY datetime(created_at) DESC, code_id DESC LIMIT 1",
                statement -> statement.setInt(1, userId),
                PasswordResetDAO::mapRow);
    }

    private boolean markUsed(int codeId) {
        int rows = update(
                "UPDATE password_reset_codes SET used = 1 WHERE code_id = ? AND used = 0",
                statement -> statement.setInt(1, codeId));
        return rows == 1;
    }

    private void incrementAttempts(int codeId) {
        update(
                "UPDATE password_reset_codes "
                        + "SET attempt_count = attempt_count + 1, "
                        + "used = CASE WHEN attempt_count + 1 >= ? THEN 1 ELSE used END "
                        + "WHERE code_id = ? AND used = 0",
                statement -> {
                    statement.setInt(1, MAX_ATTEMPTS);
                    statement.setInt(2, codeId);
                });
    }

    private static PasswordResetCode mapRow(ResultSet resultSet) throws SQLException {
        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setCodeId(resultSet.getInt("code_id"));
        resetCode.setUserId(resultSet.getInt("user_id"));
        resetCode.setCodeHash(resultSet.getString("code_hash"));
        resetCode.setExpiresAt(resultSet.getString("expires_at"));
        resetCode.setAttemptCount(resultSet.getInt("attempt_count"));
        resetCode.setUsed(resultSet.getInt("used") != 0);
        resetCode.setCreatedAt(resultSet.getString("created_at"));
        return resetCode;
    }

    private static String format(Instant instant) {
        return SQLITE_TIMESTAMP.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    private static Instant parse(String timestamp) {
        return LocalDateTime.parse(timestamp, SQLITE_TIMESTAMP).toInstant(ZoneOffset.UTC);
    }
}
