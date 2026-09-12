package com.biodex.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Reads and writes the {@code api_cache} table, which holds raw JSON responses from the Atlas of
 * Living Australia so the app still has data to show when the network is unavailable.
 *
 * <p>Cache keys are opaque strings built by the caller — they identify a request, not a species.
 * Nothing here interprets the payload; it is stored and returned verbatim.
 *
 * <p>Page owners should not use this class. The API layer's caching is already handled by
 * {@code CachedSpeciesService}; go through {@code ServiceFactory.speciesService()} instead.
 */
public class ApiCacheDao extends BaseDao {

    /** The format SQLite's {@code CURRENT_TIMESTAMP} writes, which is always UTC. */
    private static final DateTimeFormatter SQLITE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Uses the shared application connection. */
    public ApiCacheDao() {
        super();
    }

    /** Uses the given connection. Tests pass an in-memory one here. */
    public ApiCacheDao(Connection connection) {
        super(connection);
    }

    /** Looks up a cached response. Returns empty when the key has never been written. */
    public Optional<CacheEntry> get(String key) {
        return queryOne(
                "SELECT payload, fetched_at FROM api_cache WHERE cache_key = ?",
                statement -> statement.setString(1, key),
                ApiCacheDao::mapRow);
    }

    /** Stores a response, replacing any earlier one for the same key and restamping its age. */
    public void put(String key, String payload) {
        update(
                "INSERT INTO api_cache (cache_key, payload, fetched_at) "
                        + "VALUES (?, ?, CURRENT_TIMESTAMP) "
                        + "ON CONFLICT (cache_key) DO UPDATE SET "
                        + "payload = excluded.payload, fetched_at = CURRENT_TIMESTAMP",
                statement -> {
                    statement.setString(1, key);
                    statement.setString(2, payload);
                });
    }

    /**
     * Deletes every entry older than the given time to live.
     *
     * @return the number of entries removed
     */
    public int deleteExpired(Duration ttl) {
        String cutoff = format(Instant.now().minus(ttl));
        return update(
                "DELETE FROM api_cache WHERE fetched_at < ?",
                statement -> statement.setString(1, cutoff));
    }

    private static CacheEntry mapRow(ResultSet resultSet) throws SQLException {
        return new CacheEntry(
                resultSet.getString("payload"), parse(resultSet.getString("fetched_at")));
    }

    private static String format(Instant instant) {
        return SQLITE_TIMESTAMP.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    private static Instant parse(String timestamp) {
        if (timestamp == null) {
            // An entry of unknown age is treated as expired rather than trusted.
            return Instant.EPOCH;
        }
        return LocalDateTime.parse(timestamp, SQLITE_TIMESTAMP).toInstant(ZoneOffset.UTC);
    }

    /** One cached response: the stored payload and the moment it was written. */
    public static final class CacheEntry {

        private final String payload;
        private final Instant fetchedAt;

        public CacheEntry(String payload, Instant fetchedAt) {
            this.payload = payload;
            this.fetchedAt = fetchedAt;
        }

        public String getPayload() {
            return payload;
        }

        public Instant getFetchedAt() {
            return fetchedAt;
        }

        /** True when this entry has outlived the given time to live. */
        public boolean isOlderThan(Duration ttl) {
            return fetchedAt.plus(ttl).isBefore(Instant.now());
        }
    }
}
