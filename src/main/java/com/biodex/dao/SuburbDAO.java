package com.biodex.dao;

import com.biodex.model.Suburb;

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
