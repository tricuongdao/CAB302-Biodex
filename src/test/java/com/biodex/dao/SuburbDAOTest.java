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
}
