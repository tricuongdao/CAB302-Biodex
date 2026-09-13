package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wraps another {@link SpeciesService} and overlays Biodex's bundled pest knowledge base
 * (see {@code /com/biodex/data/pest-content.json}) onto whatever the delegate returns.
 *
 * <p>The Atlas of Living Australia is the authority for names, photos and occurrence records, but
 * its profiles are patchy: many species carry no description at all, and none carry the practical
 * facts a pest tracker needs. The curated content fixes that with detailed, multi-paragraph
 * descriptions and safe handling guidance for the species Biodex deals with most. Merge rules:
 *
 * <ul>
 *   <li>The delegate wins on identity and media: guid, names, photo and the invasive flag.</li>
 *   <li>The curated entry wins on description text, so every covered species gets a real write-up
 *       instead of a one-line Wikipedia extract.</li>
 *   <li>Curated-only fields fill what the Atlas does not model: habitat, size, disposal guidance,
 *       report authority, threat ratings and extra tag chips.</li>
 * </ul>
 *
 * <p>Content is matched by guid first, then scientific name, then common name. If the resource is
 * missing or unparsable the wrapper passes the delegate through untouched — the knowledge base is
 * a bonus, never a hard dependency. Like every other {@link SpeciesService}, all methods block;
 * callers run them on a background task.
 */
public final class CuratedSpeciesService implements SpeciesService {

    /** Classpath location of the bundled knowledge base. */
    static final String CONTENT_RESOURCE = "/com/biodex/data/pest-content.json";

    private static final Gson GSON = new Gson();
    private static final Type CURATED_LIST = new TypeToken<List<Curated>>() {}.getType();

    private final SpeciesService delegate;
    private final Map<String, Curated> byGuid = new LinkedHashMap<>();
    private final Map<String, Curated> byScientificName = new LinkedHashMap<>();
    private final Map<String, Curated> byCommonName = new LinkedHashMap<>();

    /** Wraps the given service. Profiles that have no curated match pass through unchanged. */
    public CuratedSpeciesService(SpeciesService delegate) {
        this.delegate = delegate;
        load();
    }

    @Override
    public List<SpeciesSummary> autocomplete(String query, int limit) {
        List<SpeciesSummary> results = delegate.autocomplete(query, limit);
        List<SpeciesSummary> enriched = new ArrayList<>(results.size());
        for (SpeciesSummary summary : results) {
            if (summary.getCommonName() == null || summary.getCommonName().isBlank()) {
                Curated curated = byScientificName.get(normalise(summary.getScientificName()));
                if (curated != null && curated.commonName != null) {
                    summary = new SpeciesSummary(summary.getGuid(), summary.getScientificName(),
                            curated.commonName, summary.getImageUrl());
                }
            }
            enriched.add(summary);
        }
        return List.copyOf(enriched);
    }

    @Override
    public SpeciesProfile profile(String guid) {
        SpeciesProfile base = delegate.profile(guid);
        Curated curated = find(base, guid);
        if (curated == null) {
            return base;
        }
        if (base == null) {
            // The lookup failed or the delegate does not know this guid, but the knowledge base
            // does - answer from the curated entry alone rather than showing "could not load".
            return fromCurated(curated, guid);
        }
        return merge(base, curated);
    }

    @Override
    public List<OccurrencePoint> occurrencesNear(
            String scientificName, double lat, double lon, double radiusKm, int limit) {
        return delegate.occurrencesNear(scientificName, lat, lon, radiusKm, limit);
    }

    @Override
    public List<AreaCount> densityByArea(String scientificName, String facetField) {
        return delegate.densityByArea(scientificName, facetField);
    }

    // ---------------------------------------------------------------- merging

    private SpeciesProfile merge(SpeciesProfile base, Curated curated) {
        return new SpeciesProfile(
                base.getGuid(),
                firstNonNull(base.getScientificName(), curated.scientificName),
                firstNonNull(base.getCommonName(), curated.commonName),
                firstNonNull(curated.description, base.getDescription()),
                base.getImageUrl(),
                base.isInvasive() || mentionsPest(curated.tags),
                firstNonNull(base.getFamily(), curated.family),
                firstNonNull(base.getOrder(), curated.order),
                firstNonNull(base.getTaxonClass(), curated.taxonClass),
                firstNonNull(base.getKingdom(), curated.kingdom),
                firstNonNull(base.getConservationStatus(), curated.conservationStatus),
                unionTags(curated.tags, base.getTags()),
                firstNonNull(base.getTypicalHabitat(), curated.typicalHabitat),
                firstNonNull(base.getSizeRange(), curated.sizeRange),
                firstNonNull(base.getDisposalGuidance(), curated.disposalGuidance),
                firstNonNull(base.getReportAuthority(), curated.reportAuthority),
                mergedRatings(base.getThreatRatings(), curated.threatRatings));
    }

