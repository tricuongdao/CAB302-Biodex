package com.biodex.api;

/**
 * Thrown by {@link AlaClient} when a request to the Atlas of Living Australia cannot be completed —
 * the network is unreachable, the request timed out, the service answered with a non-2xx status, or
 * the response body was not the JSON we expected.
 *
 * <p>Unchecked, because there is nothing a caller can usefully do about it at the call site. It is
 * caught and turned into cached or empty results by {@code CachedSpeciesService}, so it should never
 * reach a controller.
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
