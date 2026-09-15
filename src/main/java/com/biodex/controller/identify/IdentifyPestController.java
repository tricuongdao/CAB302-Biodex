package com.biodex.controller.identify;

import com.biodex.api.ServiceFactory;
import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.recognition.MatchCandidate;
import com.biodex.recognition.RecognitionService;
import com.biodex.routing.Route;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Identify a pest screen: photo upload, classifier matches and a sighting form.
 *
 * <p>Picking or dropping a photo runs it through {@link RecognitionService} on a background task
 * and rebuilds the Possible matches card with ranked candidates. Selecting a match stores it for
 * the sighting the Submit report button creates.
 */
public class IdentifyPestController extends BaseController {

    /** Matches the identification_confidence_threshold default in the settings schema. */
    private static final double CONFIDENCE_THRESHOLD = 0.70;

    /** Per the screen's own copy: 10 MB per photo. */
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;

    /** Injected from the fx:include with fx:id="sidebar" in IdentifyPestView.fxml. */
    @FXML
    private SidebarController sidebarController;
    @FXML
    private VBox dropZone;
    @FXML
    private ImageView photoPreview;
    @FXML
    private Label promptIcon;
    @FXML
    private Label promptTitle;
    @FXML
    private Label messageLabel;
    @FXML
    private VBox matchesBox;
    @FXML
    private Label matchesPlaceholder;

    private final RecognitionService recognitionService = ServiceFactory.recognitionService();

    private Path selectedPhoto;
    private MatchCandidate selectedMatch;
    private final Map<MatchCandidate, Button> selectButtons = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        sidebarController.setActive("identify");
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        dropZone.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            if (!files.isEmpty()) {
                handlePickedFile(files.get(0));
            }
            event.setDropCompleted(!files.isEmpty());
            event.consume();
        });
    }

    @FXML
    private void onBrowseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a pest photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Photos", "*.jpg", "*.jpeg", "*.png"));
        Window window = dropZone.getScene() == null ? null : dropZone.getScene().getWindow();
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            handlePickedFile(file);
        }
    }

    /** Validates the picked file, shows its preview and starts classification. */
    private void handlePickedFile(File file) {
        if (file.length() > MAX_PHOTO_BYTES) {
            showMessage("That photo is over 10 MB - please choose a smaller one.");
            return;
        }
        selectedPhoto = file.toPath();
        selectedMatch = null;
        showPreview(selectedPhoto);
        showMessage(null);
        runRecognition(selectedPhoto);
    }

    private void showPreview(Path photo) {
        photoPreview.setImage(new Image(photo.toUri().toString(), 260, 180, true, true));
        photoPreview.setVisible(true);
        photoPreview.setManaged(true);
        promptIcon.setVisible(false);
        promptIcon.setManaged(false);
        promptTitle.setVisible(false);
        promptTitle.setManaged(false);
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(message != null);
        messageLabel.setManaged(message != null);
    }

    private void runRecognition(Path photo) {
        Task<List<MatchCandidate>> task = new Task<>() {
            @Override
            protected List<MatchCandidate> call() {
                return recognitionService.identify(photo, 3);
            }
        };
        task.setOnSucceeded(event -> showMatches(task.getValue()));
        task.setOnFailed(event -> {
            showMatches(List.of());
            showMessage("Identification failed - try a different photo.");
        });
        Thread thread = new Thread(task, "pest-recognition");
        thread.setDaemon(true);
        thread.start();
    }

    /** Rebuilds the Possible matches card. Task callbacks run on the FX thread, as required. */
    private void showMatches(List<MatchCandidate> matches) {
        selectButtons.clear();
        matchesBox.getChildren().remove(1, matchesBox.getChildren().size());
        if (matches.isEmpty()) {
            matchesPlaceholder.setText("No match found - try a clearer, side-on photo.");
            matchesBox.getChildren().add(matchesPlaceholder);
            return;
        }

        boolean first = true;
        for (MatchCandidate match : matches) {
            if (!first) {
                Region divider = new Region();
                divider.getStyleClass().add("divider");
                matchesBox.getChildren().add(divider);
            }
            matchesBox.getChildren().add(rowFor(match));
            first = false;
        }

        if (matches.get(0).getConfidence() < CONFIDENCE_THRESHOLD) {
            Label lowConfidence = new Label("Low confidence - please confirm the species yourself.");
            lowConfidence.getStyleClass().add("muted");
            lowConfidence.setWrapText(true);
            matchesBox.getChildren().add(lowConfidence);
        }
    }

    private HBox rowFor(MatchCandidate match) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox names = new VBox(1);
        Label name = new Label(match.getCommonName());
        name.getStyleClass().add("section-title");
        Label scientific = new Label(match.getScientificName() == null ? "" : match.getScientificName());
        scientific.getStyleClass().addAll("muted", "sci-name");
        names.getChildren().addAll(name, scientific);
        HBox.setHgrow(names, Priority.ALWAYS);

        Label percent = new Label(Math.round(match.getConfidence() * 100) + "%");
        percent.getStyleClass().add("muted");

        Button select = new Button("Select");
        select.getStyleClass().add("chip");
        select.setMnemonicParsing(false);
        select.setOnAction(event -> selectMatch(match));
        selectButtons.put(match, select);

        row.getChildren().addAll(names, percent, select);
        return row;
    }

    private void selectMatch(MatchCandidate match) {
        selectedMatch = match;
        selectButtons.forEach((candidate, button) -> {
            boolean active = candidate == match;
            button.setText(active ? "Selected" : "Select");
            button.getStyleClass().remove("chip-selected");
            if (active) {
                button.getStyleClass().add("chip-selected");
            }
        });
    }

    @FXML
    private void onBackToHeatMap() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onSubmitReport() {
        // Sprint TODO: write the sighting through a SightingDAO using selectedMatch and
        // selectedPhoto, then return to the heat map so the new report appears immediately.
        router.go(Route.HEAT_MAP);
    }
}
