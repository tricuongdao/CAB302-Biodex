package com.biodex.controller.heatmap;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.dao.SightingDAO;
import com.biodex.model.MapSighting;
import com.biodex.routing.Route;
import com.biodex.util.BrisbaneMapProjection;
import com.biodex.util.GeographicBasemap;
import com.biodex.util.BrisbaneMapProjection.ProjectedPoint;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Heat-map screen backed by saved SQLite sightings. */
public class HeatMapController extends BaseController {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("d MMM uuuu");

    /** Injected from the fx:include with fx:id="sidebar" in HeatMapView.fxml. */
    @FXML
    private SidebarController sidebarController;

    @FXML
    private Pane hotspotLayer;

    @FXML
    private Label summaryLabel;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label selectedAreaLabel;

    @FXML
    private Label selectedSpeciesLabel;

    @FXML
    private Label selectedDateLabel;

    @FXML
    private Label selectedDescriptionLabel;

    @FXML
    private Button last7DaysButton;

    @FXML
    private Button last30DaysButton;

    @FXML
    private Button caneToadButton;

    @FXML
    private Button fireAntButton;

    @FXML
    private Button waterHyacinthButton;

    @FXML
    private WebView basemapView;

    @FXML
    private Label basemapStatusLabel;

    private GeographicBasemap basemap;
    private final List<Runnable> markerLayouts = new ArrayList<>();

    private final SightingDAO sightingDAO;

    public HeatMapController() {
        this(new SightingDAO());
    }

    /** Allows full-screen integration checks with an isolated SQLite database. */
    public HeatMapController(SightingDAO sightingDAO) {
        this.sightingDAO = java.util.Objects.requireNonNull(sightingDAO);
    }

    private String selectedSpecies;
    private LocalDate fromDate;
    private Task<List<MapSighting>> activeLoad;

    @FXML
    private void initialize() {
        sidebarController.setActive("heatmap");
        fromDate = LocalDate.now().minusDays(29);
        updateFilterStyles();
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(hotspotLayer.widthProperty());
        clip.heightProperty().bind(hotspotLayer.heightProperty());
        hotspotLayer.setClip(clip);
        hotspotLayer.widthProperty().addListener(observable -> repositionMarkers());
        hotspotLayer.heightProperty().addListener(observable -> repositionMarkers());
        basemap = new GeographicBasemap(basemapView, basemapStatusLabel, this::repositionMarkers);
        loadSightings();
    }

    private void repositionMarkers() {
        markerLayouts.forEach(Runnable::run);
    }

    @FXML
    private void onResetMap() {
        basemap.reset();
    }

    @FXML
    private void onRetryMap() {
        basemap.reload();
    }

    @FXML
    private void onLast7Days() {
        fromDate = LocalDate.now().minusDays(6);
        updateFilterStyles();
        loadSightings();
    }

    @FXML
    private void onLast30Days() {
        fromDate = LocalDate.now().minusDays(29);
        updateFilterStyles();
        loadSightings();
    }

    @FXML
    private void onCaneToad() {
        selectSpecies("Cane Toad");
    }

    @FXML
    private void onFireAnt() {
        selectSpecies("Fire Ant");
    }

    @FXML
    private void onWaterHyacinth() {
        selectSpecies("Water Hyacinth");
    }

    @FXML
    private void onClearFilters() {
        selectedSpecies = null;
        fromDate = null;
        updateFilterStyles();
        loadSightings();
    }

    @FXML
    private void onReportSighting() {
        router.go(Route.IDENTIFY_PEST);
    }

    private void selectSpecies(String speciesName) {
        selectedSpecies = speciesName.equals(selectedSpecies) ? null : speciesName;
        updateFilterStyles();
        loadSightings();
    }

