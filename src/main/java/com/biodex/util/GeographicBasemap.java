package com.biodex.util;

import javafx.concurrent.Worker;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.Objects;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/** Owns the local Leaflet page; saved sighting details never enter the WebView. */
public final class GeographicBasemap {
    private final WebEngine engine;
    private final Label status;
    private final Runnable repositionMarkers;
    // WebEngine holds Java bridge objects weakly, so retain this for the page's lifetime.
    private final Bridge bridge = new Bridge();
    private boolean ready;

    public GeographicBasemap(WebView view, Label status, Runnable repositionMarkers) {
        this.engine = view.getEngine();
        this.status = status;
        this.repositionMarkers = repositionMarkers;
        view.setContextMenuEnabled(false);
        engine.setUserAgent("Biodex/1.0 (CAB302 desktop assignment; JavaFX WebView)");
        engine.getLoadWorker().stateProperty().addListener((observable, previous, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("biodexBridge", bridge);
                    engine.executeScript("biodexMap.start()");
                    ready = true;
                    repositionMarkers.run();
                } catch (RuntimeException exception) {
                    unavailable();
                }
            } else if (state == Worker.State.FAILED || state == Worker.State.CANCELLED) {
                unavailable();
            }
        });
        reload();
    }

    public boolean isReady() {
        return ready;
    }

    public Point2D project(double latitude, double longitude) {
        JSObject map = (JSObject) engine.executeScript("biodexMap");
        JSObject point = (JSObject) map.call("project", latitude, longitude);
        return new Point2D(((Number) point.getMember("x")).doubleValue(),
                ((Number) point.getMember("y")).doubleValue());
    }

    public void reset() {
        if (ready) {
            engine.executeScript("biodexMap.reset()");
        }
    }

    public void reload() {
        ready = false;
        status.setText("Loading map...");
        repositionMarkers.run();
        engine.load(Objects.requireNonNull(GeographicBasemap.class
                .getResource("/com/biodex/map/basemap.html")).toExternalForm());
    }

    private void unavailable() {
        ready = false;
        status.setText("Basemap unavailable. Showing approximate sighting positions. Retry map to reconnect.");
        repositionMarkers.run();
    }

    /** Narrow, public callback surface used only by the bundled page. */
    public final class Bridge {
        public void moved() {
            if (ready) {
                repositionMarkers.run();
            }
        }

        public void openAttribution(String url) {
            if (!url.equals("https://www.openstreetmap.org/copyright")
                    && !url.equals("https://leafletjs.com")) {
                return;
            }
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                } else {
                    status.setText("Map attribution: " + url);
                }
            } catch (IOException | RuntimeException exception) {
                status.setText("Map attribution: " + url);
            }
        }

        public void tileStatus(String message) {
            status.setText(message);
        }
    }
}
