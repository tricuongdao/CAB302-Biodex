package com.biodex.session;

import com.biodex.api.dto.SpeciesSummary;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The one-shot Species Details to Identify hand-off: set once, taken once. */
class IdentifyDraftTest {

    private static final SpeciesSummary CANE_TOAD = new SpeciesSummary(
            "fake:rhinella-marina", "Rhinella marina", "Cane Toad", null);

    @Test
    void takeReturnsTheParkedDraftAndClearsIt() {
        IdentifyDraft draft = IdentifyDraft.getInstance();
        draft.take(); // drain any leftover from an earlier test

        draft.set(CANE_TOAD, Path.of("toad.jpg"));

        IdentifyDraft.Item item = draft.take();
        assertNotNull(item);
        assertSame(CANE_TOAD, item.species());
        assertEquals(Path.of("toad.jpg"), item.photo());
        assertNull(draft.take(), "the hand-off is one-shot: a second take returns nothing");
    }

    @Test
    void speciesWithoutAPhotoStillParks() {
        IdentifyDraft draft = IdentifyDraft.getInstance();
        draft.take();

        draft.set(CANE_TOAD, null);

        IdentifyDraft.Item item = draft.take();
        assertNotNull(item);
        assertSame(CANE_TOAD, item.species());
        assertNull(item.photo());
    }

    @Test
    void emptyDraftTakesNull() {
        IdentifyDraft draft = IdentifyDraft.getInstance();
        draft.take();
        assertNull(draft.take());
    }
}
