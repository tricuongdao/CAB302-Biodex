package com.biodex.controller.identify;

import com.biodex.model.Suburb;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure logic behind Submit report: suburb matching, the location line and the WHEN pickers.
 * No JavaFX toolkit is needed for these helpers.
 */
class IdentifyPestControllerTest {

    @Test
    void inferSuburbKeepsTheLastCommaSegment() {
        assertEquals("Gordon Park", IdentifyPestController.inferSuburb("Kedron Brook, Gordon Park"));
        assertEquals("Gordon Park", IdentifyPestController.inferSuburb("   Gordon Park   "));
        assertEquals("Pullenvale", IdentifyPestController.inferSuburb("About 3 km past the creek, Pullenvale"));
    }

    @Test
    void findSuburbByTextMatchesTheLastSegmentThenAnyContainedName() {
        List<Suburb> suburbs = List.of(
                suburb("Gordon Park", "4031"),
                suburb("Kedron", "4031"),
                suburb("West End", "4101"));

        assertEquals("Gordon Park",
                IdentifyPestController.findSuburbByText("Kedron Brook, Gordon Park", suburbs).getName());
        assertEquals("Kedron",
                IdentifyPestController.findSuburbByText("kedron", suburbs).getName());
        assertEquals("West End",
                IdentifyPestController.findSuburbByText("next to the West End ferry", suburbs).getName());
        assertEquals("Gordon Park",
                IdentifyPestController.findSuburbByText("gord", suburbs).getName(),
                "a typed prefix selects the suburb");
        assertNull(IdentifyPestController.findSuburbByText("Nowhere", suburbs));
        assertNull(IdentifyPestController.findSuburbByText(null, suburbs));
        assertNull(IdentifyPestController.findSuburbByText("Kedron", List.of()));
    }

    private static Suburb suburb(String name, String postcode) {
        return new Suburb(name, postcode, -27.4, 153.0);
    }

    @Test
    void locationLabelAppendsCountAndNotesWhenGiven() {
        assertEquals("Gordon Park (3 adults) - Heard after rain",
                IdentifyPestController.locationLabel("Gordon Park", "3 adults", "Heard after rain"));
        assertEquals("Gordon Park (3 adults)",
                IdentifyPestController.locationLabel("Gordon Park", "3 adults", ""));
        assertEquals("Gordon Park",
                IdentifyPestController.locationLabel("Gordon Park", "", ""));
    }

    @Test
    void combineWhenUsesThePickedDateAndTime() {
        LocalDate day = LocalDate.of(2026, 8, 27);
        LocalTime time = LocalTime.of(16, 40);

        assertEquals(day.atTime(time).atZone(ZoneId.systemDefault()).toInstant(),
                IdentifyPestController.combineWhen(day, time));
    }

    @Test
    void combineWhenFillsInSensibleDefaults() {
        assertNull(IdentifyPestController.combineWhen(null, null),
                "no date and no time leaves the timestamp to the database");

        LocalDate pastDay = LocalDate.now().minusDays(3);
        assertEquals(pastDay.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(),
                IdentifyPestController.combineWhen(pastDay, null),
                "a past day without a time defaults to midday");

        LocalTime time = LocalTime.of(9, 30);
        assertEquals(LocalDate.now().atTime(time).atZone(ZoneId.systemDefault()).toInstant(),
                IdentifyPestController.combineWhen(null, time),
                "a time without a day means today at that time");

        Instant today = IdentifyPestController.combineWhen(LocalDate.now(), null);
        assertTrue(Duration.between(today, Instant.now()).abs().toMinutes() <= 2,
                "today without a time uses the current time");
    }
}
