package com.biodex.controller.identify;

import com.biodex.api.ServiceFactory;
import com.biodex.api.dto.SpeciesSummary;
import com.biodex.controller.BaseController;
import com.biodex.controller.common.SidebarController;
import com.biodex.dao.SightingDAO;
import com.biodex.dao.SightingReportDAO;
import com.biodex.dao.SpeciesDAO;
import com.biodex.dao.SuburbDAO;
import com.biodex.model.SightingReport;
import com.biodex.model.Species;
import com.biodex.model.Suburb;
import com.biodex.model.User;
import com.biodex.recognition.MatchCandidate;
import com.biodex.recognition.RecognitionService;
import com.biodex.routing.Route;
import com.biodex.session.IdentifyDraft;
import com.biodex.session.SpeciesSelection;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
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
 * Identify a pest screen: photo upload, classifier matches and the sighting form.
 *
 * <p>Picking or dropping a photo runs it through {@link RecognitionService} on a background task
 * and rebuilds the Possible matches card with ranked candidates. Submit report files the chosen
 * species and the Where &amp; when details into the local sighting reports table, so the report
 * shows up immediately under Local sightings on Species Details.
 *
 * <p>The page can also be opened from Species Details with an {@link IdentifyDraft} parked
 * ("Log sighting" or "Add photo"): the species is pinned as the report's subject, and a photo
 * picked on that page is preloaded and classified as soon as the screen opens.
 */
public class IdentifyPestController extends BaseController {

    /** Matches the identification_confidence_threshold default in the settings schema. */
    private static final double CONFIDENCE_THRESHOLD = 0.70;

    /** Per the screen's own copy: 10 MB per photo. */
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private static final int MAX_PHOTO = 5;

