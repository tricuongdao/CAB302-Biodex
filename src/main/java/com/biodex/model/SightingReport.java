package com.biodex.model;

import java.time.Instant;

/**
 * Local user-submitted sighting report (per pest-detail-technical-spec.md).
 * Mirrors the {@code sighting_reports} table with verification status.
 */
public class SightingReport {

    private int reportId;
    private int speciesId;
    private String suburb;
    private String locationLabel;
    private double latitude;
    private double longitude;
    private Instant reportedAt;
    private boolean verified;
    private int reporterUserId;
    private String photoPath;

    public SightingReport() {
    }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public int getSpeciesId() { return speciesId; }
    public void setSpeciesId(int speciesId) { this.speciesId = speciesId; }

    public String getSuburb() { return suburb; }
    public void setSuburb(String suburb) { this.suburb = suburb; }

    public String getLocationLabel() { return locationLabel; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }

    /**
     * Parses the SQLite {@code reported_at} value. The column defaults to
     * {@code datetime('now')}, which writes {@code yyyy-MM-dd HH:mm:ss}, so that
     * shape is parsed as UTC; ISO-8601 instants keep working as well.
     */
    public void setReportedAt(String value) {
        if (value == null || value.isBlank()) {
            this.reportedAt = null;
            return;
        }
        String text = value.trim();
        if (text.contains("T")) {
            this.reportedAt = Instant.parse(text);
            return;
        }
        String normalised = text.replace(' ', 'T') + "Z";
        this.reportedAt = Instant.parse(normalised);
    }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public int getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(int reporterUserId) { this.reporterUserId = reporterUserId; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}