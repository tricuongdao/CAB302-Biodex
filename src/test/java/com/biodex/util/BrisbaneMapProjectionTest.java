package com.biodex.util;

import com.biodex.util.BrisbaneMapProjection.ProjectedPoint;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrisbaneMapProjectionTest {

    @Test
    void projectsAValidBrisbaneCoordinateInsideTheCanvas() {
        ProjectedPoint point = BrisbaneMapProjection.project(-27.4698, 153.0251).orElseThrow();

        assertTrue(point.xRatio() > 0 && point.xRatio() < 1);
        assertTrue(point.yRatio() > 0 && point.yRatio() < 1);
    }

    @Test
    void projectsTheNorthWestAndSouthEastBoundsToOppositeCorners() {
        ProjectedPoint northWest =
                BrisbaneMapProjection.project(-26.80, 152.30).orElseThrow();
        ProjectedPoint southEast =
                BrisbaneMapProjection.project(-28.35, 153.65).orElseThrow();

        assertEquals(0, northWest.xRatio(), 0.00001);
        assertEquals(0, northWest.yRatio(), 0.00001);
        assertEquals(1, southEast.xRatio(), 0.00001);
        assertEquals(1, southEast.yRatio(), 0.00001);
    }

    @Test
    void rejectsCoordinatesOutsideGreaterBrisbane() {
        Optional<ProjectedPoint> sydney = BrisbaneMapProjection.project(-33.8688, 151.2093);

        assertTrue(sydney.isEmpty());
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertTrue(BrisbaneMapProjection.project(Double.NaN, 153.0251).isEmpty());
        assertTrue(BrisbaneMapProjection.project(-27.4698, Double.POSITIVE_INFINITY).isEmpty());
    }
}
