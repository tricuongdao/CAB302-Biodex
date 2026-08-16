package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;

import java.time.LocalDate;
import java.util.List;

/**
 * Test stub standing in for the network-backed service. It counts how many times it was asked for
 * something, which is how the cache tests tell a cache hit from a refetch, and it can be told to
 * start failing so the fallback paths can be exercised.
 *
 * <p>Calls are counted before a failure is raised, so a test can prove that the cache really did try
 * the delegate rather than skipping it.
 */
final class CountingSpeciesService implements SpeciesService {

    static final String SCIENTIFIC_NAME = "Rhinella marina";
    static final String COMMON_NAME = "Cane Toad";
    static final String GUID = "stub:rhinella-marina";

    private int autocompleteCalls;
    private int profileCalls;
    private int occurrencesNearCalls;
    private int densityByAreaCalls;
    private boolean failing;

    /** Every later call throws {@link ApiException}, as if the network had dropped. */
    void startFailing() {
        failing = true;
    }

    int getAutocompleteCalls() {
        return autocompleteCalls;
    }

    int getProfileCalls() {
        return profileCalls;
    }

    int getOccurrencesNearCalls() {
        return occurrencesNearCalls;
    }

    int getDensityByAreaCalls() {
        return densityByAreaCalls;
    }

    @Override
    public List<SpeciesSummary> autocomplete(String query, int limit) {
        autocompleteCalls++;
        failIfAsked();
        return List.of(new SpeciesSummary(GUID, SCIENTIFIC_NAME, COMMON_NAME, null));
    }

    @Override
    public SpeciesProfile profile(String guid) {
        profileCalls++;
        failIfAsked();
        return new SpeciesProfile(GUID, SCIENTIFIC_NAME, COMMON_NAME, "A toxic toad.", null, true);
    }

    @Override
    public List<OccurrencePoint> occurrencesNear(
            String scientificName, double lat, double lon, double radiusKm, int limit) {
        occurrencesNearCalls++;
        failIfAsked();
        return List.of(
                new OccurrencePoint(-27.4698, 153.0251, LocalDate.of(2024, 11, 3), "Stub Dataset"),
                new OccurrencePoint(-27.3858, 153.0334, null, "Stub Dataset"));
    }

    @Override
    public List<AreaCount> densityByArea(String scientificName, String facetField) {
        densityByAreaCalls++;
        failIfAsked();
        return List.of(new AreaCount("Brisbane City", 1893));
    }

    private void failIfAsked() {
        if (failing) {
            throw new ApiException("stub was told to fail");
        }
    }
}
