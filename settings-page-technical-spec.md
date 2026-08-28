# Settings Page — Technical Specification

## Document Control

| Field | Value |
|---|---|
| Module | Profile Page → **Settings** |
| Page Owner | Yash |
| Related Sub-pages | My Uploads, My Photos |
| App | Invasive Species Tracker (pest photo ID, location logging, ethical handling/disposal guidance) |
| Tech Stack | JavaFX (UI/Controllers), SQL via JDBC (persistence) |
| Status | Draft v1.0 |

---

## 1. Overview

The Settings page is a sub-view of the **Profile Page**, reached via a tab or side-navigation item alongside **My Uploads** and **My Photos**. It lets a signed-in user configure their account, app appearance, photo/upload behaviour, pest-identification behaviour, notifications, and privacy/data controls. All settings are user-scoped, persisted in a relational database, and loaded once per session into memory for fast UI binding.

This spec covers: functional requirements, UI/JavaFX layout, FXML structure, controller design, data model, SQL schema, DAO/query layer, validation, security, and testing.

---

## 2. Scope Within the Profile Page

The Profile Page is expected to use a `TabPane` (or a left-hand `ListView`/`VBox` nav acting as a router) with three destinations:

```
ProfileView (BorderPane)
 ├── left: NavigationPane (My Uploads | My Photos | Settings)
 └── center: <dynamically loaded FXML content>
        ├── MyUploadsView.fxml
        ├── MyPhotosView.fxml
        └── SettingsView.fxml   <-- this spec
```

Only the **Settings** destination is detailed below; the other two are referenced only where they intersect (e.g. "clear my uploads" living under Settings > Data).

---

## 3. Functional Requirements

| # | Requirement | Priority |
|---|---|---|
| F1 | User can view current settings on page load, pre-populated from the database | Must |
| F2 | User can edit account details: display name, email, password | Must |
| F3 | User can toggle Light/Dark/System theme, applied instantly without restart | Must |
| F4 | User can set default suburb (used to pre-fill new sighting location) | Must |
| F5 | User can toggle auto-identification of pests on photo upload | Must |
| F6 | User can adjust the identification confidence threshold (0–100%) | Should |
| F7 | User can control upload behaviour: Wi-Fi only, auto-compress, max resolution | Should |
| F8 | User can toggle notification categories independently | Should |
| F9 | User can control data sharing: public location sharing, anonymised uploads | Must |
| F10 | User can export their data (uploads/sightings) to a file | Could |
| F11 | User can clear local cache / thumbnails without deleting server data | Should |
| F12 | User can delete their account (with confirmation + typed-consent step) | Must |
| F13 | User can log out from Settings | Must |
| F14 | All changes are persisted immediately (auto-save on control change) or via an explicit "Save" action (see §14) | Must |
| F15 | Invalid input (e.g. malformed email) is rejected with inline error messaging | Must |
| F16 | Settings page respects accessibility text-size setting on its own controls | Could |

---

## 4. User Stories

- *As a user*, I want to switch to dark mode so the app is easier to use at night.
- *As a user*, I want to set my default suburb so I don't have to re-enter it every time I log a sighting.
- *As a user*, I want to turn off auto-identification if I only want to log a photo manually.
- *As a user*, I want to control whether my sightings are visible to other users, since some locations are on private property.
- *As a user*, I want to delete my account and be told clearly what will be removed.

---

## 5. UI/UX Specification

### 5.1 Layout Structure

`SettingsView.fxml` root is a `ScrollPane` (settings lists can exceed viewport height) containing a `VBox` of `TitledPane`s grouped inside an `Accordion` (or plain `VBox` if all sections should be visible at once — recommended for discoverability). Each `TitledPane` maps to one functional group:

