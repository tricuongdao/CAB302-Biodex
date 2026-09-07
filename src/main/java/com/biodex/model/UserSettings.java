package com.biodex.model;

/** Per-user preferences. Mirrors the {@code user_settings} table. */
public class UserSettings {

    private int userId;
    private String theme = "light";
    private String language = "en-AU";
    private String measurementUnit = "METRIC";
    private Integer defaultSuburbId;
    private boolean autoIdentifyOnUpload = true;
    private double identificationConfidenceThreshold = 0.70;
    private boolean wifiOnlyUpload;
    private boolean autoCompressPhotos = true;
    private String maxUploadResolution = "1920x1080";
    private boolean notifyNewSightingsNearby = true;
    private boolean notifyCommunityAlerts = true;
    private boolean notifyAppUpdates;
    private boolean shareLocationPublicly;
    private boolean anonymizeUploads;
    private String textSize = "MEDIUM";
    private boolean twoFactorEnabled;

    public UserSettings() {
    }

    public UserSettings(int userId, String theme, boolean twoFactorEnabled) {
        this.userId = userId;
        this.theme = theme;
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getMeasurementUnit() { return measurementUnit; }
    public void setMeasurementUnit(String measurementUnit) { this.measurementUnit = measurementUnit; }
    public Integer getDefaultSuburbId() { return defaultSuburbId; }
    public void setDefaultSuburbId(Integer defaultSuburbId) { this.defaultSuburbId = defaultSuburbId; }
    public boolean isAutoIdentifyOnUpload() { return autoIdentifyOnUpload; }
    public void setAutoIdentifyOnUpload(boolean value) { this.autoIdentifyOnUpload = value; }
    public double getIdentificationConfidenceThreshold() { return identificationConfidenceThreshold; }
    public void setIdentificationConfidenceThreshold(double value) {
        this.identificationConfidenceThreshold = value;
    }
    public boolean isWifiOnlyUpload() { return wifiOnlyUpload; }
    public void setWifiOnlyUpload(boolean value) { this.wifiOnlyUpload = value; }
    public boolean isAutoCompressPhotos() { return autoCompressPhotos; }
    public void setAutoCompressPhotos(boolean value) { this.autoCompressPhotos = value; }
    public String getMaxUploadResolution() { return maxUploadResolution; }
    public void setMaxUploadResolution(String value) { this.maxUploadResolution = value; }
    public boolean isNotifyNewSightingsNearby() { return notifyNewSightingsNearby; }
    public void setNotifyNewSightingsNearby(boolean value) { this.notifyNewSightingsNearby = value; }
    public boolean isNotifyCommunityAlerts() { return notifyCommunityAlerts; }
    public void setNotifyCommunityAlerts(boolean value) { this.notifyCommunityAlerts = value; }
    public boolean isNotifyAppUpdates() { return notifyAppUpdates; }
    public void setNotifyAppUpdates(boolean value) { this.notifyAppUpdates = value; }
    public boolean isShareLocationPublicly() { return shareLocationPublicly; }
    public void setShareLocationPublicly(boolean value) { this.shareLocationPublicly = value; }
    public boolean isAnonymizeUploads() { return anonymizeUploads; }
    public void setAnonymizeUploads(boolean value) { this.anonymizeUploads = value; }
    public String getTextSize() { return textSize; }
    public void setTextSize(String value) { this.textSize = value; }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
}
