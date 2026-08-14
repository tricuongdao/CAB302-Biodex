package com.biodex.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Test fixture: a throwaway SQLite database held in memory, with the real schema applied.
 *
 * <p>Tests never touch {@code ./biodex.db}. Each connection is a fresh, empty database, so tests
 * cannot leak state into one another.
 */
public final class InMemoryDatabase {

    private InMemoryDatabase() {
    }

    /** Opens an empty in-memory database with foreign keys on and the schema applied. */
    public static Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseConnection.enableForeignKeys(connection);
        SchemaInitialiser.initialise(connection);
        return connection;
    }
}