    /** Shape the WHEN field accepts, e.g. "27 Aug 2026, 16:40". */
    static final DateTimeFormatter WHEN_FORMAT =
            DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.ENGLISH);

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
    @FXML
    private ListView<Path> photoListView;
    @FXML
    private Label contextLabel;
    @FXML
    private TextField whereField;
    @FXML
    private ComboBox<Suburb> suburbCombo;
    @FXML
    private TextField whenField;
    @FXML
    private TextField countField;
    @FXML
    private TextArea notesField;
    @FXML
    private Label reportMessage;
    @FXML
    private Hyperlink viewSightingsLink;
    @FXML
    private Button submitButton;

    private final RecognitionService recognitionService = ServiceFactory.recognitionService();
    private final SpeciesDAO speciesDAO = new SpeciesDAO();
    private final SightingReportDAO reportDAO = new SightingReportDAO();
    private final SuburbDAO suburbDAO = new SuburbDAO();
    private final SightingDAO sightingDAO = new SightingDAO();

    private final List<Path> selectedPhotos = new ArrayList<>();
    private MatchCandidate selectedMatch;

    /** Species pinned by a Species Details hand-off, used when no match has been picked. */
    private SpeciesSummary contextSpecies;

    /** Suburb picker options, loaded off the FX thread on arrival; empty until the load finishes. */
    private List<Suburb> suburbOptions = List.of();

    /** The species of the last filed report, for the "view local sightings" link. */
    private SpeciesSummary reportedSpecies;

    private boolean submitting;
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
                handlePickedFile(files);
            }
            event.setDropCompleted(!files.isEmpty());
            event.consume();
        });
        applyDraft(IdentifyDraft.getInstance().take());
        loadSuburbs();
    }

    /** Loads the suburb picker options off the FX thread; empty until the load finishes. */
    private void loadSuburbs() {
        Task<List<Suburb>> task = new Task<>() {
            @Override
            protected List<Suburb> call() {
                return suburbDAO.findAll();
            }
        };
        task.setOnSucceeded(event -> {
            List<Suburb> loaded = task.getValue();
            suburbOptions = loaded == null ? List.of() : loaded;
            suburbCombo.getItems().setAll(suburbOptions);
        });
        Thread thread = new Thread(task, "suburb-loader");
        thread.setDaemon(true);
        thread.start();
    }

    /** Applies a hand-off from Species Details: pins the species and preloads any photo. */
    private void applyDraft(IdentifyDraft.Item draft) {
        if (draft == null) {
            return;
        }
        if (draft.species() != null) {
            contextSpecies = draft.species();
            contextLabel.setText("Logging a sighting for " + contextSpecies.displayName());
            contextLabel.setVisible(true);
            contextLabel.setManaged(true);
        }
        if (draft.photo() != null) {
            handlePickedFile(List.of(draft.photo().toFile()));
        }
    }

    @FXML
    private void onBrowseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a pest photo (up to five)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Photos", "*.jpg", "*.jpeg", "*.png"));
        Window window = dropZone.getScene() == null ? null : dropZone.getScene().getWindow();
        List<File> files = chooser.showOpenMultipleDialog(window);
        if (files != null && !files.isEmpty()) {
            handlePickedFile(files);
        }
    }
    public boolean uploadValidation(File file) {
        if (file.length() > MAX_PHOTO_BYTES) {
            showMessage("File exceed size limit. Max size is 10MB.");
            return false;
        }
        if (selectedPhotos.contains(file.toPath())) {
            showMessage("Already Selected: " + file.getName());
            return false;
        }
        if (selectedPhotos.size() >= MAX_PHOTO) {
            showMessage("Max of 5 is already selected.");
            return false;
        }
        selectedPhotos.add(file.toPath());
        return true;
    }


    /** Validates the picked files, then previews and classifies the last accepted one. */
    private void handlePickedFile(List<File> files) {
        Path lastAccepted = null;
        for (File file : files) {
            if (uploadValidation(file)) {
                lastAccepted = file.toPath();
            }
        }
        if (lastAccepted == null) {
            return;
        }
        selectedMatch = null;
        clearReportOutcome();
        showPreview(lastAccepted);
        updatePhotoList();
        runRecognition(lastAccepted);
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

    private void updatePhotoList() {
        photoListView.getItems().clear();
        photoListView.getItems().addAll(selectedPhotos);
        photoListView.setCellFactory(param -> new PhotoListCell());
    }

    private class PhotoListCell extends ListCell<Path> {
        @Override
        protected void updateItem(Path photo, boolean empty) {
            super.updateItem(photo, empty);
            if (empty || photo == null) {
                setGraphic(null);
                return;
            }
            HBox row = new HBox(10);
            row.setStyle("-fx-padding: 4; -fx-border-radius: 2");
            Label filename = new Label(photo.getFileName().toString());
            HBox.setHgrow(filename, Priority.ALWAYS);

            Button removeFile = new Button("Remove");
            removeFile.setStyle("-fx-font-size: 6;");
            removeFile.setOnAction(event -> {
                selectedPhotos.remove(photo);
                updatePhotoList();
            });

            row.getChildren().addAll(filename, removeFile);
            setGraphic(row);
        }

    }

    @FXML
    private void onBackToHeatMap() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onSubmitReport() {
        if (submitting) {
            return;
        }
        SpeciesSummary target = targetSpecies();
        if (target == null) {
            showReportMessage("Choose one of the possible matches, or add a photo so the app can identify a species.");
            return;
        }
        String where = text(whereField);
        if (where.isEmpty()) {
            showReportMessage("Add where you found it - a suburb or a nearby landmark.");
            return;
        }
        String whenText = text(whenField);
        Instant when = null;
        if (!whenText.isEmpty()) {
            when = parseWhen(whenText);
            if (when == null) {
                showReportMessage("Couldn't read the date and time - try a format like \"27 Aug 2026, 16:40\".");
                return;
            }
        }
        Suburb suburb = resolveSuburb(where);
        if (suburb == null && !suburbOptions.isEmpty()) {
            showReportMessage("Pick the suburb from the list, or name it in Where, so the sighting can be placed on the heat map.");
            return;
        }
        User user = currentUser();
        if (user == null) {
            showReportMessage("Sign in first so the sighting can be attributed to your account.");
            return;
        }

        SightingReport report = new SightingReport();
        report.setSuburb(suburb != null ? suburb.getName() : inferSuburb(where));
        report.setLocationLabel(locationLabel(where, text(countField), text(notesField)));
        report.setReporterUserId(user.getUserId());
        report.setPhotoPath(selectedPhotos.isEmpty() ? null : selectedPhotos.get(0).toString());
        report.setReportedAt(when);

        beginSubmitting();
        Task<Optional<FiledReport>> task = new Task<>() {
            @Override
            protected Optional<FiledReport> call() {
                Species species = resolveSpecies(target);
                if (species == null) {
                    return Optional.empty();
                }
                report.setSpeciesId(species.getSpeciesId());
                reportDAO.insertReport(report);
                boolean placedOnMap = false;
                if (suburb != null) {
                    sightingDAO.insertSighting(user.getUserId(), suburb.getSuburbId(),
                            species.getCommonName(), report.getLocationLabel(),
                            report.getPhotoPath(), report.getReportedAt());
                    placedOnMap = true;
                }
                return Optional.of(new FiledReport(species, placedOnMap));
            }
        };
        task.setOnSucceeded(event -> {
            endSubmitting();
            Optional<FiledReport> filed = task.getValue();
            if (filed.isEmpty()) {
                showReportMessage("That species isn't in the local database yet, so the report can't be filed. Try another match.");
                return;
            }
            onReportFiled(filed.get());
        });
        task.setOnFailed(event -> {
            endSubmitting();
            showReportMessage("Couldn't save the report. Please try again.");
        });
        Thread thread = new Thread(task, "sighting-report-save");
        thread.setDaemon(true);
        thread.start();
    }

    /** The species the report is about: an explicitly picked match beats the pinned species. */
    private SpeciesSummary targetSpecies() {
        if (selectedMatch != null) {
            return new SpeciesSummary(
                    null, selectedMatch.getScientificName(), selectedMatch.getCommonName(), null);
        }
        return contextSpecies;
    }

    /** Finds the local record for the report's species, by scientific name then common name. */
    private Species resolveSpecies(SpeciesSummary target) {
        if (target.getScientificName() != null && !target.getScientificName().isBlank()) {
            Optional<Species> byScientific = speciesDAO.findByScientificName(target.getScientificName());
            if (byScientific.isPresent()) {
                return byScientific.get();
            }
        }
        if (target.getCommonName() != null && !target.getCommonName().isBlank()) {
            Optional<Species> byCommon = speciesDAO.findByCommonName(target.getCommonName());
            if (byCommon.isPresent()) {
                return byCommon.get();
            }
        }
        return null;
    }

    /** The suburb for the report: an explicit pick wins, then a match against the typed text. */
    private Suburb resolveSuburb(String where) {
        Suburb chosen = suburbCombo.getValue();
        if (chosen != null) {
            return chosen;
        }
        return findSuburbByText(where, suburbOptions);
    }

    /**
     * Matches free text against the known suburbs: an exact match on the last comma segment or
     * the whole text first, then the longest suburb name contained in the text.
     */
    static Suburb findSuburbByText(String where, List<Suburb> suburbs) {
        if (where == null || where.isBlank() || suburbs == null || suburbs.isEmpty()) {
            return null;
        }
        String text = where.trim().toLowerCase(Locale.ROOT);
        String tail = text;
        int comma = text.lastIndexOf(',');
        if (comma >= 0) {
            tail = text.substring(comma + 1).trim();
        }
        for (Suburb suburb : suburbs) {
            String name = suburb.getName().toLowerCase(Locale.ROOT);
            if (name.equals(tail) || name.equals(text)) {
                return suburb;
            }
        }
        Suburb best = null;
        for (Suburb suburb : suburbs) {
            String name = suburb.getName().toLowerCase(Locale.ROOT);
            if (text.contains(name) && (best == null || name.length() > best.getName().length())) {
                best = suburb;
            }
        }
        return best;
    }

    /** Outcome of a filed report: the species it was about, and whether it reached the heat map. */
    private record FiledReport(Species species, boolean placedOnMap) {
    }

    private void onReportFiled(FiledReport filed) {
        Species species = filed.species();
        reportedSpecies = new SpeciesSummary(
                species.getAlaGuid(), species.getScientificName(),
                species.getCommonName(), species.getPhotoPath());
        showReportMessage("Report filed. It now shows under Local sightings for "
                + species.getCommonName()
                + (filed.placedOnMap() ? " and on the heat map." : "."));
        viewSightingsLink.setVisible(true);
        viewSightingsLink.setManaged(true);
    }

    @FXML
    private void onViewSightings() {
        if (reportedSpecies == null) {
            return;
        }
        SpeciesSelection.getInstance().setCurrent(reportedSpecies);
        router.go(Route.PEST_DETAIL);
    }

    private void beginSubmitting() {
        submitting = true;
        submitButton.setDisable(true);
        submitButton.setText("Filing report...");
    }

    private void endSubmitting() {
        submitting = false;
        submitButton.setDisable(false);
        submitButton.setText("Submit report");
    }

    private void clearReportOutcome() {
        reportedSpecies = null;
        viewSightingsLink.setVisible(false);
        viewSightingsLink.setManaged(false);
        reportMessage.setVisible(false);
        reportMessage.setManaged(false);
    }

    private void showReportMessage(String message) {
        reportMessage.setText(message);
        reportMessage.setVisible(true);
        reportMessage.setManaged(true);
    }

    /** "Kedron Brook, Gordon Park" reports under "Gordon Park"; plain text is kept as-is. */
    static String inferSuburb(String where) {
        String text = where == null ? "" : where.trim();
        int comma = text.lastIndexOf(',');
        String suburb = (comma >= 0 ? text.substring(comma + 1) : text).trim();
        if (suburb.isEmpty()) {
            suburb = text;
        }
        return suburb.length() > 100 ? suburb.substring(0, 100) : suburb;
    }

    /** The full location line: where, plus the count and free-text notes when given. */
    static String locationLabel(String where, String count, String notes) {
        StringBuilder label = new StringBuilder(where == null ? "" : where.trim());
        if (count != null && !count.isEmpty()) {
            label.append(" (").append(count).append(")");
        }
        if (notes != null && !notes.isEmpty()) {
            label.append(" - ").append(notes);
        }
        String text = label.toString();
        return text.length() > 255 ? text.substring(0, 255) : text;
    }

    /**
     * Parses the WHEN field as local date and time. Returns null when blank or unreadable; the
     * caller only passes non-blank text, so null means the user needs to fix the format.
     */
    static Instant parseWhen(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), WHEN_FORMAT)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String text(TextInputControl field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
