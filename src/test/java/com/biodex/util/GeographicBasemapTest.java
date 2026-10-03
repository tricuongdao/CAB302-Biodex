package com.biodex.util;

import com.biodex.controller.heatmap.HeatMapController;
import com.biodex.dao.SightingDAO;
import com.biodex.dao.SuburbDAO;
import com.biodex.db.InMemoryDatabase;
import com.biodex.model.Suburb;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import netscape.javascript.JSObject;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Objects;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** Optional desktop integration test: -Dbiodex.test.webview=true (requires a display). */
@EnabledIfSystemProperty(named = "biodex.test.webview", matches = "true")
class GeographicBasemapTest {
    private Stage stage;
    private WebView view;
    private GeographicBasemap basemap;
    private Label status;
    private final AtomicInteger moves = new AtomicInteger();

    @Test
    void bundledMapProjectsCoordinatesAndKeepsBridgeAcrossNavigationAndReload() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS));
        try {
            fx(() -> {
                view = new WebView();
                status = new Label();
                stage = new Stage();
                stage.setScene(new Scene(new StackPane(view), 700, 500));
                stage.show();
                basemap = new GeographicBasemap(view, status, moves::incrementAndGet);
            });
            await(() -> basemap.isReady());
            fx(() -> {
                assertFalse(basemap.navigateTo(Double.NaN, 153.0251));
                assertFalse(basemap.navigateTo(-33.86, 151.21));
                Point2D brisbane = basemap.project(-27.4698, 153.0251);
                assertTrue(brisbane.getX() > 0 && brisbane.getX() < view.getWidth());
                assertTrue(brisbane.getY() > 0 && brisbane.getY() < view.getHeight());
                Point2D east = basemap.project(-27.4698, 153.1251);
                Point2D south = basemap.project(-27.5698, 153.0251);
                assertTrue(east.getX() > brisbane.getX());
                assertTrue(south.getY() > brisbane.getY());
                int before = moves.get();
                view.getEngine().executeScript("document.querySelector('.leaflet-control-zoom-in').click()");
                assertTrue(moves.get() > before, "Zoom must notify the JavaFX overlay");
                assertNotEquals(brisbane, basemap.project(-27.4698, 153.0251));
                basemap.reset();
                assertEquals(brisbane, basemap.project(-27.4698, 153.0251));
                // Simulate blocked tiles: the bridge must leave geographic projection usable.
                view.getEngine().executeScript("biodexBridge.tileStatus('Map tiles unavailable')");
                assertEquals("Map tiles unavailable", status.getText());
                assertTrue(basemap.isReady());
                stage.setWidth(900);
            });
            await(() -> view.getWidth() > 800);
            fx(() -> {
                assertEquals(view.getWidth(), ((Number) view.getEngine()
                        .executeScript("document.getElementById('map').clientWidth")).doubleValue(), 1);
                basemap.reload();
            });
            await(() -> basemap.isReady());
            fx(() -> assertNotNull(basemap.project(-27.4698, 153.0251)));
            verifyFullHeatMapScreen();
            verifyEmptyScreenAndLoadRecovery();
            verifyAreaSearchRecovery();
        } finally {
            fx(() -> { if (stage != null) stage.close(); });
            Platform.exit();
        }
    }

    private void verifyFullHeatMapScreen() throws Exception {
        try (var connection = InMemoryDatabase.open()) {
            try (var sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO users (username,email,password_hash) VALUES ('map-test','map@example.test','test')");
                sql.executeUpdate("INSERT INTO suburbs (name,postcode,latitude,longitude) VALUES ('Brisbane','4000',-27.4698,153.0251)");
                sql.executeUpdate("INSERT INTO suburbs (name,postcode,latitude,longitude) VALUES ('Bardon','4065',-27.46,152.98), ('Rainworth','4065',-27.47,152.98)");
                sql.executeUpdate("INSERT INTO suburbs (name,postcode,latitude,longitude) VALUES ('City Without Reports','4001',-27.4698,153.0251)");
                sql.executeUpdate("INSERT INTO sightings (user_id,suburb_id,species_name,sighted_at) VALUES (1,1,'Cane Toad',date('now'))");
                sql.executeUpdate("INSERT INTO sightings (user_id,suburb_id,species_name,sighted_at) VALUES (1,1,'Fire Ant',date('now')), (1,1,'Water Hyacinth',date('now')), (1,1,'Lantana',date('now','-10 days')), (1,1,'Lantana',date('now','-40 days'))");
            }
            showHeatMap(new SightingDAO(connection) {
                @Override
                public List<String> findSpeciesForMap() {
                    assertFalse(Platform.isFxApplicationThread(), "Species lookup must run off the FX thread");
                    return super.findSpeciesForMap();
                }
            }, new SuburbDAO(connection) {
                @Override
                public List<Suburb> searchForMap(String query) {
                    assertFalse(Platform.isFxApplicationThread(), "Area search must run off the FX thread");
                    return super.searchForMap(query);
                }
            });
            await(() -> view.getEngine().getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED
                    && stage.getScene().getRoot().lookup(".hotspot-marker") != null);
            verifySpeciesFiltering();
            verifyAreaNavigation();
            fx(() -> {
                Parent root = stage.getScene().getRoot();
                root.applyCss();
                root.layout();
                Region legend = (Region) root.lookup(".map-legend");
                assertTrue(legend.isMouseTransparent(), "Legend must let map clicks through");
                assertTrue(legend.getHeight() < 180, "Legend must not stretch across the zoom controls");
                assertTrue(root.lookup("#emptyStateLabel").isMouseTransparent());
                assertTrue(root.lookup("#loadingIndicator").isMouseTransparent());
                assertMarkerAligned(root);
                view.getEngine().executeScript("document.querySelector('.leaflet-control-zoom-in').click()");
                assertMarkerAligned(root);
                view.getEngine().executeScript("document.querySelector('.leaflet-control-zoom-out').click()");
                assertMarkerAligned(root);
                assertTrue((Boolean) view.getEngine().executeScript("""
                    (function () {
                        var map = document.getElementById('map').getBoundingClientRect();
                        var tiles = Array.from(document.querySelectorAll('.leaflet-tile')).map(function (tile) {
                            return tile.getBoundingClientRect();
                        });
                        // All viewport samples need a requested tile, including outside Brisbane's bounds.
                        for (var y = map.top + 2; y < map.bottom; y += 32) {
                            for (var x = map.left + 2; x < map.right; x += 32) {
                                if (!tiles.some(function (tile) {
                                    return x >= tile.left && x < tile.right && y >= tile.top && y < tile.bottom;
                                })) return false;
                            }
                        }
                        return true;
                    }())
                    """), "Tile coverage must fill the viewport instead of stopping at suburb validation bounds");
            });
            awaitLiveTiles();
            String snapshot = System.getProperty("biodex.test.snapshot");
            if (snapshot != null || Boolean.getBoolean("biodex.test.tiles")) {
                fx(() -> {
                    var image = stage.getScene().getRoot().snapshot(null, null);
                    if (Boolean.getBoolean("biodex.test.tiles")) {
                        // Loaded image elements alone missed WebView's 3D tile painting bug.
                        // Reject a map still exposing large areas of its blank placeholder colour.
                        var bounds = view.localToScene(view.getBoundsInLocal());
                        int blank = 0;
                        int samples = 0;
                        for (int y = (int) bounds.getMinY() + 2; y < bounds.getMaxY() - 2; y += 4) {
                            for (int x = (int) bounds.getMinX() + 2; x < bounds.getMaxX() - 2; x += 4) {
                                samples++;
                                if ((image.getPixelReader().getArgb(x, y) & 0xFFFFFF) == 0xE4E9E4) blank++;
                            }
                        }
                        assertTrue(blank < samples * 0.05, "Loaded tiles must actually paint the map viewport");
                    }
                    if (snapshot == null) return;
                    saveSnapshot(image, snapshot);
                });
            }
            searchArea("4065");
            await(() -> areaResults().getItems().size() == 2 && !areaResults().isDisabled());
            verifyThemesAndResize(snapshot);
        }
    }

    private void showHeatMap(SightingDAO dao, SuburbDAO suburbDAO) throws Exception {
        fx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/biodex/fxml/heatmap/HeatMapView.fxml"));
                loader.setControllerFactory(type -> {
                    try {
                        return type == HeatMapController.class
                                ? new HeatMapController(dao, suburbDAO) : type.getDeclaredConstructor().newInstance();
                    } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException(exception);
                    }
                });
                Parent root = loader.load();
                Scene scene = new Scene(root, 1200, 760);
                scene.getStylesheets().add(Objects.requireNonNull(
                        getClass().getResource("/com/biodex/css/light-theme.css")).toExternalForm());
                stage.setScene(scene);
                stage.sizeToScene();
                root.applyCss();
                root.layout();
                view = (WebView) root.lookup("#basemapView");
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private void verifyThemesAndResize(String snapshot) throws Exception {
        for (String theme : List.of("light", "dark")) {
            fx(() -> {
                Parent root = stage.getScene().getRoot();
                stage.getScene().getStylesheets().setAll(Objects.requireNonNull(getClass()
                        .getResource("/com/biodex/css/" + theme + "-theme.css")).toExternalForm());
                root.applyCss();
                root.layout();
            });
            awaitViewportResize();
            fx(() -> {
                Parent root = stage.getScene().getRoot();
                if (snapshot != null) saveSnapshot(root.snapshot(null, null), snapshot + "." + theme + ".png");
                speciesFilter().show();
            });
            fx(() -> {
                Node popup = ((ComboBoxListViewSkin<?>) speciesFilter().getSkin()).getPopupContent();
                popup.applyCss();
                ((Parent) popup).layout();
                Color expected = Color.web(theme.equals("dark") ? "#e9e6d8" : "#2e2b1f");
                var visibleCells = popup.lookupAll(".list-cell").stream()
                        .filter(node -> node instanceof ListCell<?> cell && !cell.isEmpty())
                        .map(node -> (ListCell<?>) node)
                        .toList();
                visibleCells.forEach(cell -> assertEquals(expected, cell.getTextFill(),
                        "Popup text must follow the active theme"));
                assertTrue(visibleCells.size() >= 5, "All fixture choices must be visible in the popup");
                if (snapshot != null) saveSnapshot(popup.snapshot(null, null), snapshot + "." + theme + "-choices.png");
                speciesFilter().hide();
                areaResults().show();
            });
            fx(() -> {
                Node popup = ((ComboBoxListViewSkin<?>) areaResults().getSkin()).getPopupContent();
                popup.applyCss();
                ((Parent) popup).layout();
                Color expected = Color.web(theme.equals("dark") ? "#e9e6d8" : "#2e2b1f");
                var cells = popup.lookupAll(".list-cell").stream()
                        .filter(node -> node instanceof ListCell<?> cell && !cell.isEmpty())
                        .map(node -> (ListCell<?>) node).toList();
                assertEquals(2, cells.size());
                cells.forEach(cell -> assertEquals(expected, cell.getTextFill(), "Area choices must follow the theme"));
                if (snapshot != null) saveSnapshot(popup.snapshot(null, null), snapshot + "." + theme + "-areas.png");
                areaResults().hide();
                stage.setWidth(1050);
                stage.setHeight(680);
            });
            await(() -> view.getWidth() < 550);
            awaitViewportResize();
            fx(() -> {
                Parent root = stage.getScene().getRoot();
                root.applyCss();
                root.layout();
                assertMarkerAligned(root);
                var filterBounds = speciesFilter().localToScene(speciesFilter().getBoundsInLocal());
                assertTrue(filterBounds.getMaxX() <= stage.getScene().getWidth(), "Dropdown must fit after resizing");
                var areaBounds = areaResults().localToScene(areaResults().getBoundsInLocal());
                assertTrue(areaBounds.getMaxX() <= stage.getScene().getWidth(), "Area results must fit after resizing");
                assertEquals("Choose a matching area", areaResults().getButtonCell().getText(),
                        "Area prompt must survive layout and resizing");
                assertTrue(view.getHeight() > 350, "Search controls must leave useful map space");
                if (snapshot != null) saveSnapshot(root.snapshot(null, null), snapshot + "." + theme + "-resized.png");
                stage.setWidth(1200);
                stage.setHeight(790);
            });
            await(() -> view.getWidth() > 550);
            awaitViewportResize();
        }
    }

    /** Navigation keeps the existing sighting filters and overlays; areas without reports are searchable. */
    private void verifyAreaNavigation() throws Exception {
        fx(() -> {
            speciesFilter().getSelectionModel().select(3); // Lantana
            ((Button) stage.getScene().getRoot().lookup("#last30DaysButton")).fire();
        });
        awaitSummary("1 sighting -");
        searchArea(" 4065 ");
        await(() -> areaResults().getItems().size() == 2 && !areaResults().isDisabled());
        fx(() -> {
            assertEquals(List.of("Bardon, 4065", "Rainworth, 4065"),
                    areaResults().getItems().stream().map(Object::toString).toList());
            assertTrue(areaResults().lookupAll(".list-cell").stream()
                            .anyMatch(node -> node instanceof ListCell<?> cell
                                    && "Choose a matching area".equals(cell.getText())),
                    "The unopened result picker must display its prompt");
            Parent root = stage.getScene().getRoot();
            Node marker = root.lookup(".hotspot-marker");
            areaResults().getSelectionModel().selectFirst();
            assertCoordinateAtCentre(-27.46, 152.98);
            assertSame(marker, root.lookup(".hotspot-marker"), "Navigation must not rebuild/remove hotspots");
            assertMarkerAligned(root);
            assertEquals("Lantana", speciesFilter().getValue().toString());
            assertTrue(root.lookup("#last30DaysButton").getStyleClass().contains("chip-selected"));
            assertTrue(((Label) root.lookup("#summaryLabel")).getText().startsWith("1 sighting -"));
            assertEquals("Bardon, 4065: No sightings match the current filters.", areaSearchStatus().getText());
        });
        awaitLiveTiles();
        fx(() -> {
            Parent root = stage.getScene().getRoot();
            assertCoordinateAtCentre(-27.46, 152.98);
            assertMarkerAligned(root);
            String snapshot = System.getProperty("biodex.test.snapshot");
            if (snapshot != null) saveSnapshot(root.snapshot(null, null), snapshot + ".navigation.png");
            ((Button) root.lookup("#last7DaysButton")).fire();
        });
        awaitSummary("0 sightings");
        fx(() -> assertCoordinateAtCentre(-27.46, 152.98));
        searchArea("bRiS"); // Enter in the text field, as well as the Search button below.
        await(() -> areaResults().getItems().size() == 1 && !areaResults().isDisabled());
        fx(() -> {
            areaResults().getSelectionModel().selectFirst();
            assertCoordinateAtCentre(-27.4698, 153.0251);
            assertTrue(areaSearchStatus().getText().contains("No sightings match"));
            ((Button) stage.getScene().getRoot().lookup("#last30DaysButton")).fire();
        });
        awaitSummary("1 sighting -");
        fx(() -> {
            assertEquals("Brisbane, 4000: 1 sighting matches the current filters.", areaSearchStatus().getText());
        });
        searchArea("4001");
        await(() -> areaResults().getItems().size() == 1 && !areaResults().isDisabled());
        fx(() -> {
            areaResults().getSelectionModel().selectFirst();
            assertTrue(areaSearchStatus().getText().contains("No sightings match"),
                    "Neighbouring areas sharing coordinates must not borrow each other's sightings");
            ((Button) stage.getScene().getRoot().lookup("#resetMapButton")).fire();
            assertTrue(areaResults().getItems().isEmpty());
            assertTrue(areaSearchField().getText().isEmpty());
            assertTrue(areaResults().isDisabled());
            assertEquals("Lantana", speciesFilter().getValue().toString());
            assertTrue(stage.getScene().getRoot().lookup("#last30DaysButton").getStyleClass().contains("chip-selected"));
        });
        searchArea("Nowhere");
        await(() -> areaSearchStatus().getText().startsWith("No matching saved suburbs"));
        fx(() -> {
            assertTrue(areaResults().getItems().isEmpty());
            assertTrue(areaResults().isDisabled());
            areaSearchField().setText(" ");
            ((Button) stage.getScene().getRoot().lookup("#areaSearchButton")).fire();
            assertEquals("Enter a suburb name or a full postcode.", areaSearchStatus().getText());
            ((Button) stage.getScene().getRoot().lookup("#clearFiltersButton")).fire();
        });
        awaitSummary("5 sightings");
    }

    private void verifyAreaSearchRecovery() throws Exception {
        try (var connection = InMemoryDatabase.open()) {
            try (var sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO suburbs (name,postcode,latitude,longitude) VALUES ('Brisbane','4000',-27.4698,153.0251), ('Bardon','4065',-27.46,152.98)");
            }
            var failSearch = new java.util.concurrent.atomic.AtomicBoolean(true);
            CountDownLatch oldStarted = new CountDownLatch(1);
            CountDownLatch releaseOld = new CountDownLatch(1);
            CountDownLatch oldFinished = new CountDownLatch(1);
            showHeatMap(new SightingDAO(connection), new SuburbDAO(connection) {
                @Override
                public List<Suburb> searchForMap(String query) {
                    if (failSearch.get()) throw new IllegalStateException("Simulated area lookup failure");
                    if (query.equals("slow")) {
                        oldStarted.countDown();
                        // Simulate a query that cannot be interrupted; its late result must stay discarded.
                        boolean released = false;
                        while (!released) {
                            try {
                                releaseOld.await();
                                released = true;
                            } catch (InterruptedException ignored) {
                                // Continue until the test lets the old query finish.
                            }
                        }
                        try {
                            return super.searchForMap("4000");
                        } finally {
                            oldFinished.countDown();
                        }
                    }
                    return super.searchForMap(query);
                }
            });
            awaitSummary("0 sightings");
            searchArea("4000");
            await(() -> areaSearchStatus().getText().equals("Areas could not be loaded. Try Search again."));
            fx(() -> {
                assertTrue(areaResults().isDisabled());
                assertFalse(speciesFilter().isDisabled(), "Search failure must not disable sighting filters");
            });
            failSearch.set(false);
            searchArea("4000");
            await(() -> areaResults().getItems().size() == 1 && !areaResults().isDisabled());
            fx(() -> {
                areaResults().getSelectionModel().selectFirst();
                assertCoordinateAtCentre(-27.4698, 153.0251);
                assertTrue(areaSearchStatus().getText().contains("No sightings match"));
                assertTrue(stage.getScene().getRoot().lookup("#emptyStateLabel").isVisible());
                ((Button) stage.getScene().getRoot().lookup("#retryMapButton")).fire();
                assertTrue(areaResults().isDisabled(), "Navigation must wait for the reloaded map");
            });
            await(() -> !areaResults().isDisabled());
            fx(() -> assertNull(areaResults().getValue(), "Retry must not leave a stale area selection"));
            searchArea("slow");
            try {
                assertTrue(oldStarted.await(5, TimeUnit.SECONDS));
                fx(() -> {
                    areaSearchField().setText("4065");
                    assertTrue(areaResults().getItems().isEmpty(), "Editing clears stale choices");
                    assertTrue(areaResults().isDisabled());
                    ((Button) stage.getScene().getRoot().lookup("#areaSearchButton")).fire();
                });
                await(() -> areaResults().getItems().size() == 1 && !areaResults().isDisabled());
            } finally {
                releaseOld.countDown();
            }
            assertTrue(oldFinished.await(5, TimeUnit.SECONDS));
            fx(() -> assertEquals("Bardon, 4065", areaResults().getItems().get(0).toString(),
                    "Late results from the cancelled search must not replace the new query"));
        }
    }

    private void searchArea(String query) throws Exception {
        fx(() -> {
            areaSearchField().setText(query);
            areaSearchField().fireEvent(new javafx.event.ActionEvent());
        });
    }

    private void assertCoordinateAtCentre(double latitude, double longitude) {
        JSObject point = (JSObject) view.getEngine().executeScript("biodexMap.project(" + latitude + "," + longitude + ")");
        assertEquals(view.getWidth() / 2, ((Number) point.getMember("x")).doubleValue(), 1);
        assertEquals(view.getHeight() / 2, ((Number) point.getMember("y")).doubleValue(), 1);
    }

    private TextField areaSearchField() {
        return (TextField) stage.getScene().getRoot().lookup("#areaSearchField");
    }

    private ComboBox<?> areaResults() {
        return (ComboBox<?>) stage.getScene().getRoot().lookup("#areaResults");
    }

    private Label areaSearchStatus() {
        return (Label) stage.getScene().getRoot().lookup("#areaSearchStatusLabel");
    }

    private void awaitViewportResize() throws Exception {
        await(() -> Math.abs(view.getWidth() - ((Number) view.getEngine()
                .executeScript("document.getElementById('map').clientWidth")).doubleValue()) <= 1);
    }

    private void awaitLiveTiles() throws Exception {
        if (!Boolean.getBoolean("biodex.test.tiles")) return;
        await(() -> (Boolean) view.getEngine().executeScript("""
            (function () {
                var tiles = Array.from(document.querySelectorAll('.leaflet-tile'));
                return tiles.length > 0 && tiles.every(function (tile) {
                    return tile.complete && tile.naturalWidth > 0;
                });
            }())
            """));
        // Image loading can finish before WebView paints its next frame.
        await(this::tilesHavePainted);
    }

    private boolean tilesHavePainted() {
        WritableImage image = view.snapshot(null, null);
        int blank = 0;
        int samples = 0;
        for (int y = 2; y < image.getHeight() - 2; y += 4) {
            for (int x = 2; x < image.getWidth() - 2; x += 4) {
                samples++;
                if ((image.getPixelReader().getArgb(x, y) & 0xFFFFFF) == 0xE4E9E4) blank++;
            }
        }
        return samples > 0 && blank < samples * 0.05;
    }

    private void verifyEmptyScreenAndLoadRecovery() throws Exception {
        try (var connection = InMemoryDatabase.open()) {
            java.util.concurrent.atomic.AtomicBoolean failLookup = new java.util.concurrent.atomic.AtomicBoolean(true);
            showHeatMap(new SightingDAO(connection) {
                @Override
                public List<String> findSpeciesForMap() {
                    if (failLookup.get()) throw new IllegalStateException("Simulated database failure");
                    return super.findSpeciesForMap();
                }
            }, new SuburbDAO(connection));
            await(() -> ((Label) stage.getScene().getRoot().lookup("#summaryLabel"))
                    .getText().equals("Saved sightings could not be loaded"));
            fx(() -> {
                assertTrue(speciesFilter().isDisabled(), "Failed species lookup must not expose incomplete choices");
                failLookup.set(false);
                ((Button) stage.getScene().getRoot().lookup("#clearFiltersButton")).fire();
                assertTrue(speciesFilter().isDisabled(), "Dropdown must be disabled while loading");
            });
            awaitSummary("0 sightings");
            fx(() -> {
                assertEquals(List.of("All species"), speciesFilter().getItems().stream().map(Object::toString).toList());
                assertEquals("No saved sightings yet. Report one to add it to the map.",
                        ((Label) stage.getScene().getRoot().lookup("#emptyStateLabel")).getText());
            });
        }
    }

    private static void saveSnapshot(WritableImage image, String path) {
        var png = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++) {
            for (int x = 0; x < png.getWidth(); x++) {
                png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        try {
            javax.imageio.ImageIO.write(png, "png", new java.io.File(path));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** Exercises the dropdown through the actual FXML/controller and SQLite fixture. */
    private void verifySpeciesFiltering() throws Exception {
        awaitSummary("4 sightings");
        fx(() -> {
            ComboBox<?> filter = speciesFilter();
            Label speciesLabel = stage.getScene().getRoot().lookupAll(".label").stream()
                    .filter(node -> node instanceof Label label && "SPECIES".equals(label.getText()))
                    .map(node -> (Label) node).findFirst().orElseThrow();
            assertSame(filter, speciesLabel.getLabelFor(), "Species label must be linked to its dropdown");
            assertEquals(List.of("All species", "Cane Toad", "Fire Ant", "Lantana", "Water Hyacinth"),
                    filter.getItems().stream().map(Object::toString).toList());
            assertFalse(filter.isDisabled());
            filter.getSelectionModel().select(3); // Fourth species beyond the original buttons.
        });
        awaitSummary("1 sighting -");
        fx(() -> ((Button) stage.getScene().getRoot().lookup("#last7DaysButton")).fire());
        awaitSummary("0 sightings");
        fx(() -> {
            Label empty = (Label) stage.getScene().getRoot().lookup("#emptyStateLabel");
            assertTrue(empty.isVisible());
            assertEquals("No saved sightings match the selected filters.", empty.getText());
            assertEquals("Lantana", speciesFilter().getValue().toString());
            assertEquals(5, speciesFilter().getItems().size(), "Date filters must not remove choices");
            ((Button) stage.getScene().getRoot().lookup("#last30DaysButton")).fire();
        });
        awaitSummary("1 sighting -");
        fx(() -> speciesFilter().getSelectionModel().selectFirst());
        awaitSummary("4 sightings");
        fx(() -> ((Button) stage.getScene().getRoot().lookup("#clearFiltersButton")).fire());
        awaitSummary("5 sightings");
        fx(() -> {
            assertEquals("All species", speciesFilter().getValue().toString());
            assertFalse(stage.getScene().getRoot().lookup("#last30DaysButton")
                    .getStyleClass().contains("chip-selected"));
            speciesFilter().getSelectionModel().select(3);
        });
        awaitSummary("2 sightings");
        fx(() -> ((Button) stage.getScene().getRoot().lookup("#clearFiltersButton")).fire());
        awaitSummary("5 sightings");
    }

    private ComboBox<?> speciesFilter() {
        return (ComboBox<?>) stage.getScene().getRoot().lookup("#speciesFilter");
    }

    private void awaitSummary(String prefix) throws Exception {
        await(() -> ((Label) stage.getScene().getRoot().lookup("#summaryLabel"))
                .getText().startsWith(prefix) && !speciesFilter().isDisabled());
    }

    private void assertMarkerAligned(Parent root) {
        Pane overlay = (Pane) root.lookup("#hotspotLayer");
        StackPane marker = (StackPane) overlay.getChildren().get(0);
        JSObject point = (JSObject) view.getEngine().executeScript("biodexMap.project(-27.4698,153.0251)");
        assertEquals(((Number) point.getMember("x")).doubleValue(), marker.getLayoutX() + marker.getWidth() / 2, 1);
        assertEquals(((Number) point.getMember("y")).doubleValue(), marker.getLayoutY() + marker.getHeight() / 2, 1);
    }

    private static void fx(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            FutureTask<Boolean> check = new FutureTask<>(condition::getAsBoolean);
            Platform.runLater(check);
            if (check.get(5, TimeUnit.SECONDS)) return;
            Thread.sleep(50);
        }
        fail("Timed out waiting for the bundled WebView map");
    }
}
