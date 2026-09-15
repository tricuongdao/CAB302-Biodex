package com.biodex.model;

import java.time.LocalDate;

/**
 * A sighting projected into the form needed by the heat map.
 *
 * <p>The coordinates belong to the sighting's suburb in the current database schema. Keeping this
 * read model separate from {@link Sighting} means the general sighting model does not need to know
 * about joined suburb data.
 */
public final class MapSighting {

    private final int sightingId;
    private final String speciesName;
    private final String description;
    private final LocalDate sightingDate;
    private final String suburbName;
    private final String postcode;
    private final double latitude;
    private final double longitude;

    public MapSighting(
            int sightingId,
            String speciesName,
            String description,
            LocalDate sightingDate,
            String suburbName,
            String postcode,
            double latitude,
            double longitude) {
        this.sightingId = sightingId;
        this.speciesName = speciesName;
        this.description = description;
        this.sightingDate = sightingDate;
        this.suburbName = suburbName;
        this.postcode = postcode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getSightingId() {
        return sightingId;
    }

    public String getSpeciesName() {
        return speciesName;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getSightingDate() {
        return sightingDate;
    }

    public String getSuburbName() {
        return suburbName;
    }

    public String getPostcode() {
        return postcode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
