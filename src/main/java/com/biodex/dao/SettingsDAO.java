package com.biodex.dao;

import com.biodex.model.UserSettings;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Reads and writes the per-user settings row. */
public class SettingsDAO extends BaseDao {

    public enum SettingColumn {
        THEME("theme"),
        LANGUAGE("language"),
        MEASUREMENT_UNIT("measurement_unit"),
        DEFAULT_SUBURB_ID("default_suburb_id"),
        AUTO_IDENTIFY_ON_UPLOAD("auto_identify_on_upload"),
        IDENTIFICATION_CONFIDENCE_THRESHOLD("identification_confidence_threshold"),
        WIFI_ONLY_UPLOAD("wifi_only_upload"),
        AUTO_COMPRESS_PHOTOS("auto_compress_photos"),
        MAX_UPLOAD_RESOLUTION("max_upload_resolution"),
        NOTIFY_NEW_SIGHTINGS_NEARBY("notify_new_sightings_nearby"),
        NOTIFY_COMMUNITY_ALERTS("notify_community_alerts"),
        NOTIFY_APP_UPDATES("notify_app_updates"),
        SHARE_LOCATION_PUBLICLY("share_location_publicly"),
        ANONYMIZE_UPLOADS("anonymize_uploads"),
        TEXT_SIZE("text_size"),
        TWO_FACTOR_ENABLED("two_factor_enabled");

        private final String column;

        SettingColumn(String column) {
            this.column = column;
        }

        public String column() {
            return column;
        }

        public static SettingColumn from(String column) {
            for (SettingColumn setting : values()) {
                if (setting.column.equals(column)) {
                    return setting;
                }
            }
            throw new IllegalArgumentException("Unsupported setting column");
        }
    }

    public SettingsDAO() {
        super();
    }

    public SettingsDAO(Connection connection) {
        super(connection);
    }

    /**
     * Returns the stored settings or a safe default object when a legacy database has no row.
     */
    public UserSettings getSettingsForUser(int userId) {
        return queryOne(
                "SELECT user_id, theme, language, measurement_unit, default_suburb_id, "
                        + "auto_identify_on_upload, identification_confidence_threshold, "
                        + "wifi_only_upload, auto_compress_photos, max_upload_resolution, "
                        + "notify_new_sightings_nearby, notify_community_alerts, "
                        + "notify_app_updates, share_location_publicly, anonymize_uploads, "
                        + "text_size, two_factor_enabled FROM user_settings WHERE user_id = ?",
                statement -> statement.setInt(1, userId),
                SettingsDAO::mapRow).orElseGet(() -> {
                    UserSettings settings = new UserSettings();
                    settings.setUserId(userId);
                    return settings;
                });
    }

