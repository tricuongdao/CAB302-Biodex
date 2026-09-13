package com.biodex.dao;

import com.biodex.db.DataSeeder;
import com.biodex.model.Species;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for the local curated {@code species} table.
 */
public class SpeciesDAO extends BaseDao {

    private static final String SELECT_BY_ID = """
            SELECT s.*, GROUP_CONCAT(st.tag, ',') AS tags
              FROM species s
              LEFT JOIN species_tags st ON st.species_id = s.species_id
             WHERE s.species_id = ?
             GROUP BY s.species_id
            """;

    private static final String SELECT_BY_ALA_GUID = """
            SELECT s.*, GROUP_CONCAT(st.tag, ',') AS tags
              FROM species s
              LEFT JOIN species_tags st ON st.species_id = s.species_id
             WHERE s.ala_guid = ?
             GROUP BY s.species_id
            """;

    private static final String SELECT_BY_SCIENTIFIC = """
            SELECT s.*, GROUP_CONCAT(st.tag, ',') AS tags
              FROM species s
              LEFT JOIN species_tags st ON st.species_id = s.species_id
             WHERE lower(s.scientific_name) = lower(?)
             GROUP BY s.species_id
            """;

    /** Uses the shared application connection. */
    public SpeciesDAO() {
        super();
    }

    /** Uses the given connection (for tests). */
    public SpeciesDAO(Connection connection) {
        super(connection);
    }

    /** Finds a species by its local primary key. */
    public Optional<Species> findById(int speciesId) {
        return queryOne(SELECT_BY_ID,
                stmt -> stmt.setInt(1, speciesId),
                this::mapRow);
    }

    /** Finds a species by its Atlas of Living Australia GUID. */
    public Optional<Species> findByAlaGuid(String alaGuid) {
        if (alaGuid == null || alaGuid.isBlank()) {
            return Optional.empty();
        }
        return queryOne(SELECT_BY_ALA_GUID,
                stmt -> stmt.setString(1, alaGuid.trim()),
                this::mapRow);
    }

    /** Finds a species by exact scientific name (case-insensitive). */
    public Optional<Species> findByScientificName(String scientificName) {
        if (scientificName == null || scientificName.isBlank()) {
            return Optional.empty();
        }
        return queryOne(SELECT_BY_SCIENTIFIC,
                stmt -> stmt.setString(1, scientificName.trim()),
                this::mapRow);
    }

    /** Returns all species for the showcase grid. */
    public List<Species> findAll() {
        String sql = "SELECT s.*, GROUP_CONCAT(st.tag, ',') AS tags FROM species s LEFT JOIN species_tags st ON st.species_id = s.species_id GROUP BY s.species_id ORDER BY s.common_name";
        return queryMany(sql, stmt -> {}, this::mapRow);
    }

    /** Seeds the species table with curated data if empty. Called lazily on first Pest access. */
    public void seedIfEmpty() {
        synchronized (getConnection()) {
            try (var statement = getConnection().createStatement();
                    var count = statement.executeQuery("SELECT COUNT(*) FROM species")) {
                if (count.next() && count.getInt(1) > 0) {
                    return;
                }
            } catch (SQLException e) {
                throw new DataAccessException("Failed to check species count", e);
            }
            DataSeeder.seedIfEmpty(getConnection());
        }
    }

    private Species mapRow(ResultSet rs) throws SQLException {
        Species s = new Species();
        s.setSpeciesId(rs.getInt("species_id"));
        s.setCommonName(rs.getString("common_name"));
        s.setScientificName(rs.getString("scientific_name"));
        s.setThreatLevel(rs.getString("threat_level"));
        s.setAggression(rs.getInt("aggression"));
        s.setStingSeverity(rs.getInt("sting_severity"));
        s.setSpreadRisk(rs.getInt("spread_risk"));
        s.setTypicalHabitat(rs.getString("typical_habitat"));
        s.setSizeMinMm(rs.getDouble("size_min_mm"));
        s.setSizeMaxMm(rs.getDouble("size_max_mm"));
        s.setDisposalGuidance(rs.getString("disposal_guidance"));
        s.setPhotoPath(rs.getString("photo_path"));
        s.setAlaGuid(rs.getString("ala_guid"));

        String tagsCsv = rs.getString("tags");
        if (tagsCsv != null && !tagsCsv.isBlank()) {
            List<String> tags = new ArrayList<>();
            for (String tag : tagsCsv.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) tags.add(trimmed);
            }
            s.setTags(tags);
        }
        return s;
    }
}