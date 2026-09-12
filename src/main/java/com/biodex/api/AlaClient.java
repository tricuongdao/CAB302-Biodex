package com.biodex.api;

import com.biodex.api.dto.AreaCount;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Speaks HTTP to the Atlas of Living Australia and maps the replies onto Biodex DTOs.
 *
 * <p>This is the only class that performs requests or reads JSON from the Atlas. Nothing above it
 * knows that an HTTP call is involved — controllers must go through
 * {@link ServiceFactory#speciesService()} and must never touch this class directly.
 *
 * <p>Every method blocks and throws {@link ApiException} on any failure. Callers are expected to be
 * {@link CachedSpeciesService}, which turns those failures into cached or empty results.
 *
 * <p>Parsing is deliberately forgiving: fields the Atlas omits become null rather than errors, and
 * records without coordinates are skipped, because real occurrence data is patchy.
 */
public class AlaClient {

    /** Applied to both connection setup and the individual request. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** Identifies Biodex to the Atlas, which asks callers to say who they are. */
    private static final String USER_AGENT = "Biodex/1.0 (QUT CAB302 student project)";

    private final HttpClient httpClient;

    /** Builds a client. Opens no connection — the first request does that. */
    public AlaClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Suggests taxa matching a partial name. */
    public List<SpeciesSummary> autocomplete(String query, int limit) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query);
        params.put("idxType", "TAXON");
        params.put("limit", String.valueOf(Math.max(limit, 0)));

        JsonObject root = getJson(AlaEndpoints.SPECIES_BASE + AlaEndpoints.AUTOCOMPLETE_PATH, params);

        List<SpeciesSummary> results = new ArrayList<>();
        for (JsonElement element : array(root, "autoCompleteList")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            results.add(new SpeciesSummary(
                    string(entry, "guid", "lsid"),
                    string(entry, "scientificName", "name", "nameComplete"),
                    string(entry, "commonName", "commonNameSingle"),
                    string(entry, "imageUrl", "thumbnailUrl", "smallImageUrl")));
        }
        return List.copyOf(results);
    }

    /**
     * Fetches one species profile by taxon guid.
     *
     * <p>The profile response nests its fields, and which of them are present varies by taxon, so
     * each value is looked for in a few places and left null when nowhere holds it.
     */
    public SpeciesProfile profile(String guid) {
        String path = AlaEndpoints.SPECIES_BASE + AlaEndpoints.PROFILE_PATH + encode(guid);
        JsonObject root = getJson(path, Map.of());

        JsonObject taxonConcept = object(root, "taxonConcept");
        return new SpeciesProfile(
                firstNonNull(string(taxonConcept, "guid"), guid),
                string(taxonConcept, "nameString", "nameComplete", "scientificName"),
                firstCommonName(root),
                description(root),
                imageUrl(root, taxonConcept),
                looksInvasive(root));
    }

    /** Finds individual records within a radius of a point. */
    public List<OccurrencePoint> occurrencesNear(
            String scientificName, double lat, double lon, double radiusKm, int limit) {
        int pageSize = Math.min(Math.max(limit, 0), AlaEndpoints.MAX_PAGE_SIZE);
        if (pageSize == 0) {
            return List.of();
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", taxonQuery(scientificName));
        params.put("lat", String.valueOf(lat));
        params.put("lon", String.valueOf(lon));
        params.put("radius", String.valueOf(radiusKm));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("start", "0");

        JsonObject root =
                getJson(AlaEndpoints.BIOCACHE_BASE + AlaEndpoints.OCCURRENCE_SEARCH_PATH, params);

        List<OccurrencePoint> points = new ArrayList<>();
        for (JsonElement element : array(root, "occurrences")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject record = element.getAsJsonObject();
            Double latitude = number(record, "decimalLatitude");
            Double longitude = number(record, "decimalLongitude");
            if (latitude == null || longitude == null) {
                // A record with no coordinates cannot be drawn, so it is of no use to us.
                continue;
            }
            points.add(new OccurrencePoint(
                    latitude,
                    longitude,
                    eventDate(record),
                    string(record, "dataResourceName", "dataResourceUid")));
        }
        return List.copyOf(points);
    }

    /**
     * Counts records grouped by a facet field. Asks for no records at all, only the facet buckets,
     * which is what keeps this usable for whole-region coverage.
     */
    public List<AreaCount> densityByArea(String scientificName, String facetField) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", taxonQuery(scientificName));
        params.put("facets", facetField);
        params.put("pageSize", "0");

        JsonObject root =
                getJson(AlaEndpoints.BIOCACHE_BASE + AlaEndpoints.OCCURRENCE_SEARCH_PATH, params);

        List<AreaCount> counts = new ArrayList<>();
        for (JsonElement element : array(root, "facetResults")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject facet = element.getAsJsonObject();
            String fieldName = string(facet, "fieldName");
            if (fieldName != null && !fieldName.equals(facetField)) {
                continue;
            }
            for (JsonElement bucketElement : array(facet, "fieldResult")) {
                if (!bucketElement.isJsonObject()) {
                    continue;
                }
                JsonObject bucket = bucketElement.getAsJsonObject();
                String label = string(bucket, "label", "i18nCode");
                Double count = number(bucket, "count");
                if (label != null && count != null) {
                    counts.add(new AreaCount(label, count.intValue()));
                }
            }
        }
        return List.copyOf(counts);
    }

    // ---------------------------------------------------------------- transport

    /** Performs the request and parses the body, or throws {@link ApiException}. */
    private JsonObject getJson(String path, Map<String, String> params) {
        URI uri = URI.create(path + queryString(params));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // Covers both connection failures and HttpTimeoutException.
            throw new ApiException("Could not reach the Atlas of Living Australia: " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Interrupted while calling the Atlas: " + uri, e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiException(
                    "The Atlas answered HTTP " + response.statusCode() + " for " + uri);
        }

        try {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (!parsed.isJsonObject()) {
                throw new ApiException("The Atlas returned a non-object body for " + uri);
            }
            return parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new ApiException("The Atlas returned unreadable JSON for " + uri, e);
        }
    }

    private static String queryString(Map<String, String> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder query = new StringBuilder("?");
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (query.length() > 1) {
                query.append('&');
            }
            query.append(encode(param.getKey())).append('=').append(encode(param.getValue()));
        }
        return query.toString();
    }

    private static String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Restricts a search to one taxon rather than matching the words anywhere in a record. */
    private static String taxonQuery(String scientificName) {
        return "taxon_name:\"" + (scientificName == null ? "" : scientificName) + "\"";
    }

    // ------------------------------------------------------------------ parsing

    /** Reads the first of the given keys that holds a usable string. */
    private static String string(JsonObject object, String... keys) {
        if (object == null) {
            return null;
        }
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonPrimitive()) {
                String value = element.getAsString().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static Double number(JsonObject object, String key) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsDouble();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null) {
            return null;
        }
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject parent, String key) {
        if (parent == null) {
            return new JsonArray();
        }
        JsonElement element = parent.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    /** Occurrence dates arrive either as epoch milliseconds or as an ISO date or date-time. */
    private static LocalDate eventDate(JsonObject record) {
        JsonElement element = record.get("eventDate");
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        if (element.getAsJsonPrimitive().isNumber()) {
            return Instant.ofEpochMilli(element.getAsLong()).atZone(ZoneOffset.UTC).toLocalDate();
        }
        String text = element.getAsString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String firstCommonName(JsonObject root) {
        for (JsonElement element : array(root, "commonNames")) {
            if (element.isJsonObject()) {
                String name = string(element.getAsJsonObject(), "nameString", "name");
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
    }

    /** Descriptions live in a list of loosely typed name/value pairs, when they are present. */
    private static String description(JsonObject root) {
        for (JsonElement element : array(root, "simpleProperties")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject property = element.getAsJsonObject();
            String name = string(property, "name", "title");
            if (name != null && name.toLowerCase(Locale.ROOT).contains("description")) {
                String value = string(property, "value");
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String imageUrl(JsonObject root, JsonObject taxonConcept) {
        for (JsonElement element : array(root, "images")) {
            if (element.isJsonObject()) {
                String url = string(
                        element.getAsJsonObject(), "largeImageUrl", "imageUrl", "thumbnailUrl");
                if (url != null) {
                    return url;
                }
            }
        }
        return string(taxonConcept, "imageUrl", "thumbnailUrl");
    }

    /**
     * Best-effort invasive flag. The Atlas has no single field for this, so the taxon's categories
     * and conservation lists are scanned for pest or invasive wording.
     */
    private static boolean looksInvasive(JsonObject root) {
        for (JsonElement element : array(root, "categories")) {
            if (element.isJsonPrimitive() && mentionsPest(element.getAsString())) {
                return true;
            }
        }
        JsonObject statuses = object(root, "conservationStatuses");
        if (statuses != null) {
            for (Map.Entry<String, JsonElement> entry : statuses.entrySet()) {
                if (entry.getValue().isJsonObject()
                        && mentionsPest(string(entry.getValue().getAsJsonObject(), "status"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean mentionsPest(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("invasive") || lower.contains("pest");
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }
}
