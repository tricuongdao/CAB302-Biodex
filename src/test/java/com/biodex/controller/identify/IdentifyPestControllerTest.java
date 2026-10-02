package com.biodex.controller.identify;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
