package com.biodex.controller.upload;

import com.biodex.controller.BaseController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;

import java.util.ArrayList;

public class UploadController extends BaseController {
    @FXML
    private VBox dropSection;
    @FXML
    private VBox uploadSection;
    @FXML
    private Label uploadLabel;
    @FXML
    private Button browseButton;
    @FXML
    private ToggleButton lightModeToggle;

    public final List<File> selectedImages = new ArrayList<>();
    private static final long  MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_FILES = 5;

    /** Why the most recent validation failed, so the caller can show it. */
    private String lastErrorTitle;
    private String lastErrorMessage;

    @FXML
    public void initialize() {

    }

    //
    @FXML
    private ListView<String> uploadList;
    private void updateUploadList() {
        if (uploadList == null) {
            // The list only exists once the view is loaded; validation still works without it.
            return;
        }
        uploadList.getItems().clear();
        for (File file : selectedImages) {
            uploadList.getItems().add(file.getName());
        }
    }

    @FXML
    private void handleButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select up to five files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.gif", "*.jpeg")
        );
        Stage stage = (Stage) browseButton.getScene().getWindow();

        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null) {
            for (File file : files) {
                if (!uploadValidation(file)) {
                    showError(lastErrorTitle, lastErrorMessage);
                }
            }
        }
    }

    /**
     * Validates one picked file, adding it to {@link #selectedImages} when it passes. Returns
     * false for a file that cannot be taken and records why in {@link #lastErrorTitle} /
     * {@link #lastErrorMessage}; the caller shows that message. Kept free of UI calls so the
     * rules stay unit testable.
     */
    public boolean uploadValidation(File file) {
        if (file.length() > MAX_FILE_SIZE) {
            lastErrorTitle = "File exceed size";
            lastErrorMessage = "Max size of 10MB.";
            return false;
        }
        if (selectedImages.contains(file)) {
            lastErrorTitle = "Already Selected";
            lastErrorMessage = file.getName() + "already added";
            return false;
        }
        if (selectedImages.size() >= MAX_FILES) {
            lastErrorTitle = "Max  file selected.";
            lastErrorMessage = " Max of 5 allowed";
            return false;
        }
        selectedImages.add(file);
        updateUploadList();
        return true;
    }

    /** Shows the validation message; only called from the button handler, where a UI exists. */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

}
