package com.biodex.model;

import java.util.List;

/**
 * Local curated species record (per pest-detail-technical-spec.md).
 * Mirrors the {@code species} table with threat ratings the Atlas does not provide.
 */
public class Species {

    private int speciesId;
    private String commonName;
    private String scientificName;
    private ThreatLevel threatLevel;
    private int aggression;           // 0-100
    private int stingSeverity;        // 0-100
    private int spreadRisk;           // 0-100
    private String typicalHabitat;
    private double sizeMinMm;
    private double sizeMaxMm;
    private String disposalGuidance;
    private String photoPath;
    private String alaGuid;
    private List<String> tags;

    public Species() {
    }

    public enum ThreatLevel {
        LOW, MEDIUM, HIGH
    }

    public int getSpeciesId() { return speciesId; }
    public void setSpeciesId(int speciesId) { this.speciesId = speciesId; }

    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }

    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }

    public ThreatLevel getThreatLevel() { return threatLevel; }
    public void setThreatLevel(ThreatLevel threatLevel) { this.threatLevel = threatLevel; }
    public void setThreatLevel(String threatLevel) { this.threatLevel = ThreatLevel.valueOf(threatLevel); }

    public int getAggression() { return aggression; }
    public void setAggression(int aggression) { this.aggression = aggression; }

    public int getStingSeverity() { return stingSeverity; }
    public void setStingSeverity(int stingSeverity) { this.stingSeverity = stingSeverity; }

    public int getSpreadRisk() { return spreadRisk; }
    public void setSpreadRisk(int spreadRisk) { this.spreadRisk = spreadRisk; }

    public String getTypicalHabitat() { return typicalHabitat; }
    public void setTypicalHabitat(String typicalHabitat) { this.typicalHabitat = typicalHabitat; }

    public double getSizeMinMm() { return sizeMinMm; }
    public void setSizeMinMm(double sizeMinMm) { this.sizeMinMm = sizeMinMm; }

    public double getSizeMaxMm() { return sizeMaxMm; }
    public void setSizeMaxMm(double sizeMaxMm) { this.sizeMaxMm = sizeMaxMm; }

    public String getDisposalGuidance() { return disposalGuidance; }
    public void setDisposalGuidance(String disposalGuidance) { this.disposalGuidance = disposalGuidance; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getAlaGuid() { return alaGuid; }
    public void setAlaGuid(String alaGuid) { this.alaGuid = alaGuid; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    /** Convenience: "10 to 15 mm" style string for UI. */
    public String getSizeRange() {
        if (sizeMinMm <= 0 && sizeMaxMm <= 0) return null;
        if (sizeMinMm <= 0) return String.format("%.0f mm", sizeMaxMm);
        if (sizeMaxMm <= 0) return String.format("%.0f mm", sizeMinMm);
        if (sizeMinMm == sizeMaxMm) return String.format("%.0f mm", sizeMinMm);
        return String.format("%.0f to %.0f mm", sizeMinMm, sizeMaxMm);
    }
}