    /** Runs the SQLite query away from the JavaFX application thread. */
    private void loadSightings() {
        if (activeLoad != null) {
            activeLoad.cancel();
        }

        String speciesSnapshot = selectedSpecies;
        LocalDate dateSnapshot = fromDate;
        Task<List<MapSighting>> task = new Task<>() {
            @Override
            protected List<MapSighting> call() {
                return sightingDAO.findForMap(speciesSnapshot, dateSnapshot);
            }
        };
        activeLoad = task;

        task.setOnRunning(event -> setLoading(true));
        task.setOnSucceeded(event -> {
            if (activeLoad == task) {
                setLoading(false);
                renderSightings(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (activeLoad == task) {
                setLoading(false);
                showLoadError();
            }
        });

        Thread loader = new Thread(task, "heat-map-sighting-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void renderSightings(List<MapSighting> sightings) {
        markerLayouts.clear();
        hotspotLayer.getChildren().clear();
        resetDetails();

        Map<HotspotKey, List<MapSighting>> hotspots = new LinkedHashMap<>();
        for (MapSighting sighting : sightings) {
            BrisbaneMapProjection.project(sighting.getLatitude(), sighting.getLongitude())
                    .ifPresent(point -> hotspots
                            .computeIfAbsent(HotspotKey.from(sighting, point), key -> new ArrayList<>())
                            .add(sighting));
        }

        int plottedSightings = 0;
        for (Map.Entry<HotspotKey, List<MapSighting>> hotspot : hotspots.entrySet()) {
            List<MapSighting> records = hotspot.getValue();
            plottedSightings += records.size();
            hotspotLayer.getChildren().add(createMarker(hotspot.getKey(), records));
        }

        summaryLabel.setText(plottedSightings + " " + plural(plottedSightings, "sighting", "sightings")
                + " - " + hotspots.size() + " " + plural(hotspots.size(), "hot spot", "hot spots")
                + " - Greater Brisbane");

        boolean empty = hotspots.isEmpty();
        emptyStateLabel.setText(emptyMessage());
        emptyStateLabel.setVisible(empty);
        emptyStateLabel.setManaged(empty);
    }

    private StackPane createMarker(HotspotKey key, List<MapSighting> sightings) {
        int count = sightings.size();
        double radius = Math.min(36, 13 + Math.sqrt(count) * 5);

        Circle circle = new Circle(radius);
        circle.getStyleClass().addAll("heat-marker-circle", densityClass(count));

        Label countLabel = new Label(Integer.toString(count));
        countLabel.getStyleClass().add("hotspot-count");
        countLabel.setMouseTransparent(true);

        StackPane marker = new StackPane(circle, countLabel);
        marker.getStyleClass().add("hotspot-marker");
        marker.setManaged(false);
        marker.setPrefSize(radius * 2, radius * 2);
        marker.setMaxSize(radius * 2, radius * 2);
        marker.resize(radius * 2, radius * 2); // Unmanaged overlays are sized explicitly.
        MapSighting location = sightings.get(0);
        Runnable position = () -> {
            Point2D point = basemap != null && basemap.isReady()
                    ? basemap.project(location.getLatitude(), location.getLongitude())
                    : new Point2D(hotspotLayer.getWidth() * key.point().xRatio(),
                            hotspotLayer.getHeight() * key.point().yRatio());
            marker.relocate(point.getX() - radius, point.getY() - radius);
        };
        markerLayouts.add(position);
        position.run();

        String tooltipText = key.suburbName() + " " + key.postcode() + "\n"
                + count + " " + plural(count, "sighting", "sightings");
        Tooltip.install(marker, new Tooltip(tooltipText));
        marker.setOnMouseClicked(event -> showHotspot(key, sightings));
        return marker;
    }

    private void showHotspot(HotspotKey key, List<MapSighting> sightings) {
        MapSighting newest = sightings.get(0);
        String species = sightings.stream()
                .map(MapSighting::getSpeciesName)
                .distinct()
                .collect(Collectors.joining(", "));

        selectedAreaLabel.setText(key.suburbName() + " " + key.postcode());
        selectedSpeciesLabel.setText(species);
        selectedDateLabel.setText("Most recent: " + DISPLAY_DATE.format(newest.getSightingDate()));
        String description = newest.getDescription();
        selectedDescriptionLabel.setText(
                description == null || description.isBlank()
                        ? "No description was provided for the latest sighting."
                        : description);
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        if (loading) {
            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);
            summaryLabel.setText("Loading saved sightings...");
        }
    }

    private void showLoadError() {
        markerLayouts.clear();
        hotspotLayer.getChildren().clear();
        summaryLabel.setText("Saved sightings could not be loaded");
        emptyStateLabel.setText("Something went wrong while reading the Biodex database.");
        emptyStateLabel.setVisible(true);
        emptyStateLabel.setManaged(true);
        resetDetails();
    }

    private void resetDetails() {
        selectedAreaLabel.setText("Select a hot spot");
        selectedSpeciesLabel.setText("Click a numbered marker to inspect its sightings.");
        selectedDateLabel.setText("");
        selectedDescriptionLabel.setText("");
    }

    private String emptyMessage() {
        if (selectedSpecies != null || fromDate != null) {
            return "No saved sightings match the selected filters.";
        }
        return "No saved sightings yet. Report one to add it to the map.";
    }

    private void updateFilterStyles() {
        setSelected(last7DaysButton, fromDate != null
                && fromDate.equals(LocalDate.now().minusDays(6)));
        setSelected(last30DaysButton, fromDate != null
                && fromDate.equals(LocalDate.now().minusDays(29)));
        setSelected(caneToadButton, "Cane Toad".equals(selectedSpecies));
        setSelected(fireAntButton, "Fire Ant".equals(selectedSpecies));
        setSelected(waterHyacinthButton, "Water Hyacinth".equals(selectedSpecies));
    }

    private static void setSelected(Button button, boolean selected) {
        button.getStyleClass().remove("chip-selected");
        if (selected) {
            button.getStyleClass().add("chip-selected");
        }
    }

    private static String densityClass(int count) {
        if (count >= 4) {
            return "blob-high";
        }
        if (count >= 2) {
            return "blob-mid";
        }
        return "blob-low";
    }

    private static String plural(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    private record HotspotKey(String suburbName, String postcode, ProjectedPoint point) {

        private static HotspotKey from(MapSighting sighting, ProjectedPoint point) {
            return new HotspotKey(sighting.getSuburbName(), sighting.getPostcode(), point);
        }
    }
}
