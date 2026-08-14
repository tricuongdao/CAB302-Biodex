package com.biodex.model;

/** Per-user preferences. Mirrors the {@code user_settings} table. */
public class UserSettings {

    private int userId;
    private String theme;
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

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
}
