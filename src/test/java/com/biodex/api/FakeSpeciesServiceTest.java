package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the offline stand-in honours the {@link SpeciesService} contract, so a page built
 * against it behaves the same way once the real service is switched back on.
 */
class FakeSpeciesServiceTest {

    private static final String FIRE_ANT = "Solenopsis invicta";
    private static final String CANE_TOAD = "Rhinella marina";
    private static final String WATER_HYACINTH = "Eichhornia crassipes";

    private FakeSpeciesService service;

    @BeforeEach
    void setUp() {
        service = new FakeSpeciesService();
    }

    @Test
    void carriesThreeInvasiveSpecies() {
        List<SpeciesSummary> all = service.autocomplete("a", 10);

        assertEquals(3, all.size(), "the fake is documented as holding three species");
        for (SpeciesSummary summary : all) {
            SpeciesProfile profile = service.profile(summary.getGuid());
            assertNotNull(profile, "every suggested guid should resolve to a profile");
            assertTrue(profile.isInvasive(), profile.getScientificName() + " should be invasive");
        }
    }

    @Test
    void autocompleteMatchesOnScientificName() {
        List<SpeciesSummary> results = service.autocomplete("Rhinella", 10);

        assertEquals(1, results.size());
        assertEquals(CANE_TOAD, results.get(0).getScientificName());
    }

    @Test
    void autocompleteMatchesOnCommonNameIgnoringCase() {
        List<SpeciesSummary> results = service.autocomplete("cane TOAD", 10);

        assertEquals(1, results.size());
        assertEquals("Cane Toad", results.get(0).getCommonName());
    }

    @Test
    void autocompleteRespectsTheLimit() {
        assertEquals(2, service.autocomplete("a", 2).size());
        assertEquals(1, service.autocomplete("a", 1).size());
        assertTrue(service.autocomplete("a", 0).isEmpty(), "a limit of zero yields nothing");
    }

    @Test
    void autocompleteReturnsEmptyRatherThanNullForNoMatch() {
        assertTrue(service.autocomplete("tyrannosaurus", 10).isEmpty());
        assertTrue(service.autocomplete("", 10).isEmpty(), "a blank query should not suggest anything");
        assertTrue(service.autocomplete("   ", 10).isEmpty());
        assertNotNull(service.autocomplete(null, 10), "must return a list, never null");
        assertTrue(service.autocomplete(null, 10).isEmpty());
    }

    @Test
    void everySuggestionCarriesAGuidAndAScientificName() {
        for (SpeciesSummary summary : service.autocomplete("a", 10)) {
            assertNotNull(summary.getGuid(), "guid is needed to look up the profile");
            assertNotNull(summary.getScientificName(), "scientific name is needed to search occurrences");
        }
    }

    @Test
    void profileReturnsTheFullRecordForAKnownGuid() {
        String guid = service.autocomplete("Rhinella", 1).get(0).getGuid();

        SpeciesProfile profile = service.profile(guid);

        assertNotNull(profile);
        assertEquals(CANE_TOAD, profile.getScientificName());
        assertEquals("Cane Toad", profile.getCommonName());
        assertNotNull(profile.getDescription(), "the fake should supply something to display");
        assertFalse(profile.getDescription().isBlank());
        assertTrue(profile.isInvasive());
    }

    @Test
    void profileReturnsNullForAnUnknownGuid() {
        assertNull(service.profile("no-such-guid"));
        assertNull(service.profile(null));
    }

    @Test
    void occurrencesNearReturnsBrisbaneAreaPoints() {
        List<OccurrencePoint> points = service.occurrencesNear(FIRE_ANT, -27.4698, 153.0251, 50, 100);

        assertFalse(points.isEmpty(), "the fake should have points for a species it knows");
        for (OccurrencePoint point : points) {
            assertTrue(point.getLatitude() < -26 && point.getLatitude() > -29,
                    "latitude should be in south east Queensland: " + point.getLatitude());
            assertTrue(point.getLongitude() > 152 && point.getLongitude() < 154,
                    "longitude should be in south east Queensland: " + point.getLongitude());
            assertNotNull(point.getDataResourceName(), "points should be attributed");
        }
    }

    @Test
    void occurrencesNearRespectsTheLimit() {
        assertEquals(2, service.occurrencesNear(CANE_TOAD, -27.47, 153.02, 50, 2).size());
        assertTrue(service.occurrencesNear(CANE_TOAD, -27.47, 153.02, 50, 0).isEmpty());
    }

    @Test
    void occurrencesNearReturnsEmptyForAnUnknownSpecies() {
        assertTrue(service.occurrencesNear("Tyrannosaurus rex", -27.47, 153.02, 50, 100).isEmpty());
    }

    @Test
    void densityByAreaReturnsPositiveCountsPerNamedArea() {
        List<AreaCount> counts = service.densityByArea(WATER_HYACINTH, "cl22");

        assertFalse(counts.isEmpty());
        for (AreaCount count : counts) {
            assertNotNull(count.getAreaName());
            assertTrue(count.getCount() > 0, "a facet bucket should never be empty");
        }
    }

    @Test
    void densityByAreaReturnsEmptyForAnUnknownSpecies() {
        assertTrue(service.densityByArea("Tyrannosaurus rex", "cl22").isEmpty());
    }

    @Test
    void resultsAreStableAcrossCalls() {
        assertEquals(
                service.densityByArea(FIRE_ANT, "cl22").size(),
                service.densityByArea(FIRE_ANT, "cl22").size(),
                "the fake must be deterministic so demos are repeatable");
    }
}
