package com.biodex.controller.pests;

import com.biodex.controller.BaseController;
import com.biodex.dao.SightingReportDAO;
import com.biodex.model.SightingReport;
import com.biodex.routing.Route;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Right pane of Pest Detail: local sighting density by suburb + recent reports.
 * Loads data asynchronously via {@link SightingReportDAO}.
 */
public class LocalSightingsController extends BaseController {

    private static final int WINDOW_DAYS = 30;
    private static final int RECENT_LIMIT = 3;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMM uuuu");

    @FXML private VBox densityCardContainer;
    @FXML private VBox recentReportsContainer;
    @FXML private VBox recentReportsList;
    @FXML private Label emptyStateLabel;

    private final SightingReportDAO reportDAO = new SightingReportDAO();

    @FXML
    private void initialize() {
        // UI setup happens in load()
    }

    /** Loads density cards and recent reports for the given species. */
    public void load(int speciesId) {
        // Density task
        Task<Map<String, Integer>> densityTask = new Task<>() {
            @Override
            protected Map<String, Integer> call() {
                return reportDAO.getDensityBySuburb(speciesId, WINDOW_DAYS);
            }
        };
        densityTask.setOnSucceeded(e -> renderDensityCards(densityTask.getValue()));
        densityTask.setOnFailed(e -> showEmptyState("Could not load local sightings."));
        new Thread(densityTask, "sightings-density-loader").start();

        // Recent reports task
        Task<List<SightingReport>> recentTask = new Task<>() {
            @Override
            protected List<SightingReport> call() {
                return reportDAO.getRecentReports(speciesId, RECENT_LIMIT);
            }
        };
        recentTask.setOnSucceeded(e -> renderRecentReports(recentTask.getValue()));
        recentTask.setOnFailed(e -> showEmptyState("Could not load recent reports."));
        new Thread(recentTask, "sightings-recent-loader").start();
    }

    private void renderDensityCards(Map<String, Integer> counts) {
        densityCardContainer.getChildren().clear();

        if (counts == null || counts.isEmpty()) {
            showEmptyState("No reports in the last 30 days");
            return;
        }

        int maxCount = counts.values().stream().max(Integer::compare).orElse(1);

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            densityCardContainer.getChildren().add(buildDensityCard(entry.getKey(), entry.getValue(), maxCount));
        }
    }

    private VBox buildDensityCard(String suburb, int count, int maxCount) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPrefWidth(350);

        // Suburb name + count
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(suburb);
        name.getStyleClass().add("section-title");
        HBox.setHgrow(name, Priority.ALWAYS);
        Label countLabel = new Label(count + " report" + (count == 1 ? "" : "s"));
        countLabel.getStyleClass().add("muted");
        header.getChildren().addAll(name, countLabel);

        // Visual bar representing relative density
        Region bar = new Region();
        bar.getStyleClass().add("density-bar");
        bar.setPrefHeight(6);
        double ratio = Math.max(0.1, (double) count / maxCount);
        bar.setPrefWidth(300 * ratio);
        bar.setMaxWidth(300 * ratio);

        card.getChildren().addAll(header, bar);
        return card;
    }

    private void renderRecentReports(List<SightingReport> reports) {
        recentReportsList.getChildren().clear();

        if (reports == null || reports.isEmpty()) {
            showEmptyState("No recent reports");
            return;
        }

        recentReportsContainer.setVisible(true);
        emptyStateLabel.setVisible(false);

        for (int i = 0; i < reports.size(); i++) {
            SightingReport r = reports.get(i);
            if (i > 0) {
                Region divider = new Region();
                divider.getStyleClass().add("divider");
                recentReportsList.getChildren().add(divider);
            }
            recentReportsList.getChildren().add(buildReportRow(r));
        }
    }

    private VBox buildReportRow(SightingReport report) {
        VBox row = new VBox(2);

        // Suburb + location
        Label location = new Label(report.getSuburb() + (report.getLocationLabel() != null && !report.getLocationLabel().isBlank()
                ? " — " + report.getLocationLabel() : ""));
        location.getStyleClass().add("section-title");
        location.setWrapText(true);

        // Date + verification badge
        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label(report.getReportedAt() != null
                ? DISPLAY_DATE.format(LocalDate.ofInstant(report.getReportedAt(), ZoneId.systemDefault()))
                : "Unknown date");
        dateLabel.getStyleClass().add("muted");

        Label verifiedLabel = new Label(report.isVerified() ? "Verified" : "Unverified");
        verifiedLabel.getStyleClass().add("threat-chip");
        // Re-add based on verified status
        if (report.isVerified()) {
            verifiedLabel.getStyleClass().add("threat-low");
        } else {
            verifiedLabel.getStyleClass().add("threat-medium");
        }

        meta.getChildren().addAll(dateLabel, verifiedLabel);
        row.getChildren().addAll(location, meta);
        return row;
    }

    private void showEmptyState(String message) {
        densityCardContainer.getChildren().clear();
        recentReportsContainer.setVisible(false);
        emptyStateLabel.setText(message);
        emptyStateLabel.setVisible(true);
    }

    @FXML
    private void handleOpenFullMap() {
        router.go(Route.HEAT_MAP);
    }
}