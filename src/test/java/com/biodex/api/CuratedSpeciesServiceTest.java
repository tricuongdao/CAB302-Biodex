package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The curated knowledge base and the merge it performs on top of a species service. All tests are
 * offline: the delegate is an in-memory stub and the content comes from the bundled resource, so
 * nothing here can depend on the network.
 */
class CuratedSpeciesServiceTest {

    private static final String CANE_TOAD_GUID = "stub:rhinella-marina";
    private static final String CANE_TOAD_SCIENTIFIC = "Rhinella marina";
    private static final String CANE_TOAD_COMMON = "Cane Toad";

    // ------------------------------------------------------------ profile merging

    @Test
    void curatedDetailFieldsFillWhatTheDelegateLacks() {
        SpeciesProfile bare = new SpeciesProfile(
                CANE_TOAD_GUID, CANE_TOAD_SCIENTIFIC, CANE_TOAD_COMMON, null, null, false);
        CuratedSpeciesService service = new CuratedSpeciesService(new StubDelegate(bare));

        SpeciesProfile merged = service.profile(CANE_TOAD_GUID);

        assertNotNull(merged);
        assertEquals(CANE_TOAD_COMMON, merged.getCommonName(), "identity still comes from the delegate");
        assertNotNull(merged.getDescription(), "the curated write-up should fill the gap");
        assertTrue(merged.getDescription().length() > 200,
                "the curated description should be a real write-up, not one line");
        assertNotNull(merged.getTypicalHabitat());
        assertNotNull(merged.getSizeRange());
        assertNotNull(merged.getDisposalGuidance());
        assertNotNull(merged.getReportAuthority());
        assertFalse(merged.getTags().isEmpty(), "curated tags should be present");
        assertFalse(merged.getThreatRatings().isEmpty(), "curated threat ratings should be present");
        assertTrue(merged.isInvasive(), "an invasive-curated tag should flip the flag");
    }

    @Test
    void theDelegateStillWinsOnTheFieldsItHas() {
        SpeciesProfile fromAtlas = new SpeciesProfile(
                CANE_TOAD_GUID, CANE_TOAD_SCIENTIFIC, "Toad", "Atlas's own words.",
                "https://images.ala.org.au/x.png", false,
                "Bufonidae", "Anura", "Amphibia", "Animalia", "Some status",
                List.of("From Atlas"), null, null, null, null,
                Map.of("Album count", 3));
        CuratedSpeciesService service = new CuratedSpeciesService(new StubDelegate(fromAtlas));

        SpeciesProfile merged = service.profile(CANE_TOAD_GUID);

        assertEquals("Toad", merged.getCommonName(), "delegate common name wins");
        assertEquals("https://images.ala.org.au/x.png", merged.getImageUrl(), "delegate photo wins");
        assertEquals("Bufonidae", merged.getFamily(), "delegate taxonomy wins");
        assertEquals("Album count", merged.getThreatRatings().keySet().iterator().next(),
                "delegate-only ratings survive the merge");
        assertTrue(merged.getThreatRatings().containsKey("Aggression"),
                "curated ratings are added alongside the delegate's");
        assertTrue(merged.getTags().contains("From Atlas"), "delegate tags are kept");
        assertTrue(merged.getTags().contains("Invasive"), "curated tags are kept too");
    }

    @Test
    void curatedDescriptionIsUsedInPreferenceToAThinOne() {
        SpeciesProfile withDescription = new SpeciesProfile(
                CANE_TOAD_GUID, CANE_TOAD_SCIENTIFIC, CANE_TOAD_COMMON,
                "A one sentence summary.", null, true);
        CuratedSpeciesService service = new CuratedSpeciesService(new StubDelegate(withDescription));

        SpeciesProfile merged = service.profile(CANE_TOAD_GUID);

        assertNotNull(merged.getDescription());
        assertTrue(merged.getDescription().length() > 200,
                "the detailed curated write-up replaces the short source text");
    }

    @Test
    void speciesWithoutACuratedEntryPassThroughUntouched() {
        SpeciesProfile unknown = new SpeciesProfile(
                "stub:tyrannosaurus-rex", "Tyrannosaurus rex", "T. rex",
                "A giant dinosaur.", "https://images.ala.org.au/rex.png", false);
        CuratedSpeciesService service = new CuratedSpeciesService(new StubDelegate(unknown));

        SpeciesProfile result = service.profile("stub:tyrannosaurus-rex");

        assertEquals("A giant dinosaur.", result.getDescription());
        assertEquals("T. rex", result.getCommonName());
        assertTrue(result.getTags().isEmpty(), "no curated tags for a species we do not cover");
        assertTrue(result.getThreatRatings().isEmpty());
    }

    @Test
    void curatedContentAnswersEvenWhenTheDelegateKnowsNothing() {
        CuratedSpeciesService service = new CuratedSpeciesService(new EmptyDelegate());
        // The bundled entry for the cane toad carries the fake guid used by the offline service,
        // so a null delegate result still resolves to a full profile.
        SpeciesProfile result = service.profile("fake:rhinella-marina");

        assertNotNull(result);
        assertNotNull(result.getDescription());
        assertTrue(result.isInvasive());
    }

