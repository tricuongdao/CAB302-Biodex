package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;

import java.util.List;

/**
 * The live {@link SpeciesService}: every call goes straight to the Atlas of Living Australia.
 *
 * <p>Unlike the rest of the API layer this one <b>does</b> throw {@link ApiException} when a request
 * fails, because it has no cache to fall back on. Do not hand it to a controller — wrap it in
 * {@link CachedSpeciesService}, which is what {@link ServiceFactory#speciesService()} returns.
 */
public class AlaSpeciesService implements SpeciesService {

    private final AlaClient client;

    /** Uses a client of its own. */
    public AlaSpeciesService() {
        this(new AlaClient());
    }

    /** Uses the given client. */
    public AlaSpeciesService(AlaClient client) {
        this.client = client;
    }

    @Override
    public List<SpeciesSummary> autocomplete(String query, int limit) {
        return client.autocomplete(query, limit);
    }

    @Override
    public SpeciesProfile profile(String guid) {
        return client.profile(guid);
    }

    @Override
    public List<OccurrencePoint> occurrencesNear(
            String scientificName, double lat, double lon, double radiusKm, int limit) {
        return client.occurrencesNear(scientificName, lat, lon, radiusKm, limit);
    }

    @Override
    public List<AreaCount> densityByArea(String scientificName, String facetField) {
        return client.densityByArea(scientificName, facetField);
    }
}