```
Accordion (or VBox)
 ├── TitledPane "Account"
 │     ├── TextField        displayName
 │     ├── TextField        email
 │     ├── PasswordField    currentPassword (shown only in "change password" flow)
 │     ├── Hyperlink        "Change Password"
 │     └── Button           "Save Account Changes"
 │
 ├── TitledPane "Appearance"
 │     ├── ToggleGroup (RadioButton: Light / Dark / System)
 │     ├── ComboBox<String>  language
 │     └── ComboBox<String>  textSize (Small / Medium / Large)
 │
 ├── TitledPane "Location"
 │     └── ComboBox<Suburb>  defaultSuburb  (searchable / filterable)
 │
 ├── TitledPane "Pest Identification"
 │     ├── CheckBox          autoIdentifyOnUpload
 │     ├── Slider            confidenceThreshold (0–100, step 5)
 │     └── Label             live-bound "%.0f%%" readout of slider
 │
 ├── TitledPane "Photos & Uploads"
 │     ├── CheckBox          wifiOnlyUpload
 │     ├── CheckBox          autoCompressPhotos
 │     └── ComboBox<String>  maxUploadResolution
 │
 ├── TitledPane "Notifications"
 │     ├── CheckBox  notifyNewSightingsNearby
 │     ├── CheckBox  notifyCommunityAlerts
 │     └── CheckBox  notifyAppUpdates
 │
 ├── TitledPane "Privacy & Data"
 │     ├── CheckBox  shareLocationPublicly
 │     ├── CheckBox  anonymizeUploads
 │     ├── Button    "Export My Data"
 │     └── Button    "Clear Local Cache"
 │
 ├── TitledPane "About"
 │     ├── Label   appVersion
 │     ├── Hyperlink "Privacy Policy"
 │     ├── Hyperlink "Terms of Service"
 │     └── Hyperlink "Report a Bug"
 │
 └── HBox (bottom, sticky footer)
       ├── Button "Log Out"           (styled secondary)
       └── Button "Delete Account"    (styled danger, red)
```

### 5.2 Interaction Notes

- Theme changes apply **live** (stylesheet swap on the root `Scene`) so the user can preview before it's persisted.
- The confidence-threshold `Slider` shows a bound `Label` with the live percentage.
- "Delete Account" opens a modal `Dialog<ButtonType>` requiring the user to type `DELETE` into a `TextField` before the confirm button enables — mirrors destructive-action best practice.
- Fields disable and a `ProgressIndicator` shows during any async DB write to prevent double-submits.

---

## 6. FXML Skeleton

```xml
<!-- SettingsView.fxml -->
<ScrollPane fitToWidth="true" xmlns="http://javafx.com/javafx"
            xmlns:fx="http://javafx.com/fxml"
            fx:controller="com.pesttracker.controller.SettingsController">
    <VBox spacing="16" styleClass="settings-root">

        <TitledPane text="Account" collapsible="false">
            <VBox spacing="8">
                <Label text="Display Name"/>
                <TextField fx:id="displayNameField"/>
                <Label text="Email"/>
                <TextField fx:id="emailField"/>
                <Hyperlink fx:id="changePasswordLink" text="Change Password"/>
                <Button fx:id="saveAccountButton" text="Save Account Changes"
                        onAction="#handleSaveAccount"/>
            </VBox>
        </TitledPane>

        <TitledPane text="Appearance" collapsible="false">
            <VBox spacing="8">
                <HBox spacing="12" fx:id="themeToggleGroupBox"/>
                <ComboBox fx:id="languageComboBox"/>
                <ComboBox fx:id="textSizeComboBox"/>
            </VBox>
        </TitledPane>

        <TitledPane text="Pest Identification" collapsible="false">
            <VBox spacing="8">
                <CheckBox fx:id="autoIdentifyCheckBox" text="Auto-identify pests on upload"/>
                <Slider fx:id="confidenceSlider" min="0" max="100" value="70"
                        showTickLabels="true" majorTickUnit="25"/>
                <Label fx:id="confidenceLabel"/>
            </VBox>
        </TitledPane>

        <!-- ...remaining TitledPanes per §5.1... -->

        <HBox spacing="12" alignment="CENTER_RIGHT">
            <Button fx:id="logoutButton" text="Log Out" onAction="#handleLogout"/>
            <Button fx:id="deleteAccountButton" text="Delete Account"
                    styleClass="danger-button" onAction="#handleDeleteAccount"/>
        </HBox>

    </VBox>
</ScrollPane>
```

