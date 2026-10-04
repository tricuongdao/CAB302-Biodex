package com.biodex.dao;

import com.biodex.model.Suburb;
import com.biodex.util.BrisbaneMapProjection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Loads suburbs for settings and sighting location choices. */
public class SuburbDAO extends BaseDao {

    public SuburbDAO() {
        super();
    }

    public SuburbDAO(Connection connection) {
        super(connection);
    }

    public List<Suburb> findAllOrderedByName() {
        return queryMany(
                "SELECT suburb_id, name, postcode, latitude, longitude "
                        + "FROM suburbs ORDER BY name COLLATE NOCASE ASC, postcode ASC",
                statement -> {
                },
                SuburbDAO::mapRow);
    }

    public List<Suburb> findAll() {
        return findAllOrderedByName();
    }

    public List<Suburb> searchByName(String name) {
        return queryMany(
                "SELECT suburb_id, name, postcode, latitude, longitude "
                        + "FROM suburbs WHERE name LIKE ? "
                        + "ORDER BY name COLLATE NOCASE ASC, postcode ASC",
                statement -> statement.setString(1, name + "%"),
                SuburbDAO::mapRow);
    }

    /**
     * Finds saved, mappable areas by a partial name or an exact postcode, even without sightings.
     * Search text is literal: SQL wildcards are escaped and all parameters are bound.
     */
    public List<Suburb> searchForMap(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String term = query.trim();
        String namePattern = "%" + term.replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_") + "%";
        return queryMany(
                "SELECT suburb_id, name, postcode, latitude, longitude FROM suburbs "
                        + "WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND trim(name) <> '' "
                        + "AND (name LIKE ? ESCAPE '\\' OR trim(postcode) = ?) "
                        + "ORDER BY name COLLATE NOCASE ASC, postcode ASC",
                statement -> {
                    statement.setString(1, namePattern);
                    statement.setString(2, term);
                }, SuburbDAO::mapRow).stream()
                .filter(suburb -> BrisbaneMapProjection.project(suburb.getLatitude(), suburb.getLongitude()).isPresent())
                .toList();
    }

    private static Suburb mapRow(ResultSet resultSet) throws SQLException {
        Suburb suburb = new Suburb();
        suburb.setSuburbId(resultSet.getInt("suburb_id"));
        suburb.setName(resultSet.getString("name"));
        suburb.setPostcode(resultSet.getString("postcode"));
        suburb.setLatitude(resultSet.getDouble("latitude"));
        suburb.setLongitude(resultSet.getDouble("longitude"));
        return suburb;
    }
}
