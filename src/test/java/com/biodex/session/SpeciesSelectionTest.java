package com.biodex.session;

import com.biodex.api.dto.SpeciesSummary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The species carried between the Pest Details list and the detail screen. */
class SpeciesSelectionTest {

    @BeforeEach
    void clearSelection() {
        SpeciesSelection.getInstance().setCurrent(null);
    }

    @Test
    void startsEmptySoNoSpeciesIsFavoured() {
        assertNull(SpeciesSelection.getInstance().getCurrent());
    }

    @Test
    void remembersThePickedSpecies() {
        SpeciesSummary picked = new SpeciesSummary("guid-1", "Solenopsis invicta", "Fire ant", null);
        SpeciesSelection.getInstance().setCurrent(picked);
        assertSame(picked, SpeciesSelection.getInstance().getCurrent());
    }

    @Test
    void clearingEmptiesTheSelection() {
        SpeciesSelection.getInstance().setCurrent(
                new SpeciesSummary("guid-2", "Eichhornia crassipes", "Water hyacinth", null));
        SpeciesSelection.getInstance().setCurrent(null);
        assertNull(SpeciesSelection.getInstance().getCurrent());
    }
}