---

## 7. Controller Architecture

`SettingsController` implements `Initializable`, is instantiated by the FXMLLoader when the Profile Page swaps content into its center region, and receives the logged-in `User` context (via a shared `SessionManager` singleton — avoids threading a `userId` through every FXML load).

```java
public class SettingsController implements Initializable {

    // --- FXML-injected controls ---
    @FXML private TextField displayNameField;
    @FXML private TextField emailField;
    @FXML private CheckBox autoIdentifyCheckBox;
    @FXML private Slider confidenceSlider;
    @FXML private Label confidenceLabel;
    @FXML private ComboBox<Suburb> defaultSuburbComboBox;
    @FXML private ComboBox<String> textSizeComboBox;
    // ...remaining controls per §6

    private final SettingsDAO settingsDAO = new SettingsDAO(DatabaseConnection.getConnection());
    private final UserDAO userDAO = new UserDAO(DatabaseConnection.getConnection());
    private final SuburbDAO suburbDAO = new SuburbDAO(DatabaseConnection.getConnection());
    private UserSettings currentSettings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int userId = SessionManager.getInstance().getCurrentUser().getUserId();
        loadSettings(userId);
        bindLiveControls();
        wireAutoSaveListeners();
    }

    private void loadSettings(int userId) {
        currentSettings = settingsDAO.getSettingsForUser(userId);
        populateControlsFrom(currentSettings);
    }

    private void bindLiveControls() {
        confidenceLabel.textProperty().bind(
            Bindings.format("%.0f%%", confidenceSlider.valueProperty()));
    }

    /** Attaches a listener per control so changes persist without a page-wide Save button. */
    private void wireAutoSaveListeners() {
        autoIdentifyCheckBox.selectedProperty().addListener((obs, was, isNow) ->
            persist("auto_identify_on_upload", isNow));

        confidenceSlider.valueProperty().addListener((obs, was, isNow) ->
            persistDebounced("identification_confidence_threshold", isNow.doubleValue() / 100.0));

        // theme, language, text size, suburb, notification checkboxes follow the same pattern
    }

    private void persist(String column, Object value) {
        Task<Void> task = new Task<>() {
            protected Void call() {
                settingsDAO.updateSetting(currentSettings.getUserId(), column, value);
                return null;
            }
        };
        new Thread(task).start(); // off the FX Application Thread
    }

    @FXML
    private void handleSaveAccount() {
        if (!Validator.isValidEmail(emailField.getText())) {
            showFieldError(emailField, "Enter a valid email address.");
            return;
        }
        userDAO.updateProfile(currentSettings.getUserId(),
                displayNameField.getText(), emailField.getText());
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();
        SceneRouter.getInstance().goToLogin();
    }

    @FXML
    private void handleDeleteAccount() {
        boolean confirmed = ConfirmDeleteDialog.show(); // typed "DELETE" confirmation
        if (confirmed) {
            userDAO.deleteUserCascade(currentSettings.getUserId());
            SessionManager.getInstance().clear();
            SceneRouter.getInstance().goToLogin();
        }
    }
}
```

**Key architectural decisions**

- All DB writes run on a background `Task`/`Thread`, never the JavaFX Application Thread, to keep the UI responsive.
- `persistDebounced` (for the slider) should coalesce rapid-fire drag events into a single write ~300ms after the user stops dragging.
- Theme switching calls a shared `ThemeManager.apply(scene, theme)` utility so both Settings and app startup use one code path.

---

## 8. Data Model (Java)

