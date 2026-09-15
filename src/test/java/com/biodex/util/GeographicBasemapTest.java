package com.biodex.util;

import com.biodex.controller.heatmap.HeatMapController;
import com.biodex.dao.SightingDAO;
import com.biodex.db.InMemoryDatabase;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import netscape.javascript.JSObject;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Objects;
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
                sql.executeUpdate("INSERT INTO sightings (user_id,suburb_id,species_name,sighted_at) VALUES (1,1,'Cane Toad',date('now'))");
            }
            fx(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/biodex/fxml/heatmap/HeatMapView.fxml"));
                    loader.setControllerFactory(type -> {
                        try {
                            return type == HeatMapController.class
                                    ? new HeatMapController(new SightingDAO(connection))
                                    : type.getDeclaredConstructor().newInstance();
                        } catch (ReflectiveOperationException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 1200, 760);
                    scene.getStylesheets().add(Objects.requireNonNull(
                            getClass().getResource("/com/biodex/css/light-theme.css"),
                            "Light theme stylesheet must be bundled").toExternalForm());
                    stage.setScene(scene);
                    stage.sizeToScene();
                    root.applyCss();
                    root.layout();
                    view = (WebView) root.lookup("#basemapView");
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            await(() -> view.getEngine().getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED
                    && stage.getScene().getRoot().lookup(".hotspot-marker") != null);
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
            if (Boolean.getBoolean("biodex.test.tiles")) {
                await(() -> (Boolean) view.getEngine().executeScript("""
                    (function () {
                        var tiles = Array.from(document.querySelectorAll('.leaflet-tile'));
                        return tiles.length > 0 && tiles.every(function (tile) {
                            return tile.complete && tile.naturalWidth > 0;
                        });
                    }())
                    """));
            }
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
                    var png = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < png.getHeight(); y++) {
                        for (int x = 0; x < png.getWidth(); x++) {
                            png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
                        }
                    }
                    try {
                        javax.imageio.ImageIO.write(png, "png", new java.io.File(snapshot));
                    } catch (java.io.IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                });
            }
        }
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
