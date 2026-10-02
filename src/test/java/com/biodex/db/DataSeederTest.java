package com.biodex.db;

import com.biodex.dao.SightingDAO;
import com.biodex.dao.SuburbDAO;
import com.biodex.model.MapSighting;
import com.biodex.model.Suburb;
import com.biodex.util.BrisbaneMapProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The demo filler: Brisbane suburbs with map coordinates and the sample sightings the heat map
 * plots on a fresh install. Each test runs against an empty in-memory database.
 */
class DataSeederTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = InMemoryDatabase.open();
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void seedsSuburbsInsideTheMapBounds() {
        DataSeeder.seedIfEmpty(connection);

        List<Suburb> suburbs = new SuburbDAO(connection).findAll();
        assertTrue(suburbs.size() >= 40, "the map should have a decent spread of suburbs");
        for (Suburb suburb : suburbs) {
            assertTrue(
                    BrisbaneMapProjection.project(suburb.getLatitude(), suburb.getLongitude()).isPresent(),
                    suburb.getName() + " must sit inside the heat map's bounds");
        }
    }

    @Test
    void sampleSightingsAreSkippedUntilAUserExistsThenSeeded() throws SQLException {
        DataSeeder.seedIfEmpty(connection);
        assertTrue(new SightingDAO(connection).findForMap(null, null).isEmpty(),
                "sightings need a user to attribute them to");

        insertUser();
        DataSeeder.seedIfEmpty(connection);

        List<MapSighting> sightings = new SightingDAO(connection).findForMap(null, null);
        assertFalse(sightings.isEmpty());
        for (MapSighting sighting : sightings) {
            assertNotNull(sighting.getSpeciesName());
            assertNotNull(sighting.getSightingDate());
            assertTrue(
                    BrisbaneMapProjection.project(sighting.getLatitude(), sighting.getLongitude()).isPresent(),
                    sighting.getSuburbName() + " must plot on the map");
        }
    }

    @Test
    void sampleSightingsCoverTheHeatMapFilterChips() throws SQLException {
        insertUser();
        DataSeeder.seedIfEmpty(connection);

        Set<String> species = new SightingDAO(connection).findForMap(null, null).stream()
                .map(MapSighting::getSpeciesName)
                .collect(Collectors.toSet());

        assertTrue(species.containsAll(Set.of("Cane Toad", "Fire Ant", "Water Hyacinth")),
                "the map's filter chips need sightings to match, found: " + species);
    }

    @Test
    void seedingTwiceChangesNothing() throws SQLException {
        insertUser();
        DataSeeder.seedIfEmpty(connection);
        int species = count("species");
        int suburbs = count("suburbs");
        int sightings = count("sightings");
        assertTrue(sightings > 0);

        DataSeeder.seedIfEmpty(connection);

        assertEquals(species, count("species"));
        assertEquals(suburbs, count("suburbs"));
        assertEquals(sightings, count("sightings"));
    }

    private int count(String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void insertUser() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)")) {
            statement.setString(1, "demo");
            statement.setString(2, "demo@example.com");
            statement.setString(3, "hash");
            statement.executeUpdate();
        }
    }
}