```java
public class UserSettings {
    private int userId;
    private String theme;                 // LIGHT | DARK | SYSTEM
    private String language;
    private String measurementUnit;       // METRIC | IMPERIAL
    private Integer defaultSuburbId;
    private boolean autoIdentifyOnUpload;
    private double identificationConfidenceThreshold; // 0.0–1.0
    private boolean wifiOnlyUpload;
    private boolean autoCompressPhotos;
    private String maxUploadResolution;
    private boolean notifyNewSightingsNearby;
    private boolean notifyCommunityAlerts;
    private boolean notifyAppUpdates;
    private boolean shareLocationPublicly;
    private boolean anonymizeUploads;
    private String textSize;              // SMALL | MEDIUM | LARGE
    // getters / setters / builder
}
```

```java
public class Suburb {
    private int suburbId;
    private String name;
    private String state;
    private String postcode;
    // toString() -> "Name, STATE 0000" for ComboBox display
}
```

---

## 9. Database Schema (SQL)

> Assumes SQLite/MySQL-compatible DDL via JDBC. Adjust `AUTOINCREMENT`/`AUTO_INCREMENT` per target engine.

```sql
CREATE TABLE users (
    user_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    username       VARCHAR(50)  UNIQUE NOT NULL,
    email          VARCHAR(100) UNIQUE NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(100),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suburbs (
    suburb_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name       VARCHAR(100) NOT NULL,
    state      VARCHAR(50),
    postcode   VARCHAR(10)
);

CREATE TABLE user_settings (
    setting_id                            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                                INTEGER NOT NULL UNIQUE,
    theme                                   VARCHAR(10)  DEFAULT 'LIGHT',
    language                                VARCHAR(10)  DEFAULT 'en-AU',
    measurement_unit                        VARCHAR(10)  DEFAULT 'METRIC',
    default_suburb_id                       INTEGER,
    auto_identify_on_upload                 BOOLEAN      DEFAULT 1,
    identification_confidence_threshold     DECIMAL(3,2) DEFAULT 0.70,
    wifi_only_upload                        BOOLEAN      DEFAULT 0,
    auto_compress_photos                    BOOLEAN      DEFAULT 1,
    max_upload_resolution                   VARCHAR(20)  DEFAULT '1920x1080',
    notify_new_sightings_nearby             BOOLEAN      DEFAULT 1,
    notify_community_alerts                 BOOLEAN      DEFAULT 1,
    notify_app_updates                      BOOLEAN      DEFAULT 0,
    share_location_publicly                 BOOLEAN      DEFAULT 0,
    anonymize_uploads                       BOOLEAN      DEFAULT 0,
    text_size                               VARCHAR(10)  DEFAULT 'MEDIUM',
    updated_at                              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (default_suburb_id) REFERENCES suburbs(suburb_id)
);

-- Referenced for "Export My Data" / "Delete Account" cascade, defined in full
-- elsewhere (My Uploads / My Photos specs) but shown here for FK context:
CREATE TABLE sightings (
    sighting_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    suburb_id    INTEGER NOT NULL,
    pest_id      INTEGER,
    photo_path   VARCHAR(255),
    logged_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)   REFERENCES users(user_id)   ON DELETE CASCADE,
    FOREIGN KEY (suburb_id) REFERENCES suburbs(suburb_id)
);
```

Trigger to auto-create a default `user_settings` row on signup:

```sql
CREATE TRIGGER trg_create_default_settings
AFTER INSERT ON users
BEGIN
    INSERT INTO user_settings (user_id) VALUES (NEW.user_id);
END;
```

---

## 10. DAO / SQL Query Reference

```java
public class SettingsDAO {

    private final Connection connection;
    public SettingsDAO(Connection connection) { this.connection = connection; }

    public UserSettings getSettingsForUser(int userId) {
        String sql = "SELECT * FROM user_settings WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    /** Generic single-column update, used by the auto-save listeners in the controller. */
    public void updateSetting(int userId, String column, Object value) {
        // Column name is NEVER taken from user input directly — it is selected from
        // a fixed whitelist enum in the controller, preventing SQL injection via column name.
        String sql = "UPDATE user_settings SET " + column + " = ?, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, value);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private UserSettings mapRow(ResultSet rs) throws SQLException {
        UserSettings s = new UserSettings();
        s.setUserId(rs.getInt("user_id"));
        s.setTheme(rs.getString("theme"));
        s.setAutoIdentifyOnUpload(rs.getBoolean("auto_identify_on_upload"));
        s.setIdentificationConfidenceThreshold(rs.getDouble("identification_confidence_threshold"));
        // ...remaining fields
        return s;
    }
}
```

