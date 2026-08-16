package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;

import java.util.List;

/**
 * Species and occurrence data for the feature pages. This is the only API type a controller should
 * name — obtain one from {@link ServiceFactory#speciesService()} and never construct an
 * implementation directly.
 *
 * <p><b>Every method blocks.</b> They perform network and disk work on the calling thread, so a
 * controller must call them from a {@code javafx.concurrent.Task} and apply the result in
 * {@code setOnSucceeded}. Calling one on the FX Application Thread will freeze the window. The
 * implementations deliberately contain no threading of their own; concurrency is the caller's
 * decision.
 *
 * <p><b>These methods do not throw on network failure.</b> When the network is unavailable the
 * implementation falls back to cached data, and failing that returns an empty list or null. A page
 * should render an empty state, not an error dialog.
 */
public interface SpeciesService {

    /**
     * Suggests species matching a partial name, for a search or autocomplete field.
     *
     * <p>Debounce the calling field by 300ms rather than firing on every keystroke.
     *
     * @param query partial scientific or common name; blank yields no suggestions
     * @param limit maximum number of suggestions to return
     * @return matching species, empty if none or if the lookup failed; never null
     */
    List<SpeciesSummary> autocomplete(String query, int limit);

    /**
     * Fetches the full detail record for one species.
     *
     * @param guid the identifier from a {@link SpeciesSummary}
     * @return the profile, or null when the species is unknown or the lookup failed
     */
    SpeciesProfile profile(String guid);

    /**
     * Finds individual recorded sightings within a radius of a point, for pin-style mapping.
     *
     * @param scientificName the species to search for
     * @param lat            centre latitude in decimal degrees
     * @param lon            centre longitude in decimal degrees
     * @param radiusKm       search radius in kilometres
     * @param limit          maximum number of points; the upstream API caps a request at 5000
     * @return matching points, empty if none or if the lookup failed; never null
     */
    List<OccurrencePoint> occurrencesNear(
            String scientificName, double lat, double lon, double radiusKm, int limit);

    /**
     * Counts records per named area, for shading a heat map.
     *
     * <p>Prefer this over {@link #occurrencesNear} for map density: it returns aggregated buckets
     * rather than raw points, so it is not subject to the 5000-record cap.
     *
     * @param scientificName the species to count
     * @param facetField     the upstream field to group by, such as {@code cl22} for states
     * @return one count per area, empty if none or if the lookup failed; never null
     */
    List<AreaCount> densityByArea(String scientificName, String facetField);
}
