package com.biodex.dao;

import com.biodex.db.InMemoryDatabase;
import com.biodex.model.Suburb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuburbDAOTest {

    private Connection connection;
    private SuburbDAO suburbDAO;

    @BeforeEach
    void setUp() throws Exception {
        connection = InMemoryDatabase.open();
        suburbDAO = new SuburbDAO(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void suburbsAreOrderedByNameAndPostcode() throws Exception {
        try (var statement = connection.prepareStatement(
                "INSERT INTO suburbs (name, postcode) VALUES (?, ?), (?, ?), (?, ?)")) {
            statement.setString(1, "Zillmere");
            statement.setString(2, "4034");
            statement.setString(3, "Archerfield");
            statement.setString(4, "4108");
            statement.setString(5, "Bardon");
            statement.setString(6, "4065");
            statement.executeUpdate();
        }

        List<Suburb> suburbs = suburbDAO.findAllOrderedByName();

        assertEquals(List.of("Archerfield", "Bardon", "Zillmere"),
                suburbs.stream().map(Suburb::getName).toList());
    }

    @Test
    void emptyLookupReturnsEmptyList() {
        assertTrue(suburbDAO.searchByName("Nowhere").isEmpty());
        assertTrue(suburbDAO.findAll().isEmpty());
    }

    @Test
    void mapSearchMatchesPartialNamesAndSharedPostcodesWithoutSightings() throws Exception {
        insertSuburb("Bardon", "4065", -27.46, 152.98);
        insertSuburb("Rainworth", "4065", -27.47, 152.98);
        insertSuburb("Brisbane", "4000", -27.4698, 153.0251);

        assertEquals(List.of("Bardon"), names(suburbDAO.searchForMap("  ARD  ")));
        assertEquals(List.of("Bardon", "Rainworth"), names(suburbDAO.searchForMap(" 4065 ")));
        assertTrue(suburbDAO.searchForMap("406").isEmpty(), "Postcodes must match exactly");
        assertTrue(suburbDAO.searchForMap("Nowhere").isEmpty());
    }

    @Test
    void mapSearchOnlyReturnsNamedAreasWithValidBrisbaneCoordinates() throws Exception {
        insertSuburb("Valid", "4000", -27.4698, 153.0251);
        insertSuburb("Outside Brisbane", "4000", -33.86, 151.21);
        insertSuburb("Missing latitude", "4000", null, 153.0251);
        insertSuburb("Missing longitude", "4000", -27.4698, null);
        insertSuburb("Invalid coordinates", "4000", Double.POSITIVE_INFINITY, 153.0251);
        insertSuburb("   ", "4000", -27.4698, 153.0251);

        assertEquals(List.of("Valid"), names(suburbDAO.searchForMap("4000")));
    }

    @Test
    void mapSearchTreatsWildcardsAndQuotesAsLiteralText() throws Exception {
        insertSuburb("Creek_100%\\Road", "4000", -27.4698, 153.0251);
        insertSuburb("Brisbane", "4000", -27.4698, 153.0251);
        insertSuburb("O'Connell", "4001", -27.4698, 153.0251);

        for (String query : List.of("_", "%", "\\")) {
            assertEquals(List.of("Creek_100%\\Road"), names(suburbDAO.searchForMap(query)));
        }
        assertEquals(List.of("O'Connell"), names(suburbDAO.searchForMap("O'Conn")));
        assertTrue(suburbDAO.searchForMap("' OR 1=1 --").isEmpty());
    }

    @Test
    void blankMapSearchDoesNotReturnAllAreas() throws Exception {
        insertSuburb("Brisbane", "4000", -27.4698, 153.0251);
        assertTrue(suburbDAO.searchForMap(null).isEmpty());
        assertTrue(suburbDAO.searchForMap("").isEmpty());
        assertTrue(suburbDAO.searchForMap("   ").isEmpty());
    }

    private static List<String> names(List<Suburb> suburbs) {
        return suburbs.stream().map(Suburb::getName).toList();
    }

    private void insertSuburb(String name, String postcode, Double latitude, Double longitude) throws Exception {
        try (var statement = connection.prepareStatement(
                "INSERT INTO suburbs (name, postcode, latitude, longitude) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, name);
            statement.setString(2, postcode);
            statement.setObject(3, latitude);
            statement.setObject(4, longitude);
            statement.executeUpdate();
        }
    }
}
