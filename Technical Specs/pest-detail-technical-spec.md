# Pest Detail Screen

## Document Control

| Field | Value |
| --- | --- |
| Module | Species Search → Pest Detail Screen (Species Detail pane + Local Sightings pane) |
| Page Owner | Vinny |
| Related Sub-pages | Species Search, Log Sighting, Add Photo, Full Sightings Map |
| App | Biodex |
| Tech Stack | JavaFX (UI/Controllers), SQL via JDBC (persistence) |
| Status | Draft v1.0 |

---

## 1. Overview

Pest Detail opens when a user taps a species from Species Search. The screen splits into two panes. The left pane, **Species Detail**, shows what the pest is and how dangerous it is. The right pane, **Local Sightings**, shows where it has turned up around Brisbane in the last 30 days.

The two panes load from the same species ID but query different tables, so they run as separate async tasks and render independently. A slow sightings query never blocks the species photo and threat profile from appearing.

---

## 2. Scope Within Species Search

```text
Species Search
 |-- SpeciesSearchView.fxml
 |     |-- search field, filter chips
 |     \-- species list -> opens Pest Detail Screen (this spec)
 |
 |-- Pest Detail Screen (PestDetailView.fxml)
 |     |-- SpeciesDetailView.fxml   (left pane, fx:include)
 |     \-- LocalSightingsView.fxml  (right pane, fx:include)
 |
 |-- Log Sighting wizard   (out of scope here, opened from "Log sighting")
 \-- Full Sightings Map    (out of scope here, opened from "Open full map")
```

Species Search hands Pest Detail a single `speciesId`. Both panes key off that ID and neither pane navigates anywhere on its own except through Log Sighting, Add Photo, or Open Full Map.

---

## 3. Functional Requirements

| # | Requirement | Priority |
| --- | --- | --- |
| F0 | Species list shows a "View details" action per row that opens Pest Detail Screen | Must |
| F1 | Species Detail pane shows the species photo, common name, scientific name, threat badge, and category tags | Must |
| F2 | Species Detail pane shows a Threat Profile section with Aggression, Sting Severity, and Spread Risk bars | Must |
| F3 | Species Detail pane shows Typical Habitat and Size | Must |
| F4 | Species Detail pane shows Disposal Guidance text | Must |
| F5 | "Log sighting" opens the Log Sighting wizard pre-filled with this species | Must |
| F6 | "Add photo" opens the photo capture/upload dialog attached to this species | Should |
| F7 | Local Sightings pane shows report density per suburb for the last 30 days | Must |
| F8 | Density cards sort by report count, highest first | Should |
| F9 | Local Sightings pane lists the 3 most recent reports for the species, each with suburb, relative date, and verification status | Must |
| F10 | "Open full map" opens a Brisbane map with a pin for every report of this species | Should |
| F11 | Both panes show a loading state while their query runs and keep the last successful data on screen if a query fails | Should |
| F12 | A submitted report starts Unverified; only a council-staff account can flip it to Verified | Must |

---

## 4. UI Flow

### Species Detail Pane

Circular photo at the top, common name in bold, scientific name in italics below it, a threat-level badge ("High threat", coloured by severity), and category tags ("Invasive", "Stinging"). Below that: a short description, then the three Threat Profile bars, then Typical Habitat and Size side by side, then Disposal Guidance. "Log sighting" and "Add photo" sit as two buttons at the bottom of the pane.

### Local Sightings Pane

Header reads "Local sightings" with "Brisbane, QLD" aligned right. A grid of suburb cards follows, each showing a suburb name and its report count over the last 30 days, shaded from light to dark red by volume. Below the cards, a "Recent reports near you" list shows up to 3 entries, each with a short description, suburb and relative timestamp, and an Unverified/Verified badge. "Open full map" sits as a full-width button at the bottom of the pane.

---

## 5. Controller Architecture

`PestDetailController` hosts both child panes and loads the species once, then hands the result to each child controller. `SpeciesDetailController` and `LocalSightingsController` never query the database directly for the species record; only `PestDetailController` does that lookup, so the two panes can't drift out of sync on which species they're showing.

