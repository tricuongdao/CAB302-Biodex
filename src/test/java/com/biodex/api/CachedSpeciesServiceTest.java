package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.dao.ApiCacheDao;
import com.biodex.db.InMemoryDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the caching policy: what is served from the cache, what triggers a refetch, and what a
 * caller gets when the network is gone.
 *
 * <p>Entirely offline. The delegate is {@link CountingSpeciesService}, so no HTTP is involved, and
 * the cache is a throwaway in-memory database.
 */
class CachedSpeciesServiceTest {

    private static final DateTimeFormatter SQLITE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection connection;
    private CountingSpeciesService client;
    private CachedSpeciesService service;

    @BeforeEach
    void setUp() throws SQLException {
        connection = InMemoryDatabase.open();
        client = new CountingSpeciesService();
        service = new CachedSpeciesService(client, new ApiCacheDao(connection));
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void firstLookupAsksTheClientAndReturnsItsData() {
        List<SpeciesSummary> results = service.autocomplete("cane", 5);

        assertEquals(1, client.getAutocompleteCalls());
        assertEquals(1, results.size());
        assertEquals(CountingSpeciesService.SCIENTIFIC_NAME, results.get(0).getScientificName());
    }

    @Test
    void cacheHitDoesNotInvokeTheClient() {
        service.autocomplete("cane", 5);
        assertEquals(1, client.getAutocompleteCalls(), "the first call has to reach the client");

        List<SpeciesSummary> second = service.autocomplete("cane", 5);

        assertEquals(1, client.getAutocompleteCalls(), "the second call should be served from cache");
        assertEquals(CountingSpeciesService.SCIENTIFIC_NAME, second.get(0).getScientificName());
    }

    @Test
    void differentArgumentsAreCachedSeparately() {
        service.autocomplete("cane", 5);
        service.autocomplete("toad", 5);

        assertEquals(2, client.getAutocompleteCalls(), "a different query is a different request");
    }

    @Test
    void queryCasingAndPaddingShareOneCacheEntry() {
        service.autocomplete("cane", 5);
        service.autocomplete("  CANE  ", 5);

        assertEquals(1, client.getAutocompleteCalls(), "the key should be normalised");
    }

    @Test
    void expiredEntryTriggersARefetch() throws SQLException {
        service.autocomplete("cane", 5);
        assertEquals(1, client.getAutocompleteCalls());

        ageEveryEntryBy(Duration.ofHours(25));
        List<SpeciesSummary> refetched = service.autocomplete("cane", 5);

        assertEquals(2, client.getAutocompleteCalls(), "a stale species entry should be refetched");
        assertEquals(CountingSpeciesService.SCIENTIFIC_NAME, refetched.get(0).getScientificName());
    }

    @Test
    void occurrenceDataExpiresAfterAnHourWhileSpeciesDataDoesNot() throws SQLException {
        service.autocomplete("cane", 5);
        service.occurrencesNear(CountingSpeciesService.SCIENTIFIC_NAME, -27.47, 153.02, 10, 100);

        ageEveryEntryBy(Duration.ofHours(2));
        service.autocomplete("cane", 5);
        service.occurrencesNear(CountingSpeciesService.SCIENTIFIC_NAME, -27.47, 153.02, 10, 100);

        assertEquals(1, client.getAutocompleteCalls(), "species data is good for 24 hours");
        assertEquals(2, client.getOccurrencesNearCalls(), "occurrence data is good for 1 hour");
    }

    @Test
    void clientFailureWithStaleCacheReturnsTheStaleData() throws SQLException {
        service.autocomplete("cane", 5);
        ageEveryEntryBy(Duration.ofHours(25));
        client.startFailing();

        List<SpeciesSummary> stale = service.autocomplete("cane", 5);

        assertEquals(2, client.getAutocompleteCalls(), "it should have tried the client first");
        assertEquals(1, stale.size(), "stale data beats no data");
        assertEquals(CountingSpeciesService.SCIENTIFIC_NAME, stale.get(0).getScientificName());
    }

    @Test
    void clientFailureWithStaleCacheReturnsStaleOccurrencePointsIncludingNullDates()
            throws SQLException {
        service.occurrencesNear(CountingSpeciesService.SCIENTIFIC_NAME, -27.47, 153.02, 10, 100);
        ageEveryEntryBy(Duration.ofHours(2));
        client.startFailing();

        List<OccurrencePoint> stale =
                service.occurrencesNear(CountingSpeciesService.SCIENTIFIC_NAME, -27.47, 153.02, 10, 100);

        assertEquals(2, stale.size());
        assertEquals(-27.4698, stale.get(0).getLatitude(), 0.00001);
        assertNotNull(stale.get(0).getEventDate(), "a stored date should survive the round trip");
        assertEquals("2024-11-03", stale.get(0).getEventDate().toString());
        assertNull(stale.get(1).getEventDate(), "a missing date should stay missing");
    }

    @Test
    void clientFailureWithStaleCacheReturnsTheStaleProfile() throws SQLException {
        service.profile(CountingSpeciesService.GUID);
        ageEveryEntryBy(Duration.ofHours(25));
        client.startFailing();

        SpeciesProfile stale = service.profile(CountingSpeciesService.GUID);

        assertNotNull(stale, "a cached profile should be served when the client fails");
        assertEquals(CountingSpeciesService.SCIENTIFIC_NAME, stale.getScientificName());
        assertTrue(stale.isInvasive());
    }

    @Test
    void clientFailureWithEmptyCacheReturnsAnEmptyList() {
        client.startFailing();

        assertTrue(service.autocomplete("cane", 5).isEmpty(), "autocomplete should degrade to empty");
        assertTrue(
                service.occurrencesNear(CountingSpeciesService.SCIENTIFIC_NAME, -27.47, 153.02, 10, 100)
                        .isEmpty(),
                "occurrencesNear should degrade to empty");
        assertTrue(
                service.densityByArea(CountingSpeciesService.SCIENTIFIC_NAME, "cl22").isEmpty(),
                "densityByArea should degrade to empty");
    }

    @Test
    void clientFailureWithEmptyCacheReturnsNullProfileRatherThanThrowing() {
        client.startFailing();

        assertNull(service.profile(CountingSpeciesService.GUID));
    }

    @Test
    void densityCountsSurviveTheCacheRoundTrip() {
        service.densityByArea(CountingSpeciesService.SCIENTIFIC_NAME, "cl22");

        List<AreaCount> cached = service.densityByArea(CountingSpeciesService.SCIENTIFIC_NAME, "cl22");

        assertEquals(1, client.getDensityByAreaCalls(), "the second call should be served from cache");
        assertEquals("Brisbane City", cached.get(0).getAreaName());
        assertEquals(1893, cached.get(0).getCount());
    }

    @Test
    void anUnreadableCachedPayloadIsTreatedAsAMiss() throws SQLException {
        service.autocomplete("cane", 5);
        corruptEveryPayload();

        List<SpeciesSummary> results = service.autocomplete("cane", 5);

        assertEquals(2, client.getAutocompleteCalls(), "a corrupt entry should not be trusted");
        assertEquals(CountingSpeciesService.SCIENTIFIC_NAME, results.get(0).getScientificName());
    }

    /** Backdates every cached entry so the service sees it as expired. */
    private void ageEveryEntryBy(Duration amount) throws SQLException {
        String stamp = SQLITE_TIMESTAMP.format(
                LocalDateTime.ofInstant(Instant.now().minus(amount), ZoneOffset.UTC));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE api_cache SET fetched_at = '" + stamp + "'");
        }
    }

    private void corruptEveryPayload() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE api_cache SET payload = '{ not json'");
        }
    }
}
