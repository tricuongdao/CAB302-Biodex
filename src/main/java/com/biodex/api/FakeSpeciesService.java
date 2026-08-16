package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A {@link SpeciesService} backed by three hardcoded invasive species, for building pages without a
 * network connection. Switch it on with the offline constant in {@link ServiceFactory}.
 *
 * <p>Makes no network calls and touches no database. Results are fixed, so a page built against it
 * behaves identically on every run — useful for demos and for working offline.
 *
 * <p>Two deliberate simplifications to be aware of when testing a page against it:
 *
 * <ul>
 *   <li>{@code imageUrl} is always null. There are no bundled images, so image handling must be
 *       exercised against the real service.
 *   <li>{@link #occurrencesNear} ignores the centre point and radius, and {@link #densityByArea}
 *       ignores the facet field. Each returns the same fixed Brisbane-area data for a species
 *       regardless of those arguments.
 * </ul>
 */
public final class FakeSpeciesService implements SpeciesService {

    private static final SpeciesProfile FIRE_ANT = new SpeciesProfile(
            "fake:solenopsis-invicta",
            "Solenopsis invicta",
            "Red Imported Fire Ant",
            "A small, aggressive ant first detected at the Port of Brisbane in 2001. Builds dome "
                    + "shaped mounds in open ground and inflicts a painful, blistering sting. "
                    + "Subject to a national eradication program.",
            null,
            true);

    private static final SpeciesProfile CANE_TOAD = new SpeciesProfile(
            "fake:rhinella-marina",
            "Rhinella marina",
            "Cane Toad",
            "A large toxic toad released in Queensland in 1935 in a failed attempt to control cane "
                    + "beetles. Secretes a poison from its shoulder glands that kills native "
                    + "predators which try to eat it.",
            null,
            true);

    private static final SpeciesProfile WATER_HYACINTH = new SpeciesProfile(
            "fake:eichhornia-crassipes",
            "Eichhornia crassipes",
            "Water Hyacinth",
            "A free floating aquatic weed with mauve flowers. Forms mats dense enough to block "
                    + "waterways, strip the water of oxygen and crowd out native plants.",
            null,
            true);

    private static final List<SpeciesProfile> SPECIES =
            List.of(FIRE_ANT, CANE_TOAD, WATER_HYACINTH);

    /** Fixed sighting points, roughly matching where each species is found around Brisbane. */
    private static final Map<String, List<OccurrencePoint>> OCCURRENCES = Map.of(
            FIRE_ANT.getScientificName(),
            List.of(
                    point(-27.6171, 152.7608, "2024-11-03", "Queensland Fire Ant Surveillance"),
                    point(-27.5502, 152.9884, "2024-09-17", "Queensland Fire Ant Surveillance"),
                    point(-27.4698, 153.0251, "2023-12-01", "iNaturalist Australia"),
                    point(-27.6392, 153.1086, "2023-08-22", "Queensland Fire Ant Surveillance")),
            CANE_TOAD.getScientificName(),
            List.of(
                    point(-27.4436, 153.1731, "2025-02-14", "iNaturalist Australia"),
                    point(-27.3858, 153.0334, "2024-10-30", "Queensland Museum"),
                    point(-27.4858, 152.9896, "2024-03-09", "iNaturalist Australia"),
                    point(-27.0847, 152.9510, null, "Queensland Museum")),
            WATER_HYACINTH.getScientificName(),
            List.of(
                    point(-27.5500, 152.9900, "2025-01-20", "Queensland Herbarium"),
                    point(-27.4300, 152.9100, "2024-07-11", "Queensland Herbarium"),
                    point(-27.6089, 153.3000, "2023-11-05", "iNaturalist Australia")));

    /** Fixed per-area counts, shaped like the facet buckets the real service returns. */
    private static final Map<String, List<AreaCount>> DENSITY = Map.of(
            FIRE_ANT.getScientificName(),
            List.of(
                    new AreaCount("Brisbane City", 412),
                    new AreaCount("Ipswich City", 268),
                    new AreaCount("Logan City", 197),
                    new AreaCount("Moreton Bay", 64)),
            CANE_TOAD.getScientificName(),
            List.of(
                    new AreaCount("Brisbane City", 1893),
                    new AreaCount("Moreton Bay", 1204),
                    new AreaCount("Redland City", 731),
                    new AreaCount("Logan City", 655)),
            WATER_HYACINTH.getScientificName(),
            List.of(
                    new AreaCount("Brisbane City", 143),
                    new AreaCount("Ipswich City", 88),
                    new AreaCount("Redland City", 39)));

    @Override
    public List<SpeciesSummary> autocomplete(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<SpeciesSummary> matches = new ArrayList<>();
        for (SpeciesProfile species : SPECIES) {
            if (matches.size() == limit) {
                break;
            }
            if (contains(species.getScientificName(), needle)
                    || contains(species.getCommonName(), needle)) {
                matches.add(new SpeciesSummary(
                        species.getGuid(),
                        species.getScientificName(),
                        species.getCommonName(),
                        species.getImageUrl()));
            }
        }
        return List.copyOf(matches);
    }

    @Override
    public SpeciesProfile profile(String guid) {
        for (SpeciesProfile species : SPECIES) {
            if (species.getGuid().equals(guid)) {
                return species;
            }
        }
        return null;
    }

    @Override
    public List<OccurrencePoint> occurrencesNear(
            String scientificName, double lat, double lon, double radiusKm, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<OccurrencePoint> points = OCCURRENCES.getOrDefault(scientificName, List.of());
        return points.size() <= limit ? points : List.copyOf(points.subList(0, limit));
    }

    @Override
    public List<AreaCount> densityByArea(String scientificName, String facetField) {
        return DENSITY.getOrDefault(scientificName, List.of());
    }

    private static boolean contains(String haystack, String lowercaseNeedle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(lowercaseNeedle);
    }

    private static OccurrencePoint point(
            double lat, double lon, String isoDate, String dataResourceName) {
        return new OccurrencePoint(
                lat, lon, isoDate == null ? null : LocalDate.parse(isoDate), dataResourceName);
    }
}
