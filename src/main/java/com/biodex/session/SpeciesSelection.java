package com.biodex.session;

import com.biodex.api.dto.SpeciesSummary;

/**
 * The species currently picked on the Pest Details screen.
 *
 * <p>Pest Details records the chosen {@link SpeciesSummary} here before routing to the detail
 * screen, which reads it back — the router itself is stateless, so this is how a selection crosses
 * screens. Nothing here knows or favours any particular species: with no pick made the selection is
 * simply empty, and the detail screen shows its own empty state.
 */
public final class SpeciesSelection {

    private static SpeciesSelection instance;

    private SpeciesSummary current;

    private SpeciesSelection() {
    }

    /** Returns the singleton instance. */
    public static synchronized SpeciesSelection getInstance() {
        if (instance == null) {
            instance = new SpeciesSelection();
        }
        return instance;
    }

    /** The species to show on the detail screen, or null when nothing has been picked. */
    public synchronized SpeciesSummary getCurrent() {
        return current;
    }

    /** Records the species a card click chose, or clears the selection with null. */
    public synchronized void setCurrent(SpeciesSummary selection) {
        this.current = selection;
    }
}