package com.biodex.controller.pests;

import com.biodex.api.ServiceFactory;
import com.biodex.api.SpeciesImageResolver;
import com.biodex.api.SpeciesService;
import com.biodex.controller.BaseController;
import com.biodex.model.Species;
import com.biodex.routing.Route;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Left pane of Pest Detail: species photo, threat profile, facts, disposal guidance.
 * Receives a fully-loaded {@link Species} from the parent {@link PestDetailController}.
 */
public class SpeciesDetailController extends BaseController {

    @FXML private Region photoPlaceholder;
    @FXML private ImageView photoView;
    @FXML private Label commonNameLabel;
    @FXML private Label scientificNameLabel;
    @FXML private Label threatBadge;
    @FXML private HBox tagsBox;
    @FXML private VBox descriptionBox;
    @FXML private VBox threatBox;
    @FXML private VBox threatBars;
    @FXML private VBox factsBox;
    @FXML private Label habitatLabel;
    @FXML private Label sizeLabel;
    @FXML private Label disposalLabel;
    @FXML private Button logSightingBtn;
    @FXML private Button addPhotoBtn;

    private Species species;
    private final SpeciesService speciesService = ServiceFactory.speciesService();
    private final SpeciesImageResolver imageResolver = new SpeciesImageResolver(speciesService);
    private int photoRequest;

    @FXML
    private void initialize() {
        // UI setup happens in display()
    }

    /** Populates all fields from the local Species record. */
    public void display(Species species) {
        if (species == null) {
            showEmptyState();
            return;
        }
        this.species = species;

        commonNameLabel.setText(species.getCommonName());
        scientificNameLabel.setText(species.getScientificName());

        // Threat badge
        threatBadge.setVisible(true);
        threatBadge.getStyleClass().removeAll("threat-high", "threat-medium", "threat-low");
        threatBadge.setText(species.getThreatLevel().name() + " threat");
        threatBadge.getStyleClass().add("threat-" + species.getThreatLevel().name().toLowerCase());

        // Tags
        renderTags(species.getTags());

        // Photo: local path first, otherwise resolve the ALA image in the background.
        loadPhoto(species);

        // Description
        renderDescription(species);

        // Threat profile bars
        renderThreatBars(species);

        // Facts: habitat + size
        renderFacts(species);

        // Disposal guidance
        if (species.getDisposalGuidance() != null && !species.getDisposalGuidance().isBlank()) {
            disposalLabel.setText(species.getDisposalGuidance());
            disposalLabel.setVisible(true);
        } else {
            disposalLabel.setText("");
            disposalLabel.setVisible(false);
        }
    }

    private void showEmptyState() {
        photoRequest++;
        commonNameLabel.setText("Select a species");
        scientificNameLabel.setText("");
        threatBadge.setVisible(false);
        tagsBox.getChildren().clear();
        tagsBox.setVisible(false);
        photoView.setVisible(false);
        descriptionBox.getChildren().clear();
        Label placeholder = new Label("Choose a species from the search to see details.");
        placeholder.getStyleClass().add("muted");
        placeholder.setWrapText(true);
        descriptionBox.getChildren().add(placeholder);
        threatBox.setVisible(false);
        factsBox.setVisible(false);
        disposalLabel.setVisible(false);
    }