    @Test
    void occurrencesAndDensityAreNotAltered() {
        SpeciesProfile profile = new SpeciesProfile(
                CANE_TOAD_GUID, CANE_TOAD_SCIENTIFIC, CANE_TOAD_COMMON, null, null, false);
        StubDelegate delegate = new StubDelegate(profile);
        CuratedSpeciesService service = new CuratedSpeciesService(delegate);

        assertTrue(service.occurrencesNear(CANE_TOAD_SCIENTIFIC, -27.47, 153.03, 50, 10).isEmpty());
        assertTrue(service.densityByArea(CANE_TOAD_SCIENTIFIC, "cl22").isEmpty());
        assertEquals(1, delegate.occurrenceCalls);
        assertEquals(1, delegate.densityCalls);
    }

    // ------------------------------------------------------------ autocomplete

    @Test
    void autocompleteFillsAMissingCommonNameFromCuratedContent() {
        SpeciesProfile profile = new SpeciesProfile(
                CANE_TOAD_GUID, CANE_TOAD_SCIENTIFIC, null, null, null, false);
        StubDelegate delegate = new StubDelegate(profile);
        delegate.autoWithoutCommonName = true;
        CuratedSpeciesService service = new CuratedSpeciesService(delegate);

        List<SpeciesSummary> results = service.autocomplete("Rhinella", 5);

        assertEquals(1, results.size());
        assertEquals(CANE_TOAD_COMMON, results.get(0).getCommonName(),
                "the curated knowledge base supplies the missing common name");
    }

    // ------------------------------------------------------------ content integrity

    @Test
    void everyCuratedEntryHasADetailedDescriptionAndHandlingGuidance() {
        List<CuratedSpeciesService.Curated> entries = CuratedSpeciesService.readEntries();
        assertFalse(entries.isEmpty(), "the knowledge base should be bundled with the app");

        for (CuratedSpeciesService.Curated entry : entries) {
            assertNotNull(entry.scientificName, "every entry needs a key to match on");
            assertFalse(entry.scientificName.isBlank());
            assertNotNull(entry.description, entry.scientificName + " needs a description");
            assertTrue(entry.description.length() > 200,
                    entry.scientificName + " needs a genuinely detailed description");
            assertNotNull(entry.typicalHabitat, entry.scientificName + " needs a habitat");
            assertNotNull(entry.sizeRange, entry.scientificName + " needs a size range");
            assertNotNull(entry.disposalGuidance, entry.scientificName + " needs disposal guidance");
            assertNotNull(entry.reportAuthority, entry.scientificName + " needs a report authority");
            assertNotNull(entry.tags, entry.scientificName + " needs tags");
            assertFalse(entry.tags.isEmpty());
            assertNotNull(entry.threatRatings, entry.scientificName + " needs threat ratings");
            assertFalse(entry.threatRatings.isEmpty());
        }
    }

    @Test
    void everyCuratedEntryCoversEachOfTheThreeStandardMetrics() {
        for (CuratedSpeciesService.Curated entry : CuratedSpeciesService.readEntries()) {
            Map<String, Integer> ratings = entry.threatRatings == null
                    ? Map.of()
                    : entry.threatRatings;
            for (String metric : List.of("Aggression", "Sting severity", "Spread risk")) {
                Integer score = ratings.get(metric);
                assertNotNull(score, entry.scientificName + " is missing " + metric);
                assertTrue(score >= 0 && score <= 100,
                        entry.scientificName + " score " + metric + " must be 0-100");
            }
        }
    }

    // ------------------------------------------------------------ stubs

    /** Returns one fixed profile, carrying either the full Atlas-style record or just the basics. */
    private static final class StubDelegate implements SpeciesService {

        private final SpeciesProfile profile;
        private boolean autoWithoutCommonName;
        int occurrenceCalls;
        int densityCalls;

        StubDelegate(SpeciesProfile profile) {
            this.profile = profile;
        }

        @Override
        public List<SpeciesSummary> autocomplete(String query, int limit) {
            return List.of(new SpeciesSummary(profile.getGuid(), profile.getScientificName(),
                    autoWithoutCommonName ? null : profile.getCommonName(), profile.getImageUrl()));
        }

        @Override
        public SpeciesProfile profile(String guid) {
            return profile;
        }

        @Override
        public List<OccurrencePoint> occurrencesNear(
                String scientificName, double lat, double lon, double radiusKm, int limit) {
            occurrenceCalls++;
            return new ArrayList<>();
        }

        @Override
        public List<AreaCount> densityByArea(String scientificName, String facetField) {
            densityCalls++;
            return new ArrayList<>();
        }
    }

    /** A service that answers nothing, for the "delegate knows nothing" path. */
    private static final class EmptyDelegate implements SpeciesService {

        @Override
        public List<SpeciesSummary> autocomplete(String query, int limit) {
            return List.of();
        }

        @Override
        public SpeciesProfile profile(String guid) {
            return null;
        }

        @Override
        public List<OccurrencePoint> occurrencesNear(
                String scientificName, double lat, double lon, double radiusKm, int limit) {
            return List.of();
        }

        @Override
        public List<AreaCount> densityByArea(String scientificName, String facetField) {
            return List.of();
        }
    }
}