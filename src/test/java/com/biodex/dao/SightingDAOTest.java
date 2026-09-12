package com.biodex.dao;

import com.biodex.db.InMemoryDatabase;
import com.biodex.model.MapSighting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SightingDAOTest {

    private Connection connection;
    private SightingDAO sightingDAO;
    private int userId;

    @BeforeEach
    void setUp() throws SQLException {
        connection = InMemoryDatabase.open();
        sightingDAO = new SightingDAO(connection);
        userId = insertUser();
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void returnsMapSightingsWithJoinedSuburbCoordinatesNewestFirst() throws SQLException {
        int kedron = insertSuburb("Kedron", "4031", -27.4020, 153.0300);
        int indooroopilly = insertSuburb("Indooroopilly", "4068", -27.4990, 152.9730);
        insertSighting(kedron, "Cane Toad", "Near the creek", "2026-09-01 18:30:00");
        insertSighting(indooroopilly, "Fire Ant", "Small nest", "2026-09-07 09:15:00");

        List<MapSighting> sightings = sightingDAO.findForMap(null, null);

        assertEquals(2, sightings.size());
        MapSighting newest = sightings.get(0);
        assertEquals("Fire Ant", newest.getSpeciesName());
        assertEquals("Indooroopilly", newest.getSuburbName());
        assertEquals("4068", newest.getPostcode());
        assertEquals(LocalDate.of(2026, 9, 7), newest.getSightingDate());
        assertEquals(-27.4990, newest.getLatitude(), 0.00001);
        assertEquals(152.9730, newest.getLongitude(), 0.00001);
    }

    @Test
    void filtersBySpeciesIgnoringCaseAndSurroundingWhitespace() throws SQLException {
        int kedron = insertSuburb("Kedron", "4031", -27.4020, 153.0300);
        insertSighting(kedron, "Cane Toad", null, "2026-09-06");
        insertSighting(kedron, "Fire Ant", null, "2026-09-07");

        List<MapSighting> sightings = sightingDAO.findForMap("  cane toad  ", null);

        assertEquals(1, sightings.size());
        assertEquals("Cane Toad", sightings.get(0).getSpeciesName());
    }

    @Test
    void includesOnlySightingsOnOrAfterTheStartDate() throws SQLException {
        int kedron = insertSuburb("Kedron", "4031", -27.4020, 153.0300);
        insertSighting(kedron, "Cane Toad", null, "2026-08-31 23:59:59");
        insertSighting(kedron, "Cane Toad", null, "2026-09-01 00:00:00");
        insertSighting(kedron, "Cane Toad", null, "2026-09-08 12:00:00");

        List<MapSighting> sightings =
                sightingDAO.findForMap(null, LocalDate.of(2026, 9, 1));

        assertEquals(2, sightings.size());
        assertEquals(LocalDate.of(2026, 9, 8), sightings.get(0).getSightingDate());
        assertEquals(LocalDate.of(2026, 9, 1), sightings.get(1).getSightingDate());
    }

    @Test
    void excludesSightingsWithoutUsableCoordinatesOrDates() throws SQLException {
        int mapped = insertSuburb("Kedron", "4031", -27.4020, 153.0300);
        int unmapped = insertSuburb("Unknown", "4000", null, null);
        insertSighting(mapped, "Cane Toad", null, "2026-09-08");
        insertSighting(unmapped, "Fire Ant", null, "2026-09-08");
        insertSighting(mapped, "Water Hyacinth", null, "not-a-date");

        List<MapSighting> sightings = sightingDAO.findForMap(null, null);

        assertEquals(1, sightings.size());
        assertEquals("Cane Toad", sightings.get(0).getSpeciesName());
    }

    @Test
    void returnsAnEmptyListWhenNothingMatches() {
        List<MapSighting> sightings = sightingDAO.findForMap("Cane Toad", null);

        assertTrue(sightings.isEmpty());
    }

    private int insertUser() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "map-tester");
            statement.setString(2, "map@example.com");
            statement.setString(3, "hash");
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private int insertSuburb(
            String name, String postcode, Double latitude, Double longitude) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO suburbs (name, postcode, latitude, longitude) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, postcode);
            statement.setObject(3, latitude);
            statement.setObject(4, longitude);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertSighting(
            int suburbId, String speciesName, String description, String sightedAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sightings "
                        + "(user_id, suburb_id, species_name, description, sighted_at) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            statement.setInt(1, userId);
            statement.setInt(2, suburbId);
            statement.setString(3, speciesName);
            statement.setString(4, description);
            statement.setString(5, sightedAt);
            statement.executeUpdate();
        }
    }
}
