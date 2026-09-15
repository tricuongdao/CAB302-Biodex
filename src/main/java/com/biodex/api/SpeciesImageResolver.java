package com.biodex.api;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Resolves species photo URLs with as few network calls as possible.
 *
 * <p>Image enrichment used to fan out one full {@code profile()} per card — each paying for the
 * Wikipedia description fallback the image caller never uses — and local {@code fake:} seeds
 * additionally paid for an {@code autocomplete()} per card, all sequentially. This resolver
 * keeps an in-memory cache of photo URLs by real ALA guid, remembers which real guid each
 * scientific name resolved to, and deduplicates in-flight lookups so ten cards for the same
 * species share one network call, not ten.
 *
 * <p>Network calls still go through the given {@link SpeciesService} (cached on disk), so
 * first-run results persist across restarts too. All methods are thread-safe.
 */
public final class SpeciesImageResolver {

    private final SpeciesService speciesService;
    private final Executor executor;

    /** Real ALA guid to photo URL lookup (null values are never stored). */
    private final ConcurrentHashMap<String, CompletableFuture<String>> urlByGuid =
            new ConcurrentHashMap<>();

    /** Lower-cased scientific name to real ALA guid for local seeds. */
    private final ConcurrentHashMap<String, String> guidByScientificName = new ConcurrentHashMap<>();

    /** Scientific names currently being resolved, so concurrent cards share one lookup. */
    private final ConcurrentHashMap<String, CompletableFuture<String>> guidLookupByScientificName =
            new ConcurrentHashMap<>();

    /** Creates a resolver over the given service with its own pool. */
    public SpeciesImageResolver(SpeciesService speciesService) {
        this.speciesService = speciesService;
        // Cached pool: each lookup blocks on network/disk, so a fixed pool of 6 can
        // deadlock when name lookups wait on image lookups with no free thread.
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "species-image-resolver");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Resolves the photo URL for a real ALA guid. Returns a completed future with null when the
     * guid is blank, fake, or has no image — callers keep the placeholder in that case.
     */
    public CompletableFuture<String> imageForGuid(String guid) {
        if (guid == null || guid.isBlank() || guid.startsWith("fake:")) {
            return CompletableFuture.completedFuture(null);
        }
        return urlByGuid.computeIfAbsent(guid, key ->
                CompletableFuture.supplyAsync(() -> speciesService.imageUrl(key), executor)
                        .whenComplete((url, error) -> {
                            // Never cache failures or nulls: a transient network
                            // error must not poison later visits.
                            if (error != null || url == null || url.isBlank()) {
                                urlByGuid.remove(key);
                            }
                        }));
    }

    /** Remembers a resolved URL, so direct-url cards also warm the cache. */
    public void remember(String guid, String url) {
        if (guid != null && !guid.isBlank() && !guid.startsWith("fake:")
                && url != null && !url.isBlank()) {
            urlByGuid.put(guid, CompletableFuture.completedFuture(url));
        }
    }

    /** Forgets a guid that failed to load, so a retry can try the network again. */
    public void forget(String guid) {
        if (guid != null) {
            urlByGuid.remove(guid);
        }
    }

    /**
     * Resolves the photo URL for a local seed by scientific name: the remembered ALA guid is
     * reused when known, otherwise one autocomplete finds the real taxon and its image. The
     * in-flight marker is dropped on completion so a later visit takes the fast guid path.
     */
    public CompletableFuture<String> imageForScientificName(String scientificName) {
        if (scientificName == null || scientificName.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        String normalised = scientificName.trim().toLowerCase(Locale.ROOT);
        String knownGuid = guidByScientificName.get(normalised);
        if (knownGuid != null) {
            return imageForGuid(knownGuid);
        }
        return guidLookupByScientificName.computeIfAbsent(normalised, key ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return resolveByScientificName(scientificName.trim());
                    } finally {
                        guidLookupByScientificName.remove(key);
                    }
                }, executor));
    }

    /** One autocomplete finds the real taxon; its image then comes from the guid cache. */
    private String resolveByScientificName(String scientificName) {
        String wanted = scientificName.toLowerCase(Locale.ROOT);
        java.util.List<com.biodex.api.dto.SpeciesSummary> matches;
        try {
            matches = speciesService.autocomplete(scientificName, 5);
        } catch (RuntimeException e) {
            return null;
        }
        if (matches == null) {
            return null;
        }
        com.biodex.api.dto.SpeciesSummary exact = null;
        com.biodex.api.dto.SpeciesSummary fallback = null;
        for (com.biodex.api.dto.SpeciesSummary match : matches) {
            if (match == null || match.getGuid() == null || match.getGuid().isBlank()
                    || match.getGuid().startsWith("fake:")) {
                continue;
            }
            boolean nameOk = (match.getScientificName() != null
                        && match.getScientificName().toLowerCase(Locale.ROOT).contains(wanted))
                    || (match.getCommonName() != null
                        && match.getCommonName().equalsIgnoreCase(scientificName));
            if (!nameOk) {
                continue;
            }
            if (fallback == null) {
                fallback = match;
            }
            if (match.getScientificName() != null
                    && match.getScientificName().trim().equalsIgnoreCase(scientificName)) {
                exact = match;
                break;
            }
        }
        com.biodex.api.dto.SpeciesSummary chosen = exact != null ? exact : fallback;
        if (chosen == null) {
            return null;
        }
        guidByScientificName.put(wanted, chosen.getGuid());
        if (chosen.getImageUrl() != null && !chosen.getImageUrl().isBlank()) {
            remember(chosen.getGuid(), chosen.getImageUrl());
            return chosen.getImageUrl();
        }
        // Nested lookup on the shared image cache (deadlock-free now the pool is
        // unbounded, and it deduplicates concurrent cards for the same taxon).
        try {
            return imageForGuid(chosen.getGuid()).join();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
