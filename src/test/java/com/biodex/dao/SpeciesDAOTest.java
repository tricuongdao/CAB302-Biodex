package com.biodex.dao;

import com.biodex.db.InMemoryDatabase;
import com.biodex.model.Species;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The lookups Species Details (and the Identify report flow) rely on, against the real seeds. */
class SpeciesDAOTest {

    private Connection connection;
    private SpeciesDAO speciesDAO;

    @BeforeEach
    void setUp() throws SQLException {
        connection = InMemoryDatabase.open();
        speciesDAO = new SpeciesDAO(connection);
        speciesDAO.seedIfEmpty();
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void seedsTheCuratedSpeciesSetOnce() {
        assertEquals(9, speciesDAO.findAll().size());

        speciesDAO.seedIfEmpty();

        assertEquals(9, speciesDAO.findAll().size(), "seeding twice must not duplicate rows");
    }

    @Test
    void findsSeededSpeciesByGuidAndNames() {
        Optional<Species> byGuid = speciesDAO.findByAlaGuid("fake:rhinella-marina");
        assertTrue(byGuid.isPresent());
        assertEquals("Cane Toad", byGuid.get().getCommonName());

        assertTrue(speciesDAO.findByScientificName("Rhinella marina").isPresent());
        assertTrue(speciesDAO.findByScientificName("rhinella MARINA").isPresent(),
                "scientific name lookup ignores case");
        assertTrue(speciesDAO.findByCommonName("cane toad").isPresent(),
                "common name lookup ignores case");
    }

    @Test
    void unknownSpeciesComeBackEmpty() {
        assertTrue(speciesDAO.findByScientificName("Tyrannosaurus rex").isEmpty());
        assertTrue(speciesDAO.findByCommonName("Drop Bear").isEmpty());
        assertTrue(speciesDAO.findByAlaGuid("").isEmpty());
        assertTrue(speciesDAO.findById(9999).isEmpty());
    }

    @Test
    void findByIdCarriesTheThreatProfileAndTags() {
        Species caneToad = speciesDAO.findByAlaGuid("fake:rhinella-marina").orElseThrow();

        Species loaded = speciesDAO.findById(caneToad.getSpeciesId()).orElseThrow();

        assertEquals(Species.ThreatLevel.HIGH, loaded.getThreatLevel());
        assertNotNull(loaded.getDisposalGuidance());
        assertNotNull(loaded.getTypicalHabitat());
        assertTrue(loaded.getSizeRange().contains("mm"));
        assertTrue(loaded.getTags().contains("Invasive"), "tags are joined from species_tags");
    }
}
