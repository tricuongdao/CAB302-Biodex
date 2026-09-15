package com.biodex.recognition;

/**
 * One row of a recognition result: a species the classifier thinks the photo shows, with how
 * confident it is.
 *
 * <p>Immutable. Any field except {@code confidence} may be null when the classifier's labels carry
 * no value for it.
 */
public final class MatchCandidate {

    private final String commonName;
    private final String scientificName;
    private final float confidence;

    public MatchCandidate(String commonName, String scientificName, float confidence) {
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.confidence = confidence;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    /** Probability from 0.0 to 1.0 that the photo shows this species. */
    public float getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "MatchCandidate[" + commonName + " (" + scientificName + ") " + confidence + "]";
    }
}