```java
public class PestDetailController implements Initializable {

    @FXML private SpeciesDetailController speciesDetailController;   // injected via fx:include
    @FXML private LocalSightingsController localSightingsController; // injected via fx:include

    private final SpeciesDAO speciesDAO = new SpeciesDAO(DatabaseConnection.getConnection());

    public void loadSpecies(int speciesId) {
        Task<Species> task = new Task<>() {
            protected Species call() { return speciesDAO.findById(speciesId); }
        };
        task.setOnSucceeded(e -> {
            Species species = task.getValue();
            speciesDetailController.display(species);
            localSightingsController.load(species.getSpeciesId());
        });
        new Thread(task).start();
    }

    @Override public void initialize(URL location, ResourceBundle resources) {}
}
```

```java
public class SpeciesDetailController implements Initializable {

    @FXML private ImageView photoView;
    @FXML private Label commonNameLabel;
    @FXML private Label scientificNameLabel;
    @FXML private Label threatBadge;
    @FXML private ProgressBar aggressionBar;
    @FXML private ProgressBar stingSeverityBar;
    @FXML private ProgressBar spreadRiskBar;
    @FXML private Label habitatLabel;
    @FXML private Label sizeLabel;
    @FXML private Label disposalLabel;

    private Species species;

    public void display(Species species) {
        this.species = species;
        commonNameLabel.setText(species.getCommonName());
        scientificNameLabel.setText(species.getScientificName());
        threatBadge.setText(species.getThreatLevel().name());
        aggressionBar.setProgress(species.getAggression() / 100.0);
        stingSeverityBar.setProgress(species.getStingSeverity() / 100.0);
        spreadRiskBar.setProgress(species.getSpreadRisk() / 100.0);
        habitatLabel.setText(species.getTypicalHabitat());
        sizeLabel.setText(species.getSizeMinMm() + " to " + species.getSizeMaxMm() + " mm");
        disposalLabel.setText(species.getDisposalGuidance());
        photoView.setImage(new Image(species.getPhotoPath()));
    }

    @FXML
    private void handleLogSighting() {
        SceneRouter.getInstance().goToLogSighting(species.getSpeciesId());
    }

    @FXML
    private void handleAddPhoto() {
        SceneRouter.getInstance().goToAddPhoto(species.getSpeciesId());
    }

    @Override public void initialize(URL location, ResourceBundle resources) {}
}
```

```java
public class LocalSightingsController implements Initializable {

    @FXML private VBox densityCardContainer;
    @FXML private VBox recentReportsContainer;

    private final SightingReportDAO reportDAO = new SightingReportDAO(DatabaseConnection.getConnection());
    private static final int WINDOW_DAYS = 30;
    private static final int RECENT_LIMIT = 3;

    public void load(int speciesId) {
        Task<Map<String, Integer>> densityTask = new Task<>() {
            protected Map<String, Integer> call() { return reportDAO.getDensityBySuburb(speciesId, WINDOW_DAYS); }
        };
        densityTask.setOnSucceeded(e -> renderDensityCards(densityTask.getValue()));
        new Thread(densityTask).start();

        Task<List<SightingReport>> recentTask = new Task<>() {
            protected List<SightingReport> call() { return reportDAO.getRecentReports(speciesId, RECENT_LIMIT); }
        };
        recentTask.setOnSucceeded(e -> renderRecentReports(recentTask.getValue()));
        new Thread(recentTask).start();
    }

    private void renderDensityCards(Map<String, Integer> counts) {
        densityCardContainer.getChildren().clear();
        counts.forEach((suburb, count) -> densityCardContainer.getChildren().add(DensityCardFactory.build(suburb, count)));
    }

    private void renderRecentReports(List<SightingReport> reports) {
        recentReportsContainer.getChildren().clear();
        reports.forEach(r -> recentReportsContainer.getChildren().add(ReportRowFactory.build(r)));
    }

    @FXML
    private void handleOpenFullMap() {
        SceneRouter.getInstance().goToSightingsMap();
    }

    @Override public void initialize(URL location, ResourceBundle resources) {}
}
```

---

## 6. Data Model and Schema

```java
public class Species {
    private int speciesId;
    private String commonName;
    private String scientificName;
    private ThreatLevel threatLevel;   // LOW, MEDIUM, HIGH
    private int aggression;            // 0-100
    private int stingSeverity;         // 0-100
    private int spreadRisk;            // 0-100
    private String typicalHabitat;
    private double sizeMinMm;
    private double sizeMaxMm;
    private String disposalGuidance;
    private String photoPath;
}

public class SightingReport {
    private int reportId;
    private int speciesId;
    private String suburb;
    private String locationLabel;   // e.g. "Mound near footpath, Corsair Ave"
    private double latitude;
    private double longitude;
    private Instant reportedAt;
    private boolean verified;
    private int reporterUserId;
    private String photoPath;       // nullable
}
```

