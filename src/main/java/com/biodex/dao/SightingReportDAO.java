package com.biodex.dao;

import com.biodex.model.SightingReport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for local user-submitted sighting reports (per pest-detail-technical-spec.md).
 * Provides density by suburb (last 30 days) and recent reports for a species.
 */
public class SightingReportDAO extends BaseDao {

    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int DEFAULT_RECENT_LIMIT = 3;

    private static final String SELECT_DENSITY_BY_SUBURB = """
            SELECT suburb, COUNT(*) AS report_count
              FROM sighting_reports
             WHERE species_id = ?
               AND reported_at >= datetime('now', ? || ' days')
             GROUP BY suburb
             ORDER BY report_count DESC
            """;

    private static final String SELECT_RECENT_REPORTS = """
            SELECT *
              FROM sighting_reports
             WHERE species_id = ?
             ORDER BY reported_at DESC
             LIMIT ?
            """;

    private static final String INSERT_REPORT = """
            INSERT INTO sighting_reports
                  (species_id, suburb, location_label, latitude, longitude, reporter_user_id, photo_path)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    /** Uses the shared application connection. */
    public SightingReportDAO() {
        super();
    }

    /** Uses the given connection (for tests). */
    public SightingReportDAO(Connection connection) {
        super(connection);
    }

    /**
     * Returns report counts per suburb for the last {@code windowDays} days,
     * sorted highest first. Empty map if none.
     */
    public Map<String, Integer> getDensityBySuburb(int speciesId, int windowDays) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String daysSql = "-" + windowDays;
        queryMany(SELECT_DENSITY_BY_SUBURB,
                stmt -> {
                    stmt.setInt(1, speciesId);
                    stmt.setString(2, daysSql);
                },
                rs -> {
                    counts.put(rs.getString("suburb"), rs.getInt("report_count"));
                    return null;
                });
        return counts;
    }

    /** Overload with default 30-day window. */
    public Map<String, Integer> getDensityBySuburb(int speciesId) {
        return getDensityBySuburb(speciesId, DEFAULT_WINDOW_DAYS);
    }

    /**
     * Returns the most recent reports for a species, up to {@code limit}.
     * Ordered newest first.
     */
    public List<SightingReport> getRecentReports(int speciesId, int limit) {
        return queryMany(SELECT_RECENT_REPORTS,
                stmt -> {
                    stmt.setInt(1, speciesId);
                    stmt.setInt(2, limit);
                },
                this::mapRow);
    }

    /** Overload with default limit of 3. */
    public List<SightingReport> getRecentReports(int speciesId) {
        return getRecentReports(speciesId, DEFAULT_RECENT_LIMIT);
    }

    /** Inserts a new sighting report. Returns the generated report_id. */
    public int insertReport(SightingReport report) {
        return insertReturningKey(INSERT_REPORT,
                stmt -> {
                    stmt.setInt(1, report.getSpeciesId());
                    stmt.setString(2, report.getSuburb());
                    stmt.setString(3, report.getLocationLabel());
                    if (report.getLatitude() != 0.0) {
                        stmt.setDouble(4, report.getLatitude());
                    } else {
                        stmt.setNull(4, Types.DOUBLE);
                    }
                    if (report.getLongitude() != 0.0) {
                        stmt.setDouble(5, report.getLongitude());
                    } else {
                        stmt.setNull(5, Types.DOUBLE);
                    }
                    if (report.getReporterUserId() > 0) {
                        stmt.setInt(6, report.getReporterUserId());
                    } else {
                        stmt.setNull(6, Types.INTEGER);
                    }
                    stmt.setString(7, report.getPhotoPath());
                });
    }

    private SightingReport mapRow(ResultSet rs) throws SQLException {
        SightingReport r = new SightingReport();
        r.setReportId(rs.getInt("report_id"));
        r.setSpeciesId(rs.getInt("species_id"));
        r.setSuburb(rs.getString("suburb"));
        r.setLocationLabel(rs.getString("location_label"));
        r.setLatitude(rs.getDouble("latitude"));
        r.setLongitude(rs.getDouble("longitude"));
        String reportedAt = rs.getString("reported_at");
        if (reportedAt != null) {
            r.setReportedAt(reportedAt);
        }
        r.setVerified(rs.getInt("verified") == 1);
        r.setReporterUserId(rs.getInt("reporter_user_id"));
        r.setPhotoPath(rs.getString("photo_path"));
        return r;
    }
}