```java
public class UserDAO {
    // ...
    public void deleteUserCascade(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?"; // ON DELETE CASCADE handles children
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException(e); }
    }
}
```

**Important:** `updateSetting`'s `column` parameter must be validated against a fixed `Set<String> ALLOWED_COLUMNS` (or an enum) before string-concatenation into the query — parameterised placeholders cannot bind column/table names, so a whitelist is the safeguard against injection here.

---

## 11. Settings Catalog

| Setting | Control | Type | Default | DB Column |
|---|---|---|---|---|
| Theme | RadioButtons | enum | LIGHT | `theme` |
| Language | ComboBox | enum | en-AU | `language` |
| Measurement Unit | ComboBox | enum | METRIC | `measurement_unit` |
| Default Suburb | Searchable ComboBox | FK int | null | `default_suburb_id` |
| Auto-identify on Upload | CheckBox | bool | true | `auto_identify_on_upload` |
| ID Confidence Threshold | Slider | decimal | 0.70 | `identification_confidence_threshold` |
| Wi-Fi Only Upload | CheckBox | bool | false | `wifi_only_upload` |
| Auto-compress Photos | CheckBox | bool | true | `auto_compress_photos` |
| Max Upload Resolution | ComboBox | enum | 1920x1080 | `max_upload_resolution` |
| Notify: Nearby Sightings | CheckBox | bool | true | `notify_new_sightings_nearby` |
| Notify: Community Alerts | CheckBox | bool | true | `notify_community_alerts` |
| Notify: App Updates | CheckBox | bool | false | `notify_app_updates` |
| Share Location Publicly | CheckBox | bool | false | `share_location_publicly` |
| Anonymize Uploads | CheckBox | bool | false | `anonymize_uploads` |
| Text Size | ComboBox | enum | MEDIUM | `text_size` |

---

## 12. Validation & Error Handling

| Field | Rule | Failure Behaviour |
|---|---|---|
| Email | RFC-5322-style regex, non-empty | Inline red border + helper `Label` under field |
| Password (on change) | ≥8 chars, 1 number, 1 letter | Inline error, submit button disabled until valid |
| Confidence Threshold | Clamped 0–100 by `Slider` bounds | N/A (control-level) |
| Delete Account | Must type `DELETE` exactly | Confirm button stays disabled |
| DB write failure | Any `SQLException` in a Task | Caught in `setOnFailed`, shows a non-blocking `Notification`/`Alert`, reverts the control to its last known-good value |

All controller-level DB exceptions are wrapped into an unchecked `DataAccessException` at the DAO boundary and caught centrally at the controller/Task layer — the UI never surfaces raw SQL errors to the user.

---

## 13. Security & Privacy Considerations

- Passwords are never stored or compared in plaintext — hash with **BCrypt** (`jBCrypt` library) at signup/change; `password_hash` column stores only the hash.
- All SQL access uses `PreparedStatement` with bound parameters; the one exception (dynamic column name in `updateSetting`) is protected by an application-level whitelist, never raw user input.
- `share_location_publicly` and `anonymize_uploads` gate what the **My Uploads/My Photos/community** views are allowed to query — enforced at the DAO query level (e.g. `WHERE anonymize_uploads = 0 OR requester = owner`), not just hidden in the UI.
- Account deletion is a hard delete cascading via FK constraints (`ON DELETE CASCADE`) — spec should confirm with stakeholders whether a **soft-delete** (e.g. `is_deleted` flag + data retention window) is required instead, given this is user-generated ecological data that may have research value.
- Session state (`SessionManager`) is in-memory only, cleared on logout; no credentials persisted to disk outside the hashed DB column.

