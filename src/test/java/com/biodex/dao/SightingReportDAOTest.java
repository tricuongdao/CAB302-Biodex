package com.biodex.dao;

import com.biodex.db.InMemoryDatabase;
import com.biodex.model.SightingReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Density and recency behaviour of the local sighting reports - what the Species Details
 * "Local sightings" pane renders, and what the Identify screen's Submit report writes.
 * Each test runs against a fresh in-memory database.
 */
class SightingReportDAOTest {

    private Connection connection;
    private SightingReportDAO reportDAO;
    private int speciesId;
    private int otherSpeciesId;

    @BeforeEach
    void setUp() throws SQLException {
        connection = InMemoryDatabase.open();
        reportDAO = new SightingReportDAO(connection);
        speciesId = insertSpecies("Cane Toad", "Rhinella marina");
        otherSpeciesId = insertSpecies("Common Myna", "Acridotheres tristis");
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void insertStoresAllFieldsAndReturnsGeneratedId() {
        Instant when = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        int id = reportDAO.insertReport(report(speciesId, "Kedron", "Near the creek", when));

        assertTrue(id > 0);
        List<SightingReport> reports = reportDAO.getRecentReports(speciesId, 10);
        assertEquals(1, reports.size());
        SightingReport saved = reports.get(0);
        assertEquals("Kedron", saved.getSuburb());
        assertEquals("Near the creek", saved.getLocationLabel());
        assertEquals(when, saved.getReportedAt());
        assertFalse(saved.isVerified(), "new reports start unverified");
    }

    @Test
    void missingReportedAtFallsBackToNow() {
        int id = reportDAO.insertReport(report(speciesId, "Kedron", "No timestamp", null));

        assertTrue(id > 0);
        SightingReport saved = reportDAO.getRecentReports(speciesId, 1).get(0);
        assertNotNull(saved.getReportedAt());
        assertTrue(saved.getReportedAt().isAfter(Instant.now().minus(5, ChronoUnit.MINUTES)),
                "a report without its own time is stamped with the current time");
    }

    @Test
    void recentReportsAreNewestFirstAndRespectTheLimit() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        reportDAO.insertReport(report(speciesId, "Kedron", "oldest", now.minus(10, ChronoUnit.DAYS)));
        reportDAO.insertReport(report(speciesId, "Gordon Park", "middle", now.minus(2, ChronoUnit.DAYS)));
        reportDAO.insertReport(report(speciesId, "Kedron", "newest", now.minus(1, ChronoUnit.HOURS)));

        List<SightingReport> topTwo = reportDAO.getRecentReports(speciesId, 2);

        assertEquals(2, topTwo.size());
        assertEquals("newest", topTwo.get(0).getLocationLabel());
        assertEquals("middle", topTwo.get(1).getLocationLabel());
        assertEquals(3, reportDAO.getRecentReports(speciesId, 10).size());
    }

    @Test
    void densityGroupsBySuburbWithinTheWindowAndSortsByCount() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        reportDAO.insertReport(report(speciesId, "Kedron", "a", now.minus(1, ChronoUnit.DAYS)));
        reportDAO.insertReport(report(speciesId, "Kedron", "b", now.minus(3, ChronoUnit.DAYS)));
        reportDAO.insertReport(report(speciesId, "Gordon Park", "c", now.minus(5, ChronoUnit.DAYS)));
        reportDAO.insertReport(report(speciesId, "Kedron", "too old for the window",
                now.minus(40, ChronoUnit.DAYS)));

        Map<String, Integer> density = reportDAO.getDensityBySuburb(speciesId, 30);

        assertEquals(2, density.size());
        assertEquals(2, density.get("Kedron"), "the 40-day-old report is outside the 30-day window");
        assertEquals(1, density.get("Gordon Park"));
        assertEquals("Kedron", new ArrayList<>(density.keySet()).get(0), "highest count first");
    }

    @Test
    void reportsForOtherSpeciesAreNotIncluded() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        reportDAO.insertReport(report(otherSpeciesId, "Kedron", "myna", now));

        assertTrue(reportDAO.getRecentReports(speciesId, 10).isEmpty());
        assertTrue(reportDAO.getDensityBySuburb(speciesId, 30).isEmpty());
    }

    private SightingReport report(int speciesId, String suburb, String label, Instant when) {
        SightingReport report = new SightingReport();
        report.setSpeciesId(speciesId);
        report.setSuburb(suburb);
        report.setLocationLabel(label);
        report.setReportedAt(when);
        return report;
    }

    private int insertSpecies(String commonName, String scientificName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO species (common_name, scientific_name, threat_level, "
                        + "aggression, sting_severity, spread_risk) VALUES (?, ?, 'HIGH', 50, 50, 50)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, commonName);
            statement.setString(2, scientificName);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}
