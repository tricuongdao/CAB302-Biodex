package com.biodex.api.dto;

/**
 * The full detail record for one species, as shown on the Species Details page.
 *
 * <p>Immutable. Any field except {@code guid} may be null when the Atlas of Living Australia has no
 * value for it.
 */
public final class SpeciesProfile {

    private final String guid;
    private final String scientificName;
    private final String commonName;
    private final String description;
    private final String imageUrl;
    private final boolean invasive;

    public SpeciesProfile(
            String guid,
            String scientificName,
            String commonName,
            String description,
            String imageUrl,
            boolean invasive) {
        this.guid = guid;
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.invasive = invasive;
    }

    /** The Atlas of Living Australia identifier for this species. */
    public String getGuid() {
        return guid;
    }

    public String getScientificName() {
        return scientificName;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    /** True when the Atlas lists this species with an invasive or pest status. */
    public boolean isInvasive() {
        return invasive;
    }
}
