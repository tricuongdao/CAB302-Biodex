package com.biodex.controller.pests;

import com.biodex.api.ServiceFactory;
import com.biodex.api.SpeciesService;
import com.biodex.api.dto.OccurrencePoint;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.routing.Route;
import com.biodex.session.SpeciesSelection;
import com.biodex.util.BrisbaneMapProjection;
import com.biodex.util.BrisbaneMapProjection.ProjectedPoint;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Single pest page.
 *
 * <p>The species shown is whatever {@link SpeciesSelection} holds — Pest Details sets it before
 * routing here, and a cold open with nothing picked shows an empty state. The profile and the
 * Brisbane-area
 * occurrence records both load from the ALA species service on background tasks, so a slow or
 * failing network leaves the page usable: fields keep their loading text and the mini-map simply
 * shows fewer dots.
 */
public class PestDetailController extends BaseController {

    /** Date format for the recent-records list, matching the heat map. */
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMM uuuu");

    /** Occurrence searches centre on the Brisbane CBD. */
    private static final double BRISBANE_LAT = -27.47;
    private static final double BRISBANE_LON = 153.03;
    private static final double SEARCH_RADIUS_KM = 50;

    /** How many dots are drawn at most; beyond that the mini-map is unreadable. */
    private static final int MAX_DOTS = 150;

    /** Injected from the fx:include with fx:id="sidebar" in PestDetailView.fxml. */
    @FXML
    private SidebarController sidebarController;
    @FXML
    private Label breadcrumbLabel;
    @FXML
    private Region photoPlaceholder;
    @FXML
    private ImageView photoView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label sciNameLabel;
    @FXML
    private Label threatChip;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label alaRecordsLabel;
    @FXML
    private Pane sightingsMap;
    @FXML
    private VBox recentSightingsBox;

    private final SpeciesService speciesService = ServiceFactory.speciesService();

    private Task<SpeciesProfile> activeProfile;
    private Task<List<OccurrencePoint>> activeOccurrences;

    @FXML
    private void initialize() {
        sidebarController.setActive("pests");
        SpeciesSummary selection = SpeciesSelection.getInstance().getCurrent();
        if (selection == null) {
            showNoSelection();
            return;
        }
        nameLabel.setText(displayName(selection));
        sciNameLabel.setText(orBlank(selection.getScientificName()));
        breadcrumbLabel.setText("/ " + displayName(selection));
        loadProfile(selection);
        loadOccurrences(selection);
    }

    /** The screen opened without a species picked from Pest Details. */
    private void showNoSelection() {
        nameLabel.setText("No species selected");
        sciNameLabel.setText("");
        breadcrumbLabel.setText("/ nothing selected");
        threatChip.setVisible(false);
        descriptionLabel.setText(
                "Pick a species from the Pest details search and its Atlas record will load here.");
        photoView.setVisible(false);
        alaRecordsLabel.setText("");
        sightingsMap.getChildren().clear();
        recentSightingsBox.getChildren().clear();
        recentSightingsBox.getChildren().add(
                placeholderRow("No species chosen yet - open Pest details to search."));
    }

    @FXML
    private void onBackToHeatMap() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onReportSighting() {
        router.go(Route.IDENTIFY_PEST);
    }

    // ---------------------------------------------------------------- profile

    /** Loads the species profile away from the FX thread, resolving the guid by name if needed. */
    private void loadProfile(SpeciesSummary selection) {
        if (activeProfile != null) {
            activeProfile.cancel();
        }
        Task<SpeciesProfile> task = new Task<>() {
            @Override
            protected SpeciesProfile call() {
                return resolveProfile(selection);
            }
        };
        activeProfile = task;
        task.setOnSucceeded(event -> {
            if (activeProfile == task) {
                showProfile(task.getValue(), selection);
            }
        });
        task.setOnFailed(event -> {
            if (activeProfile == task) {
                descriptionLabel.setText("Species description could not be loaded.");
            }
        });

        Thread loader = new Thread(task, "species-profile-loader");
        loader.setDaemon(true);
        loader.start();
    }

    /**
     * Looks the profile up by guid, falling back to a name search for services that do not know the
     * stored guid — the offline fake service only answers to its own guids, and the Atlas answer is
     * then cached for next time.
     */
    private SpeciesProfile resolveProfile(SpeciesSummary selection) {
        SpeciesProfile profile = speciesService.profile(selection.getGuid());
        if (profile != null) {
            return profile;
        }
        String query = selection.getCommonName() != null
                ? selection.getCommonName()
                : selection.getScientificName();
        if (query == null) {
            return null;
        }
        List<SpeciesSummary> matches = speciesService.autocomplete(query, 1);
        if (matches.isEmpty()) {
            return null;
        }
        SpeciesSummary resolved = matches.get(0);
        SpeciesSelection.getInstance().setCurrent(resolved);
        return speciesService.profile(resolved.getGuid());
    }

