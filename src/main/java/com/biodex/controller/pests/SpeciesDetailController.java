package com.biodex.controller.pests;

import com.biodex.api.CuratedSpeciesService;
import com.biodex.api.ServiceFactory;
import com.biodex.api.SpeciesImageResolver;
import com.biodex.api.SpeciesService;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.controller.BaseController;
import com.biodex.model.Species;
import com.biodex.routing.Route;
import com.biodex.session.IdentifyDraft;

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
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
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
    @FXML private HBox actionBox;
    @FXML private Label actionMessageLabel;

    /** Keep in step with the Identify screen's own photo limit. */
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;

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
        hideActionMessage();
        actionBox.setVisible(true);
        actionBox.setManaged(true);

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
        actionBox.setVisible(false);
        actionBox.setManaged(false);
        hideActionMessage();
    }

    /**
     * Empty state for a search result with no local record: shows the photo and names from the
     * search card, explains the gap, and hides everything that needs a curated record.
     */
    public void displayUnavailable(SpeciesSummary selection) {
        species = null;
        hideActionMessage();
        actionBox.setVisible(false);
        actionBox.setManaged(false);

        commonNameLabel.setText(selection.displayName());
        scientificNameLabel.setText(
                selection.getScientificName() == null ? "" : selection.getScientificName());
        threatBadge.setVisible(false);
        tagsBox.getChildren().clear();
        tagsBox.setVisible(false);

        photoView.setImage(null);
        photoView.setVisible(false);
        photoPlaceholder.setVisible(true);
        String guid = selection.getGuid();
        if (guid != null && !guid.isBlank() && !guid.startsWith("fake:")) {
            int request = ++photoRequest;
            photoView.setVisible(true);
            imageResolver.imageForGuid(guid).whenComplete((url, error) -> {
                if (url != null && !url.isBlank()) {
                    Platform.runLater(() -> {
                        if (request == photoRequest) {
                            showPhoto(url, request);
                        }
                    });
                }
            });
        }

        descriptionBox.getChildren().clear();
        Label message = new Label("This species doesn't have a local profile yet. It isn't in "
                + "the Biodex knowledge base, so there's no threat profile, habitat or disposal "
                + "guidance to show.");
        message.getStyleClass().add("muted");
        message.setWrapText(true);
        descriptionBox.getChildren().add(message);

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

    /** Renders the bundled knowledge base write-up, one Label per paragraph. */
    private void renderDescription(Species species) {
        descriptionBox.getChildren().clear();
        String text = CuratedSpeciesService.bundledDescription(
                species.getAlaGuid(), species.getScientificName(), species.getCommonName());
        if (text == null || text.isBlank()) {
            Label fallback = new Label("No detailed write-up is available for this species yet.");
            fallback.getStyleClass().add("muted");
            fallback.setWrapText(true);
            descriptionBox.getChildren().add(fallback);
            return;
        }
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Label label = new Label(trimmed);
            label.setWrapText(true);
            descriptionBox.getChildren().add(label);
        }
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

    /** Hands the species to the Identify screen, where the report form is pinned to it. */
    @FXML
    private void handleLogSighting() {
        if (species == null) {
            return;
        }
        IdentifyDraft.getInstance().set(asSummary(species), null);
        router.go(Route.IDENTIFY_PEST);
    }

    /** Picks a photo for this species, then opens Identify with it preloaded and classified. */
    @FXML
    private void handleAddPhoto() {
        if (species == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a photo of " + species.getCommonName());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Photos", "*.jpg", "*.jpeg", "*.png"));
        Window window = addPhotoBtn.getScene() == null ? null : addPhotoBtn.getScene().getWindow();
        File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        if (file.length() > MAX_PHOTO_BYTES) {
            showActionMessage("That photo is larger than 10 MB - choose a smaller one.");
            return;
        }
        IdentifyDraft.getInstance().set(asSummary(species), file.toPath());
        router.go(Route.IDENTIFY_PEST);
    }

    private static SpeciesSummary asSummary(Species species) {
        return new SpeciesSummary(
                species.getAlaGuid(), species.getScientificName(),
                species.getCommonName(), species.getPhotoPath());
    }

    private void showActionMessage(String message) {
        actionMessageLabel.setText(message);
        actionMessageLabel.setVisible(true);
        actionMessageLabel.setManaged(true);
    }

    private void hideActionMessage() {
        actionMessageLabel.setVisible(false);
        actionMessageLabel.setManaged(false);
    }
}