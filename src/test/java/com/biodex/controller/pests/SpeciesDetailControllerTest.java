package com.biodex.controller.pests;

import com.biodex.api.dto.SpeciesProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Pins the small formatting helpers the Atlas species path relies on. */
class SpeciesDetailControllerTest {

    @Test
    void taxonomyLineJoinsTheRanksTheAtlasSupplied() {
        assertEquals("Family: Formicidae \u00b7 Order: Hymenoptera \u00b7 Class: Insecta",
                SpeciesDetailController.taxonomyLine("Formicidae", "Hymenoptera", "Insecta"));
    }

    @Test
    void taxonomyLineTitlesAllCapsRanks() {
        assertEquals("Family: Varanidae \u00b7 Order: Squamata \u00b7 Class: Reptilia",
                SpeciesDetailController.taxonomyLine("VARANIDAE", "SQUAMATA", "REPTILIA"));
    }

    @Test
    void taxonomyLineSkipsMissingAndBlankRanks() {
        assertEquals("Order: Anura", SpeciesDetailController.taxonomyLine(null, "Anura", " "));
        assertNull(SpeciesDetailController.taxonomyLine(null, null, null));
    }

    @Test
    void tagsForAddsAnInvasiveChipWhenTheAtlasFlagsIt() {
        SpeciesProfile flagged = new SpeciesProfile("g1", "Solenopsis invicta",
                "Red Imported Fire Ant", null, null, true);
        assertEquals(List.of("Invasive"), SpeciesDetailController.tagsFor(flagged));
    }

    @Test
    void tagsForKeepsExistingTagsAndAvoidsDuplicateInvasiveChips() {
        SpeciesProfile flagged = new SpeciesProfile("g2", "Vulpes vulpes", "Red Fox",
                null, null, true, null, null, null, null, null, List.of("Invasive", "Predator"),
                null, null, null, null, Map.of());
        assertEquals(List.of("Invasive", "Predator"), SpeciesDetailController.tagsFor(flagged));

        SpeciesProfile quiet = new SpeciesProfile("g3", "Litoria caerulea", "Green Tree Frog",
                null, null, false, null, null, null, null, null, List.of("Native"),
                null, null, null, null, Map.of());
        assertEquals(List.of("Native"), SpeciesDetailController.tagsFor(quiet));
    }
}
