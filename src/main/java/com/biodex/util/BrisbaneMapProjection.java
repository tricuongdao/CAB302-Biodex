package com.biodex.util;

import java.util.Optional;

/** Converts Greater Brisbane coordinates into proportional positions on the heat-map canvas. */
public final class BrisbaneMapProjection {

    private static final double NORTH_LATITUDE = -26.80;
    private static final double SOUTH_LATITUDE = -28.35;
    private static final double WEST_LONGITUDE = 152.30;
    private static final double EAST_LONGITUDE = 153.65;

    private BrisbaneMapProjection() {
    }

    /**
     * Projects a coordinate to an x/y ratio between zero and one.
     *
     * @return empty when the coordinate is not finite or lies outside Greater Brisbane
     */
    public static Optional<ProjectedPoint> project(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < SOUTH_LATITUDE || latitude > NORTH_LATITUDE
                || longitude < WEST_LONGITUDE || longitude > EAST_LONGITUDE) {
            return Optional.empty();
        }

        double xRatio = (longitude - WEST_LONGITUDE) / (EAST_LONGITUDE - WEST_LONGITUDE);
        double yRatio = (NORTH_LATITUDE - latitude) / (NORTH_LATITUDE - SOUTH_LATITUDE);
        return Optional.of(new ProjectedPoint(xRatio, yRatio));
    }

    /** A proportional canvas location where zero is the top or left and one is the bottom/right. */
    public record ProjectedPoint(double xRatio, double yRatio) {
    }
}
