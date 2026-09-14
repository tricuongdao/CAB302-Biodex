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

    @FXML
    public void initialize() {

    }

    //
    @FXML
    private ListView<String> uploadList;
    private void updateUploadList() {
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
                uploadValidation(file);
            }

        }

    }

    public boolean uploadValidation(File file) {
        if (file.length() > MAX_FILE_SIZE) {
            showError("File exceed size", "Max size of 10MB.");
            return false;
        }
        if (selectedImages.contains(file)) {
            showError("Already Selected", file.getName() + "already added");
            return false;
        }
        if (selectedImages.size() >= MAX_FILES) {
            showError("Max  file selected.", " Max of 5 allowed");
            return false;
        }
        selectedImages.add(file);
        updateUploadList();
        return true;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

}
