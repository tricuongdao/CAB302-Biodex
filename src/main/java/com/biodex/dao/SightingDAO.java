package com.biodex.dao;

import com.biodex.db.DataSeeder;
import com.biodex.model.MapSighting;
import com.biodex.util.BrisbaneMapProjection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TreeSet;

/**
 * Reads sightings in a map-ready form by joining each record to its suburb coordinates, and
 * accepts new sightings so a filed report appears on the heat map. Both filters are optional: a
 * null or blank species includes every species, while a null start date includes every date. All
 * user-supplied values are bound through a prepared statement.
 */
public class SightingDAO extends BaseDao {

    private static final String SELECT_MAP_SIGHTINGS = """
            SELECT s.sighting_id,
                   s.species_name,
                   s.description,
                   date(s.sighted_at) AS sighting_date,
                   su.name AS suburb_name,
                   su.postcode,
                   su.latitude,
                   su.longitude
              FROM sightings s
              JOIN suburbs su ON su.suburb_id = s.suburb_id
             WHERE su.latitude IS NOT NULL
               AND su.longitude IS NOT NULL
               AND date(s.sighted_at) IS NOT NULL
               AND (? IS NULL OR lower(trim(s.species_name)) = lower(?))
               AND (? IS NULL OR date(s.sighted_at) >= date(?))
             ORDER BY datetime(s.sighted_at) DESC, s.sighting_id DESC
            """;

    /** Matches the shape SQLite writes for datetime('now'), so stored values stay comparable. */
    private static final DateTimeFormatter SQLITE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final String INSERT_SIGHTING = """
            INSERT INTO sightings
                  (user_id, suburb_id, species_name, description, image_path, sighted_at)
            VALUES (?, ?, ?, ?, ?, COALESCE(?, datetime('now')))
            """;

    /** Uses the shared application connection. */
    public SightingDAO() {
        super();
    }

    /** Uses the given connection. Tests pass an in-memory connection here. */
    public SightingDAO(Connection connection) {
        super(connection);
    }

    /**
     * Returns alphabetically ordered, case-insensitive species choices from plottable sightings.
     * Deliberately ignores the active date filter so older species remain available to select.
     */
    public List<String> findSpeciesForMap() {
        TreeSet<String> species = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (MapSighting sighting : findForMap(null, null)) {
            String name = normaliseOptional(sighting.getSpeciesName());
            if (name != null && BrisbaneMapProjection.project(
                    sighting.getLatitude(), sighting.getLongitude()).isPresent()) {
                species.add(name);
            }
        }
        return List.copyOf(species);
    }

    /**
     * Returns sightings suitable for plotting on the heat map.
     *
     * @param speciesName exact species name to include, or null/blank for every species
     * @param fromDate earliest sighting date to include (inclusive), or null for every date
     */
    public List<MapSighting> findForMap(String speciesName, LocalDate fromDate) {
        String speciesFilter = normaliseOptional(speciesName);
        String dateFilter = fromDate == null ? null : fromDate.toString();

        return queryMany(
                SELECT_MAP_SIGHTINGS,
                statement -> {
                    statement.setString(1, speciesFilter);
                    statement.setString(2, speciesFilter);
                    statement.setString(3, dateFilter);
                    statement.setString(4, dateFilter);
                },
                SightingDAO::mapSighting);
    }

    private static String normaliseOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Inserts a sighting the heat map can plot. {@code sightedAt} defaults to the current time
     * when the caller has none. Returns the generated sighting_id.
     */
    public int insertSighting(int userId, int suburbId, String speciesName, String description,
            String imagePath, Instant sightedAt) {
        return insertReturningKey(INSERT_SIGHTING,
                statement -> {
                    statement.setInt(1, userId);
                    statement.setInt(2, suburbId);
                    statement.setString(3, speciesName);
                    statement.setString(4, description);
                    statement.setString(5, imagePath);
                    statement.setString(6, sightedAt == null
                            ? null
                            : SQLITE_TIMESTAMP.format(sightedAt));
                });
    }

    /**
     * Seeds the demo suburbs and sample sightings through this DAO's own connection when those
     * tables are empty, so a fresh install still shows a working heat map. Repeat calls are
     * cheap (a count per table) and never duplicate rows.
     */
    public void seedDemoDataIfEmpty() {
        DataSeeder.seedIfEmpty(getConnection());
    }

    private static MapSighting mapSighting(ResultSet resultSet) throws SQLException {
        return new MapSighting(
                resultSet.getInt("sighting_id"),
                resultSet.getString("species_name"),
                resultSet.getString("description"),
                LocalDate.parse(resultSet.getString("sighting_date")),
                resultSet.getString("suburb_name"),
                resultSet.getString("postcode"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"));
    }
}
