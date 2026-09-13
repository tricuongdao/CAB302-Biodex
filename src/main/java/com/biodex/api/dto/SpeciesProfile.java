package com.biodex.api.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The full detail record for one species, as shown on the Species Details page.
 *
 * <p>Immutable. Any field except {@code guid} may be null when the Atlas of Living Australia has no
 * value for it. Rich fields the Atlas does not hold at all — {@code typicalHabitat},
 * {@code disposalGuidance}, threat ratings and the like — are filled by
 * {@link com.biodex.api.CuratedSpeciesService} from Biodex's bundled pest knowledge base, so the
 * detail page always has something useful to show.
 */
public final class SpeciesProfile {

    private final String guid;
    private final String scientificName;
    private final String commonName;
    private final String description;
    private final String imageUrl;
    private final boolean invasive;

    /** Taxonomic ranks as reported by the Atlas, best effort — any may be null. */
    private final String family;
    private final String order;
    private final String taxonClass;
    private final String kingdom;

    /** A short conservation or pest status such as "Not evaluated" or "Least concern". */
    private final String conservationStatus;

    /** Short category labels ("Invasive", "Stinging", "Toxic"...), shown as chips on the page. */
    private final List<String> tags;

    /** Facts the Atlas does not hold, provided by Biodex's curated knowledge base. */
    private final String typicalHabitat;
    private final String sizeRange;
    private final String disposalGuidance;
    private final String reportAuthority;

    /** Named 0-100 risk scores, e.g. Aggression / Sting severity / Spread risk. */
    private final Map<String, Integer> threatRatings;

    /**
     * Builds a profile with every field. Callers that only have the basic Atlas record can use the
     * shorter 6-argument constructor, which leaves the richer fields null or empty.
     */
    public SpeciesProfile(
            String guid,
            String scientificName,
            String commonName,
            String description,
            String imageUrl,
            boolean invasive,
            String family,
            String order,
            String taxonClass,
            String kingdom,
            String conservationStatus,
            List<String> tags,
            String typicalHabitat,
            String sizeRange,
            String disposalGuidance,
            String reportAuthority,
            Map<String, Integer> threatRatings) {
        this.guid = Objects.requireNonNull(guid, "guid");
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.invasive = invasive;
        this.family = family;
        this.order = order;
        this.taxonClass = taxonClass;
        this.kingdom = kingdom;
        this.conservationStatus = conservationStatus;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.typicalHabitat = typicalHabitat;
        this.sizeRange = sizeRange;
        this.disposalGuidance = disposalGuidance;
        this.reportAuthority = reportAuthority;
        this.threatRatings = threatRatings == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(threatRatings));
    }

    /** The original 6-field form, for sources that only supply the basic Atlas record. */
    public SpeciesProfile(
            String guid,
            String scientificName,
            String commonName,
            String description,
            String imageUrl,
            boolean invasive) {
        this(guid, scientificName, commonName, description, imageUrl, invasive,
                null, null, null, null, null, List.of(), null, null, null, null, Map.of());
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

    public String getFamily() {
        return family;
    }

    public String getOrder() {
        return order;
    }

    /** The class rank (Reptilia, Insecta...); named taxonClass because {@code class} is reserved. */
    public String getTaxonClass() {
        return taxonClass;
    }

    public String getKingdom() {
        return kingdom;
    }

    public String getConservationStatus() {
        return conservationStatus;
    }

    /** Short category labels for chips; never null, possibly empty. */
    public List<String> getTags() {
        return tags;
    }

    public String getTypicalHabitat() {
        return typicalHabitat;
    }

    public String getSizeRange() {
        return sizeRange;
    }

    /** Safe, non-lethal guidance on what to do when the species is found. */
    public String getDisposalGuidance() {
        return disposalGuidance;
    }

    /** Who to report the species to, e.g. "Biosecurity Queensland - 13 25 23". */
    public String getReportAuthority() {
        return reportAuthority;
    }

    /** Named 0-100 risk scores; never null, possibly empty. */
    public Map<String, Integer> getThreatRatings() {
        return threatRatings;
    }
}
