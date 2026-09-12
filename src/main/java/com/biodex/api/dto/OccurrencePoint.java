package com.biodex.api.dto;

import java.time.LocalDate;

/**
 * A single recorded sighting of a species at a point on the map.
 *
 * <p>Immutable. {@code eventDate} is null for records the Atlas of Living Australia holds without a
 * date, which is common in older data, so callers must null-check before formatting it.
 */
public final class OccurrencePoint {

    private final double latitude;
    private final double longitude;
    private final LocalDate eventDate;
    private final String dataResourceName;

    public OccurrencePoint(
            double latitude, double longitude, LocalDate eventDate, String dataResourceName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.eventDate = eventDate;
        this.dataResourceName = dataResourceName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** The day the sighting was recorded, or null when the record carries no date. */
    public LocalDate getEventDate() {
        return eventDate;
    }

    /** The dataset the record came from, for attribution. */
    public String getDataResourceName() {
        return dataResourceName;
    }
}
