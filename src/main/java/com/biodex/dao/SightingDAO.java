package com.biodex.dao;

import com.biodex.model.MapSighting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Reads sightings in a map-ready form by joining each record to its suburb coordinates.
 *
 * <p>Both filters are optional. A null or blank species includes every species, while a null start
 * date includes every date. All user-supplied values are bound through a prepared statement.
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
               AND (? IS NULL OR lower(s.species_name) = lower(?))
               AND (? IS NULL OR date(s.sighted_at) >= date(?))
             ORDER BY datetime(s.sighted_at) DESC, s.sighting_id DESC
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