    private void showProfile(SpeciesProfile profile, SpeciesSummary fallback) {
        if (profile == null) {
            descriptionLabel.setText("Species description could not be loaded.");
            return;
        }
        String title = firstNonBlank(profile.getCommonName(), displayName(fallback));
        nameLabel.setText(title);
        router.setTitle("Biodex - " + title);
        if (profile.getScientificName() != null) {
            sciNameLabel.setText(profile.getScientificName());
            breadcrumbLabel.setText("/ " + profile.getScientificName());
        }
        descriptionLabel.setText(profile.getDescription() != null
                ? profile.getDescription()
                : "No description is available for this species.");

        // The Atlas has no true threat level; it either lists the species as a pest/invasive or
        // it does not, so the chip says exactly that rather than inventing a severity.
        threatChip.getStyleClass().removeAll("threat-high", "threat-medium", "threat-low");
        threatChip.setVisible(true);
        if (profile.isInvasive()) {
            threatChip.setText("Invasive pest");
            threatChip.getStyleClass().add("threat-high");
        } else {
            threatChip.setText("No pest listing");
            threatChip.getStyleClass().add("threat-medium");
        }

        if (profile.getImageUrl() != null) {
            // Background loading: the placeholder shows through until the pixels arrive.
            photoView.setImage(new Image(profile.getImageUrl(), true));
            photoView.setVisible(true);
        } else {
            photoView.setImage(null);
            photoView.setVisible(false);
        }
    }

    // ---------------------------------------------------------------- sightings

    /** Runs the occurrence search away from the FX thread. */
    private void loadOccurrences(SpeciesSummary selection) {
        if (activeOccurrences != null) {
            activeOccurrences.cancel();
        }
        String scientificName = selection.getScientificName();
        if (scientificName == null) {
            alaRecordsLabel.setText("No occurrence data available.");
            return;
        }

        Task<List<OccurrencePoint>> task = new Task<>() {
            @Override
            protected List<OccurrencePoint> call() {
                return speciesService.occurrencesNear(
                        scientificName, BRISBANE_LAT, BRISBANE_LON, SEARCH_RADIUS_KM, 300);
            }
        };
        activeOccurrences = task;
        task.setOnSucceeded(event -> {
            if (activeOccurrences == task) {
                renderSightings(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (activeOccurrences == task) {
                showSightingsUnavailable();
            }
        });

        Thread loader = new Thread(task, "species-occurrence-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void renderSightings(List<OccurrencePoint> points) {
        sightingsMap.getChildren().clear();
        recentSightingsBox.getChildren().clear();

        if (points == null || points.isEmpty()) {
            alaRecordsLabel.setText("No Atlas records within "
                    + (int) SEARCH_RADIUS_KM + " km of Brisbane.");
            recentSightingsBox.getChildren().add(
                    placeholderRow("No dated Atlas records for this species nearby."));
            return;
        }

        int plotted = 0;
        for (OccurrencePoint point : points) {
            Optional<ProjectedPoint> projected =
                    BrisbaneMapProjection.project(point.getLatitude(), point.getLongitude());
            if (projected.isEmpty()) {
                continue;
            }
            if (plotted < MAX_DOTS) {
                sightingsMap.getChildren().add(dotFor(projected.get()));
            }
            plotted++;
        }

        if (plotted == 0) {
            alaRecordsLabel.setText("No Atlas records within "
                    + (int) SEARCH_RADIUS_KM + " km of Brisbane.");
        } else {
            alaRecordsLabel.setText(plotted + " Atlas " + plural(plotted, "record", "records")
                    + " within " + (int) SEARCH_RADIUS_KM + " km of Brisbane.");
        }

        List<OccurrencePoint> recent = points.stream()
                .filter(point -> point.getEventDate() != null)
                .sorted(Comparator.comparing(OccurrencePoint::getEventDate).reversed())
                .limit(3)
                .toList();
        if (recent.isEmpty()) {
            recentSightingsBox.getChildren().add(
                    placeholderRow("None of the nearby records carry a date."));
            return;
        }
        for (int i = 0; i < recent.size(); i++) {
            if (i > 0) {
                recentSightingsBox.getChildren().add(divider());
            }
            recentSightingsBox.getChildren().add(recentRow(recent.get(i)));
        }
    }

    private void showSightingsUnavailable() {
        sightingsMap.getChildren().clear();
        recentSightingsBox.getChildren().clear();
        alaRecordsLabel.setText("Atlas records could not be loaded.");
        recentSightingsBox.getChildren().add(
                placeholderRow("Atlas records are unavailable right now."));
    }

    /** A small dot placed proportionally, so it follows the pane as it resizes. */
    private Circle dotFor(ProjectedPoint point) {
        Circle dot = new Circle(4);
        dot.getStyleClass().addAll("heat-marker-circle", "blob-mid");
        dot.centerXProperty().bind(sightingsMap.widthProperty().multiply(point.xRatio()));
        dot.centerYProperty().bind(sightingsMap.heightProperty().multiply(point.yRatio()));
        return dot;
    }

    private VBox recentRow(OccurrencePoint point) {
        VBox row = new VBox(2);
        Label dataset = new Label(point.getDataResourceName() != null
                ? point.getDataResourceName()
                : "Atlas record");
        dataset.getStyleClass().add("section-title");
        Label date = new Label(DISPLAY_DATE.format(point.getEventDate()));
        date.getStyleClass().add("muted");
        row.getChildren().addAll(dataset, date);
        return row;
    }

    private Label placeholderRow(String text) {
        Label placeholder = new Label(text);
        placeholder.getStyleClass().add("muted");
        placeholder.setWrapText(true);
        return placeholder;
    }

    private Region divider() {
        Region divider = new Region();
        divider.getStyleClass().add("divider");
        return divider;
    }

    // ---------------------------------------------------------------- helpers

    private static String displayName(SpeciesSummary species) {
        if (species == null) {
            return "Unknown species";
        }
        if (species.getCommonName() != null && !species.getCommonName().isBlank()) {
            return species.getCommonName();
        }
        return species.getScientificName() != null ? species.getScientificName() : "Unknown species";
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String orBlank(String value) {
        return value != null ? value : "";
    }

    private static String plural(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
}
