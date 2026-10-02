package com.biodex.controller.identify;

import com.biodex.model.Suburb;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The pure logic behind Submit report: suburb inference, the location line and WHEN parsing.
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
    void parseWhenReadsLocalDatesAndRejectsJunk() {
        Instant expected = LocalDateTime.of(2026, 8, 27, 16, 40)
                .atZone(ZoneId.systemDefault())
                .toInstant();

        assertEquals(expected, IdentifyPestController.parseWhen("27 Aug 2026, 16:40"));
        assertEquals(expected, IdentifyPestController.parseWhen("  27 Aug 2026, 16:40  "),
                "surrounding whitespace is ignored");
        assertNull(IdentifyPestController.parseWhen(""));
        assertNull(IdentifyPestController.parseWhen("sometime last week"));
        assertNull(IdentifyPestController.parseWhen("2026-08-27"));
    }
}
