package com.biodex.dao;

import com.biodex.db.InMemoryDatabase;
import com.biodex.model.User;
import com.biodex.model.UserSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsDAOTest {

    private Connection connection;
    private UserDAO userDAO;
    private SettingsDAO settingsDAO;

    @BeforeEach
    void setUp() throws Exception {
        connection = InMemoryDatabase.open();
        userDAO = new UserDAO(connection);
        settingsDAO = new SettingsDAO(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void newUserHasCompleteDefaultSettings() {
        User user = userDAO.insert(new User("ada", "ada@example.com", "hash"));

        UserSettings settings = settingsDAO.getSettingsForUser(user.getUserId());

        assertEquals("light", settings.getTheme());
        assertEquals("en-AU", settings.getLanguage());
        assertEquals("METRIC", settings.getMeasurementUnit());
        assertTrue(settings.isAutoIdentifyOnUpload());
        assertEquals(0.70, settings.getIdentificationConfidenceThreshold());
        assertTrue(settings.isAutoCompressPhotos());
        assertTrue(settings.isNotifyNewSightingsNearby());
        assertTrue(settings.isNotifyCommunityAlerts());
        assertFalse(settings.isTwoFactorEnabled());
    }

    @Test
    void everySupportedSettingCanBeUpdatedAndReadBack() {
        User user = userDAO.insert(new User("ada", "ada@example.com", "hash"));
        int id = user.getUserId();

        settingsDAO.updateSetting(id, "theme", "DARK");
        settingsDAO.updateSetting(id, "language", "en-GB");
        settingsDAO.updateSetting(id, "measurement_unit", "IMPERIAL");
        settingsDAO.updateSetting(id, "default_suburb_id", null);
        settingsDAO.updateSetting(id, "auto_identify_on_upload", false);
        settingsDAO.updateSetting(id, "identification_confidence_threshold", 0.85);
        settingsDAO.updateSetting(id, "wifi_only_upload", true);
        settingsDAO.updateSetting(id, "auto_compress_photos", false);
        settingsDAO.updateSetting(id, "max_upload_resolution", "1280x720");
        settingsDAO.updateSetting(id, "notify_new_sightings_nearby", false);
        settingsDAO.updateSetting(id, "notify_community_alerts", false);
        settingsDAO.updateSetting(id, "notify_app_updates", true);
        settingsDAO.updateSetting(id, "share_location_publicly", true);
        settingsDAO.updateSetting(id, "anonymize_uploads", true);
        settingsDAO.updateSetting(id, "text_size", "LARGE");
        settingsDAO.updateAccountSettings(id, true);

        UserSettings settings = settingsDAO.getSettingsForUser(id);
        assertEquals("DARK", settings.getTheme());
        assertEquals("en-GB", settings.getLanguage());
        assertEquals("IMPERIAL", settings.getMeasurementUnit());
        assertFalse(settings.isAutoIdentifyOnUpload());
        assertEquals(0.85, settings.getIdentificationConfidenceThreshold());
        assertTrue(settings.isWifiOnlyUpload());
        assertFalse(settings.isAutoCompressPhotos());
        assertEquals("1280x720", settings.getMaxUploadResolution());
        assertFalse(settings.isNotifyNewSightingsNearby());
        assertFalse(settings.isNotifyCommunityAlerts());
        assertTrue(settings.isNotifyAppUpdates());
        assertTrue(settings.isShareLocationPublicly());
        assertTrue(settings.isAnonymizeUploads());
        assertEquals("LARGE", settings.getTextSize());
        assertTrue(settings.isTwoFactorEnabled());
    }

    @Test
    void invalidSettingColumnIsRejected() {
        User user = userDAO.insert(new User("ada", "ada@example.com", "hash"));

        assertThrows(IllegalArgumentException.class,
                () -> settingsDAO.updateSetting(user.getUserId(), "theme = password_hash", "x"));
    }

    @Test
    void usersCannotReadOrUpdateAnotherUsersSettings() {
        User first = userDAO.insert(new User("ada", "ada@example.com", "hash"));
        User second = userDAO.insert(new User("grace", "grace@example.com", "hash"));

        settingsDAO.updateSetting(first.getUserId(), "theme", "DARK");

        assertEquals("light", settingsDAO.getSettingsForUser(second.getUserId()).getTheme());
        settingsDAO.updateSetting(second.getUserId(), "theme", "SYSTEM");
        assertEquals("DARK", settingsDAO.getSettingsForUser(first.getUserId()).getTheme());
    }

    @Test
    void missingSettingsRowReturnsSafeDefaults() throws Exception {
        User user = userDAO.insert(new User("ada", "ada@example.com", "hash"));
        try (var statement = connection.prepareStatement("DELETE FROM user_settings WHERE user_id = ?")) {
            statement.setInt(1, user.getUserId());
            statement.executeUpdate();
        }

        assertEquals("light", settingsDAO.getSettingsForUser(user.getUserId()).getTheme());
    }

    @Test
    void bulkAccountUpdateIsPersistedAtomically() {
        User user = userDAO.insert(new User("ada", "ada@example.com", "hash"));
        UserSettings replacement = settingsDAO.getSettingsForUser(user.getUserId());
        replacement.setTheme("SYSTEM");
        replacement.setLanguage("fr-FR");
        replacement.setTwoFactorEnabled(true);

        settingsDAO.updateAccountSettings(user.getUserId(), replacement);

        UserSettings stored = settingsDAO.getSettingsForUser(user.getUserId());
        assertEquals("SYSTEM", stored.getTheme());
        assertEquals("fr-FR", stored.getLanguage());
        assertTrue(stored.isTwoFactorEnabled());
    }
}
