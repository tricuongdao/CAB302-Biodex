package com.biodex.api.dto;

/**
 * How many records fall inside one named area — a facet bucket from the Atlas of Living Australia.
 *
 * <p>This is what the Heat Map page shades by. Aggregated counts are used instead of raw points
 * because occurrence search returns at most 5000 records per request.
 *
 * <p>Immutable.
 */
public final class AreaCount {

    private final String areaName;
    private final int count;

    public AreaCount(String areaName, int count) {
        this.areaName = areaName;
        this.count = count;
    }

    /** The area this bucket covers, such as a state or local government area. */
    public String getAreaName() {
        return areaName;
    }

    /** The number of records the Atlas holds for the area. */
    public int getCount() {
        return count;
    }
}