    private void renderTags(List<String> tags) {
        tagsBox.getChildren().clear();
        if (tags == null || tags.isEmpty()) {
            tagsBox.setVisible(false);
            return;
        }
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) continue;
            Label chip = new Label(tag.trim());
            chip.getStyleClass().add("tag-chip");
            tagsBox.getChildren().add(chip);
        }
        tagsBox.setVisible(true);
    }

    private void renderDescription(Species species) {
        descriptionBox.getChildren().clear();
        // For local species, we could have a description field; for now show a placeholder
        // The full description comes from the curated content merged by CuratedSpeciesService
        // but we don't have a description field on the local Species model yet.
        Label placeholder = new Label("Detailed description loaded from curated knowledge base.");
        placeholder.getStyleClass().add("muted");
        placeholder.setWrapText(true);
        descriptionBox.getChildren().add(placeholder);
    }

    private void renderThreatBars(Species species) {
        threatBars.getChildren().clear();
        threatBox.setVisible(true);

        addThreatBar("Aggression", species.getAggression());
        addThreatBar("Sting severity", species.getStingSeverity());
        addThreatBar("Spread risk", species.getSpreadRisk());
    }

    private void addThreatBar(String name, int score) {
        int clamped = Math.max(0, Math.min(100, score));
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label metric = new Label(name);
        metric.getStyleClass().add("metric-name");
        ProgressBar bar = new ProgressBar();
        bar.getStyleClass().addAll("threat-bar", severityClass(clamped));
        bar.setProgress(clamped / 100.0);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label percent = new Label(clamped + "%");
        percent.getStyleClass().add("muted");
        percent.setMinWidth(38);
        row.getChildren().addAll(metric, bar, percent);
        threatBars.getChildren().add(row);
    }

    private String severityClass(int score) {
        if (score >= 70) return "threat-bar-high";
        if (score >= 35) return "threat-bar-mid";
        return "threat-bar-low";
    }

    private void renderFacts(Species species) {
        if (species.getTypicalHabitat() == null && species.getSizeRange() == null) {
            factsBox.setVisible(false);
            return;
        }
        factsBox.setVisible(true);

        if (species.getTypicalHabitat() != null && !species.getTypicalHabitat().isBlank()) {
            habitatLabel.setText("Typical habitat: " + species.getTypicalHabitat());
        } else {
            habitatLabel.setText("");
        }

        if (species.getSizeRange() != null && !species.getSizeRange().isBlank()) {
            sizeLabel.setText("Size: " + species.getSizeRange());
        } else {
            sizeLabel.setText("");
        }
    }

    /**
     * Shows the local photo immediately when one exists; otherwise resolves the ALA image via
     * the shared resolver — image-only fetch, in-memory dedup, disk cache underneath.
     */
    private void loadPhoto(Species species) {
        int request = ++photoRequest;

        String local = species.getPhotoPath();
        if (local != null && !local.isBlank()) {
            showPhoto(local, request);
            return;
        }

        photoView.setImage(null);
        photoView.setVisible(true);
        photoView.setManaged(true);
        photoPlaceholder.setVisible(true);

        String guid = species.getAlaGuid();
        String scientificName = species.getScientificName();
        if ((guid == null || guid.isBlank() || guid.startsWith("fake:"))
                && (scientificName == null || scientificName.isBlank())) {
            return;
        }

        if (guid != null && !guid.isBlank() && !guid.startsWith("fake:")) {
            imageResolver.imageForGuid(guid).whenComplete((url, error) ->
                    onPhotoResolved(species, request, url));
        } else {
            imageResolver.imageForScientificName(scientificName).whenComplete((url, error) ->
                    onPhotoResolved(species, request, url));
        }
    }

    /** Caches the resolved URL on the record and paints it unless a newer request won. */
    private void onPhotoResolved(Species species, int request, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        species.setPhotoPath(url);
        Platform.runLater(() -> {
            if (request == photoRequest) {
                showPhoto(url, request);
            }
        });
    }

    /** Paints the image, falling back to the placeholder when the URL cannot load. */
    private void showPhoto(String url, int request) {
        Image image = new Image(url, 220, 220, true, true, true);
        image.errorProperty().addListener((observable, wasError, isError) -> {
            if (isError && request == photoRequest) {
                photoView.setImage(null);
                photoPlaceholder.setVisible(true);
            }
        });
        image.progressProperty().addListener((observable, oldProgress, progress) -> {
            if (request == photoRequest && (image.getWidth() > 0 || image.getHeight() > 0)) {
                photoPlaceholder.setVisible(false);
            }
        });
        photoView.setImage(image);
        photoView.setVisible(true);
        photoView.setManaged(true);
    }

    @FXML
    private void handleLogSighting() {
        if (species != null) {
            router.go(Route.IDENTIFY_PEST); // TODO: pass speciesId to Log Sighting wizard
        }
    }

    @FXML
    private void handleAddPhoto() {
        // TODO: open photo capture/upload dialog for this species
    }
}