    public void updateSetting(int userId, SettingColumn setting, Object value) {
        if (setting == null) {
            throw new IllegalArgumentException("Setting column must not be null");
        }
        validateValue(setting, value);
        update("UPDATE user_settings SET " + setting.column()
                        + " = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                statement -> {
                    statement.setObject(1, value);
                    statement.setInt(2, userId);
                });
    }

    public void updateSetting(int userId, String column, Object value) {
        updateSetting(userId, SettingColumn.from(column), value);
    }

    /** Updates the account-level two-factor preference. */
    public void updateAccountSettings(int userId, boolean twoFactorEnabled) {
        updateSetting(userId, SettingColumn.TWO_FACTOR_ENABLED, twoFactorEnabled);
    }

    /** Replaces all persisted settings for a user as one atomic operation. */
    public void updateAccountSettings(int userId, UserSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings must not be null");
        }
        String sql = "UPDATE user_settings SET theme = ?, language = ?, measurement_unit = ?, "
                + "default_suburb_id = ?, auto_identify_on_upload = ?, "
                + "identification_confidence_threshold = ?, wifi_only_upload = ?, "
                + "auto_compress_photos = ?, max_upload_resolution = ?, "
                + "notify_new_sightings_nearby = ?, notify_community_alerts = ?, "
                + "notify_app_updates = ?, share_location_publicly = ?, anonymize_uploads = ?, "
                + "text_size = ?, two_factor_enabled = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ?";
        boolean originalAutoCommit = true;
        boolean autoCommitChanged = false;
        try {
            originalAutoCommit = getConnection().getAutoCommit();
            getConnection().setAutoCommit(false);
            autoCommitChanged = true;
            try (var statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, settings.getTheme());
                statement.setString(2, settings.getLanguage());
                statement.setString(3, settings.getMeasurementUnit());
                if (settings.getDefaultSuburbId() == null) {
                    statement.setNull(4, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(4, settings.getDefaultSuburbId());
                }
                statement.setBoolean(5, settings.isAutoIdentifyOnUpload());
                statement.setDouble(6, settings.getIdentificationConfidenceThreshold());
                statement.setBoolean(7, settings.isWifiOnlyUpload());
                statement.setBoolean(8, settings.isAutoCompressPhotos());
                statement.setString(9, settings.getMaxUploadResolution());
                statement.setBoolean(10, settings.isNotifyNewSightingsNearby());
                statement.setBoolean(11, settings.isNotifyCommunityAlerts());
                statement.setBoolean(12, settings.isNotifyAppUpdates());
                statement.setBoolean(13, settings.isShareLocationPublicly());
                statement.setBoolean(14, settings.isAnonymizeUploads());
                statement.setString(15, settings.getTextSize());
                statement.setBoolean(16, settings.isTwoFactorEnabled());
                statement.setInt(17, userId);
                if (statement.executeUpdate() != 1) {
                    getConnection().rollback();
                    throw new DataAccessException("No settings row exists for user " + userId);
                }
            }
            getConnection().commit();
        } catch (SQLException e) {
            try {
                getConnection().rollback();
            } catch (SQLException rollbackException) {
                e.addSuppressed(rollbackException);
            }
            throw new DataAccessException("Unable to update settings for user " + userId, e);
        } finally {
            if (autoCommitChanged) {
                try {
                    getConnection().setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    throw new DataAccessException("Unable to restore database transaction state", e);
                }
            }
        }
    }

    private static void validateValue(SettingColumn setting, Object value) {
        if (setting == SettingColumn.DEFAULT_SUBURB_ID && value != null
                && !(value instanceof Integer)) {
            throw new IllegalArgumentException("default_suburb_id must be an Integer or null");
        }
        if (setting == SettingColumn.IDENTIFICATION_CONFIDENCE_THRESHOLD) {
            if (!(value instanceof Number)
                    || ((Number) value).doubleValue() < 0.0
                    || ((Number) value).doubleValue() > 1.0) {
                throw new IllegalArgumentException(
                        "identification_confidence_threshold must be between 0 and 1");
            }
        }
    }

    private static UserSettings mapRow(ResultSet resultSet) throws SQLException {
        UserSettings settings = new UserSettings();
        settings.setUserId(resultSet.getInt("user_id"));
        settings.setTheme(resultSet.getString("theme"));
        settings.setLanguage(resultSet.getString("language"));
        settings.setMeasurementUnit(resultSet.getString("measurement_unit"));
        int suburbId = resultSet.getInt("default_suburb_id");
        settings.setDefaultSuburbId(resultSet.wasNull() ? null : suburbId);
        settings.setAutoIdentifyOnUpload(resultSet.getBoolean("auto_identify_on_upload"));
        settings.setIdentificationConfidenceThreshold(
                resultSet.getDouble("identification_confidence_threshold"));
        settings.setWifiOnlyUpload(resultSet.getBoolean("wifi_only_upload"));
        settings.setAutoCompressPhotos(resultSet.getBoolean("auto_compress_photos"));
        settings.setMaxUploadResolution(resultSet.getString("max_upload_resolution"));
        settings.setNotifyNewSightingsNearby(resultSet.getBoolean("notify_new_sightings_nearby"));
        settings.setNotifyCommunityAlerts(resultSet.getBoolean("notify_community_alerts"));
        settings.setNotifyAppUpdates(resultSet.getBoolean("notify_app_updates"));
        settings.setShareLocationPublicly(resultSet.getBoolean("share_location_publicly"));
        settings.setAnonymizeUploads(resultSet.getBoolean("anonymize_uploads"));
        settings.setTextSize(resultSet.getString("text_size"));
        settings.setTwoFactorEnabled(resultSet.getBoolean("two_factor_enabled"));
        return settings;
    }
}