```sql
CREATE TABLE species (
    species_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    common_name       VARCHAR(100) NOT NULL,
    scientific_name   VARCHAR(150) NOT NULL,
    threat_level      VARCHAR(10)  NOT NULL,   -- LOW, MEDIUM, HIGH
    aggression        INTEGER      NOT NULL,
    sting_severity    INTEGER      NOT NULL,
    spread_risk       INTEGER      NOT NULL,
    typical_habitat   VARCHAR(255),
    size_min_mm       DECIMAL(5,2),
    size_max_mm       DECIMAL(5,2),
    disposal_guidance TEXT,
    photo_path        VARCHAR(255)
);

CREATE TABLE species_tags (
    species_id   INTEGER     NOT NULL,
    tag          VARCHAR(30) NOT NULL,   -- Invasive, Stinging, etc.
    PRIMARY KEY (species_id, tag),
    FOREIGN KEY (species_id) REFERENCES species(species_id) ON DELETE CASCADE
);

CREATE TABLE sighting_reports (
    report_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    species_id       INTEGER NOT NULL,
    suburb           VARCHAR(100) NOT NULL,
    location_label   VARCHAR(255) NOT NULL,
    latitude         DECIMAL(9,6),
    longitude        DECIMAL(9,6),
    reported_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified         BOOLEAN   DEFAULT 0,
    reporter_user_id INTEGER,
    photo_path       VARCHAR(255),
    FOREIGN KEY (species_id) REFERENCES species(species_id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX idx_reports_species_suburb ON sighting_reports(species_id, suburb);
CREATE INDEX idx_reports_species_date   ON sighting_reports(species_id, reported_at);
```

`species_tags` sits in its own table rather than a comma-packed column on `species`, so a future filter-by-tag feature can query it directly instead of parsing strings.

---

## 7. DAO Reference

```java
public class SpeciesDAO {

    private final Connection connection;

    public SpeciesDAO(Connection connection) { this.connection = connection; }

    public Species findById(int speciesId) {
        String sql = "SELECT * FROM species WHERE species_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, speciesId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return mapRow(rs);
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private Species mapRow(ResultSet rs) throws SQLException {
        Species s = new Species();
        s.setSpeciesId(rs.getInt("species_id"));
        s.setCommonName(rs.getString("common_name"));
        s.setScientificName(rs.getString("scientific_name"));
        s.setThreatLevel(ThreatLevel.valueOf(rs.getString("threat_level")));
        s.setAggression(rs.getInt("aggression"));
        s.setStingSeverity(rs.getInt("sting_severity"));
        s.setSpreadRisk(rs.getInt("spread_risk"));
        s.setTypicalHabitat(rs.getString("typical_habitat"));
        s.setSizeMinMm(rs.getDouble("size_min_mm"));
        s.setSizeMaxMm(rs.getDouble("size_max_mm"));
        s.setDisposalGuidance(rs.getString("disposal_guidance"));
        s.setPhotoPath(rs.getString("photo_path"));
        return s;
    }
}
```

