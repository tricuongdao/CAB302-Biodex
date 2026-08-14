package com.biodex.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies {@code db/schema.sql} to a connection.
 *
 * <p>Every statement in the script is written with {@code IF NOT EXISTS}, so running this at every
 * startup is harmless and leaves existing data untouched.
 */
public final class SchemaInitialiser {

    private static final String SCHEMA_RESOURCE = "/com/biodex/db/schema.sql";

    private SchemaInitialiser() {
    }

    /** Applies the schema to the shared application connection. */
    public static void initialise() {
        initialise(DatabaseConnection.getInstance().getConnection());
    }

    /** Applies the schema to the given connection. */
    public static void initialise(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            for (String sql : readStatements()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to apply the Biodex database schema", e);
        }
    }

    /**
     * Splits the schema script into executable statements. A statement ends at a line finishing
     * with a semicolon, except inside a trigger body, which is only closed by a line reading
     * {@code END;}.
     */
    private static List<String> readStatements() {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inTriggerBody = false;

        try (InputStream in = SchemaInitialiser.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Schema script not found on the classpath: " + SCHEMA_RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                current.append(line).append('\n');

                if (trimmed.equalsIgnoreCase("BEGIN")) {
                    inTriggerBody = true;
                } else if (inTriggerBody && trimmed.equalsIgnoreCase("END;")) {
                    inTriggerBody = false;
                    statements.add(current.toString().trim());
                    current.setLength(0);
                } else if (!inTriggerBody && trimmed.endsWith(";")) {
                    statements.add(current.toString().trim());
                    current.setLength(0);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the Biodex database schema", e);
        }

        if (current.toString().trim().length() > 0) {
            statements.add(current.toString().trim());
        }
        return statements;
    }
}
