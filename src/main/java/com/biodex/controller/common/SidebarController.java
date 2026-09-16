package com.biodex.controller.common;

import java.util.List;

import com.biodex.controller.BaseController;
import com.biodex.routing.Route;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller for the shared navigation rail included by every main screen.
 * Screens call {@link #setActive(String)} to highlight their own nav entry.
 */
public class SidebarController extends BaseController {

    @FXML
    private Button heatMapLink;
    @FXML
    private Button identifyLink;
    @FXML
    private Button speciesSearchLink;
    @FXML
    private Button profileLink;
    @FXML
    private Label userInitials;
    @FXML
    private Label username;

    @FXML
    private void initialize() {
        if (currentUser() != null) {
            username.setText(currentUser().getUsername());
            userInitials.setText(initials(currentUser().getUsername()));
        }
    }

    @FXML
    private void onHeatMap() {
        router.go(Route.HEAT_MAP);
    }

    @FXML
    private void onIdentifyPest() {
        router.go(Route.IDENTIFY_PEST);
    }

    @FXML
    private void onSpeciesSearch() {
        router.go(Route.SPECIES_SEARCH);
    }

    @FXML
    private void onProfile() {
        router.go(Route.PROFILE);
    }

    /**
     * Highlights the nav entry for the screen currently showing the sidebar.
     *
     * @param key one of "heatmap", "identify", "pests" or "profile"
     */
    public void setActive(String key) {
        Button target = switch (key) {
            case "identify" -> identifyLink;
            case "pests" -> speciesSearchLink;
            case "profile" -> profileLink;
            default -> heatMapLink;
        };
        for (Button link : List.of(heatMapLink, identifyLink, speciesSearchLink, profileLink)) {
            link.getStyleClass().remove("sidebar-link-active");
        }
        target.getStyleClass().add("sidebar-link-active");
    }

    private static String initials(String value) {
        if (value == null || value.isBlank()) {
            return "?";
        }
        return value.substring(0, Math.min(2, value.length())).toUpperCase();
    }
}
