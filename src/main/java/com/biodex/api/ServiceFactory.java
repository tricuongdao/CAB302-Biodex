package com.biodex.api;

import com.biodex.dao.ApiCacheDao;

/**
 * Hands out the API layer's services. This is the entry point for every feature page:
 *
 * <pre>{@code
 * SpeciesService svc = ServiceFactory.speciesService();
 * }</pre>
 *
 * <p>Controllers should name no other type from this package. Which implementation comes back, and
 * whether it caches, is decided here so no page has to care.
 */
public final class ServiceFactory {

    /**
     * Set to true to run the whole app against hardcoded data, with no network calls at all. Useful
     * on a bad connection, on a plane, or for a demo that must not depend on the Atlas being up.
     * Leave it false when committing.
     */
    private static final boolean OFFLINE = false;

    private static SpeciesService speciesService;

    private ServiceFactory() {
    }

    /**
     * The shared species service — cached, network-backed, and safe to call from any page. Built on
     * first use, so importing this class costs nothing.
     *
     * <p>Remember that every method on the returned service blocks. Call it inside a
     * {@code javafx.concurrent.Task}, never on the FX Application Thread.
     */
    public static synchronized SpeciesService speciesService() {
        if (speciesService == null) {
            speciesService = OFFLINE
                    ? new FakeSpeciesService()
                    : new CachedSpeciesService(new AlaSpeciesService(), new ApiCacheDao());
        }
        return speciesService;
    }
}