---

## 14. Navigation & State Management

- The Settings page is loaded fresh (new `FXMLLoader`) each time the user navigates to it from the Profile nav, always re-querying the DB — avoids stale in-memory state if settings were changed elsewhere (e.g. a "quick theme toggle" in the app header).
- **Auto-save vs explicit Save:** toggles/combo-boxes/sliders auto-save on change (F14). The **Account** section (name/email/password) uses an explicit "Save Account Changes" button, since these fields warrant validation before commit and shouldn't fire a DB write per keystroke.
- Theme changes propagate app-wide via a `ThemeManager` observable so every open view (not just Settings) updates the stylesheet immediately.

---

## 15. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | Settings load (DB fetch → populated UI) completes in <300ms on local SQLite |
| Responsiveness | Layout uses `ScrollPane`/`VBox` with percentage/`Priority.ALWAYS` sizing so it resizes gracefully from 800×600 up |
| Accessibility | All controls have `accessibleText`; tab order follows visual order; text-size setting scales font via a CSS class swap |
| Portability | Runs on JDK 17+ / JavaFX 21+ across Windows, macOS, Linux |
| Offline Support | Settings read/write against local SQLite; if the app later adds cloud sync, writes should queue and reconcile on reconnect |
| Data Integrity | `user_settings.user_id` is `UNIQUE` — one settings row per user, enforced at schema level |

---

## 16. Testing Plan

- **Unit tests (JUnit 5):** `SettingsDAO` CRUD against an in-memory H2/SQLite test DB; `Validator` email/password rules.
- **UI tests (TestFX):** toggle each `CheckBox`/`Slider`, assert the underlying DB row updates; verify theme swap changes the scene's stylesheet list; verify Delete Account button stays disabled until `DELETE` is typed.
- **Manual QA checklist:** dark/light/system theme on OS-level theme change; slow/no network with Wi-Fi-only upload enabled; account deletion cascade removes related sightings/photos rows.

---

## 17. Suggested Package/File Structure

```
com.pesttracker
 ├── app/                Main.java, SceneRouter.java, SessionManager.java
 ├── controller/         ProfileController.java, SettingsController.java, ...
 ├── model/               User.java, UserSettings.java, Suburb.java, Sighting.java
 ├── dao/                 UserDAO.java, SettingsDAO.java, SuburbDAO.java
 ├── db/                   DatabaseConnection.java, schema.sql
 ├── util/                 Validator.java, PasswordHasher.java, ThemeManager.java
resources/
 ├── fxml/                 ProfileView.fxml, SettingsView.fxml
 └── css/                   light-theme.css, dark-theme.css
```

---

## 18. Future Enhancements (Out of Scope for v1)

- Cloud sync of settings across devices.
- Per-suburb notification radius control (e.g. "alert me within 5km").
- Granular data-export formats (CSV vs JSON vs PDF report of sightings).
- Two-factor authentication toggle under Account.

---

## Appendix A: Example Sequence — Toggling Dark Theme

```
User clicks "Dark" RadioButton
   -> SettingsController.onThemeChanged(DARK)
       -> ThemeManager.apply(scene, DARK)      [instant visual update]
       -> background Task: SettingsDAO.updateSetting(userId, "theme", "DARK")
           -> UPDATE user_settings SET theme='DARK', updated_at=NOW WHERE user_id=?
       -> Task.setOnFailed -> revert RadioButton + show Alert (only if write fails)
```

## Appendix B: Core Class Relationships

```
SettingsController --uses--> SettingsDAO --queries--> user_settings (SQL)
SettingsController --uses--> UserDAO      --queries--> users (SQL)
SettingsController --uses--> SuburbDAO    --queries--> suburbs (SQL)
SettingsController --reads--> SessionManager (current UserSettings/User context)
ThemeManager        --applied by--> SettingsController, and any other open Scene
```