    /** A profile built purely from curated content, for when the delegate comes back empty. */
    private SpeciesProfile fromCurated(Curated curated, String guid) {
        return new SpeciesProfile(
                firstNonNull(curated.guid, guid),
                curated.scientificName,
                curated.commonName,
                curated.description,
                null,
                mentionsPest(curated.tags),
                curated.family,
                curated.order,
                curated.taxonClass,
                curated.kingdom,
                curated.conservationStatus,
                nullToEmpty(curated.tags),
                curated.typicalHabitat,
                curated.sizeRange,
                curated.disposalGuidance,
                curated.reportAuthority,
                curated.threatRatings == null ? Map.of() : curated.threatRatings);
    }

    private Curated find(SpeciesProfile base, String guid) {
        if (base == null) {
            return byGuid.get(normalise(guid));
        }
        Curated match = byGuid.get(normalise(base.getGuid()));
        if (match == null) {
            match = byScientificName.get(normalise(base.getScientificName()));
        }
        if (match == null) {
            match = byCommonName.get(normalise(base.getCommonName()));
        }
        return match;
    }

    /** Curated tags first, delegate tags appended, duplicates removed. */
    private static List<String> unionTags(List<String> curated, List<String> delegate) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String tag : nullToEmpty(curated)) {
            if (tag != null && !tag.isBlank()) {
                tags.add(tag.trim());
            }
        }
        for (String tag : nullToEmpty(delegate)) {
            if (tag != null && !tag.isBlank()) {
                tags.add(tag.trim());
            }
        }
        return List.copyOf(tags);
    }

    private static Map<String, Integer> mergedRatings(
            Map<String, Integer> delegate, Map<String, Integer> curated) {
        LinkedHashMap<String, Integer> merged = new LinkedHashMap<>();
        if (curated != null) {
            merged.putAll(curated);
        }
        if (delegate != null) {
            merged.putAll(delegate);
        }
        return Map.copyOf(merged);
    }

    private static boolean mentionsPest(List<String> tags) {
        for (String tag : nullToEmpty(tags)) {
            if (tag == null) {
                continue;
            }
            String lower = tag.toLowerCase(Locale.ROOT);
            if (lower.contains("invasive") || lower.contains("pest")) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonNull(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    // ---------------------------------------------------------------- knowledge base

    private void load() {
        for (Curated entry : readEntries()) {
            if (entry == null || entry.scientificName == null || entry.scientificName.isBlank()) {
                continue;
            }
            byScientificName.putIfAbsent(normalise(entry.scientificName), entry);
            if (entry.commonName != null && !entry.commonName.isBlank()) {
                byCommonName.putIfAbsent(normalise(entry.commonName), entry);
            }
            if (entry.guid != null && !entry.guid.isBlank()) {
                byGuid.putIfAbsent(normalise(entry.guid), entry);
            }
        }
    }

    /** Parses the bundled knowledge base. Returns an empty list on any failure, never throws. */
    static List<Curated> readEntries() {
        try (InputStream input = CuratedSpeciesService.class.getResourceAsStream(CONTENT_RESOURCE)) {
            if (input == null) {
                return List.of();
            }
            List<Curated> entries = GSON.fromJson(
                    new InputStreamReader(input, StandardCharsets.UTF_8), CURATED_LIST);
            return entries == null ? List.of() : entries;
        } catch (Exception e) {
            // A broken or missing knowledge base must never break the app; the delegate's data is
            // enough to render the page, just with fewer rich fields filled in.
            return List.of();
        }
    }

    private static String normalise(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * One row of the curated knowledge base. Fields are package-private so this wrapper can read
     * them directly; Gson populates them from {@code pest-content.json}.
     */
    static final class Curated {
        String guid;
        String scientificName;
        String commonName;
        String description;
        String family;
        String order;
        String taxonClass;
        String kingdom;
        String conservationStatus;
        List<String> tags;
        String typicalHabitat;
        String sizeRange;
        String disposalGuidance;
        String reportAuthority;
        Map<String, Integer> threatRatings;
    }
}