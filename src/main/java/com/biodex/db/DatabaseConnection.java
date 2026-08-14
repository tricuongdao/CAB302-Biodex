package com.biodex.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Single shared SQLite connection for the application.
 *
 * <p>The database lives in a file called {@code biodex.db} in the working directory. Foreign key
 * enforcement is switched on for the connection, as SQLite leaves it off by default.
 */
public final class DatabaseConnection {

    /** JDBC URL of the application database. */
    public static final String DATABASE_URL = "jdbc:sqlite:./biodex.db";

    private static DatabaseConnection instance;

    private final Connection connection;

    private DatabaseConnection() {
        try {
            this.connection = DriverManager.getConnection(DATABASE_URL);
            enableForeignKeys(this.connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to open the Biodex database at " + DATABASE_URL, e);
        }
    }

    /** Returns the singleton instance, opening the connection on first use. */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /** Returns the open JDBC connection. */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Turns on foreign key enforcement for a connection. Exposed so tests can apply the same
     * setting to their own in-memory connections.
     */
    public static void enableForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    /** Closes the shared connection and drops the instance, so a later call reopens it. */
    public static synchronized void close() {
        if (instance != null) {
            try {
                instance.connection.close();
            } catch (SQLException e) {
                // Nothing useful left to do while shutting down.
            }
            instance = null;
        }
    }
}
