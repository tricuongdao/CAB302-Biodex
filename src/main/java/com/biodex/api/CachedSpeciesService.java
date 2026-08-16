package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.dao.ApiCacheDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wraps another {@link SpeciesService} in the {@code api_cache} table so repeated lookups cost
 * nothing and the app keeps working when the network does not.
 *
 * <p>Each call reads the cache first and only asks the delegate on a miss or once the entry has
 * expired. If the delegate then fails, a stale entry is served in preference to nothing; if there is
 * no entry at all, the result is an empty list — or null for a profile. {@link ApiException} never
 * escapes this class, so a controller never has to catch anything.
 *
 * <p>Cached entries hold Biodex's own DTOs, serialised here. Reading the Atlas's response format
 * remains {@link AlaClient}'s job alone — this class has no idea what the Atlas returns.
 */
public class CachedSpeciesService implements SpeciesService {

    /** Names and profiles barely change, so they are held for a day. */
    private static final Duration SPECIES_TTL = Duration.ofHours(24);

    /** Occurrence data is added to continually, so it is held for an hour. */
    private static final Duration OCCURRENCE_TTL = Duration.ofHours(1);

    private static final Type SUMMARY_LIST = new TypeToken<List<SpeciesSummary>>() {}.getType();
    private static final Type POINT_LIST = new TypeToken<List<OccurrencePoint>>() {}.getType();
    private static final Type AREA_LIST = new TypeToken<List<AreaCount>>() {}.getType();

    /** Gson cannot reflect into {@code java.time}, so dates are stored as plain ISO strings. */
    private static final TypeAdapter<LocalDate> LOCAL_DATE_ADAPTER =
            new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    return LocalDate.parse(in.nextString());
                }
            }.nullSafe();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, LOCAL_DATE_ADAPTER)
            .create();

    private final SpeciesService delegate;
    private final ApiCacheDao cache;

    /**
     * @param delegate the service consulted on a cache miss
     * @param cache    where responses are kept between runs
     */
    public CachedSpeciesService(SpeciesService delegate, ApiCacheDao cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public List<SpeciesSummary> autocomplete(String query, int limit) {
        String normalised = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return cached(
                key("species:auto", normalised, limit),
                SPECIES_TTL,
                SUMMARY_LIST,
                () -> delegate.autocomplete(query, limit),
                List.of());
    }

    @Override
    public SpeciesProfile profile(String guid) {
        return cached(
                key("species:profile", guid),
                SPECIES_TTL,
                SpeciesProfile.class,
                () -> delegate.profile(guid),
                null);
    }

    @Override
    public List<OccurrencePoint> occurrencesNear(
            String scientificName, double lat, double lon, double radiusKm, int limit) {
        return cached(
                key("occurrence:near", scientificName, lat, lon, radiusKm, limit),
                OCCURRENCE_TTL,
                POINT_LIST,
                () -> delegate.occurrencesNear(scientificName, lat, lon, radiusKm, limit),
                List.of());
    }

    @Override
    public List<AreaCount> densityByArea(String scientificName, String facetField) {
        return cached(
                key("occurrence:density", scientificName, facetField),
                OCCURRENCE_TTL,
                AREA_LIST,
                () -> delegate.densityByArea(scientificName, facetField),
                List.of());
    }

    /**
     * The whole caching policy, in one place.
     *
     * @param key          identifies this exact request
     * @param ttl          how long an entry stays fresh
     * @param type         what the payload deserialises to
     * @param fetch        how to get the value when the cache cannot supply it
     * @param emptyResult  what to return when the fetch fails and the cache is empty
     */
    private <T> T cached(String key, Duration ttl, Type type, Supplier<T> fetch, T emptyResult) {
        Optional<ApiCacheDao.CacheEntry> entry = cache.get(key);

        if (entry.isPresent() && !entry.get().isOlderThan(ttl)) {
            T hit = deserialise(entry.get().getPayload(), type);
            if (hit != null) {
                return hit;
            }
            // Unreadable payload, most likely written by an older build. Treat it as a miss.
        }

        try {
            T value = fetch.get();
            if (value != null) {
                cache.put(key, GSON.toJson(value));
            }
            return value;
        } catch (ApiException e) {
            if (entry.isPresent()) {
                T stale = deserialise(entry.get().getPayload(), type);
                if (stale != null) {
                    return stale;
                }
            }
            return emptyResult;
        }
    }

    /** Returns null rather than throwing when a stored payload cannot be read back. */
    private static <T> T deserialise(String payload, Type type) {
        try {
            return GSON.fromJson(payload, type);
        } catch (JsonParseException e) {
            return null;
        }
    }

    private static String key(String prefix, Object... parts) {
        StringBuilder builder = new StringBuilder(prefix);
        for (Object part : parts) {
            builder.append(':').append(part);
        }
        return builder.toString();
    }
}
