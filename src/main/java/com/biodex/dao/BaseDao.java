package com.biodex.dao;

import com.biodex.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared plumbing for every DAO: it holds the connection and turns {@link SQLException} into an
 * unchecked {@link DataAccessException}.
 *
 * <p>A new DAO is a subclass that supplies queries and row mapping, nothing else:
 *
 * <pre>{@code
 * public class SuburbDAO extends BaseDao {
 *     public Optional<Suburb> findById(int id) {
 *         return queryOne("SELECT * FROM suburbs WHERE suburb_id = ?",
 *                 statement -> statement.setInt(1, id),
 *                 SuburbDAO::mapRow);
 *     }
 * }
 * }</pre>
 *
 * <p>Statements are always prepared with bound parameters — never build SQL by concatenating user
 * input.
 */
public abstract class BaseDao {

    private final Connection connection;

    /** Uses the shared application connection. */
    protected BaseDao() {
        this(DatabaseConnection.getInstance().getConnection());
    }

    /** Uses the given connection. Tests pass an in-memory one here. */
    protected BaseDao(Connection connection) {
        this.connection = connection;
    }

    /** The connection this DAO reads and writes through. */
    protected Connection getConnection() {
        return connection;
    }

    /** Runs a query expected to match at most one row. */
    protected <T> Optional<T> queryOne(String sql, ParameterBinder binder, RowMapper<T> mapper) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapper.map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
    }

    /** Runs a query and maps every row. */
    protected <T> List<T> queryMany(String sql, ParameterBinder binder, RowMapper<T> mapper) {
        List<T> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
        return rows;
    }

    /** Runs an INSERT, UPDATE or DELETE and returns the number of affected rows. */
    protected int update(String sql, ParameterBinder binder) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + sql, e);
        }
    }

    /** Runs an INSERT and returns the generated primary key. */
    protected int insertReturningKey(String sql, ParameterBinder binder) {
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DataAccessException("Insert returned no generated key: " + sql);
        } catch (SQLException e) {
            throw new DataAccessException("Insert failed: " + sql, e);
        }
    }

    /** Binds the parameters of a prepared statement. */
    @FunctionalInterface
    protected interface ParameterBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Maps the current row of a result set to a model object. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
