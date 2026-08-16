package com.biodex.api;

/**
 * The Atlas of Living Australia addresses Biodex talks to. This is the only file in the project that
 * contains a URL — if you are about to write one somewhere else, add it here instead.
 *
 * <p>All of these endpoints are public: they need no API key and no authentication.
 */
public final class AlaEndpoints {

    /** Base of the species web service, which serves name lookups and profiles. */
    public static final String SPECIES_BASE = "https://api.ala.org.au/species";

    /** Base of the biocache web service, which serves occurrence records. */
    public static final String BIOCACHE_BASE = "https://biocache-ws.ala.org.au/ws";

    /** Name autocomplete, relative to {@link #SPECIES_BASE}. */
    public static final String AUTOCOMPLETE_PATH = "/search/auto";

    /** Single species profile, relative to {@link #SPECIES_BASE}; append the taxon guid. */
    public static final String PROFILE_PATH = "/species/";

    /** Occurrence record search, relative to {@link #BIOCACHE_BASE}. */
    public static final String OCCURRENCE_SEARCH_PATH = "/occurrences/search";

    /**
     * The most records occurrence search will return for one request. Use facet counts, not raw
     * points, when a page needs coverage of a whole region.
     */
    public static final int MAX_PAGE_SIZE = 5000;

    private AlaEndpoints() {
    }
}
