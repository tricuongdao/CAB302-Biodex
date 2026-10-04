package com.biodex.controller.heatmap;

import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.dao.SightingDAO;
import com.biodex.dao.SuburbDAO;
import com.biodex.model.MapSighting;
import com.biodex.model.Suburb;
import com.biodex.routing.Route;
import com.biodex.util.BrisbaneMapProjection;
import com.biodex.util.GeographicBasemap;
import com.biodex.util.BrisbaneMapProjection.ProjectedPoint;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
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
    private ComboBox<SpeciesChoice> speciesFilter;

    @FXML
    private WebView basemapView;

    @FXML
    private Label basemapStatusLabel;

    @FXML
    private TextField areaSearchField;

    @FXML
    private ComboBox<Suburb> areaResults;

    @FXML
    private Label areaSearchStatusLabel;

    private GeographicBasemap basemap;
    private final List<Runnable> markerLayouts = new ArrayList<>();

    private final SightingDAO sightingDAO;
    private final SuburbDAO suburbDAO;
    private Task<List<Suburb>> activeAreaSearch;
    private List<MapSighting> displayedSightings = List.of();
    private boolean sightingsAvailable;

    public HeatMapController() {
        this(new SightingDAO(), new SuburbDAO());
    }

    /** Allows full-screen integration checks with an isolated SQLite database. */
    public HeatMapController(SightingDAO sightingDAO, SuburbDAO suburbDAO) {
        this.sightingDAO = java.util.Objects.requireNonNull(sightingDAO);
        this.suburbDAO = java.util.Objects.requireNonNull(suburbDAO);
    }

    private String selectedSpecies;
    private LocalDate fromDate;
    private Task<MapData> activeLoad;
    private boolean speciesChoicesLoaded;
    private boolean updatingSpeciesChoices;

    @FXML
    private void initialize() {
        sidebarController.setActive("heatmap");
        fromDate = LocalDate.now().minusDays(29);
        speciesFilter.getItems().add(new SpeciesChoice(null));
        speciesFilter.getSelectionModel().selectFirst();
        updateFilterStyles();
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(hotspotLayer.widthProperty());
        clip.heightProperty().bind(hotspotLayer.heightProperty());
        hotspotLayer.setClip(clip);
        hotspotLayer.widthProperty().addListener(observable -> repositionMarkers());
        hotspotLayer.heightProperty().addListener(observable -> repositionMarkers());
        // Retain the prompt when an unselected result picker is laid out or resized.
        areaResults.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Suburb area, boolean empty) {
                super.updateItem(area, empty);
                setText(area == null ? areaResults.getPromptText() : area.toString());
                setGraphic(null);
            }
        });
        areaSearchField.textProperty().addListener((observable, previous, text) -> clearAreaSearch());
        basemap = new GeographicBasemap(basemapView, basemapStatusLabel, () -> {
            repositionMarkers();
            updateAreaResultsAvailability();
        });
        loadSightings();
    }

    private void repositionMarkers() {
        markerLayouts.forEach(Runnable::run);
    }

    @FXML
    private void onResetMap() {
        clearAreaSearch();
        areaSearchField.clear();
        basemap.reset();
    }

    @FXML
    private void onRetryMap() {
        areaResults.getSelectionModel().clearSelection();
        if (!areaResults.getItems().isEmpty()) {
            areaSearchStatusLabel.setText("Choose an area once the map is ready.");
        }
        basemap.reload();
    }

    /** Area search is independent of the sighting filters and never reloads or removes hotspots. */
    @FXML
    private void onSearchArea() {
        clearAreaSearch();
        String query = areaSearchField.getText();
        if (query == null || query.isBlank()) {
            areaSearchStatusLabel.setText("Enter a suburb name or a full postcode.");
            return;
        }
        Task<List<Suburb>> task = new Task<>() {
            @Override
            protected List<Suburb> call() {
                return suburbDAO.searchForMap(query);
            }
        };
        activeAreaSearch = task;
        areaSearchStatusLabel.setText("Searching saved suburbs...");
        updateAreaResultsAvailability();
        task.setOnSucceeded(event -> {
            if (activeAreaSearch != task) return;
            activeAreaSearch = null;
            areaResults.getItems().setAll(task.getValue());
            updateAreaResultsAvailability();
            int count = areaResults.getItems().size();
            areaSearchStatusLabel.setText(count == 0
                    ? "No matching saved suburbs in Greater Brisbane."
                    : count + " " + plural(count, "matching area", "matching areas") + ". Choose one to move the map.");
        });
        task.setOnFailed(event -> {
            if (activeAreaSearch != task) return;
            activeAreaSearch = null;
            areaSearchStatusLabel.setText("Areas could not be loaded. Try Search again.");
            updateAreaResultsAvailability();
        });
        Thread searcher = new Thread(task, "heat-map-area-search");
        searcher.setDaemon(true);
        searcher.start();
    }

    @FXML
    private void onAreaSelected() {
        Suburb area = areaResults.getValue();
        if (area == null) return;
        if (basemap.navigateTo(area.getLatitude(), area.getLongitude())) {
            updateSelectedAreaFeedback();
        } else {
            areaSearchStatusLabel.setText("Map is not ready. Retry map, then choose the area again.");
        }
    }

    private void clearAreaSearch() {
        if (activeAreaSearch != null) {
            activeAreaSearch.cancel();
            activeAreaSearch = null;
        }
        areaResults.getItems().clear();
        areaResults.getSelectionModel().clearSelection();
        areaSearchStatusLabel.setText("Search saved suburbs; selecting an area moves the map.");
        updateAreaResultsAvailability();
    }

    private void updateAreaResultsAvailability() {
        areaResults.setDisable(basemap == null || !basemap.isReady()
                || activeAreaSearch != null || areaResults.getItems().isEmpty());
    }

    private void updateSelectedAreaFeedback() {
        Suburb area = areaResults.getValue();
        if (area == null) return;
        String message;
        if (loadingIndicator.isVisible()) {
            message = "Loading sightings...";
        } else if (!sightingsAvailable) {
            message = "Sightings could not be loaded.";
        } else {
            long count = displayedSightings.stream()
                    .filter(sighting -> java.util.Objects.equals(sighting.getSuburbName(), area.getName())
                            && java.util.Objects.equals(sighting.getPostcode(), area.getPostcode())
                            && sighting.getLatitude() == area.getLatitude()
                            && sighting.getLongitude() == area.getLongitude())
                    .count();
            message = count == 0 ? "No sightings match the current filters."
                    : count + " " + (count == 1 ? "sighting matches" : "sightings match") + " the current filters.";
        }
        areaSearchStatusLabel.setText(area + ": " + message);
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
    private void onSpeciesChanged() {
        if (updatingSpeciesChoices) {
            return;
        }
        SpeciesChoice choice = speciesFilter.getValue();
        String speciesName = choice == null ? null : choice.name();
        if (!java.util.Objects.equals(selectedSpecies, speciesName)) {
            selectedSpecies = speciesName;
            loadSightings();
        }
    }

    @FXML
    private void onClearFilters() {
        selectedSpecies = null;
        fromDate = null;
        updatingSpeciesChoices = true;
        speciesFilter.getSelectionModel().selectFirst();
        updatingSpeciesChoices = false;
        updateFilterStyles();
        loadSightings();
    }

    @FXML
    private void onReportSighting() {
        router.go(Route.IDENTIFY_PEST);
    }

    /** Runs the SQLite query away from the JavaFX application thread. */
    private void loadSightings() {
        if (activeLoad != null) {
            activeLoad.cancel();
        }

        String speciesSnapshot = selectedSpecies;
        LocalDate dateSnapshot = fromDate;
        boolean loadSpeciesChoices = !speciesChoicesLoaded;
        Task<MapData> task = new Task<>() {
            @Override
            protected MapData call() {
                // Demo filler: make sure the sample suburbs and sightings exist before loading.
                sightingDAO.seedDemoDataIfEmpty();
                List<String> species = loadSpeciesChoices ? sightingDAO.findSpeciesForMap() : null;
                return new MapData(species, sightingDAO.findForMap(speciesSnapshot, dateSnapshot));
            }
        };
        activeLoad = task;

        setLoading(true);
        task.setOnSucceeded(event -> {
            if (activeLoad == task) {
                MapData data = task.getValue();
                if (data.species() != null) {
                    updatingSpeciesChoices = true;
                    try {
                        speciesFilter.getItems().setAll(new SpeciesChoice(null));
                        data.species().forEach(name -> speciesFilter.getItems().add(new SpeciesChoice(name)));
                        speciesFilter.getSelectionModel().selectFirst();
                        speciesChoicesLoaded = true;
                    } finally {
                        updatingSpeciesChoices = false;
                    }
                }
                setLoading(false);
                renderSightings(data.sightings());
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
        displayedSightings = sightings;
        sightingsAvailable = true;
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
        updateSelectedAreaFeedback();
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
        speciesFilter.setDisable(loading || !speciesChoicesLoaded);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        if (loading) {
            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);
            summaryLabel.setText("Loading saved sightings...");
        }
        updateSelectedAreaFeedback();
    }

    private void showLoadError() {
        displayedSightings = List.of();
        sightingsAvailable = false;
        markerLayouts.clear();
        hotspotLayer.getChildren().clear();
        summaryLabel.setText("Saved sightings could not be loaded");
        emptyStateLabel.setText("Something went wrong while reading the Biodex database.");
        emptyStateLabel.setVisible(true);
        emptyStateLabel.setManaged(true);
        resetDetails();
        updateSelectedAreaFeedback();
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
    }

    private record MapData(List<String> species, List<MapSighting> sightings) {
    }

    /** A separate all-species option avoids treating any stored species name as a special value. */
    private record SpeciesChoice(String name) {
        @Override
        public String toString() {
            return name == null ? "All species" : name;
        }
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
