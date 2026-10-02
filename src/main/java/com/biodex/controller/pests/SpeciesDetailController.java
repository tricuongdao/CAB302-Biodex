package com.biodex.controller.pests;

import com.biodex.api.CuratedSpeciesService;
import com.biodex.api.ServiceFactory;
import com.biodex.api.SpeciesImageResolver;
import com.biodex.api.SpeciesService;
import com.biodex.api.dto.SpeciesProfile;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.controller.BaseController;
import com.biodex.model.Species;
import com.biodex.routing.Route;
import com.biodex.session.IdentifyDraft;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Left pane of Pest Detail: species photo, threat profile, facts, disposal guidance.
 *
 * <p>The parent {@link PestDetailController} calls {@link #display(Species)} for species with a
 * local record, and {@link #displayFromApi(SpeciesSummary)} for species the Atlas of Living
 * Australia knows but the local database does not. That path fetches the profile itself and
 * renders whatever the Atlas - plus the bundled knowledge base, when it covers the species -
 * can supply.
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
    @FXML private Label taxonomyLabel;
    @FXML private Label conservationLabel;
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

    /** Guards the profile task, so a slow Atlas reply never overwrites a newer selection. */
    private int profileRequest;

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
        // Atlas-only facts stay hidden on the local path; clear anything a previous species left.
        setFactLabel(taxonomyLabel, "", null);
        setFactLabel(conservationLabel, "", null);

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
        setFactLabel(taxonomyLabel, "", null);
        setFactLabel(conservationLabel, "", null);
        actionBox.setVisible(false);
        actionBox.setManaged(false);
        hideActionMessage();
    }

    /**
     * Shows a species with no local record by fetching its details from the Atlas of Living
     * Australia. The names and photo from the search card stay on screen while the profile loads
     * in the background; whatever the Atlas - and, when it covers the species, the bundled
     * knowledge base - supplies is then rendered. Reporting files against a local record, so the
     * action buttons stay hidden on this path.
     */
    public void displayFromApi(SpeciesSummary selection) {
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
        boolean resolvable = guid != null && !guid.isBlank() && !guid.startsWith("fake:");
        if (resolvable) {
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

        threatBox.setVisible(false);
        factsBox.setVisible(false);
        disposalLabel.setVisible(false);
        setFactLabel(taxonomyLabel, "", null);
        setFactLabel(conservationLabel, "", null);

        if (!resolvable) {
            renderDescriptionText(null, "This species has no Atlas of Living Australia record, "
                    + "so there are no further details to load.");
            return;
        }

        descriptionBox.getChildren().clear();
        addMutedLabel(descriptionBox, "Loading details from the Atlas of Living Australia...");

        int request = ++profileRequest;
        Task<SpeciesProfile> task = new Task<>() {
            @Override
            protected SpeciesProfile call() {
                return speciesService.profile(guid);
            }
        };
        task.setOnSucceeded(event -> {
            if (request == profileRequest) {
                renderProfile(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (request == profileRequest) {
                renderProfile(null);
            }
        });

        Thread loader = new Thread(task, "species-profile-loader");
        loader.setDaemon(true);
        loader.start();
    }

    /** Renders Atlas profile details once the background fetch returns; null means it failed. */
    private void renderProfile(SpeciesProfile profile) {
        if (profile == null) {
            renderDescriptionText(null, "Details for this species could not be loaded from the "
                    + "Atlas of Living Australia. Check your connection and try again.");
            return;
        }

        if (profile.getCommonName() != null && !profile.getCommonName().isBlank()) {
            commonNameLabel.setText(profile.getCommonName());
        }
        if (profile.getScientificName() != null && !profile.getScientificName().isBlank()) {
            scientificNameLabel.setText(profile.getScientificName());
        }

        renderTags(tagsFor(profile));
        renderDescriptionText(profile.getDescription(),
                "The Atlas of Living Australia has no description for this species yet.");
        renderThreatRatings(profile.getThreatRatings());
        renderProfileFacts(profile);

        if (profile.getDisposalGuidance() != null && !profile.getDisposalGuidance().isBlank()) {
            disposalLabel.setText(profile.getDisposalGuidance());
            disposalLabel.setVisible(true);
        }
    }

    /** Facts for an Atlas species: habitat, size, taxonomy and conservation status, as available. */
    private void renderProfileFacts(SpeciesProfile profile) {
        setFactLabel(habitatLabel, "Typical habitat: ", profile.getTypicalHabitat());
        setFactLabel(sizeLabel, "Size: ", profile.getSizeRange());
        setFactLabel(taxonomyLabel, "", taxonomyLine(
                profile.getFamily(), profile.getOrder(), profile.getTaxonClass()));
        setFactLabel(conservationLabel, "Conservation status: ",
                profile.getConservationStatus());

        factsBox.setVisible(habitatLabel.isVisible() || sizeLabel.isVisible()
                || taxonomyLabel.isVisible() || conservationLabel.isVisible());
    }

    /** Threat bars from named 0-100 ratings; raw Atlas species have none and keep the box hidden. */
    private void renderThreatRatings(Map<String, Integer> ratings) {
        threatBars.getChildren().clear();
        if (ratings == null || ratings.isEmpty()) {
            threatBox.setVisible(false);
            return;
        }
        threatBox.setVisible(true);
        for (Map.Entry<String, Integer> rating : ratings.entrySet()) {
            addThreatBar(rating.getKey(), rating.getValue() == null ? 0 : rating.getValue());
        }
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
        renderDescriptionText(
                CuratedSpeciesService.bundledDescription(
                        species.getAlaGuid(), species.getScientificName(), species.getCommonName()),
                "No detailed write-up is available for this species yet.");
    }

    /** Renders a write-up, one Label per paragraph; blank text gets the given muted note. */
    private void renderDescriptionText(String text, String emptyMessage) {
        descriptionBox.getChildren().clear();
        if (text == null || text.isBlank()) {
            addMutedLabel(descriptionBox, emptyMessage);
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
        setFactLabel(habitatLabel, "Typical habitat: ", species.getTypicalHabitat());
        setFactLabel(sizeLabel, "Size: ", species.getSizeRange());
        factsBox.setVisible(habitatLabel.isVisible() || sizeLabel.isVisible());
    }

    /**
     * "Family: X · Order: Y · Class: Z" for the ranks the Atlas supplied, or null when it gave
     * none. Package-private so the test can pin the format.
     */
    static String taxonomyLine(String family, String order, String taxonClass) {
        StringBuilder line = new StringBuilder();
        appendRank(line, "Family", family);
        appendRank(line, "Order", order);
        appendRank(line, "Class", taxonClass);
        return line.length() == 0 ? null : line.toString();
    }

    private static void appendRank(StringBuilder line, String rank, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (line.length() > 0) {
            line.append(" \u00b7 ");
        }
        line.append(rank).append(": ").append(titleCaseIfShouting(value.trim()));
    }

    /**
     * The Atlas reports ranks in all caps ("REPTILIA"); on screen they read better as "Reptilia".
     * Values already in mixed case are left exactly as supplied.
     */
    private static String titleCaseIfShouting(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLowerCase(value.charAt(i))) {
                return value;
            }
        }
        StringBuilder titled = new StringBuilder(value.length());
        boolean atWordStart = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || c == '-') {
                titled.append(c);
                atWordStart = true;
            } else {
                titled.append(atWordStart ? Character.toUpperCase(c) : Character.toLowerCase(c));
                atWordStart = false;
            }
        }
        return titled.toString();
    }

    /**
     * The chips to show for an Atlas profile: its own tags, plus an "Invasive" chip when the
     * Atlas flags the species and no existing tag already says so. Package-private for the test.
     */
    static List<String> tagsFor(SpeciesProfile profile) {
        List<String> chips = new ArrayList<>();
        boolean flagged = false;
        for (String tag : profile.getTags()) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String trimmed = tag.trim();
            chips.add(trimmed);
            if (trimmed.toLowerCase(Locale.ROOT).contains("invasive")) {
                flagged = true;
            }
        }
        if (profile.isInvasive() && !flagged) {
            chips.add("Invasive");
        }
        return chips;
    }

    /** Sets a fact label's text and hides it cleanly (collapsed, not just invisible) when blank. */
    private static void setFactLabel(Label label, String prefix, String value) {
        if (value == null || value.isBlank()) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
            return;
        }
        label.setText(prefix + value.trim());
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void addMutedLabel(VBox box, String message) {
        Label label = new Label(message);
        label.getStyleClass().add("muted");
        label.setWrapText(true);
        box.getChildren().add(label);
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