```java
public class SightingReportDAO {

    private final Connection connection;

    public SightingReportDAO(Connection connection) { this.connection = connection; }

    public Map<String, Integer> getDensityBySuburb(int speciesId, int windowDays) {
        String sql = "SELECT suburb, COUNT(*) AS report_count FROM sighting_reports "
                   + "WHERE species_id = ? AND reported_at >= ? "
                   + "GROUP BY suburb ORDER BY report_count DESC";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, speciesId);
            ps.setTimestamp(2, Timestamp.from(Instant.now().minus(windowDays, ChronoUnit.DAYS)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) counts.put(rs.getString("suburb"), rs.getInt("report_count"));
            return counts;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    public List<SightingReport> getRecentReports(int speciesId, int limit) {
        String sql = "SELECT * FROM sighting_reports WHERE species_id = ? "
                   + "ORDER BY reported_at DESC LIMIT ?";
        List<SightingReport> reports = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, speciesId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) reports.add(mapRow(rs));
            return reports;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    public void insertReport(SightingReport report) {
        String sql = "INSERT INTO sighting_reports "
                   + "(species_id, suburb, location_label, latitude, longitude, reporter_user_id, photo_path) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, report.getSpeciesId());
            ps.setString(2, report.getSuburb());
            ps.setString(3, report.getLocationLabel());
            ps.setDouble(4, report.getLatitude());
            ps.setDouble(5, report.getLongitude());
            ps.setInt(6, report.getReporterUserId());
            ps.setString(7, report.getPhotoPath());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private SightingReport mapRow(ResultSet rs) throws SQLException {
        SightingReport r = new SightingReport();
        r.setReportId(rs.getInt("report_id"));
        r.setSpeciesId(rs.getInt("species_id"));
        r.setSuburb(rs.getString("suburb"));
        r.setLocationLabel(rs.getString("location_label"));
        r.setLatitude(rs.getDouble("latitude"));
        r.setLongitude(rs.getDouble("longitude"));
        r.setReportedAt(rs.getTimestamp("reported_at").toInstant());
        r.setVerified(rs.getBoolean("verified"));
        r.setReporterUserId(rs.getInt("reporter_user_id"));
        r.setPhotoPath(rs.getString("photo_path"));
        return r;
    }
}
```

Every query binds its parameters through `PreparedStatement`. `getDensityBySuburb` does the grouping in SQL rather than in Java, so the suburb counts stay correct even as `sighting_reports` grows past what the app would want to pull into memory.

---

## 8. Validation and Error Handling

| Field | Rule | Failure Behaviour |
| --- | --- | --- |
| Species lookup | `speciesId` must exist | Pane shows "Species not found"; Pest Detail Screen returns to Species Search |
| Density query | Window fixed at 30 days | Zero rows shows "No reports in the last 30 days" instead of empty cards |
| Recent reports | Limit fixed at 3 | Fewer than 3 reports shows only what exists, no placeholder rows |
| Log sighting | Requires a signed-in session | Redirects to Login if `SessionManager` has no active user |
| Add photo | File must be jpg/png under 10MB | Inline error, upload blocked |
| DB failure | Any exception in a `Task` | Non-blocking alert; pane keeps its last successful data |

---

## 9. Data Integrity and Privacy Considerations

- Density cards count every report for a suburb, verified or not, so the number on screen moves as soon as someone submits a report, before council confirms anything.
- Each entry in Recent Reports keeps its Unverified/Verified badge, so the list never implies confirmation that hasn't happened.
- Only an account flagged `councilStaff` in `SessionManager` can flip a report's `verified` column.
- `location_label` holds whatever text the reporter typed, like a street name. Biodex never resolves that to a street number, and the full map jitters each pin by roughly 50 metres before it renders, so a pin can't pinpoint a reporter's yard.
- `reporter_user_id` ties a report to whoever logged it, but no screen in this spec displays a reporter's name.
- Log Sighting and Add Photo both check `SessionManager` first, so an anonymous user can browse Pest Detail but can't write to it.

---

## 10. Testing Plan

**Unit tests.** Cover `SightingReportDAO.getDensityBySuburb` across suburb boundaries and date windows, and `getRecentReports` for its `ORDER` and `LIMIT` behaviour.

**UI tests (TestFX).** Load a species with reports spread across several suburbs, a species with zero reports, and a species with more than 3 reports, and check the density cards, the recent list, and the empty states.

**Manual QA.** Try Log Sighting while signed out and confirm the redirect to Login. Verify a report as a council-staff account and confirm its badge updates everywhere that report appears.

---

## 11. Suggested File Structure

```text
com.pesttracker
 |-- app/         SceneRouter.java, SessionManager.java
 |-- controller/  PestDetailController.java, SpeciesDetailController.java, LocalSightingsController.java
 |-- model/       Species.java, SightingReport.java
 |-- dao/         SpeciesDAO.java, SightingReportDAO.java
 |-- db/          DatabaseConnection.java, schema.sql
 \-- util/        DensityCardFactory.java, ReportRowFactory.java
resources/
 |-- fxml/        PestDetailView.fxml, SpeciesDetailView.fxml, LocalSightingsView.fxml
 \-- css/         species-theme.css
```

---

## 12. Mockup

<!-- Add the mockup image to docs/images/ and update the path below. -->

![Pest Detail Screen mockup](docs/images/pest-detail-mockup.png)
