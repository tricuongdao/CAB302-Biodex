package com.biodex.api.dto;

/**
 * One row of an autocomplete result: just enough to render a suggestion and then fetch the full
 * profile by {@code guid}.
 *
 * <p>Immutable. Any field except {@code guid} may be null when the Atlas of Living Australia has no
 * value for it.
 */
public final class SpeciesSummary {

    private final String guid;
    private final String scientificName;
    private final String commonName;
    private final String imageUrl;

    public SpeciesSummary(String guid, String scientificName, String commonName, String imageUrl) {
        this.guid = guid;
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.imageUrl = imageUrl;
    }

    /** The Atlas of Living Australia identifier, used to look up the full profile. */
    public String getGuid() {
        return guid;
    }

    public String getScientificName() {
        return scientificName;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
