package com.biodex.session;

import com.biodex.api.dto.SpeciesSummary;

import java.nio.file.Path;

/**
 * One-shot hand-off from Species Details to the Identify a pest screen.
 *
 * <p>When the user taps "Log sighting" or "Add photo" on a species, the chosen species (and
 * optionally the photo they picked) is parked here. The Identify screen reads and clears it as it
 * opens, so arriving at that page later from the sidebar never silently reuses a species from an
 * earlier visit. Same idea as {@link SpeciesSelection}, but consumed instead of remembered.
 */
public final class IdentifyDraft {

    private static IdentifyDraft instance;

    private SpeciesSummary species;
    private Path photo;

    private IdentifyDraft() {
    }

    /** Returns the singleton instance. */
    public static synchronized IdentifyDraft getInstance() {
        if (instance == null) {
            instance = new IdentifyDraft();
        }
        return instance;
    }

    /** Parks the species to report on, with an optional photo to preload on the Identify screen. */
    public synchronized void set(SpeciesSummary species, Path photo) {
        this.species = species;
        this.photo = photo;
    }

    /**
     * Returns the parked draft and clears it, or null when nothing is parked. Called by the
     * Identify screen as it opens - the clear is what makes the hand-off one-shot.
     */
    public synchronized Item take() {
        if (species == null && photo == null) {
            return null;
        }
        Item item = new Item(species, photo);
        species = null;
        photo = null;
        return item;
    }

    /** What a caller parked: the species to report on and an optional photo to preload. */
    public record Item(SpeciesSummary species, Path photo) {
    }
}
