package com.biodex.model;

/** A reported invasive species sighting. Mirrors the {@code sightings} table. */
public class Sighting {

    private int sightingId;
    private int userId;
    private int suburbId;
    private String speciesName;
    private String description;
    private String imagePath;
    private String sightedAt;
    private String createdAt;

    public Sighting() {
    }

    public Sighting(int userId, int suburbId, String speciesName, String sightedAt) {
        this.userId = userId;
        this.suburbId = suburbId;
        this.speciesName = speciesName;
        this.sightedAt = sightedAt;
    }

    public int getSightingId() {
        return sightingId;
    }

    public void setSightingId(int sightingId) {
        this.sightingId = sightingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getSuburbId() {
        return suburbId;
    }

    public void setSuburbId(int suburbId) {
        this.suburbId = suburbId;
    }

    public String getSpeciesName() {
        return speciesName;
    }

    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getSightedAt() {
        return sightedAt;
    }

    public void setSightedAt(String sightedAt) {
        this.sightedAt = sightedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
